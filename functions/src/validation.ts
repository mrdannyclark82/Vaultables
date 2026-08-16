export type CardImage = {
  mimeType: "image/jpeg" | "image/png" | "image/webp";
  dataBase64: string;
};

export type ScanRequest = {
  images: {
    front: CardImage;
    back: CardImage;
  };
  category?: string;
  notes?: string;
};

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

const MAX_IMAGE_BYTES = 2_000_000;
const ALLOWED_IMAGE_TYPES = new Set<CardImage["mimeType"]>([
  "image/jpeg",
  "image/png",
  "image/webp",
]);

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function hasOnlyKeys(value: Record<string, unknown>, keys: readonly string[]): boolean {
  return Object.keys(value).every((key) => keys.includes(key));
}

function requiredString(
  value: unknown,
  field: string,
  maxLength: number,
): string {
  if (typeof value !== "string" || value.length === 0 || value.length > maxLength) {
    throw new ApiError(400, "invalid-argument", `${field} must be a non-empty string.`);
  }
  return value;
}

function optionalString(
  value: unknown,
  field: string,
  maxLength: number,
): string | undefined {
  if (value === undefined) {
    return undefined;
  }
  if (typeof value !== "string" || value.length > maxLength) {
    throw new ApiError(400, "invalid-argument", `${field} must be a string of at most ${maxLength} characters.`);
  }
  return value;
}

function decodedByteLength(base64: string): number {
  const padding = base64.endsWith("==") ? 2 : base64.endsWith("=") ? 1 : 0;
  return (base64.length * 3) / 4 - padding;
}

function validateImage(value: unknown, name: string): CardImage {
  if (!isRecord(value) || !hasOnlyKeys(value, ["mimeType", "dataBase64"])) {
    throw new ApiError(400, "invalid-argument", `${name} must contain only mimeType and dataBase64.`);
  }

  const mimeType = requiredString(value.mimeType, `${name}.mimeType`, 32);
  if (!ALLOWED_IMAGE_TYPES.has(mimeType as CardImage["mimeType"])) {
    throw new ApiError(400, "invalid-argument", `${name}.mimeType is not supported.`);
  }

  const dataBase64 = requiredString(value.dataBase64, `${name}.dataBase64`, 2_700_000);
  if (
    dataBase64.startsWith("data:") ||
    !/^[A-Za-z0-9+/]*={0,2}$/.test(dataBase64) ||
    dataBase64.length % 4 !== 0 ||
    decodedByteLength(dataBase64) > MAX_IMAGE_BYTES
  ) {
    throw new ApiError(400, "invalid-argument", `${name}.dataBase64 is invalid or too large.`);
  }

  return { mimeType: mimeType as CardImage["mimeType"], dataBase64 };
}

export function validateScanRequest(body: unknown): ScanRequest {
  if (!isRecord(body) || !hasOnlyKeys(body, ["images", "category", "notes"])) {
    throw new ApiError(400, "invalid-argument", "The scan request contains unsupported fields.");
  }
  if (!isRecord(body.images) || !hasOnlyKeys(body.images, ["front", "back"])) {
    throw new ApiError(400, "invalid-argument", "images must contain only front and back.");
  }

  return {
    images: {
      front: validateImage(body.images.front, "images.front"),
      back: validateImage(body.images.back, "images.back"),
    },
    category: optionalString(body.category, "category", 80),
    notes: optionalString(body.notes, "notes", 500),
  };
}

export function validateListingId(value: unknown): string {
  const listingId = requiredString(value, "listingId", 128);
  if (!/^[A-Za-z0-9_-]+$/.test(listingId)) {
    throw new ApiError(400, "invalid-argument", "listingId has an invalid format.");
  }
  return listingId;
}

export function validateIdempotencyKey(value: string | undefined): string {
  if (!value || !/^[A-Za-z0-9_-]{16,128}$/.test(value)) {
    throw new ApiError(
      400,
      "invalid-argument",
      "Idempotency-Key must contain 16-128 URL-safe characters.",
    );
  }
  return value;
}

export function validateEscrowId(value: unknown): string {
  const escrowId = requiredString(value, "escrowId", 128);
  if (!/^[A-Za-z0-9_-]+$/.test(escrowId)) {
    throw new ApiError(400, "invalid-argument", "escrowId has an invalid format.");
  }
  return escrowId;
}

export function validateShipping(body: unknown): { carrier: string; trackingNumber: string } {
  if (!isRecord(body) || !hasOnlyKeys(body, ["carrier", "trackingNumber"])) {
    throw new ApiError(400, "invalid-argument", "The shipping request contains unsupported fields.");
  }
  return {
    carrier: requiredString(body.carrier, "carrier", 64),
    trackingNumber: requiredString(body.trackingNumber, "trackingNumber", 128),
  };
}

export function validateDispute(body: unknown): { reason: string; details?: string } {
  if (!isRecord(body) || !hasOnlyKeys(body, ["reason", "details"])) {
    throw new ApiError(400, "invalid-argument", "The dispute request contains unsupported fields.");
  }
  const reason = requiredString(body.reason, "reason", 64);
  if (!["not_received", "not_as_described", "counterfeit_concern", "other"].includes(reason)) {
    throw new ApiError(400, "invalid-argument", "reason is not supported.");
  }
  return { reason, details: optionalString(body.details, "details", 1_000) };
}
