# Android signing and update policy

## Permanent signing identity (locked baseline)

**Decision:** The permanent FitYar release signing identity is the already configured candidate key whose APK update was previously accepted on the user's device. Its certificate is now the pinned baseline for future builds. Every later APK for the same package must use that exact keystore and certificate. Never generate a new key during a normal build or release.

- Android package ID: `com.cognitivekernel.androidfit` — do not change it for ordinary updates.
- Every release must have a strictly higher `versionCode` than the last distributed release.
- The signing certificate SHA-256 fingerprint must be pinned and checked in CI before any release APK is published.
- Keep a durable copy of the keystore and its alias/passwords. GitHub Actions caches are temporary build caches, not the source of truth.
- The previously device-accepted update candidate and v1.1.11 were signed with this same certificate. Keep using it for every later APK. Do not uninstall the app to work around any signature error; if Android rejects an update, stop and inspect the APK certificates first.
- Restoring a database backup protects app data only if the backup is valid and restoration succeeds; it does not make Android accept a mismatched APK signature.

## Release setup and key custody

The permanent release keystore is supplied through GitHub Actions secrets; the repository does not store the private keystore or passwords. The release workflow verifies the certificate against the fingerprint pinned in source.

Required Actions secrets:
1. `FIT_YAR_RELEASE_KEYSTORE_BASE64`
2. `FIT_YAR_RELEASE_KEY_ALIAS`
3. `FIT_YAR_RELEASE_STORE_PASSWORD`
4. `FIT_YAR_RELEASE_KEY_PASSWORD`

Before any future release:
1. Confirm the above secrets still reference the same permanent keystore.
2. Increment both `versionCode` and `versionName`; never reuse a published version code.
3. Run **Actions → FitYar permanent release APK** on the intended release branch and supply the new version values.
4. Download the artifact only if the workflow passes its package, signature, and pinned-fingerprint checks.
5. Keep a separate encrypted backup of the keystore and passwords in a secure password/key-management system. Never put them in GitHub source, docs, issues, or chat.
6. Verify the APK checksum, preserve the artifact and release record, and test the update on a device without uninstalling the existing app.

### Permanent release certificate (pinned)

- Latest verified candidate: version `1.1.14`, `versionCode=110`. The next candidate is `1.1.15`, `versionCode=111`; do not reuse `110`. Keep the permanent package ID and pinned certificate unchanged.
- Pinned signer SHA-256: `61:DB:AC:4C:3B:39:36:AD:0D:A7:5C:02:17:81:D1:94:D9:97:DE:0E:67:18:E0:F1:53:C8:5F:14:62:9F:0C:4A`
- This is the release certificate expected for all future production APKs. CI must stop if the keystore signs with any other certificate.

### Permanent release certificate approval

- Status: **PINNED**
- Keystore alias: stored in `FIT_YAR_RELEASE_KEY_ALIAS` secret; do not disclose it in source or logs beyond non-secret diagnostic output.
- Certificate SHA-256: `61:DB:AC:4C:3B:39:36:AD:0D:A7:5C:02:17:81:D1:94:D9:97:DE:0E:67:18:E0:F1:53:C8:5F:14:62:9F:0C:4A`
- Baseline package: `com.cognitivekernel.androidfit`
- Latest verified candidate versionCode: `110` (`1.1.14`). Every future production release must use a strictly greater versionCode.

## Current CI test certificate (not the permanent release key)

The GitHub Actions side-by-side test build uses the cached key at `~/.android/debug.keystore`, alias `androiddebugkey`. The workflow verifies this test APK's certificate before publishing the test artifact. This is separate from the production-package candidate APK, which is signed with the pinned release keystore secret.

Current expected CI test certificate SHA-256:

`57:82:64:31:52:17:B7:96:51:A8:6F:36:07:C6:EC:5A:9C:0A:17:85:D4:61:AA:D5:76:A4:CB:EC:A4:D3:64:47`

This debug/test certificate is not the permanent release certificate. The optional `signed-apk` job in `android-ci.yml` remains gated; the supported production-package build path is `android-release.yml`, and the branch test artifact is built by `build-release-candidate.yml`. A side-by-side test APK does not prove that an APK can update an existing production installation.

## Historical artifact mismatch

Older GitHub Actions artifacts were signed by different automatically generated keys because Gradle did not explicitly select the cached key. The recorded SHA-256 fingerprints are:

- Older run 90 artifact: `C2:25:37:E1:4C:60:3E:12:42:13:3B:23:4D:FC:82:6A:89:A5:32:41:A1:A7:BE:D5:C7:32:68:9C:0D:C9:D0:75`
- Older run 98 artifact: `B9:EE:43:95:BF:EF:2E:38:2F:DC:AE:C1:4E:33:FB:19:D9:B8:57:F5:16:16:33:C7:3A:F7:03:D9:F7:F3:22`

These fingerprints differ from the currently pinned CI test certificate. The old artifacts therefore are not guaranteed to update one another or the new baseline. A local Android Studio build may also use a different local debug key.

The diagnostic above concerns the old **debug/test** signing key only; it does not mean the permanent release key is missing. Production release builds use the separately configured `FIT_YAR_RELEASE_*` secrets and the pinned release certificate documented above. Never substitute the debug/test key for that release identity.

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

## Full exercise-library packaging requirement

The production/release-candidate APK must include the full `app/src/main/assets/exercises/` GIF library, not just the six offline core GIFs. The tracked catalogue contains 6,556 GIF files totaling approximately 732 MB before APK compression. A tiny APK around 2–4 MB is the separate `.test2`/side-by-side test build or a reduced-library build; it is not the full production candidate and must not be presented as the final replacement. Release workflows must check out the GIF files, include them in Android assets, and verify the expected version code/name, package ID, certificate fingerprint, and SHA-256 before handing over the artifact.

The permanent signing identity is the certificate fingerprint above. The private keystore/passwords remain in GitHub Actions secrets and the user's secure backup; the documentation deliberately does not contain the secret key material. Never uninstall the installed app to bypass an update signature error.

## Test installation

The isolated test APK uses package `com.cognitivekernel.androidfit.test2` and can be installed alongside the production package. It is for functional testing only, uses separate app data, and is not a production update.

## Historical note

The permanent signing certificate is pinned and re-verified in CI. Latest candidate build: [FitYar release candidate APK 1.1.15](https://github.com/cognitive-kernel/android-fit/actions/runs/40492366409). CI verified package `com.cognitivekernel.androidfit`, `versionCode=111`, `versionName=1.1.15`, all 6,556 GymVisual GIFs, APK size 736,757,996 bytes and the pinned certificate. The candidate includes the GIF renderer correction. The current phone still must be used for the final in-place update test; this documentation and CI cannot independently inspect its installed APK. The current phone still must be used for the final in-place update test; this documentation and CI cannot independently inspect its installed APK.
