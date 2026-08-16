import { logger } from "firebase-functions";

import {
  CARDSIGHT_API_KEY,
  CARDSIGHT_API_URL,
  GOOGLE_CUSTOM_SEARCH_API_KEY,
  GOOGLE_CUSTOM_SEARCH_CX,
} from "./config.js";
import type { CardImage, ScanRequest } from "./validation.js";

type NullableString = string | null;

export type ExtractedCardFields = {
  identity: {
    title: NullableString;
    brand: NullableString;
    set: NullableString;
    year: NullableString;
    cardNumber: NullableString;
  };
  visibleCertification: {
    company: NullableString;
    serialNumber: NullableString;
    grade: NullableString;
  };
  conditionObservations: string[];
  fieldSources: Record<string, "gemini" | "cardsight">;
};

export type ProviderNotice = {
  provider: "gemini" | "cardsight" | "googleCustomSearch";
  code: "not_configured" | "upstream_unavailable" | "upstream_error" | "skipped";
  message: string;
};

export type ProviderResult<T> = {
  status: "available" | "unavailable" | "error" | "skipped";
  evidence?: T;
  notice?: ProviderNotice;
};

type GeminiEvidence = {
  model: string;
  extracted: Omit<ExtractedCardFields, "fieldSources">;
};

type CardSightEvidence = {
  providerReference: NullableString;
  matched: boolean;
  extracted: Omit<ExtractedCardFields, "fieldSources">;
};

type SearchEvidence = {
  query: string;
  matches: Array<{ title: string; link: string; snippet: string }>;
};

export type ScanAnalysis = {
  extracted: ExtractedCardFields;
  providers: {
    gemini: ProviderResult<GeminiEvidence>;
    cardsight: ProviderResult<CardSightEvidence>;
    googleCustomSearch: ProviderResult<SearchEvidence>;
  };
  notices: ProviderNotice[];
};

class ProviderFailure extends Error {
  constructor(
    readonly kind: "not_configured" | "upstream_unavailable" | "upstream_error",
  ) {
    super(kind);
    this.name = "ProviderFailure";
  }
}

function emptyFields(): Omit<ExtractedCardFields, "fieldSources"> {
  return {
    identity: { title: null, brand: null, set: null, year: null, cardNumber: null },
    visibleCertification: { company: null, serialNumber: null, grade: null },
    conditionObservations: [],
  };
}

function asRecord(value: unknown): Record<string, unknown> | undefined {
  return typeof value === "object" && value !== null && !Array.isArray(value)
    ? value as Record<string, unknown>
    : undefined;
}

function boundedString(value: unknown, maxLength = 240): NullableString {
  return typeof value === "string" && value.length > 0 && value.length <= maxLength
    ? value
    : null;
}

function strings(value: unknown, maxItems = 12, maxLength = 240): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .filter((entry): entry is string => typeof entry === "string" && entry.length > 0 && entry.length <= maxLength)
    .slice(0, maxItems);
}

function fieldValue(
  source: Record<string, unknown> | undefined,
  ...keys: string[]
): NullableString {
  if (!source) {
    return null;
  }
  for (const key of keys) {
    const candidate = boundedString(source[key]);
    if (candidate) {
      return candidate;
    }
  }
  return null;
}

function normalizeFields(value: unknown): Omit<ExtractedCardFields, "fieldSources"> {
  const root = asRecord(value);
  const identity = asRecord(root?.identity) ?? asRecord(root?.card) ?? root;
  const certification = asRecord(root?.visibleCertification)
    ?? asRecord(root?.certification)
    ?? asRecord(root?.grading);

  return {
    identity: {
      title: fieldValue(identity, "title", "name", "cardName"),
      brand: fieldValue(identity, "brand", "manufacturer"),
      set: fieldValue(identity, "set", "setName", "series"),
      year: fieldValue(identity, "year"),
      cardNumber: fieldValue(identity, "cardNumber", "number"),
    },
    visibleCertification: {
      company: fieldValue(certification, "company", "grader", "gradingCompany"),
      serialNumber: fieldValue(certification, "serialNumber", "serial", "certNumber"),
      grade: fieldValue(certification, "grade"),
    },
    conditionObservations: strings(root?.conditionObservations ?? root?.observations),
  };
}

function valueAtPath(value: unknown, ...path: string[]): unknown {
  let current: unknown = value;
  for (const key of path) {
    if (Array.isArray(current) && /^\d+$/.test(key)) {
      current = current[Number(key)];
      continue;
    }
    const record = asRecord(current);
    if (!record) {
      return undefined;
    }
    current = record[key];
  }
  return current;
}

