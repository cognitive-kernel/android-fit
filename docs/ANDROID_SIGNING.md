# Android signing and update policy

## Permanent signing identity (locked baseline)

**Decision:** The permanent FitYar release signing identity is the already configured candidate key whose APK update was previously accepted on the user's device. Its certificate is now the pinned baseline for future builds. Every later APK for the same package must use that exact keystore and certificate. Never generate a new key during a normal build or release.

- Android package ID: `com.cognitivekernel.androidfit` — do not change it for ordinary updates.
- Every release must have a strictly higher `versionCode` than the last distributed release.
- The signing certificate SHA-256 fingerprint must be pinned and checked in CI before any release APK is published.
- Keep a durable copy of the keystore and its alias/passwords. GitHub Actions caches are temporary build caches, not the source of truth.
- The currently installed older APK may have a different certificate. Because the owner accepts a one-time reinstall and has a database backup, designate the first APK signed by the new permanent key as the **new update baseline**. Install that baseline after exporting and verifying a usable backup. From that point onward, keep the new key unchanged.
- Restoring a database backup protects app data only if the backup is valid and restoration succeeds; it does not make Android accept a mismatched APK signature.

## One-time baseline decision and required secret handling

CI has successfully built version 1.1.13 (`versionCode=109`) with the configured release keystore. The same fingerprint was recorded for the v1.1.11 release and the earlier candidate that the user confirmed installed as an update. This key is therefore the permanent baseline for future releases. The private key and passwords remain in GitHub Actions secrets and must never be committed. The production release workflow now pins the public certificate fingerprint in source and fails closed if the configured keystore differs. The user's current phone has not been inspected directly in this session, so use the existing update chain and test the APK without uninstalling the app.

1. Decide whether the existing candidate keystore (observed candidate SHA-256 below) will become the permanent baseline. If yes, do **not** generate a replacement key; securely preserve a backup of the exact keystore used by CI and verify that it can be restored. If not, generate a new dedicated keystore locally with Android SDK `keytool` and use that new key consistently.
2. Record the chosen alias and passwords in a durable place and keep a separate backup copy of the keystore. Do not rely on GitHub Actions cache or a single workstation.
3. Ensure the repository Actions secrets under **Settings → Secrets and variables → Actions** match the chosen keystore: `FIT_YAR_RELEASE_KEYSTORE_BASE64`, `FIT_YAR_RELEASE_KEY_ALIAS`, `FIT_YAR_RELEASE_STORE_PASSWORD`, and `FIT_YAR_RELEASE_KEY_PASSWORD`. The candidate-build job proves that a candidate secret is currently available, but does not by itself approve the key as the permanent baseline. Never paste the keystore or passwords into an issue, commit, or chat.
4. Set the repository Actions variable `FIT_YAR_RELEASE_CERT_SHA256` to the chosen certificate's SHA-256 fingerprint (colon-separated or unseparated hex is accepted). The release workflow compares the built APK certificate against this pin and fails closed on missing/mismatched settings.
5. Use **Actions → FitYar permanent release APK → Run workflow** on `main`, provide a `version_code` greater than the last distributed version and a `version_name`, and download the `fityar-release-*` artifact only if the workflow succeeds. This workflow builds the release variant and verifies the package ID, signature, and certificate fingerprint.
6. Install the baseline APK on a test phone only after confirming the backup is usable. Verify app launch and backup/restore, then preserve the APK, checksum, version and fingerprint as release records.

### Permanent release certificate (pinned)

- Verified build artifact: `fityar-candidate-v109.apk`, version `1.1.13`, `versionCode=109`
- Pinned signer SHA-256: `61:DB:AC:4C:3B:39:36:AD:0D:A7:5C:02:17:81:D1:94:D9:97:DE:0E:67:18:E0:F1:53:C8:5F:14:62:9F:0C:4A`
- This is the release certificate expected for all future production APKs. CI must stop if the keystore signs with any other certificate.

### Permanent release certificate approval

- Status: **PINNED**
- Keystore alias: stored in `FIT_YAR_RELEASE_KEY_ALIAS` secret; do not disclose it in source or logs beyond non-secret diagnostic output.
- Certificate SHA-256: `61:DB:AC:4C:3B:39:36:AD:0D:A7:5C:02:17:81:D1:94:D9:97:DE:0E:67:18:E0:F1:53:C8:5F:14:62:9F:0C:4A`
- Baseline package: `com.cognitivekernel.androidfit`
- Last built candidate versionCode: `109` (`1.1.13`); every future production release must use a strictly greater versionCode.

## Current CI test certificate (not the permanent release key)

The GitHub Actions side-by-side test build uses the cached key at `~/.android/debug.keystore`, alias `androiddebugkey`. The workflow verifies this test APK's certificate before publishing the test artifact. This is separate from the production-package candidate APK, which is signed with the pinned release keystore secret.

Current expected CI test certificate SHA-256:

`57:82:64:31:52:17:B7:96:51:A8:6F:36:07:C6:EC:5A:9C:0A:17:85:D4:61:AA:D5:76:A4:CB:EC:A4:D3:64:47`

This debug/test certificate is not the permanent release certificate. The signed production APK job remains disabled unless explicitly configured; a successful test APK build does not prove that an APK can update an existing production installation.

## Historical artifact mismatch

Older GitHub Actions artifacts were signed by different automatically generated keys because Gradle did not explicitly select the cached key. The recorded SHA-256 fingerprints are:

- Older run 90 artifact: `C2:25:37:E1:4C:60:3E:12:42:13:3B:23:4D:FC:82:6A:89:A5:32:41:A1:A7:BE:D5:C7:32:68:9C:0D:C9:D0:75`
- Older run 98 artifact: `B9:EE:43:95:BF:EF:2E:38:2F:DC:AE:C1:4E:33:FB:19:D9:B8:57:F5:16:16:33:C7:3A:F7:03:D9:F7:F3:22`

These fingerprints differ from the currently pinned CI test certificate. The old artifacts therefore are not guaranteed to update one another or the new baseline. A local Android Studio build may also use a different local debug key.

The diagnostic previously found `FIT_YAR_DEBUG_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_BASE64`, and `KEYSTORE_BASE64` empty; the old cache key held a certificate that did not match the pinned test fingerprint, and the v2 cache was absent. No original private keystore was found in tracked repository paths or the inspected CI artifacts. These facts are why a separate permanent release key and explicit fingerprint pin are required.

## Release checklist (required for every release)

1. Verify package ID is exactly `com.cognitivekernel.androidfit`.
2. Increase `versionCode`; never reuse or decrease a published value.
3. Sign with the permanent keystore and alias configured above.
4. Run `apksigner verify --print-certs app-release.apk`.
5. Compare the SHA-256 signer fingerprint to the pinned permanent release fingerprint.
6. Run unit tests, lint, instrumentation tests, and the APK install/signature job.
7. Verify backup export and restore on test data before distribution.
8. Preserve the release APK, version, commit SHA, and fingerprint in the release record.

Example certificate inspection:

```bash
apksigner verify --print-certs app-release.apk
```

**Never silently replace the permanent key.** If the key is lost or the certificate changes, stop release publication and plan a migration; Android does not treat a different signing key as a normal in-place update.

## Test installation

The isolated test APK uses package `com.cognitivekernel.androidfit.test2` and can be installed alongside the production package. It is for functional testing only, uses separate app data, and is not a production update.

## Historical note

The previous production signing key was not recovered or verified. The new permanent key is intended to establish a stable update chain starting with the first baseline APK signed by it. Keep a verified database backup before the one-time migration/install.
