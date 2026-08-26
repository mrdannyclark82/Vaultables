import assert from "node:assert/strict";
import test from "node:test";

import {
  buildMarketplaceSearchEvidence,
  ProviderFailure,
  cardSightIdentifyUrl,
  cardSightSegment,
  isUsableCustomSearchCx,
  mapCardSightCatalogSearch,
  mapCardSightIdentifyResponse,
  pickBestCardSightDetection,
} from "./providers.js";

const exactDetection = {
  confidence: "High",
  card: {
    id: "card-uuid",
    setId: "set-uuid",
    year: "2023",
    manufacturer: "Topps",
    releaseName: "Chrome",
    setName: "Base Set",
    name: "Aaron Judge",
    number: "99",
    parallel: { name: "Gold Refractor", numberedTo: 50 },
  },
  grading: {
    company: { name: "PSA" },
    grade: { value: "10", condition: "GEM MINT" },
    qualifier: { code: "OC" },
  },
};

test("maps category hints to CardSight identify segments", () => {
  assert.equal(cardSightSegment(undefined), undefined);
  assert.equal(cardSightSegment("Pokemon TCG"), "pokemon");
  assert.equal(cardSightSegment("NFL"), "football");
  assert.equal(cardSightSegment("mtg"), "magic");
  assert.equal(cardSightSegment("custom-set"), "custom-set");
  assert.equal(cardSightSegment("holo charizard"), undefined);
});

test("builds the official identify URL and rejects the placeholder host", () => {
  assert.equal(
    cardSightIdentifyUrl("https://api.cardsight.ai"),
    "https://api.cardsight.ai/v1/identify/card",
  );
  assert.equal(
    cardSightIdentifyUrl("https://api.cardsight.ai/", "football"),
    "https://api.cardsight.ai/v1/identify/card/football",
  );
  assert.throws(
    () => cardSightIdentifyUrl("https://example.com/cardsight"),
    (error: unknown) => error instanceof ProviderFailure && error.kind === "not_configured",
  );
  assert.throws(
    () => cardSightIdentifyUrl("http://api.cardsight.ai"),
    (error: unknown) => error instanceof ProviderFailure && error.kind === "not_configured",
  );
});

test("picks the highest-confidence exact match", () => {
  const best = pickBestCardSightDetection({
    success: true,
    detections: [
      { confidence: "Low", card: {} },
      { confidence: "Medium", card: { setId: "set-uuid", releaseName: "Prizm" } },
      exactDetection,
    ],
  });
  assert.equal(best, exactDetection);
});

test("maps an exact CardSight identify response into scan fields", () => {
  const evidence = mapCardSightIdentifyResponse({
    success: true,
    requestId: "req_abc123",
    detections: [exactDetection],
  });
  assert.equal(evidence.matched, true);
  assert.equal(evidence.providerReference, "req_abc123");
  assert.deepEqual(evidence.extracted.identity, {
    title: "Aaron Judge Gold Refractor",
    brand: "Topps",
    set: "Chrome Base Set",
    year: "2023",
    cardNumber: "99",
  });
  assert.deepEqual(evidence.extracted.visibleCertification, {
    company: "PSA",
    serialNumber: null,
    grade: "10",
  });
  assert.deepEqual(evidence.extracted.conditionObservations, [
    "CardSight confidence: High",
    "Parallel: Gold Refractor /50",
    "Grade qualifier: OC",
  ]);
});

test("accepts the SDK-wrapped body and set-level matches", () => {
  const evidence = mapCardSightIdentifyResponse({
    data: {
      success: true,
      requestId: "req_set",
      detections: [{
        confidence: "Medium",
        card: {
          setId: "set-uuid",
          year: "2024",
          manufacturer: "Panini",
          releaseName: "Prizm Football",
          setName: "Base Set",
        },
      }],
    },
  });
  assert.equal(evidence.matched, true);
  assert.equal(evidence.extracted.identity.title, null);
  assert.equal(evidence.extracted.identity.set, "Prizm Football Base Set");
  assert.equal(evidence.extracted.identity.year, "2024");
  assert.equal(evidence.extracted.identity.brand, "Panini");
});

test("treats empty detections as an unmatched but usable response", () => {
  const evidence = mapCardSightIdentifyResponse({
    success: true,
    requestId: "req_none",
    detections: [],
  });
  assert.equal(evidence.matched, false);
  assert.equal(evidence.extracted.identity.title, null);
  assert.equal(evidence.providerReference, "req_none");
});

test("treats success=false with advisory messages as unmatched, not broken", () => {
  const evidence = mapCardSightIdentifyResponse({
    success: false,
    requestId: "req_lowres",
    messages: [{
      type: "warning",
      message: "Image resolution (320x320) is below the recommended size for accurate results.",
    }],
  });
  assert.equal(evidence.matched, false);
  assert.equal(evidence.providerReference, "req_lowres");
  assert.equal(
    evidence.extracted.conditionObservations[0],
    "Image resolution (320x320) is below the recommended size for accurate results.",
  );
});

test("maps CardSight catalog search hits", () => {
  const evidence = mapCardSightCatalogSearch({
    results: [{
      id: "card-1",
      type: "card",
      name: "Derek Jeter",
      year: "1993",
      setName: "Topps",
    }],
  }, "Derek Jeter Topps");
  assert.equal(evidence.query, "Derek Jeter Topps");
  assert.equal(evidence.matches[0]?.title, "Derek Jeter");
  assert.equal(evidence.matches[0]?.snippet, "1993 · Topps · card");
  assert.ok(evidence.matches[0]?.link.includes("card-1"));
});

test("rejects placeholder Custom Search engine IDs", () => {
  assert.equal(isUsableCustomSearchCx("not-configured"), false);
  assert.equal(isUsableCustomSearchCx("dummy"), false);
  assert.equal(isUsableCustomSearchCx("012345678901234567890:vaultables"), true);
  assert.equal(isUsableCustomSearchCx("abcdefghijklmnopq"), true);
});

test("builds live marketplace searches from identified card fields", () => {
  const searches = buildMarketplaceSearchEvidence({
    identity: {
      title: "Aaron Judge Gold Refractor",
      brand: "Topps",
      set: "Chrome Base Set",
      year: "2023",
      cardNumber: "99",
    },
    visibleCertification: { company: null, serialNumber: null, grade: null },
    conditionObservations: [],
    fieldSources: {},
  });

  assert.deepEqual(searches?.markets.map((market) => market.name), [
    "eBay sold",
    "Mercari",
    "CollX",
    "Whatnot",
  ]);
  assert.ok(searches?.markets[0]?.url.includes("LH_Sold=1"));
  assert.ok(searches?.markets.every((market) => market.url.includes("Aaron%20Judge")));
});

test("skips marketplace searches when the card cannot be identified", () => {
  assert.equal(buildMarketplaceSearchEvidence({
    identity: { title: null, brand: null, set: null, year: null, cardNumber: null },
    visibleCertification: { company: null, serialNumber: null, grade: null },
    conditionObservations: [],
    fieldSources: {},
  }), undefined);
});

test("rejects a non-object CardSight payload", () => {
  assert.throws(
    () => mapCardSightIdentifyResponse("nope"),
    (error: unknown) => error instanceof ProviderFailure && error.kind === "upstream_error",
  );
});
