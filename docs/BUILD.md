# Build and test

## Prerequisites

Install Android Studio, JDK 21 and Android SDK platform **API 36.1** (the project declares `compileSdk` 36 with minor API level 1). Network access is required on first build to download the Gradle distribution and dependencies. The repository includes the Gradle wrapper; global Gradle installation is not required.

1. Open the project root in Android Studio, not the `app` subfolder.
2. Select JDK 21 for Gradle.
3. Let the IDE install the required SDK packages and sync.
4. Build an unsigned-for-distribution **debug** APK for testing:

```bash
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. Debug APKs are **not** public release assets.

## Tests

```bash
./gradlew :app:testDebugUnitTest --tests com.example.DayformUnitTest --tests com.example.DayformRobolectricTest
./gradlew :app:lintDebug
```

A device or emulator is required to run `:app:connectedDebugAndroidTest`. Screenshot generation is kept in its own `DayformPortfolioScreenshotCaptureTest` suite; CI deliberately skips it during routine tests because it creates numerous image artifacts.

## Release signing

Only use a private keystore you own and safely back up. Set these four environment variables **locally** (or inject them using protected CI secrets):

- `DAYFORM_KEYSTORE_PATH`: absolute path of the signing `.jks` file.
- `DAYFORM_STORE_PASSWORD`: keystore password.
- `DAYFORM_KEY_ALIAS`: key alias (the source project's previous release configuration used `upload`).
- `DAYFORM_KEY_PASSWORD`: private-key password.

```bash
./gradlew :app:assembleRelease
```

With all four credentials provided, Gradle should create `app/build/outputs/apk/release/app-release.apk`. With **none**, the release task may create an **unsigned APK**, which must not be distributed as an installable production release. Partial signing configuration is rejected by the build script. Verify signature, installation and upgrade continuity before publishing. Details: [RELEASE.md](RELEASE.md).

Do not blindly replace the signing key; Android generally requires the same signing identity to install an update over an existing package.
