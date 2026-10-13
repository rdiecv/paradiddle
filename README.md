<img src="fastlane/metadata/android/en-US/images/icon.png" width="96" alt="Paradiddle icon" align="right">

# Paradiddle

[![CI](https://github.com/rdiecv/paradiddle/actions/workflows/ci.yml/badge.svg)](https://github.com/rdiecv/paradiddle/actions/workflows/ci.yml)
[![License: GPL v3+](https://img.shields.io/badge/license-GPL--3.0--or--later-blue.svg)](LICENSE)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84.svg)

A free, open source Android app for learning the **40 PAS drum rudiments**.

- Notation and sticking for every rudiment (accents, flams, drags, buzz strokes)
- **Listen** mode: hear the rudiment at any tempo over a metronome
- **Practice** mode: tap two on-screen pads (left/right hand) while notes fall toward a hit line;
  timing and sticking are scored live (perfect / good / ok / wrong hand / missed)
- Results with rushing/dragging tendency and a suggested next tempo
- Local progress: best clean tempo (≥ 90 %), accuracy, practice time, daily log
- One-time latency calibration so scoring is fair on every phone
- **Styles**: 18 named beats of rock, pop, punk, metal, bossa nova, songo, reggae and cumbia,
  shown on a drawing of the Alesis Nitro Mesh kit that lights up the pads to hit, plus a drum
  grid, looping playback at any tempo, a recommended kit setup, songs that use each beat and a
  guide to learning a whole song
- **My kit**: how to set up an Alesis Nitro Mesh kit to practise with the app
- English and Spanish · no ads, no tracking, no network, no permissions

## Install on your phone

Paradiddle isn't on F-Droid yet, so for now you install the APK by hand ("sideloading").
Android 8.0 or newer is needed.

1. On your phone, open <https://github.com/rdiecv/paradiddle/releases> and download the
   latest `paradiddle-<version>.apk` (under **Assets**).
2. Open the downloaded file (from the browser's downloads or the *Files* app).
3. Android will say it can't install apps from this source. Tap **Settings**, turn on
   **Allow from this source**, then go back and tap **Install**.
4. If Google Play Protect warns about an unknown app, tap **More details → Install anyway**.
   The app asks for no permissions and doesn't use the internet.
5. Open Paradiddle and do the **calibration** first (it takes 15 seconds).

**Updating:** download the newer APK and install it over the old one. Your progress is kept.

**Test builds vs F-Droid:** the test APKs on GitHub are signed with a different key than the
future F-Droid version, so to switch to F-Droid you'll need to uninstall the test version
first (this deletes your practice history).

### Instalar en el móvil (español)

1. En el móvil, abre <https://github.com/rdiecv/paradiddle/releases> y descarga el último
   `paradiddle-<versión>.apk` (en **Assets**).
2. Abre el archivo descargado (desde las descargas del navegador o la app *Archivos*).
3. Android dirá que no puede instalar apps de este origen: pulsa **Ajustes**, activa
   **Permitir de esta fuente**, vuelve atrás y pulsa **Instalar**.
4. Si Google Play Protect avisa de una app desconocida, pulsa **Más detalles → Instalar de todas formas**.
   La app no pide permisos ni usa internet.
5. Abre Paradiddle y haz primero la **calibración** (tarda 15 segundos).

Para actualizar, instala el APK nuevo encima del anterior: se conserva tu progreso.

## Build

Requirements: JDK 17 and the Android SDK (platform 35).

```sh
./gradlew test lint assembleDebug      # tests, lint, debug APK
./gradlew assembleRelease              # unsigned release APK (F-Droid signs its own)
```

The debug APK ends up in `app/build/outputs/apk/debug/`. Install it with
`adb install app/build/outputs/apk/debug/app-debug.apk`.

## Project layout

| Module | What it contains |
|---|---|
| `core/` | Plain Kotlin, no Android: rudiment model and notation parser, timeline builder, scorer, calibration, sound synthesis and mixer. Unit tested. |
| `app/` | Android app: low-latency audio engine (`AudioTrack`), Room database, settings, Jetpack Compose UI. |

The design is described in [`docs/superpowers/specs/2026-10-13-paradiddle-design.md`](docs/superpowers/specs/2026-10-13-paradiddle-design.md).

## Rudiment data

All rudiments live in [`app/src/main/assets/rudiments.json`](app/src/main/assets/rudiments.json),
written in a compact sticking notation:

| Token | Meaning |
|---|---|
| `R` / `L` | stroke with the right / left hand (1 grid slot) |
| `>` | accent, e.g. `R>` |
| `z` | buzz stroke, e.g. `Rz` |
| `l` / `r` prefix | grace notes, e.g. `lR` = flam, `llR` = drag |
| `:n` | lasts `n` slots, e.g. `R>:4` |
| `-` | rest |

`grid` is the number of slots per beat (1, 2, 3, 4, 6 or 8). Patterns are written in their usual
open (slow) form. If you spot a pattern that doesn't match the PAS sheet, corrections are very
welcome: `./gradlew :core:test` checks the file is valid.

## Style data

The styles live in [`app/src/main/assets/styles.json`](app/src/main/assets/styles.json). Each
style has a recommended Alesis Nitro Mesh preset kit (`kit`, 1–24), setup tips and a list of
named `beats`. Texts (`setup`, a beat's `name` and `how`) are given per language
(`{"en": …, "es": …}`). Each track of a beat has one character per 16th note (16 per 4/4 bar,
one or two bars):

| Symbol | Meaning |
|---|---|
| `.` | rest |
| `x` / `X` | hit / accented hit |
| `g` | ghost note |
| `o` | open hi-hat (hi-hat track only) |

A beat can list `songs` that use it (`title`, `artist` and an optional per-language `note`).
They are listening examples only: the app never ships recordings or transcriptions, and a song
is added only when a source confirms the beat.

Instruments: `hihat`, `ride`, `cowbell`, `snare`, `cross_stick`, `tom`, `kick`. The songo and
cumbia beats are simplified drum-kit versions; corrections and new beats are welcome, and
`./gradlew :core:test` checks the file.

## Contributing

Bug reports, rudiment corrections, translations and code are all welcome. Start with
[CONTRIBUTING.md](CONTRIBUTING.md). Everyone taking part follows the
[Code of Conduct](CODE_OF_CONDUCT.md). To report a security problem, see [SECURITY.md](SECURITY.md).

Changes between versions are listed in the [CHANGELOG](CHANGELOG.md).

## License

Paradiddle is free software: you can redistribute it and/or modify it under the terms of the
[GNU General Public License](LICENSE), version 3 or (at your option) any later version.
