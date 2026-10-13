# Android APK signing and update test

## Root cause

The CI workflow previously restored or generated `~/.android/debug.keystore` and checked its certificate fingerprint, but Gradle's debug build did not explicitly select that keystore in `app/build.gradle`. Verifying a keystore before building does not prove that the produced APK was signed with it. Inspection of the APK artifacts from Actions runs 96 and 98 showed different signing-certificate fingerprints, which explains Android's `Conflict with existing package` update error.

## Fix

- The debug build now explicitly selects `$HOME/.android/debug.keystore`, alias `androiddebugkey`, and the standard Android debug-store credentials.
- CI checks the keystore fingerprint before building and uses `apksigner verify --print-certs` on both output APKs. The build fails if either APK does not have the registered fingerprint.
- CI creates two test artifacts in one run: `fit-yar-v99.apk` (versionCode 99) and `fit-yar-v100.apk` (versionCode 100). Install v99 first, then install v100 over it. Do not uninstall between these two test installs.
- The keystore is persisted in GitHub Actions cache under the fixed key `fit-yar-debug-signing-key-v1`; the private key is not committed to the repository.

## Test procedure

1. Open the successful GitHub Actions run on branch `feature/background-step-counter`.
2. Download artifacts `fit-yar-v99` and `fit-yar-v100`.
3. Install v99, launch it once, then install v100 as an update.
4. Confirm Android accepts the update and the app launches. Compare the certificate SHA-256 lines in the CI log; they must match exactly.
5. If the workflow reports a missing or mismatched signing key, do not distribute either APK. Restore the original registered keystore to Actions cache/secure storage; do not silently generate and accept a replacement.

## Important compatibility note

The newly pinned CI key has SHA-256 fingerprint:

`57:82:64:31:52:17:B7:96:51:A8:6F:36:07:C6:EC:5A:9C:0A:17:85:D4:61:AA:D5:76:A4:CB:EC:A4:D3:64:47`

This is the key checked by the CI workflow. It is not the same certificate observed in the previously downloaded run 96 and run 98 APKs. Therefore, these test APKs are intended for the user's stated clean-install test after removing the old app. They are not guaranteed to update an installation signed by the old certificate. Once a key is chosen for distribution, keep it in secure, durable storage and use it for every subsequent release. GitHub Actions cache is not a substitute for a durable backup of the private key.
