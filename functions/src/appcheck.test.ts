import assert from "node:assert/strict";
import test from "node:test";

import { decideAppCheck } from "./appcheck.js";

test("monitor mode allows missing and invalid tokens", () => {
  assert.equal(decideAppCheck({ enforce: false, hasToken: false, tokenValid: false }), "allow");
  assert.equal(decideAppCheck({ enforce: false, hasToken: true, tokenValid: false }), "allow");
  assert.equal(decideAppCheck({ enforce: false, hasToken: true, tokenValid: true }), "allow");
});

test("enforce mode rejects missing or invalid tokens", () => {
  assert.equal(decideAppCheck({ enforce: true, hasToken: false, tokenValid: false }), "reject-missing");
  assert.equal(decideAppCheck({ enforce: true, hasToken: true, tokenValid: false }), "reject-invalid");
  assert.equal(decideAppCheck({ enforce: true, hasToken: true, tokenValid: true }), "allow");
});
