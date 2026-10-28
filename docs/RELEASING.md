# Release signing

Version 1.0 uses version code 100 and a dedicated release signing key. Never commit or upload the private key or passwords to the public repository.

Provide these environment variables:

| Variable | Value |
| --- | --- |
| PADBOARD_KEYSTORE_PATH | Absolute path to the release keystore |
| PADBOARD_STORE_PASSWORD | Keystore password |
| PADBOARD_KEY_ALIAS | Key alias |
| PADBOARD_KEY_PASSWORD | Key password |

Alternatively, use an ignored `.local/release-signing.properties` file with `storeFile`, `storePassword`, `keyAlias` and `keyPassword` entries. Relative storeFile paths resolve from the project root.

```sh
./gradlew testDebugUnitTest lintRelease assembleRelease bundleRelease
```

Without signing material, Gradle produces an unsigned release artifact for build verification. Do not distribute it as an installable release.

The signed APK is in `app/build/outputs/apk/release/`; the App Bundle is in `app/build/outputs/bundle/release/`. Verify the APK with Android's `apksigner verify --verbose --print-certs`, create SHA-256 checksums, and attach the APK/checksums to a GitHub release tagged `v1.0`.

Back up the original key and its passwords privately. Future updates must use that key. An earlier test APK signed with the development key must be uninstalled before installing 1.0.

## Optional CI
The checked-in docs/ci-workflow.yml is a workflow template. Move it to .github/workflows/ci.yml to activate CI using an account/token authorized to write workflows. The initial publishing session did not have that scope. The full release checks were run locally.
