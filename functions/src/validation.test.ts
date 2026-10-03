import assert from "node:assert/strict";
import test from "node:test";

import {
  ApiError,
  validateConnectUrls,
  validateDispute,
  validateEscrowId,
  validateIdempotencyKey,
  validateListingId,
  validateScanRequest,
  validateShipping,
} from "./validation.js";

const image = {
  mimeType: "image/jpeg",
  dataBase64: Buffer.from("card image").toString("base64"),
};

test("accepts a bounded front and back scan payload", () => {
  assert.deepEqual(validateScanRequest({ images: { front: image, back: image } }), {
    images: { front: image, back: image },
    category: undefined,
    notes: undefined,
  });
});

test("rejects URLs, unrecognized image types, and extra scan fields", () => {
  assert.throws(
    () => validateScanRequest({ images: { front: { ...image, dataBase64: "data:image/jpeg;base64,AAAA" }, back: image } }),
    ApiError,
  );
  assert.throws(
    () => validateScanRequest({ images: { front: { ...image, mimeType: "image/gif" }, back: image } }),
    ApiError,
  );
  assert.throws(
    () => validateScanRequest({ images: { front: image, back: image }, ownerUid: "attacker" }),
    ApiError,
  );
});

test("requires a URL-safe idempotency key", () => {
  assert.equal(validateIdempotencyKey("this_is_a_valid_key_123"), "this_is_a_valid_key_123");
  assert.throws(() => validateIdempotencyKey("too-short"), ApiError);
  assert.throws(() => validateIdempotencyKey("contains a space and is invalid"), ApiError);
});

test("accepts marketplace listing ids and rejects unsafe ones", () => {
  assert.equal(validateListingId("market_VAULT-8F3A"), "market_VAULT-8F3A");
  assert.throws(() => validateListingId("bad id"), ApiError);
  assert.throws(() => validateListingId(""), ApiError);
});

test("accepts escrow ids and shipping payloads", () => {
  assert.equal(validateEscrowId("pi_3ABC_secretish"), "pi_3ABC_secretish");
  assert.deepEqual(
    validateShipping({ carrier: "UPS", trackingNumber: "1Z999" }),
    { carrier: "UPS", trackingNumber: "1Z999" },
  );
  assert.throws(() => validateShipping({ carrier: "UPS" }), ApiError);
  assert.throws(() => validateShipping({ carrier: "UPS", trackingNumber: "TRK-VAULT-PENDING" }), ApiError);
  assert.throws(() => validateShipping({ carrier: "UPS", trackingNumber: "x" }), ApiError);
});

test("accepts supported dispute reasons only", () => {
  assert.deepEqual(
    validateDispute({ reason: "not_as_described", details: "wrong grade" }),
    { reason: "not_as_described", details: "wrong grade" },
  );
  assert.throws(() => validateDispute({ reason: "refund_please" }), ApiError);
});

test("requires allowlisted HTTPS Connect return URLs", () => {
  assert.deepEqual(
    validateConnectUrls({
      returnUrl: "https://vaultables.com/connect/return",
      refreshUrl: "https://app.vaultables.com/connect/refresh",
    }),
    {
      returnUrl: "https://vaultables.com/connect/return",
      refreshUrl: "https://app.vaultables.com/connect/refresh",
    },
  );
  assert.throws(
    () => validateConnectUrls({
      returnUrl: "https://evil.example/phish",
      refreshUrl: "https://vaultables.com/connect/refresh",
    }),
    ApiError,
  );
  assert.throws(
    () => validateConnectUrls({
      returnUrl: "http://vaultables.com/connect/return",
      refreshUrl: "https://vaultables.com/connect/refresh",
    }),
    ApiError,
  );
});
