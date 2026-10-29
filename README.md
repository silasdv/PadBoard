# PadBoard

**A controller-first Android keyboard for handhelds, with full touch support.**

PadBoard is built for Android gaming handhelds such as the Retroid Pocket 6. It includes 13 European typing layouts. Choose your languages in settings; the globe cycles only through your selection.

## Vibecoded

This project is **vibecoded**: its implementation and documentation were created with substantial AI assistance through OpenAI Codex, guided and iteratively tested by SilasDV. Automated tests cover selected behavior; they do not guarantee compatibility with every app, firmware or controller. Reviews and contributions are welcome.

## Features

- A/B/X/Y or vector PlayStation button symbols, confirm calibration and configurable mappings.
- English, German, French, Spanish, Italian, Portuguese, Dutch, Swedish, Danish, Norwegian, Finnish, Polish and Czech typing layouts.
- Multi-select language picker; quick switching cycles only through enabled languages.
- D-pad, stick and touch support, with separate controller/touch interaction modes in settings.
- Stable Shift and symbol-toggle positions, language globe and settings shortcut.
- Icons for Search, Go, Next, Send and Done, following the active field's editor action.
- Always-on context preview with a measured caret; password text is never mirrored.
- Optional number row and contextual @, .com, .de and .net shortcuts above email fields.
- Ten themes: Light, Dark, OLED, Purple, Ocean, Forest, Rose, Amber, Crimson and Slate.
- Adjustable base height, optional Light/Medium/Strong haptics and an offline design.

## Install

Download **PadBoard-1.0.2.apk** from this repository's Releases page. Android 8.0 or newer is required.

1. Open PadBoard and choose **Enable** in the Setup section.
2. Enable PadBoard in Android's input-method settings.
3. Return and choose **Select** to select PadBoard.
4. Checkmarks show the current setup status. Try the sample fields before using your apps.

Keep another keyboard enabled so you can switch back with Android's keyboard picker.

**Upgrading from earlier test APKs:** 1.0 uses a dedicated release key. Uninstall any build signed with the old development key before installing this release. Note your custom mappings first; uninstalling clears settings. Future releases must use the same release key.

## Default controls

| Input | Action |
| --- | --- |
| D-pad / left stick | Move selection |
| A / Cross | Enter selected key |
| B / Circle | Close keyboard |
| X / Square | Delete |
| Y / Triangle | Space |
| L1 / R1 | Previous / next row |
| L2 | Toggle symbols |
| R2 | Toggle uppercase |
| Start | Invoke the field action |
| Select | Switch enabled typing language |

Use **Button Mapping >** to choose **A/B/X/Y** or **PlayStation symbols**, each with a **(swapped)** variant. These names do not assume where the physical buttons are placed.

Choose **Calibrate confirm button** and press printed **A** or **Cross**, as prompted. PadBoard reads the received Android code, selects the standard/swapped preset and resets the four face actions to confirm, close, delete and space. Other actions are preserved by swapping conflicts. Repeat after changing Android controller mode. Calibration identifies the common pair swap, not the controller model or arbitrary permutations; other arrangements can be mapped individually. Preset changes also translate custom face-button bindings so action labels stay consistent. Bindings currently apply globally across controllers.

The distinction is documented by [SDL's button-label handling](https://wiki.libsdl.org/SDL2/SDL_HINT_GAMECONTROLLER_USE_BUTTON_LABELS), while [Dolphin supports assigning actual inputs by pressing them](https://en.dolphin-emu.org/docs/guides/configuring-controllers/). These approaches inform PadBoard's presets and calibration; neither proves a handheld firmware mode from its device name alone.

[Eden's Android backend](https://github.com/eden-emulator/mirror/blob/master/src/input_common/drivers/android.cpp) checks supported keys/axes and applies separate manufacturer-based AB and XY exceptions. PadBoard uses explicit label choice and calibration rather than guessing from unverified device profiles.

## Typing languages

Open **Languages > Typing languages** for the multi-select dropdown dialog. Keep at least one language selected and save. The globe and mapped language-switch action cycle through enabled languages only. Removing the active language selects the first remaining entry. Settings remain in English; these are typing layouts, not spellchecking, prediction or translation. Accent rows sit above the base keyboard so function buttons keep their positions.

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

MIT. See [LICENSE](LICENSE).
