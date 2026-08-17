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

export class ProviderFailure extends Error {
  constructor(
    readonly kind: "not_configured" | "upstream_unavailable" | "upstream_error",
  ) {
    super(kind);
    this.name = "ProviderFailure";
  }
}

const CARDSIGHT_USER_AGENT = "Vaultables/1.0 (card-scan)";
const CARDSIGHT_SEGMENT_ALIASES: Record<string, string> = {
  baseball: "baseball",
  mlb: "baseball",
  football: "football",
  nfl: "football",
  basketball: "basketball",
  nba: "basketball",
  hockey: "hockey",
  nhl: "hockey",
  pokemon: "pokemon",
  "pokemon tcg": "pokemon",
  "pokémon": "pokemon",
  "pokémon tcg": "pokemon",
  magic: "magic",
  mtg: "magic",
  "magic the gathering": "magic",
  soccer: "soccer",
  wrestling: "wrestling",
  wwe: "wrestling",
  racing: "racing",
  nascar: "racing",
  golf: "golf",
  tennis: "tennis",
  yugioh: "yugioh",
  "yu-gi-oh": "yugioh",
  lorcana: "lorcana",
  onepiece: "onepiece",
  "one piece": "onepiece",
};
const CARDSIGHT_CONFIDENCE_RANK: Record<string, number> = {
  High: 3,
  Medium: 2,
  Low: 1,
};

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

export function cardSightSegment(category?: string): string | undefined {
  if (!category) {
    return undefined;
  }
  const normalized = category.trim().toLowerCase().replace(/\s+/g, " ");
  const aliased = CARDSIGHT_SEGMENT_ALIASES[normalized];
  if (aliased) {
    return aliased;
  }
  if (/^[a-z][a-z0-9-]{1,40}$/.test(normalized)) {
    return normalized;
  }
  return undefined;
}

export function cardSightIdentifyUrl(baseUrl: string, category?: string): string {
  let parsed: URL;
  try {
    parsed = new URL(baseUrl);
  } catch {
    throw new ProviderFailure("not_configured");
  }
  if (parsed.protocol !== "https:") {
    throw new ProviderFailure("not_configured");
  }
  const host = parsed.hostname.toLowerCase();
  if (host === "example.com" || host.endsWith(".example.com")) {
    throw new ProviderFailure("not_configured");
  }
  const segment = cardSightSegment(category);
  const path = segment
    ? `/v1/identify/card/${encodeURIComponent(segment)}`
    : "/v1/identify/card";
  return new URL(path, `${parsed.origin}/`).toString();
}

function cardSightDetectionRank(detection: Record<string, unknown>): number {
  const card = asRecord(detection.card) ?? {};
  const match = fieldValue(card, "id") ? 200 : fieldValue(card, "setId") ? 100 : 0;
  const confidence = typeof detection.confidence === "string"
    ? (CARDSIGHT_CONFIDENCE_RANK[detection.confidence] ?? 0)
    : 0;
  return match + confidence;
}

export function pickBestCardSightDetection(
  result: unknown,
): Record<string, unknown> | undefined {
  const root = asRecord(result);
  const payload = asRecord(root?.data) ?? root;
  const detections = payload?.detections;
  if (!Array.isArray(detections) || detections.length === 0) {
    return undefined;
  }

  let best: Record<string, unknown> | undefined;
  let bestRank = -1;
  for (const entry of detections) {
    const record = asRecord(entry);
    if (!record) {
      continue;
    }
    const rank = cardSightDetectionRank(record);
    if (rank > bestRank) {
      best = record;
      bestRank = rank;
    }
  }
  return best;
}

function cardSightSetLabel(card: Record<string, unknown> | undefined): NullableString {
  const setName = fieldValue(card, "setName", "set");
  const releaseName = fieldValue(card, "releaseName");
  if (setName && releaseName && setName !== releaseName) {
    const combined = `${releaseName} ${setName}`;
    return combined.length <= 240 ? combined : setName;
  }
  return setName ?? releaseName;
}

function cardSightMessages(payload: Record<string, unknown> | undefined): string[] {
  if (!Array.isArray(payload?.messages)) {
    return [];
  }
  return payload.messages.flatMap((entry) => {
    const record = asRecord(entry);
    const text = fieldValue(record, "message", "text");
    return text ? [text] : [];
  });
}

export function mapCardSightIdentifyResponse(result: unknown): CardSightEvidence {
  const root = asRecord(result);
  const payload = asRecord(root?.data) ?? root;
  if (!payload) {
    throw new ProviderFailure("upstream_error");
  }

  const detection = pickBestCardSightDetection(payload);
  const card = asRecord(detection?.card);
  const grading = asRecord(detection?.grading);
  const company = asRecord(grading?.company);
  const grade = asRecord(grading?.grade);
  const parallel = asRecord(card?.parallel);
  const qualifier = asRecord(grading?.qualifier);

  const name = fieldValue(card, "name", "title", "cardName");
  const parallelName = fieldValue(parallel, "name");
  const title = name && parallelName ? `${name} ${parallelName}` : name;
  const observations: string[] = cardSightMessages(payload);
  if (typeof detection?.confidence === "string" && detection.confidence.length <= 32) {
    observations.push(`CardSight confidence: ${detection.confidence}`);
  }
  if (parallelName) {
    const numbered = typeof parallel?.numberedTo === "number" ? ` /${parallel.numberedTo}` : "";
    observations.push(`Parallel: ${parallelName}${numbered}`);
  }
  const qualifierCode = fieldValue(qualifier, "code");
  if (qualifierCode) {
    observations.push(`Grade qualifier: ${qualifierCode}`);
  }

  const extracted = {
    identity: {
      title,
      brand: fieldValue(card, "manufacturer", "brand"),
      set: cardSightSetLabel(card),
      year: fieldValue(card, "year"),
      cardNumber: fieldValue(card, "number", "cardNumber"),
    },
    visibleCertification: {
      company: fieldValue(company, "name", "company", "grader"),
      serialNumber: fieldValue(grading, "serialNumber", "serial", "certNumber"),
      grade: fieldValue(grade, "value", "grade") ?? (
        typeof grading?.grade === "string" ? boundedString(grading.grade) : null
      ),
    },
    conditionObservations: observations,
  };

  return {
    providerReference: fieldValue(payload, "requestId", "id", "reference"),
    matched: Boolean(fieldValue(card, "id") || extracted.identity.title || extracted.identity.set),
    extracted,
  };
}

