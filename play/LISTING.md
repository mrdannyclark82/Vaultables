# Play Console paste pack (2026-08-17)

Upload key and AAB live **outside git**. Screenshots are in `play/screenshots/play-console/`.

## App identity

| Field | Value |
|-------|--------|
| App name | Vaultables |
| Package | `com.aistudio.collectiblesvault.app` |
| Default language | English (United States) |
| Category | Lifestyle (or Tools — pick one and keep it) |
| Email | mrdannyclark82@gmail.com |
| Privacy policy URL | `https://github.com/mrdannyclark82/Vaultables/blob/main/PRIVACY_POLICY.md` (until a dedicated host is up) |

## Short description (≤80 chars)

```
Photograph cards, review the ID, and save them to your vault. Payments not live.
```

## Full description

```
Vaultables is a collectibles vault for sports cards and other items.

Scan a card with your camera. Review the suggested title, set, and year. Nothing is added to your vault until you save. Identification uses Gemini and CardSight. Google web price search is not live.

Your vault can sync when you sign in with Google. Marketplace, escrow, and Stripe checkout are previews — they will not charge a card.

Messages are ordinary in-app chat, not end-to-end encryption.

This first Play release is the honest vault + scanner. Features that are not live say so in the app.
```

## Phone screenshots (1080×1920 JPEG)

Upload at least 2, in this order:

1. `play/screenshots/play-console/01-vault.jpg`
2. `play/screenshots/play-console/02-market.jpg`
3. `play/screenshots/play-console/05-hub.jpg`
4. `play/screenshots/play-console/04-messages.jpg`

Do **not** upload `03-feed` — it still implies E2EE.

## Data safety (Play form)

Answer from what the **shipping app** does, not the roadmap.

| Question | Answer |
|----------|--------|
| Collects user data? | Yes |
| Personal info — name | Yes, account (Google sign-in) |
| Personal info — email | Yes, account |
| Photos | Yes (camera scans). Transferred to Gemini + CardSight to identify the item. |
| App activity (in-app actions / saved items) | Yes. Synced to Firestore if signed in. |
| Financial info | No (payments/escrow not live; no card numbers collected) |
| Location | No |
| Contacts | No |
| Device or other IDs | Yes (Firebase / App Check attestation) |
| Data encrypted in transit? | Yes (HTTPS to Firebase / Cloud Run) |
| Users can request deletion? | Yes — email mrdannyclark82@gmail.com |
| Sold? | No |
| Used for ads / sharing for ads? | No |

## Permissions justification (if asked)

- **CAMERA** — photograph collectibles for identification. Optional hardware feature.
- **INTERNET** — account, vault sync, scan API.

## App content / ads

- No ads
- Not primarily for children
- News / COVID / COVID contact / alcohol / etc.: no
- Financial features: **no live payments**

## Release

1. Internal testing track first.
2. Upload the signed AAB from `app/build/outputs/bundle/release/`.
3. Play App Signing: let Google manage the app key; this JKS is the **upload** key. Back it up or you cannot ship updates.
