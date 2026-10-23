# Android signing and update safety

## Fixed CI test certificate

The GitHub Actions debug build is configured to use the cached key at `~/.android/debug.keystore`, alias `androiddebugkey`. The workflow verifies both the keystore certificate and the produced APK before publishing the artifact.

Expected CI certificate SHA-256:

`57:82:64:31:52:17:B7:96:51:A8:6F:36:07:C6:EC:5A:9C:0A:17:85:D4:61:AA:D5:76:A4:CB:EC:A4:D3:64:47`

The signed-APK job is currently gated off by default because the original key has not been recovered. This keeps the rest of CI (unit tests, lint, emulator tests, and the separate test APK) independent of the deferred signing/update issue. To run the signing job after restoring the exact key, set the repository Actions variable `ENABLE_SIGNED_APK` to `true`. If the key is missing or the certificate differs, the job fails and publishes no production APK; it never generates a replacement key. The private key is not committed to Git. GitHub Actions cache is not a durable backup; a release signing key needs encrypted, long-term storage.

## Historical artifact mismatch

Older GitHub Actions artifacts were signed by different, automatically generated keys because the Gradle build did not explicitly select the cached key. Inspection found these SHA-256 fingerprints:

- Older run 90 artifact: `C2:25:37:E1:4C:60:3E:12:42:13:3B:23:4D:FC:82:6A:89:A5:32:41:A1:A7:BE:D5:C7:32:68:9C:0D:C9:D0:75`
- Older run 98 artifact: `B9:EE:43:95:BF:EF:2E:38:2F:DC:AE:C1:4E:33:FB:19:D9:B8:57:F5:16:16:33:C7:3A:F7:03:D9:F7:F3:22`

Neither matches the currently pinned CI certificate. Therefore, the current CI APK is **not guaranteed to update** an installation made from those older artifacts. A local Android Studio build may also use a different local debug key. On 2026-10-23, the CI diagnostic found a cache hit under the old v1 cache key, but its certificate did not match the registered fingerprint; the `FIT_YAR_DEBUG_KEYSTORE_BASE64` secret was empty. The workflow now uses a fresh v2 cache namespace so the stale key cannot be mistaken for the original. No private keystore was found in tracked repository paths or the inspected CI artifacts. Therefore, no signed APK can be published until the exact original key is recovered and securely supplied. If you still have the exact keystore, store its base64 contents as the GitHub Actions secret `FIT_YAR_DEBUG_KEYSTORE_BASE64`; do not commit it or paste the private key into a public issue. The workflow also checks the fingerprint, so a different key will still be rejected. A non-blocking CI diagnostic checks cache/secret availability without printing or uploading the private key.

## Recovery steps in GitHub (phone-friendly)

1. First locate the **original private keystore file** used to sign the installed app. The certificate fingerprint or an exported APK is not enough to reconstruct the private key.
2. If that keystore file is available, encode the file locally as one Base64 string on a trusted device. Do not upload it to a public website, commit it to Git, or paste the keystore/private key into an issue or chat.
3. Open the repository on GitHub, then go to **Settings → Secrets and variables → Actions → New repository secret**. Use the exact name `FIT_YAR_DEBUG_KEYSTORE_BASE64` and set its value to the Base64 text. GitHub does not reveal a saved secret's value later, so a blank CI diagnostic means this workflow cannot currently read a usable value under that name.
4. After the key is securely configured, go to **Settings → Secrets and variables → Actions → Variables** and set `ENABLE_SIGNED_APK` to `true`. The workflow will verify the certificate fingerprint before it publishes anything.
5. Download the resulting APK only after the signed job and all required tests pass. Compare its certificate with the installed app before installing.

If the original keystore no longer exists in a trusted backup or an accessible old environment, the installed app cannot be updated in place with a newly generated key. The safe options are to continue searching for the original key or plan a separate migration to a differently signed/new package; do not uninstall the existing app as a workaround.

## Non-negotiable update checks

Before installing a new APK over an existing FitYar installation, all of these must be true:

1. The package name remains `com.cognitivekernel.androidfit`.
2. The new APK certificate matches the installed APK certificate exactly.
3. The new `versionCode` is not lower than the installed version; the current branch defaults to version code `104` / version name `1.1.8`.
4. A backup exists before any restore operation or database test that could replace user data.

**Do not uninstall the installed app to bypass a signature conflict. Do not install an APK with an unverified certificate.** Uninstalling can delete local workout and health records.

To inspect a local APK:

```bash
apksigner verify --print-certs app-debug.apk
```

Compare the `Signer #1 certificate SHA-256 digest` with the certificate from the APK that is already installed. If they differ, stop and locate the original signing key. A public certificate cannot be used to reconstruct its private key.
