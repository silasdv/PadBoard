# PadBoard

**A controller-first Android keyboard for handhelds, with full touch support.**

PadBoard 1.0 is built for Android gaming handhelds such as the Retroid Pocket 6. It supports English QWERTY and German QWERTZ typing layouts.

## Vibecoded

This project is **vibecoded**: its implementation and documentation were created with substantial AI assistance through OpenAI Codex, guided and iteratively tested by SilasDV. Automated tests cover selected behavior; they do not guarantee compatibility with every app, firmware or controller. Reviews and contributions are welcome.

## Features

- Nintendo (default), Xbox and PlayStation button labels with configurable mappings.
- D-pad, stick and touch support, with separate controller/touch interaction modes in settings.
- Stable Shift and symbol-toggle positions, language globe and settings shortcut.
- Icons for Search, Go, Next, Send and Done, following the active field's editor action.
- Always-on context preview with a measured caret; password text is never mirrored.
- Optional number row and contextual @, .com, .de and .net shortcuts above email fields.
- Ten themes: Light, Dark, OLED, Purple, Ocean, Forest, Rose, Amber, Crimson and Slate.
- Adjustable base height, optional Light/Medium/Strong haptics and an offline design.
- German sharp ß remains ß in uppercase mode.

## Install

Download **PadBoard-1.0.apk** from this repository's Releases page. Android 8.0 or newer is required.

1. Open PadBoard and choose **Enable** in the Setup section.
2. Enable PadBoard in Android's input-method settings.
3. Return and choose **Select** to select PadBoard.
4. Checkmarks show the current setup status. Try the sample fields before using your apps.

Keep another keyboard enabled so you can switch back with Android's keyboard picker.

**Upgrading from earlier test APKs:** 1.0 uses a dedicated release key. Uninstall any build signed with the old development key before installing this release. Note your custom mappings first; uninstalling clears settings. Future releases must use the same release key.

## Default Nintendo controls

| Input | Action |
| --- | --- |
| D-pad / left stick | Move selection |
| A | Enter selected key |
| B | Close keyboard |
| X | Delete |
| Y | Space |
| L1 / R1 | Previous / next row |
| L2 | Toggle symbols |
| R2 | Toggle uppercase |
| Start | Invoke the field action |
| Select | Switch typing language |

Retroid and Nintendo profiles have been merged into Nintendo. Firmware input modes can change Android button codes. Use **Button Mapping >** to adjust bindings or restore the defaults. Assigning an occupied button swaps the two actions. Mappings currently apply globally.

## Build

Use JDK 17, Android SDK platform 35 and Build Tools 35.0.0. Set ANDROID_HOME or configure a local SDK path in local.properties.

```sh
./gradlew testDebugUnitTest lintRelease assembleDebug
```

On Windows, use gradlew.bat. The wrapper pins Gradle 8.11.1 and verifies its distribution checksum. See [Release signing](docs/RELEASING.md) to build a signed APK and App Bundle. Secrets and local paths must never be committed.

## Compatibility

PadBoard is an Android input method, not a global game-input injector. Apps must provide a compatible input connection and route controller events appropriately. Next/Send behavior depends on editor metadata. Haptic strength depends on device hardware and Android settings. Base height is a display percentage; extra rows grow above the keyboard until available space is exhausted.

Automated checks include Android 13 simulations for settings navigation, touch/controller switching and vibration requests. These do not establish physical motor behavior on all devices. Full virtual-key accessibility, automatic controller identification and per-device profiles remain future work.

## Privacy and contributions

PadBoard has no INTERNET permission, advertising or analytics. Typed text is not saved; only settings and mappings are stored. See [Privacy](PRIVACY.md), [Contributing](CONTRIBUTING.md) and [Third-party notices](THIRD_PARTY_NOTICES.md).

## Support SilasDV

Tips are optional: [Ko-fi / silasdv](https://ko-fi.com/silasdv). All features remain free.

## License

MIT. See [LICENSE](LICENSE). No LeanKeyboard source code was copied.
