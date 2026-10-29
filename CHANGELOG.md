# Changelog

## 1.0.2

- Combined Nintendo/Xbox labeling into A/B/X/Y, alongside PlayStation symbols, each with a swapped variant.
- Calibrate confirm button reads the actual Android code, with prompts matching the selected symbol family.
- PlayStation keyboard hints and mapping entries use consistently sized vectors instead of font glyphs.
- Added French, Spanish, Italian, Portuguese, Dutch, Swedish, Danish, Norwegian, Finnish, Polish and Czech typing layouts with directly accessible accented characters.
- Language selection is a multi-select dropdown dialog. The globe cycles only through enabled languages; at least one remains enabled.
- Preset changes translate custom face-button bindings; existing effective 1.0.1 settings migrate without introducing a new swap.
- Settings confirmation follows the mapped confirm button instead of also accepting a hardcoded Android A key.

## 1.0.1

- Changing vibration strength keeps the settings page and its scroll position.
- Corrected the default Nintendo face-button mapping for label-based Android input.
- Added printed-A calibration and a separate Android button-reporting setting for mode-dependent Retroid ABXY codes.
- Keyboard hints follow the selected labels, reporting convention and custom bindings.

## 1.0

First public release: Android gamepad keyboard with English/German layouts, Nintendo/Xbox/PlayStation labels, configurable mappings, touch/controller navigation, ten themes, context preview, email shortcuts, number row and adjustable haptics.

Setup now displays current enable/selection checkmarks. Mapping has a separate submenu. Vibration strength uses outlined inactive choices and a filled active choice. Nintendo is the default label profile. Retroid settings migrate to Nintendo.
