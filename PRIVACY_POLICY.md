# Privacy Policy for Vaultables

**Last updated:** August 17, 2026

Vaultables is a collectibles vault and marketplace app operated by Danny Ray Clark (“we,” “us”). This policy describes what the **current shipping app** actually collects and who sees it. It is not a generic template.

If you do not agree, do not use the app.

## Who we are

**Contact:** mrdannyclark82@gmail.com

## What we collect

### Account

If you sign in, Firebase Authentication stores your Google account identifier, display name, and email so we can keep your vault attached to you.

### Vault and marketplace data

Items you choose to save (title, set/brand, year, notes, local photo paths, and related fields) can be stored on your device and synced to Cloud Firestore under your account.

### Camera and scan images

Vaultables asks for **camera** permission only to photograph cards and other collectibles. Photos you submit for identification are sent to our Cloud Function (`api` on Google Cloud Run in `us-central1`) so we can return a suggested ID. Nothing is added to your vault until you tap save.

Scan photos may be processed by:

- **Google Vertex Gemini** (card/item understanding and appraisal text)
- **CardSight** (card identification and catalog search)

We do not sell your scan images. Providers receive the image (or a resized copy) only to return a result.

### Price / web search

Google Custom Search is **not live**. Catalog/price fallback uses CardSight when available.

### Payments

In-app checkout, escrow, and Stripe charges are **not live**. The app does not collect card numbers. Preview screens exist for a future flow. If payments go live later, this policy will be updated and Stripe will process the payment — we still will not store full card numbers.

### Messages

In-app messages are **not end-to-end encrypted**. Treat them as ordinary server-stored chat, not a secure messenger.

### App integrity

We use Firebase App Check (Play Integrity on release builds; a debug provider on development builds) so our backend can tell a real app install from casual abuse. That involves device/app attestation tokens, not your vault photos.

### Automatically collected

Our servers and Google Cloud / Firebase may log IP address, user-agent, timestamps, and error diagnostics needed to run the service.

## What we do not collect

- Precise location
- Contacts or call logs
- Full payment card numbers
- Information from children we know are under 13

## How we use it

- Create and maintain your account and vault
- Identify collectibles you photograph
- Sync your saved inventory
- Secure the API (auth + App Check)
- Diagnose failures

## Who we share with

| Recipient | Why |
|-----------|-----|
| Google Firebase / Cloud (Auth, Firestore, Cloud Run, Vertex Gemini, App Check) | Account, sync, scan backend, integrity |
| CardSight | Card identification and catalog search on scans you submit |
| Law enforcement / legal process | Only if required by law |

We do not sell personal information.

## Retention

Account and vault data stay until you ask us to delete them or you delete your account. Scan images are processed to produce a result; we do not run a separate public photo dump. Server logs are kept only as long as needed for operations and abuse response.

## Your rights

Email **mrdannyclark82@gmail.com** to access, correct, or delete your account and vault data. We will verify it is you first.

## Children

Vaultables is not directed at children under 13. We do not knowingly collect their data. If you think we have, email us and we will delete it.

## Changes

We will update the “Last updated” date when this policy changes. Material changes (for example, turning payments on) will be reflected here before those features go live.

## Contact

Vaultables  
mrdannyclark82@gmail.com
