# Vaultables Firebase backend

The existing Android base URL and paths are retained through Firebase Hosting:
`https://api.vaultables.com/api/v1/...`. Configure the Hosting target named
`api`, attach that domain, and deploy:

```sh
firebase target:apply hosting api <hosting-site-id>
firebase deploy --only functions,firestore,hosting:api
```

Use Node 20. Run `npm --prefix functions ci && npm --prefix functions test`.

## Secrets and provider configuration

Only Firebase Functions access provider credentials. Set these in Firebase
Secret Manager; do not put them in Android resources or a committed env file:

```sh
firebase functions:secrets:set GEMINI_API_KEY
firebase functions:secrets:set CARDSIGHT_API_KEY
firebase functions:secrets:set GOOGLE_CUSTOM_SEARCH_API_KEY
firebase functions:secrets:set GOOGLE_CUSTOM_SEARCH_CX
firebase functions:secrets:set STRIPE_SECRET_KEY
firebase functions:secrets:set STRIPE_WEBHOOK_SECRET
```

`CARDSIGHT_API_URL` is a non-secret HTTPS deployment parameter. Let
`firebase deploy` prompt for it, or set it in an uncommitted
`functions/.env.<project-id>` file. The server adapter posts its documented
`images` payload to that URL with the CardSight secret as a bearer credential.
Adapt only that server-side adapter if the selected CardSight vendor differs.

Configure Stripe to send signed webhooks to
`https://api.vaultables.com/api/v1/stripe/webhook`. The webhook does not use a
Firebase token; it verifies `Stripe-Signature` with `STRIPE_WEBHOOK_SECRET`.

## Android API contract

Each app endpoint requires a Firebase ID token in the HTTP `Authorization`
header and `Content-Type: application/json`. The backend derives the caller
UID from the verified token and rejects body-supplied user IDs. Errors have
this shape:

```json
{"error":{"code":"invalid-argument","message":"..."}}
```

### `POST /api/v1/scanner/analyze`

The legacy single `imageBase64` body is intentionally rejected. Send exactly
two raw-base64 images (not data URIs); JPEG, PNG, and WebP are allowed. Each
decoded image is limited to 2,000,000 bytes and the JSON body to 6 MB.

```json
{
  "images": {
    "front": {"mimeType": "image/jpeg", "dataBase64": "<base64>"},
    "back": {"mimeType": "image/jpeg", "dataBase64": "<base64>"}
  },
  "category": "trading_card",
  "notes": "optional untrusted hint"
}
```

Responses contain `extracted`, field-level `fieldSources`, provider
`evidence`, and explicit `notices`. Provider failures yield null fields and
notices; identity, grade, certificate, price, and confidence are never
invented. Images and secrets are neither logged nor saved in Firestore.

When the card has an identified title, `marketplaceSearches` contains direct
links for sold eBay listings and live Mercari, CollX, and Whatnot searches.
These links are provided for user review only: the API does not scrape those
marketplaces or claim a price, because listing condition, sale status, and
identity must be verified by the collector.

### `POST /api/v1/escrow/create-intent`

Send a 16–128-character URL-safe `Idempotency-Key` header and:

```json
{"listingId":"firestore-listing-id"}
```

The server requires `marketplaceListings/{listingId}` to be public and active,
then reads its `sellerId`, positive integer `priceMinor`, and three-letter
`currency`. It rejects legacy `amountUsd`, `itemId`, `buyerName`, and
`paymentMethod` fields. Update Android `PaymentService` to use:

```json
{
  "paymentIntentId": "pi_...",
  "clientSecret": "pi_..._secret_...",
  "amountMinor": 12500,
  "currency": "usd"
}
```

Use `clientSecret` with the Stripe SDK. Creating an intent stores only a
private payment attempt and a server-only per-listing payment lock; it never
creates an escrow. Only a verified
`payment_intent.succeeded` webhook creates the paid escrow.

### Escrow endpoints and policy

* `POST /api/v1/escrow/{escrowId}/ship` is seller-only and accepts
  `{"carrier":"UPS","trackingNumber":"..."}`. It transitions `paid → shipped`.
* `POST /api/v1/escrow/{escrowId}/confirm-receipt` is buyer-only. It records
  receipt and moves `shipped → inspection`.
* `POST /api/v1/escrow/{escrowId}/disputes` is participant-only in
  `paid`, `shipped`, or `inspection`; send
  `{"reason":"not_as_described","details":"optional"}`. Reasons are
  `not_received`, `not_as_described`, `counterfeit_concern`, or `other`.

The server releases only after paid status plus seller shipment and buyer
receipt confirmation. It alone moves
`inspection → release_processing → released`, using an idempotent Stripe
Transfer to the trusted server-provisioned
`users/{sellerUid}/private/payout.stripeConnectedAccountId`. Firestore Rules
deny clients all writes to escrows, payment records, webhook receipts, payout
configuration, payment IDs, release fields, and dispute records. A missing
payout account or failed transfer leaves funds held with an explicit response.

## App Check

Android sends `X-Firebase-AppCheck` on every `/api/v1` call.

- Default is **monitor**: missing/invalid tokens are logged, not rejected.
- After a debug token is registered (or a Play Integrity release build is live), enforce:

```sh
gcloud run services update api --project=vaultables --region=us-central1 \
  --update-env-vars=APP_CHECK_ENFORCE=1
```

Do not put App Check on `stripeWebhook`. Stripe cannot send a Play Integrity token.

Debug APK: logcat `DebugAppCheckProvider` token → Firebase Console → App Check → Manage debug tokens.

## Testing and operations

`npm --prefix functions test` compiles the backend and runs focused scan-payload
and idempotency validation tests. Before production, exercise Firebase
Auth/Functions/Firestore emulators with signed Stripe fixtures, duplicate
webhooks, concurrent idempotency calls, unavailable scan providers, and every
invalid escrow transition.
