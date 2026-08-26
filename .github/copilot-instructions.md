# Vaultables Copilot Instructions

## Build, test, and lint

This repository has an Android app (`:app`) and a Firebase Functions backend (`functions`). Use the committed Gradle wrapper:

```bash
./gradlew build
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

Run the existing JVM screenshot test, or a single test method, with:

```bash
./gradlew :app:testDebugUnitTest --tests com.example.ui.components.AiScannerModalTest
./gradlew :app:testDebugUnitTest --tests 'com.example.ui.components.AiScannerModalTest.scanner_modal_initial_state'
```

Run one instrumented test class on a connected device or emulator:

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.ExampleInstrumentedTest
```

Roborazzi screenshots are recorded by `AiScannerModalTest`:

```bash
./gradlew :app:recordRoborazziDebug
./gradlew :app:verifyRoborazziDebug
```

Functions require Node 20. Install from the lockfile and run the focused backend suite with:

```bash
npm --prefix functions ci
npm --prefix functions test
```

JVM tests use JUnit 4 and Robolectric (SDK 36); Android resources are enabled for JVM tests. Device tests use `AndroidJUnit4`.

## Architecture

- `MainActivity` initializes Firebase and Stripe, creates the activity-scoped `VaultViewModel`, and renders `MainScreen` inside `VaultTheme`.
- The Compose app uses manual tab navigation, not Navigation Compose. `MainScreen` selects one of five screens from `VaultUiState.activeTab` and hosts dialogs and overlays. Keep transient UI state in `VaultUiState`; components and screens call view-model callbacks rather than owning application state.
- `VaultViewModel` exposes repository `Flow`s as lifecycle-aware `StateFlow`s and launches UI operations in `viewModelScope`. `VaultRepository` coordinates Room, the authenticated HTTP services, Firestore syncing, and presentation formatting.
- Room is the local source of truth. Entities are in `data/model`; DAOs provide ordered flows; `AppDatabase` owns the schema. When changing an entity, increment the database version and add an explicit migration alongside the affected DAOs—this database does not use destructive migration fallback.
- A secure trading-card scan requires both front and back image URIs. `CardImageProcessor` performs local quality checks; the repository sends validated raw base64 images to the scanner endpoint, retains the returned `ScanDraft` for user review, then persists only after confirmation and attempts Firestore sync.
- Firebase Functions exposes the authenticated `/api/v1` scanner and escrow APIs. The server derives the UID from the Firebase ID token and App Check header; never add caller-owned user IDs to request bodies. Payment intents are created server-side, and only a verified Stripe webhook creates or advances a real escrow.

## Repository conventions

- Keep Android packages under `com.example`, despite the runtime application ID being `com.aistudio.collectiblesvault.app`.
- Use catalog aliases and versions from `gradle/libs.versions.toml`; Room and Moshi code generation use KSP. The app targets Java 11 bytecode, min SDK 24, and Compose Material 3.
- `CollectibleItem.category` persists `CollectibleCategory.displayName`, while `imageType` persists the enum-style name (for example, `CARD`). `EscrowTransaction.status` persists `EscrowStatus.name`, not its display label.
- Preserve stable `Modifier.testTag(...)` values on interactive Compose surfaces. UI tests should select tagged controls rather than text whenever a tag exists.
- `.env.example` documents only Android build-time values (`STRIPE_PUBLISHABLE_KEY` and `GOOGLE_WEB_CLIENT_ID`), exposed through `BuildConfig` by the secrets Gradle plugin. Do not commit `.env` files or provider credentials. Gemini, CardSight, Google Search, and Stripe secret keys belong only in Firebase Functions Secret Manager.
- The scanner API accepts exactly two raw-base64 images (`front` and `back`), each at most 2 MB after decoding. Treat provider results as advisory: only visible card text and a confirmed catalog match establish an identification; do not manufacture identity, grade, certificate, price, or confidence.
- Preserve the Functions API contract in `functions/README.md`: scanner and escrow calls require Firebase auth; create-intent uses a URL-safe `Idempotency-Key`; errors are returned as `{"error":{"code":"...","message":"..."}}`.
