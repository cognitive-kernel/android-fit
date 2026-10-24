# Android signing and update policy

## Permanent signing identity from the next release

**Decision:** The first APK distributed as the new baseline must be signed with one deliberately created, permanent FitYar release keystore. Every later APK for the same package must use that exact keystore and certificate. Never generate a new key during a normal build or release.

- Android package ID: `com.cognitivekernel.androidfit` — do not change it for ordinary updates.
- Every release must have a strictly higher `versionCode` than the last distributed release.
- The signing certificate SHA-256 fingerprint must be pinned and checked in CI before any release APK is published.
- Keep a durable copy of the keystore and its alias/passwords. GitHub Actions caches are temporary build caches, not the source of truth.
- The currently installed older APK may have a different certificate. Because the owner accepts a one-time reinstall and has a database backup, designate the first APK signed by the new permanent key as the **new update baseline**. Install that baseline after exporting and verifying a usable backup. From that point onward, keep the new key unchanged.
- Restoring a database backup protects app data only if the backup is valid and restoration succeeds; it does not make Android accept a mismatched APK signature.

## One-time setup required before publishing the baseline APK

The permanent key has **not yet been configured in GitHub Actions**. Documentation alone does not create or store a keystore. Complete these steps once:

1. Generate a dedicated keystore locally using Android SDK `keytool`. Use a stable alias, for example `fityar-release`, and record the alias and passwords in a durable place.
2. Keep a separate backup copy of the keystore. Do not rely on GitHub Actions cache or a single workstation.
3. Encode the keystore locally as Base64 and save it in GitHub repository **Settings → Secrets and variables → Actions → New repository secret** named `FIT_YAR_RELEASE_KEYSTORE_BASE64`. Add the alias/password as separate Actions secrets (names should be documented in the release workflow).
4. Configure the release workflow to decode that secret to a temporary file, sign the APK with it, verify the APK with `apksigner verify --print-certs`, and fail closed if any required secret is missing.
5. Record the new certificate SHA-256 fingerprint below and make CI compare the actual APK fingerprint to this exact value. Do not publish the baseline APK until this check passes.
6. Build the baseline APK, install it on the test phone, verify app launch and backup/restore, then preserve the APK and fingerprint as release records.

### Permanent release certificate (fill after one-time setup)

- Keystore alias: `fityar-release` (change only before baseline creation, if needed)
- Certificate SHA-256: **PENDING — do not publish a release APK until pinned**
- Baseline package: `com.cognitivekernel.androidfit`
- Baseline versionCode: must be greater than the installed version; increment it for every subsequent release.

## Current CI test certificate (not the permanent release key)

The GitHub Actions test build currently uses the cached key at `~/.android/debug.keystore`, alias `androiddebugkey`. The workflow verifies this test APK's certificate before publishing the test artifact.

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
