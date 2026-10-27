# FitYar release and update record

This file is the release ledger for FitYar. Update it in the same pull request as every distributed APK. Do not mark a release as distributed until the APK has passed CI, its package/version/signature have been checked, and the user has confirmed or been given the artifact.

## Permanent update contract

- **Application ID:** `com.cognitivekernel.androidfit`. Never change for ordinary updates.
- **Signing secrets (GitHub Actions → Settings → Secrets and variables → Actions):**
  - `FIT_YAR_RELEASE_KEYSTORE_BASE64`
  - `FIT_YAR_RELEASE_KEY_ALIAS`
  - `FIT_YAR_RELEASE_KEY_PASSWORD`
  - `FIT_YAR_RELEASE_STORE_PASSWORD`
- The public certificate fingerprint is pinned in `.github/workflows/android-release.yml`; do not replace it without a deliberate key migration.
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
| 1.1.11 | 107 | Prior accepted signing baseline | PR [#14](https://github.com/cognitive-kernel/android-fit/pull/14) merged as `d08c798ef88b20244559acb63a78e8fb90f4746b`. CI run [40158495102](https://github.com/cognitive-kernel/android-fit/actions/runs/40158495102) passed unit tests, lint, emulator instrumentation, package/version checks, and candidate-key signing. Artifact: [fityar-corrected-release-key-v107.zip](https://github.com/cognitive-kernel/android-fit/actions/runs/40158495102). APK package `com.cognitivekernel.androidfit`, versionName `1.1.11`, versionCode `107`. Certificate SHA-256 `61:DB:AC:4C:3B:39:36:AD:0D:A7:5C:02:17:81:D1:94:D9:97:DE:0E:67:18:E0:F1:53:C8:5F:14:62:9F:0C:4A`. Device behavior still needs user confirmation. |

## Next candidate (not yet released)\n\n- Target version: `1.1.15`, `versionCode=111`. Build is triggered from the release-candidate workflow after the default-version bump. Do not mark this row as distributed until CI succeeds and the APK is delivered. Confirm the downloaded APK itself reports this version before installing.\n\n## Certificate fingerprint record

Record only the public certificate SHA-256 fingerprint from `apksigner verify --print-certs`; never record key material or passwords here.

- **Permanent pinned release certificate SHA-256:** `61:DB:AC:4C:3B:39:36:AD:0D:A7:5C:02:17:81:D1:94:D9:97:DE:0E:67:18:E0:F1:53:C8:5F:14:62:9F:0C:4A`.
- The fingerprint was verified for v1.1.11 and the previously device-accepted update candidate, and was re-verified for v1.1.13 (`versionCode=109`) in CI run [40267603799](https://github.com/cognitive-kernel/android-fit/actions/runs/40267603799).
- The production release workflow hard-pins this fingerprint and fails if a different certificate is used. The private keystore and passwords remain in Actions secrets.

## Download and checksum record

For each distributed APK, add a row with the exact artifact URL, SHA-256 checksum, source commit, and date. Do not paste a guessed or expired sandbox link.

| Version | Artifact URL | SHA-256 | Source commit | Device result |
|---|---|---|---|---|
| 1.1.9-test-key / 105 | [Actions run 40105406787](https://github.com/cognitive-kernel/android-fit/actions/runs/40105406787) | Artifact ZIP SHA-256 `01e27990c1512a6cfa5969ecb02a860a67c30f282cb188b102fb4d81eecb613f` | `f337fb3fbd78fafd3c56e5b8a5b84167e6b20f4d` | User confirmed update installed; exercise-library changes were not present as expected. |
| 1.1.13 release candidate / 109 | [Actions run 40268799244](https://github.com/cognitive-kernel/android-fit/actions/runs/40268799244) — artifact `fityar-release-candidate-1.1.13` | `SHA256SUMS.txt` included in artifact ZIP | `c506eee75bf288f230c0a1598d8a2b0b94c49889` | Earlier validated candidate; package/signature checks passed. See the newer rebuild row below for the current source commit. |


| 1.1.13 release candidate rebuild / 109 | [Download artifact from run 40269135638](https://github.com/cognitive-kernel/android-fit/actions/runs/40269135638/artifacts/12627578426) — artifact `fityar-release-candidate-1.1.13` | Artifact ZIP SHA-256: `daca154ed6641736c2b419b88cdedc22d0b88a5f7d6fa33679d0948fd383aaff`; APK checksum in `SHA256SUMS.txt` | `10f9ba0b1149d9d9281da7fbe4bd80d22eac7a73` | Rebuilt from current branch head; package `com.cognitivekernel.androidfit`, versionName `1.1.13`, versionCode `109`; CI run [40269140657](https://github.com/cognitive-kernel/android-fit/actions/runs/40269140657) passed unit tests, lint, emulator instrumentation, test APK build and release-key checks. Pinned signer SHA-256 matched. Device in-place update and data retention remain to be tested. |
| 1.1.14 release candidate / 110 | [Download artifact from run 40465881142](https://github.com/cognitive-kernel/android-fit/actions/runs/40465881142/artifacts/12716133217) — artifact `fityar-release-candidate-1.1.14` | `SHA256SUMS.txt` inside artifact contains APK SHA-256 | `dabde785d6f2f261741e46002ee00e126a99c026` | Android CI run [40465886586](https://github.com/cognitive-kernel/android-fit/actions/runs/40465886586) passed unit tests, lint, emulator instrumentation, test APK and signing/package checks. Permanent signer fingerprint matched. Device update and data retention are pending user testing. |
