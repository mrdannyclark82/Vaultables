import assert from "node:assert/strict";
import test from "node:test";

import { ApiError, validateIdempotencyKey, validateScanRequest } from "./validation.js";

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