function secretValue(value: { value: () => string }): string {
  let secret: string;
  try {
    secret = value.value();
  } catch {
    throw new ProviderFailure("not_configured");
  }
  if (!secret) {
    throw new ProviderFailure("not_configured");
  }
  return secret;
}

async function fetchJson(
  url: string,
  init: RequestInit,
  timeoutMs = 10_000,
): Promise<unknown> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, { ...init, signal: controller.signal });
    if (!response.ok) {
      logger.warn("Card scan upstream HTTP error", {
        status: response.status,
        host: new URL(url).host,
        path: new URL(url).pathname,
      });
      throw new ProviderFailure(response.status >= 500 || response.status === 429
        ? "upstream_unavailable"
        : "upstream_error");
    }
    try {
      return await response.json();
    } catch {
      throw new ProviderFailure("upstream_error");
    }
  } catch (error) {
    if (error instanceof ProviderFailure) {
      throw error;
    }
    throw new ProviderFailure("upstream_unavailable");
  } finally {
    clearTimeout(timeout);
  }
}

async function googleAccessToken(): Promise<string> {
  const response = await fetch(
    "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token",
    { headers: { "Metadata-Flavor": "Google" } },
  );
  if (!response.ok) {
    throw new ProviderFailure("upstream_unavailable");
  }
  const body = asRecord(await response.json());
  const token = typeof body?.access_token === "string" ? body.access_token : "";
  if (!token) {
    throw new ProviderFailure("upstream_unavailable");
  }
  return token;
}

function firstModelText(result: unknown): string {
  const parts = valueAtPath(result, "candidates", "0", "content", "parts");
  if (!Array.isArray(parts)) {
    const single = valueAtPath(result, "candidates", "0", "content", "parts", "0", "text");
    return typeof single === "string" ? single : "";
  }
  return parts
    .map((part) => {
      const record = asRecord(part);
      return typeof record?.text === "string" ? record.text : "";
    })
    .filter(Boolean)
    .join("\n")
    .trim();
}

function failure<T>(
  provider: ProviderNotice["provider"],
  error: unknown,
): ProviderResult<T> {
  const kind = error instanceof ProviderFailure ? error.kind : "upstream_error";
  logger.warn("Card scan provider did not return usable evidence", {
    provider,
    failureKind: kind,
  });

  const code = kind === "not_configured"
    ? "not_configured"
    : kind === "upstream_unavailable"
      ? "upstream_unavailable"
      : "upstream_error";
  const status = kind === "upstream_unavailable" || kind === "not_configured"
    ? "unavailable"
    : "error";
  const message = kind === "not_configured"
    ? `${provider} is not configured.`
    : kind === "upstream_unavailable"
      ? `${provider} is temporarily unavailable.`
      : `${provider} returned an unusable response.`;
  return { status, notice: { provider, code, message } };
}

function geminiPart(image: CardImage): Record<string, unknown> {
  return {
    inlineData: {
      mimeType: image.mimeType,
      data: image.dataBase64,
    },
  };
}

async function scanWithGemini(request: ScanRequest): Promise<ProviderResult<GeminiEvidence>> {
  const model = "gemini-2.5-flash";
  try {
    const token = await googleAccessToken();
    const endpoint = [
      "https://us-central1-aiplatform.googleapis.com/v1/projects/vaultables",
      "locations/us-central1/publishers/google/models",
      `${model}:generateContent`,
    ].join("/");
    const result = await fetchJson(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        contents: [{
          role: "user",
          parts: [
            {
              text: [
                "Inspect both card images. Return JSON only, with identity",
                "(title, brand, set, year, cardNumber), visibleCertification",
                "(company, serialNumber, grade), and conditionObservations.",
                "Use null for every value not visually supported. Do not estimate",
                "price, authenticity, market trend, or confidence.",
                request.category ? `Category hint: ${request.category}.` : "",
                request.notes ? `User notes (untrusted hint): ${request.notes}.` : "",
              ].filter(Boolean).join(" "),
            },
            geminiPart(request.images.front),
            geminiPart(request.images.back),
          ],
        }],
        generationConfig: {
          responseMimeType: "application/json",
          temperature: 0,
          maxOutputTokens: 2048,
        },
      }),
    }, 25_000);
    const text = firstModelText(result);
    if (!text || text.length > 20_000) {
      logger.warn("Gemini returned empty or oversized text", {
        model,
        length: text.length,
        finishReason: valueAtPath(result, "candidates", "0", "finishReason"),
      });
      throw new ProviderFailure("upstream_error");
    }
    try {
      return {
        status: "available",
        evidence: { model, extracted: normalizeFields(JSON.parse(text)) },
      };
    } catch {
      throw new ProviderFailure("upstream_error");
    }
  } catch (error) {
    return failure("gemini", error);
  }
}

