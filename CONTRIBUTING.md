# Contributing

Use English for issues, pull requests, documentation and interface text. English and German typing layouts are supported.

1. Fork the repository and create a branch.
2. Use JDK 17 and Android SDK 35.
3. Run `./gradlew testDebugUnitTest lintRelease assembleDebug`.
4. Describe the change and how it was tested. Controller reports should include device, Android/firmware version, controller model/mode and target application.

Do not commit APKs, signing material, SDK paths or private text. Keep changes focused and preserve both touch and controller interaction. Add regression coverage for input routing and layout changes.

The project is vibecoded. AI-assisted contributions are welcome; review generated changes and disclose meaningful AI assistance.
