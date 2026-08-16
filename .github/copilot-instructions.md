# Vaultables Copilot Instructions

## Build, test, and lint

This is a single-module Android app (`:app`). There is no committed Gradle wrapper, so use a compatible locally installed Gradle:

```bash
gradle build
gradle :app:assembleDebug
gradle :app:lintDebug
gradle :app:testDebugUnitTest
gradle :app:connectedDebugAndroidTest
```

Run one JVM test class or method with:

```bash
gradle :app:testDebugUnitTest --tests com.example.GeminiServiceTest
gradle :app:testDebugUnitTest --tests 'com.example.ExampleRobolectricTest.read string from context'
```

Run one instrumented test class on a connected device/emulator with:

```bash
gradle :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.ExampleInstrumentedTest
```

Roborazzi is configured for screenshot workflows:

```bash
gradle :app:recordRoborazziDebug
gradle :app:verifyRoborazziDebug
```

`GeminiServiceTest` can call the Gemini endpoint when `GEMINI_API_KEY` is configured; its fallback path is used for an absent placeholder key or failed request.

### Test layout and behavior

- JVM tests live in `app/src/test`. They use JUnit 4; Android-dependent JVM tests use Robolectric with SDK 36. The module enables Android resources for unit tests, so resource and application-context assertions belong in this source set.
- Device/emulator tests live in `app/src/androidTest` and use `AndroidJUnit4`. They require a connected device or running emulator.
- The Gemini service test is an integration-style test, not an isolated network mock: it exercises `GeminiService.analyzeAndAppraise`. Run it only when validating that service or its fallback behavior; do not use it as a deterministic offline unit-test template.
- Keep Compose UI controls discoverable through the existing `testTag` convention. Tests for tab selection, dialogs, and scanner actions should target those tags instead of display text where a tag exists.

## Architecture

- `MainActivity` initializes Firebase and Stripe, creates the activity-scoped `VaultViewModel`, and renders `MainScreen` inside `VaultTheme`.
- Compose navigation is manual rather than Navigation Compose: `MainScreen` selects its five tab screens from `VaultUiState.activeTab` and hosts all dialogs/overlays. Preserve this state-hoisting pattern: screen and component callbacks call `VaultViewModel`, while the view model owns transient UI state in `VaultUiState`.
- `VaultViewModel` exposes Room-backed repository flows as lifecycle-aware `StateFlow`s and translates UI events into `viewModelScope` operations. `VaultRepository` is the data boundary: it coordinates Room DAOs, AI/backend calls, Firestore sync, escrow actions, alerts, and report/currency formatting.
- Room is the local source of truth. Entities are in `data/model`, DAOs expose ordered `Flow<List<...>>` queries, and `AppDatabase` registers all entities. The database currently uses `fallbackToDestructiveMigration()`; when changing persistent entities, update the database version and affected DAOs deliberately.
- Adding a collectible follows the repository pipeline: try the Retrofit scanner endpoint, fall back to `GeminiService`, persist the resulting `CollectibleItem`, then attempt Firestore sync. Escrow payment intents similarly use the Retrofit service, while the view model handles its offline sandbox fallback.

## Repository conventions

- Keep Android code under the existing `com.example` namespace even though the runtime application ID is `com.aistudio.collectiblesvault.app`.
- Maintain dependencies and plugin versions in `gradle/libs.versions.toml`; the app module uses catalog aliases and KSP for Room and Moshi.
- `CollectibleItem.category` stores a `CollectibleCategory.displayName`, while `imageType` uses the enum `name`; retain those formats when constructing or filtering items. `EscrowTransaction.status` stores `EscrowStatus.name`, not its display label.
- Compose interaction surfaces use stable `Modifier.testTag(...)` values in `MainScreen` and scanner UI. Add or preserve tags when changing testable controls.
- Runtime configuration comes from `.env`, with `.env.example` documenting `STRIPE_PUBLISHABLE_KEY`, `GOOGLE_WEB_CLIENT_ID`, `GEMINI_API_KEY`, `CARDSIGHT_API_KEY`, `GOOGLE_CUSTOM_SEARCH_API_KEY`, and `GOOGLE_CUSTOM_SEARCH_ENGINE_ID`. The secrets Gradle plugin exposes these as `BuildConfig` values; do not commit `.env` or real credentials.
- Trading-card scans require clear front and back captures. `CardImageProcessor` rejects low-resolution, dark, overexposed, glare-heavy, or unfocused images before `CardVerificationService` submits evidence to CardSight, Google image search, and Gemini. Keep external verification advisory: only visible card text and a confirmed catalog match should establish an identification.
- The app targets Java 11 source/bytecode compatibility, Compose Material 3, and min SDK 24.