function imageFilename(image: CardImage): string {
  if (image.mimeType === "image/png") {
    return "card.png";
  }
  if (image.mimeType === "image/webp") {
    return "card.webp";
  }
  return "card.jpg";
}

async function identifyCardSightImage(
  apiKey: string,
  endpoint: string,
  image: CardImage,
): Promise<unknown> {
  const bytes = new Uint8Array(Buffer.from(image.dataBase64, "base64"));
  const file = new File([bytes], imageFilename(image), { type: image.mimeType });
  const form = new FormData();
  form.append("image", file);
  return fetchJson(endpoint, {
    method: "POST",
    headers: {
      "X-API-Key": apiKey,
      "User-Agent": CARDSIGHT_USER_AGENT,
    },
    body: form,
  }, 25_000);
}

async function scanWithCardSight(request: ScanRequest): Promise<ProviderResult<CardSightEvidence>> {
  try {
    const apiKey = secretValue(CARDSIGHT_API_KEY);
    let baseUrl: string;
    try {
      baseUrl = CARDSIGHT_API_URL.value();
    } catch {
      throw new ProviderFailure("not_configured");
    }
    if (!baseUrl) {
      throw new ProviderFailure("not_configured");
    }
    const endpoint = cardSightIdentifyUrl(baseUrl, request.category);
    const front = mapCardSightIdentifyResponse(
      await identifyCardSightImage(apiKey, endpoint, request.images.front),
    );
    if (front.matched) {
      return { status: "available", evidence: front };
    }
    const back = mapCardSightIdentifyResponse(
      await identifyCardSightImage(apiKey, endpoint, request.images.back),
    );
    return { status: "available", evidence: back.matched ? back : front };
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

export function isUsableCustomSearchCx(cx: string): boolean {
  const value = cx.trim();
  if (!value || value.length < 10 || value.length > 80) {
    return false;
  }
  if (/placeholder|changeme|dummy|example|not-configured|your[-_]?cx|todo/i.test(value)) {
    return false;
  }
  return /^[0-9]{10,}:[A-Za-z0-9_-]+$/.test(value) || /^[A-Za-z0-9_-]{17,}$/.test(value);
}

function searchQuery(extracted: ExtractedCardFields): string {
  return [extracted.identity.title, extracted.identity.brand, extracted.identity.set]
    .filter((value): value is string => Boolean(value))
    .join(" ");
}

export function mapCardSightCatalogSearch(result: unknown, query: string): SearchEvidence {
  const root = asRecord(result);
  const payload = asRecord(root?.data) ?? root;
  const rows = payload?.results ?? payload?.cards;
  const matches = Array.isArray(rows)
    ? rows.map((entry) => {
      const record = asRecord(entry);
      const name = fieldValue(record, "name", "title") ?? "";
      const id = fieldValue(record, "id");
      const year = fieldValue(record, "year");
      const setName = fieldValue(record, "setName", "releaseName", "set");
      const kind = fieldValue(record, "type");
      const snippet = [year, setName, kind].filter(Boolean).join(" · ");
      const link = fieldValue(record, "url", "link")
        ?? (id ? `https://cardsight.ai/cards/${id}` : "https://cardsight.ai");
      return { title: name, link, snippet };
    }).filter((item) => item.title).slice(0, 5)
    : [];
  return { query, matches };
}

async function searchCardSightCatalog(query: string): Promise<ProviderResult<SearchEvidence>> {
  const apiKey = secretValue(CARDSIGHT_API_KEY);
  let baseUrl: string;
  try {
    baseUrl = CARDSIGHT_API_URL.value();
  } catch {
    throw new ProviderFailure("not_configured");
  }
  const endpoint = cardSightIdentifyUrl(baseUrl).replace(/\/v1\/identify\/card$/, "/v1/catalog/search");
  const url = new URL(endpoint);
  url.searchParams.set("q", query);
  url.searchParams.set("take", "5");
  const result = await fetchJson(url.toString(), {
    method: "GET",
    headers: {
      "X-API-Key": apiKey,
      "User-Agent": CARDSIGHT_USER_AGENT,
    },
  }, 15_000);
  return { status: "available", evidence: mapCardSightCatalogSearch(result, query) };
}

async function searchGoogle(
  extracted: ExtractedCardFields,
): Promise<ProviderResult<SearchEvidence>> {
  const query = searchQuery(extracted);
  if (!query) {
    return {
      status: "skipped",
      notice: {
        provider: "googleCustomSearch",
        code: "skipped",
        message: "Search was skipped because no provider identified a card.",
      },
    };
  }

  try {
    const apiKey = secretValue(GOOGLE_CUSTOM_SEARCH_API_KEY);
    const cx = secretValue(GOOGLE_CUSTOM_SEARCH_CX);
    if (isUsableCustomSearchCx(cx)) {
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
    }
  } catch {
    // Custom Search JSON API is closed to new GCP customers (403). Fall through.
  }

  try {
    return await searchCardSightCatalog(query);
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
