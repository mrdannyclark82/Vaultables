# GEMINI.md - Vaultables Android Project

## Project Overview

**Vaultables** is a modern Android application built with Kotlin and Jetpack Compose. It appears to be a platform for managing, trading, and securing collectibles.

The project follows modern Android architecture principles, utilizing a ViewModel-Repository pattern to separate UI logic from data operations. It integrates with Firebase for backend services and uses a local Room database for offline caching and persistence.

### Key Technologies & Libraries:

*   **UI:** Jetpack Compose, Material 3
*   **Language:** Kotlin
*   **Build:** Gradle
*   **Architecture:** MVVM (ViewModel, Repository, Room for local data)
*   **Asynchronous:** Kotlin Coroutines
*   **Local Database:** AndroidX Room
*   **Backend Services:**
    *   **Firebase Auth:** For user authentication (including Google Sign-In).
    *   **Firebase Firestore:** As a cloud database for syncing data.
    *   **Firebase AI:** Likely for features like the "AI Scanner".
*   **Networking:** Retrofit, OkHttp, and Moshi for API communication.
*   **Image Loading:** Coil
*   **Testing:** JUnit, Robolectric (for unit tests on the JVM), and Roborazzi (for screenshot testing).

## Building and Running

### Build Project

To build the entire project and run checks:

```bash
./gradlew build
```

### Install on Device/Emulator

To build and install the debug version of the app on a connected device or running emulator:

```bash
./gradlew installDebug
```

### Run Tests

*   **Unit Tests:** To run all unit tests (including Robolectric) on the JVM:
    ```bash
    ./gradlew testDebugUnitTest
    ```

*   **Screenshot Tests:** To run Roborazzi screenshot tests:
    ```bash
    ./gradlew recordRoborazziDebug
    ```
    To verify changes against existing screenshots:
    ```bash
    ./gradlew verifyRoborazziDebug
    ```

*   **Instrumented Tests:** To run tests on a physical device or emulator:
    ```bash
    ./gradlew connectedAndroidTest
    ```

## Development Conventions

*   **Architecture:** The codebase is structured using the recommended Android MVVM (Model-View-ViewModel) pattern. UI state is managed in `ViewModel`s and exposed to Composables, while data operations are handled in `Repository`s.
*   **Dependency Management:** Dependencies are centrally managed in the `gradle/libs.versions.toml` file, following the TOML catalog convention.
*   **Asynchronous Operations:** The project uses Kotlin Coroutines and Flows for managing background tasks and asynchronous data streams.
*   **Secrets Management:** The `secrets-gradle-plugin` is configured to pull API keys and other secrets from `.env` and `.env.example` files, which is a common convention in web development and adapted here for Android.
*   **Modularization:** The project is contained within a single `:app` module for now.
