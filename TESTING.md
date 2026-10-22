# FitYar test plan

## CI checks

The GitHub Actions workflow runs these checks on the latest commit:

1. Java 17 / Gradle 8.11.1 build.
2. JVM unit tests for Gregorian-to-Jalali conversion, including Nowruz boundaries, zero padding, and invalid dates.
3. Android Lint for the debug variant.
4. Android 16 / API 36 debug APK build.
5. Keystore certificate verification and an explicit APK certificate check. The artifact is rejected if it is not signed with the fixed CI test certificate.
6. Instrumented tests on an API 35 emulator:
   - All home-screen navigation actions and the illustration are present.
   - Plan/exercise create, list, update, and delete behavior.
   - Invalid set counts and negative weights are rejected.
   - Detailed workout metadata and health-log persistence.
   - Additive migration from historical database schemas v1, v2, and v3 without losing records.

## Local commands

Use JDK 17, Android SDK Platform 36, Build Tools 36.0.0, and Gradle 8.11.1.

```bash
gradle test
gradle lintDebug
gradle assembleDebug
gradle connectedDebugAndroidTest
```

The instrumented test task requires a running Android emulator or device. The workflow uses a Google APIs x86_64 API 34 emulator while compiling against API 36. API 34 is used for emulator stability; newer platform behavior still needs a separate device/API-level check.

## Update-safety rule

Do not uninstall the existing app to bypass a signature conflict. Before installing an APK over an existing FitYar installation, compare its signer certificate with the installed version and confirm the new version code is not lower. The CI key is not the same as some older automatically signed artifacts; see [ANDROID_SIGNING.md](docs/ANDROID_SIGNING.md). Never replace or regenerate the registered key silently.
