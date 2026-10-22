# Android signing and update safety

## Fixed CI test certificate

The GitHub Actions debug build is configured to use the cached key at `~/.android/debug.keystore`, alias `androiddebugkey`. The workflow verifies both the keystore certificate and the produced APK before publishing the artifact.

Expected CI certificate SHA-256:

`57:82:64:31:52:17:B7:96:51:A8:6F:36:07:C6:EC:5A:9C:0A:17:85:D4:61:AA:D5:76:A4:CB:EC:A4:D3:64:47`

If the cache is missing, the signed-APK job fails and publishes no APK; it does not generate a replacement key. Unit tests, lint, and emulator tests run separately so development can continue. The private key is not committed to Git. GitHub Actions cache is not a durable backup; a release signing key needs encrypted, long-term storage.

## Historical artifact mismatch

Older GitHub Actions artifacts were signed by different, automatically generated keys because the Gradle build did not explicitly select the cached key. Inspection found these SHA-256 fingerprints:

- Older run 90 artifact: `C2:25:37:E1:4C:60:3E:12:42:13:3B:23:4D:FC:82:6A:89:A5:32:41:A1:A7:BE:D5:C7:32:68:9C:0D:C9:D0:75`
- Older run 98 artifact: `B9:EE:43:95:BF:EF:2E:38:2F:DC:AE:C1:4E:33:FB:19:D9:B8:57:F5:16:16:33:C7:3A:F7:03:D9:F7:F3:22`

Neither matches the currently pinned CI certificate. Therefore, the current CI APK is **not guaranteed to update** an installation made from those older artifacts. A local Android Studio build may also use a different local debug key. As of 2026-10-22, the CI cache lookup is missing the registered key, so no signed APK can be published until that exact key is restored. If you still have the exact keystore, store its base64 contents as the GitHub Actions secret `FIT_YAR_DEBUG_KEYSTORE_BASE64`; do not commit it or paste the private key into a public issue. The workflow also checks the fingerprint, so a different key will still be rejected.

## Non-negotiable update checks

Before installing a new APK over an existing FitYar installation, all of these must be true:

1. The package name remains `com.cognitivekernel.androidfit`.
2. The new APK certificate matches the installed APK certificate exactly.
3. The new `versionCode` is not lower than the installed version; the current branch defaults to version code `103` / version name `1.1.7`.
4. A backup exists before any restore operation or database test that could replace user data.

**Do not uninstall the installed app to bypass a signature conflict. Do not install an APK with an unverified certificate.** Uninstalling can delete local workout and health records.

To inspect a local APK:

```bash
apksigner verify --print-certs app-debug.apk
```

Compare the `Signer #1 certificate SHA-256 digest` with the certificate from the APK that is already installed. If they differ, stop and locate the original signing key. A public certificate cannot be used to reconstruct its private key.