async function scanWithCardSight(request: ScanRequest): Promise<ProviderResult<CardSightEvidence>> {
  try {
    const apiKey = secretValue(CARDSIGHT_API_KEY);
    let endpoint: string;
    try {
      endpoint = CARDSIGHT_API_URL.value();
    } catch {
      throw new ProviderFailure("not_configured");
    }
    if (!endpoint || !/^https:\/\//.test(endpoint)) {
      throw new ProviderFailure("not_configured");
    }
    const result = await fetchJson(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${apiKey}`,
      },
      body: JSON.stringify({
        images: request.images,
        category: request.category,
      }),
    });
    const root = asRecord(result);
    if (!root) {
      throw new ProviderFailure("upstream_error");
    }
    const extracted = normalizeFields(root.data ?? root);
    return {
      status: "available",
      evidence: {
        providerReference: fieldValue(root, "id", "requestId", "reference"),
        matched: Object.values(extracted.identity).some(Boolean),
        extracted,
      },
    };
  } catch (error) {
    return failure("cardsight", error);
  }
}

function mergeEvidence(
  gemini: ProviderResult<GeminiEvidence>,
  cardsight: ProviderResult<CardSightEvidence>,
): ExtractedCardFields {
  const merged = emptyFields();
  const fieldSources: ExtractedCardFields["fieldSources"] = {};
  const sources: Array<["gemini" | "cardsight", Omit<ExtractedCardFields, "fieldSources"> | undefined]> = [
    ["gemini", gemini.evidence?.extracted],
    ["cardsight", cardsight.evidence?.extracted],
  ];

  for (const [provider, evidence] of sources) {
    if (!evidence) {
      continue;
    }
    for (const key of Object.keys(merged.identity) as Array<keyof ExtractedCardFields["identity"]>) {
      if (!merged.identity[key] && evidence.identity[key]) {
        merged.identity[key] = evidence.identity[key];
        fieldSources[`identity.${key}`] = provider;
      }
    }
    for (const key of Object.keys(merged.visibleCertification) as Array<keyof ExtractedCardFields["visibleCertification"]>) {
      if (!merged.visibleCertification[key] && evidence.visibleCertification[key]) {
        merged.visibleCertification[key] = evidence.visibleCertification[key];
        fieldSources[`visibleCertification.${key}`] = provider;
      }
    }
    merged.conditionObservations.push(...evidence.conditionObservations);
  }

  return {
    ...merged,
    conditionObservations: [...new Set(merged.conditionObservations)].slice(0, 24),
    fieldSources,
  };
}

async function searchGoogle(
  extracted: ExtractedCardFields,
): Promise<ProviderResult<SearchEvidence>> {
  const query = [extracted.identity.title, extracted.identity.brand, extracted.identity.set]
    .filter((value): value is string => Boolean(value))
    .join(" ");
  if (!query) {
    return {
      status: "skipped",
      notice: {
        provider: "googleCustomSearch",
        code: "skipped",
        message: "Google Custom Search was skipped because no provider identified a card.",
      },
    };
  }

  try {
    const apiKey = secretValue(GOOGLE_CUSTOM_SEARCH_API_KEY);
    const cx = secretValue(GOOGLE_CUSTOM_SEARCH_CX);
    const url = new URL("https://www.googleapis.com/customsearch/v1");
    url.searchParams.set("key", apiKey);
    url.searchParams.set("cx", cx);
    url.searchParams.set("q", query);
    url.searchParams.set("num", "5");
    const result = await fetchJson(url.toString(), { method: "GET" });
    const items = valueAtPath(result, "items");
    const matches = Array.isArray(items)
      ? items.map((item) => {
        const record = asRecord(item);
        return {
          title: fieldValue(record, "title") ?? "",
          link: fieldValue(record, "link") ?? "",
          snippet: fieldValue(record, "snippet") ?? "",
        };
      }).filter((item) => item.title && item.link).slice(0, 5)
      : [];
    return { status: "available", evidence: { query, matches } };
  } catch (error) {
    return failure("googleCustomSearch", error);
  }
}

export async function analyzeCard(request: ScanRequest): Promise<ScanAnalysis> {
  const [gemini, cardsight] = await Promise.all([
    scanWithGemini(request),
    scanWithCardSight(request),
  ]);
  const extracted = mergeEvidence(gemini, cardsight);
  const googleCustomSearch = await searchGoogle(extracted);
  const notices = [gemini.notice, cardsight.notice, googleCustomSearch.notice]
    .filter((notice): notice is ProviderNotice => Boolean(notice));

  return { extracted, providers: { gemini, cardsight, googleCustomSearch }, notices };
}
