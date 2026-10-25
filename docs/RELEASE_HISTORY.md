# FitYar release and update record

This file is the release ledger for FitYar. Update it in the same pull request as every distributed APK. Do not mark a release as distributed until the APK has passed CI, its package/version/signature have been checked, and the user has confirmed or been given the artifact.

## Permanent update contract

- **Application ID:** `com.cognitivekernel.androidfit`. Never change for ordinary updates.
- **Signing secrets (GitHub Actions → Settings → Secrets and variables → Actions):**
  - `FIT_YAR_RELEASE_KEYSTORE_BASE64`
  - `FIT_YAR_RELEASE_KEY_ALIAS`
  - `FIT_YAR_RELEASE_KEY_PASSWORD`
  - `FIT_YAR_RELEASE_STORE_PASSWORD`
- Private key bytes, secret values, and passwords must never be committed or printed in logs.
- The keystore and its passwords must be backed up securely outside GitHub. GitHub Actions artifacts/caches are not the only backup.
- Every production update must use the same keystore, alias, and signing certificate as the last accepted update.
- Every new APK must have a strictly greater `versionCode`; update both `versionCode` and `versionName` in `app/build.gradle` and record them below.
- Before upload, CI must verify the APK package ID, version code/name, signature, and certificate SHA-256. A signing mismatch must fail the build rather than publish an APK.
- A test/side-by-side APK with package `com.cognitivekernel.androidfit.test2` is not a production update and must never be described as one.
- Keep exercise GIF assets, catalog/index data, Persian display labels, and the library dialog-navigation fix in the same tested source tree used to build the APK. Verify them in tests and include them in the release notes.
- Never delete the installed app to bypass a signature error. Keep a verified data backup before any exceptional migration.

## Required checklist for every release

1. Confirm the changes are on the intended commit/branch and no required feature PR is omitted.
2. Run unit tests, Android lint, instrumentation tests, and APK build/package checks.
3. Verify the exercise library lists the catalogued GIF assets, displays Persian labels in Persian mode, and preserves the library dialog/search/scroll state after opening and closing a GIF.
4. Build with the four `FIT_YAR_RELEASE_*` secrets and the original package ID.
5. Inspect the artifact's package, `versionCode`, `versionName`, and signing certificate SHA-256.
6. Compare its signer fingerprint with the last accepted release. Stop if it differs.
7. Upload the APK and checksum; record the Actions run, source commit, artifact name, version, and fingerprint below.
8. Update this ledger and the user-facing release notes in the same PR as the release.
9. After delivery, add device-test results and any regressions reported by the user before the next release.

## Release ledger

| Version | versionCode | Status | Notes |
|---|---:|---|---|
| 1.1.8 | 104 | Historical baseline in repository docs | Earlier CI signing configuration expected the cached debug certificate; do not assume older artifacts are interchangeable. |
| 1.1.9-test-key | 105 | Candidate key accepted by user device; feature validation incomplete | Actions run [40105406787](https://github.com/cognitive-kernel/android-fit/actions/runs/40105406787), artifact `fityar-release-key-test-v105`. User confirmed it installed as an update, but reported the new GIF library/navigation behavior was not present. This proves that this candidate artifact updated that device at that time; it does not prove the current source features were included. |
| 1.1.11 | 107 | Build candidate; not mark as delivered until a verified artifact is linked | Intended to include the library-navigation fix. Confirm the artifact is built from the tested commit before distribution. |

## Certificate fingerprint record

Record only the public certificate SHA-256 fingerprint from `apksigner verify --print-certs`; never record key material or passwords here.

- Last device-accepted candidate APK (v105): fingerprint must be copied from the artifact's `signature.txt` / Actions log and checked against the currently installed app before any later APK is distributed.
- Current release certificate pin: **verify from the successfully built artifact and compare with v105 before release**.

## Download and checksum record

For each distributed APK, add a row with the exact artifact URL, SHA-256 checksum, source commit, and date. Do not paste a guessed or expired sandbox link.

| Version | Artifact URL | SHA-256 | Source commit | Device result |
|---|---|---|---|---|
| 1.1.9-test-key / 105 | [Actions run 40105406787](https://github.com/cognitive-kernel/android-fit/actions/runs/40105406787) | See `SHA256SUMS.txt` inside the artifact | `f337fb3fbd78fafd3c56e5b8a5b84167e6b20f4d` | User confirmed update installed; exercise-library changes were not present as expected. |
