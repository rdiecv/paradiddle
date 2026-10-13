# Changelog

All notable changes to Paradiddle are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Separate hands in stereo (Settings, on by default): when a rudiment plays, the right hand
  sounds a little to the right and the left hand a little to the left, so the sticking can be
  heard with headphones. The metronome and the style beats stay centred.
- Timing difficulty (Settings): Easy (counts within ±150 ms, perfect within ±50 ms), Normal (the
  previous limits, ±100 ms and ±25 ms) or Hard (±70 ms and ±15 ms). Easy is the default.
- Practising an input that was never calibrated now asks to calibrate first, or to practise
  anyway.
- A visual metronome like the Alesis Nitro's swinging ball: a ball swings from end to end and
  touches an end on every beat, so the next beat can be seen coming. Shown when practising a
  rudiment, playing or practising a style beat, and calibrating.

### Changed

- New app icon: two crossed drumsticks on a white background, also as a themed icon.
- Practice on the screen: bands around the hit line show where a hit counts, is good and is
  perfect; the notes land on the pads, and the lanes can be tapped too (left half = left hand).
- Style beats: the drum grid is right under the kit, also when practising on real drums, so the
  beat can be followed on both while it plays.
- Calibration is easier to follow: short steps, a 4-3-2-1 countdown on the pad during the
  warm-up clicks, a counter of the clicks to play, the pad pulsing on each click, and a result
  that says what the number means.
- Clearer texts: the "How to read the kit" legend and "About" show their line breaks again,
  the Alesis Nitro Mesh guide and each style's setup are lists with one step per line, and the
  guide no longer says the module's USB port isn't used.

## [1.1.0] - 2026-10-13

### Added

- Bottom tabs: Rudiments, Styles and My kit.
- Styles: 20 named beats for rock (basic, half-time, open hi-hat, open hi-hat chorus), pop
  (16ths, four on the floor, disco, ballad), punk (skank, driving 8ths), metal (double bass,
  gallop, blast beat), bossa nova (3-2 and 2-3 clave), songo, reggae (one drop, rockers,
  steppers) and cumbia, with a drum grid, a looping playback at any tempo, tips and a
  recommended Alesis Nitro Mesh setup.
- Songs that use each beat (15 songs on 10 beats, only where a source confirms the beat). A
  play button next to each song asks your music app to play it (Android's "play from search";
  falls back to a web search, no permission needed).
- Song lessons for 11 of those songs: a page with the song's tempo, a learning path, a map of
  every section with the beat it uses, and playback of the whole song (or from any section)
  with a count-in, a crash at each new section and the kit lighting up. Each lesson comes from
  published drum guides and song charts.
- Real drums: choose in Settings → Play on between the phone screen, a microphone (practice pad
  or acoustic kit) and an electronic kit over USB-MIDI (the Alesis Nitro module's default notes
  are built in). With real drums the timing and the accents are scored (hands can't be told
  apart), and on the electronic kit also the pad. Each input has its own calibration.
- Practise any style beat on real drums: 8 bars after a one-bar count-in, with the kit lighting
  up, live feedback (early / late / wrong pad) and results.
- Each style plays with its own drum sound (tight and dry for punk and metal, deep and roomy
  for reggae, soft for bossa nova…).
- A top view of the Alesis Nitro Mesh kit on each style: while the groove plays, the pads to
  hit light up, with the hand or foot that plays them.
- My kit: a guide to setting up the Alesis Nitro Mesh kit for practice (connections, kit,
  metronome, pad sensitivity, Learning Mode).

### Fixed

- Practice: the falling notes showed the moment to hit 40–95 ms late. Notes are now centred on
  the hit line at their time, and every moving view (practice lanes, lit kit) draws the moment
  the frame reaches the screen (measured on Android 13+, estimated before).

### Changed

- New app icon: a paradiddle (R L R R · L R L L) drawn as bars, with a themed-icon layer for
  Android 13+.

## [1.0.0] - 2026-10-13

### Added

- The 40 PAS drum rudiments with notation, sticking, accents, flams, drags and buzz strokes.
- Listen mode: hear any rudiment at any tempo over a metronome.
- Practice mode: two on-screen pads with falling notes and live timing and sticking feedback,
  with tolerant scoring for grace notes.
- Results with accuracy, rushing/dragging tendency, consistency and a suggested next tempo.
- One-time latency calibration.
- Local progress: best clean tempo, last accuracy, practice time and a daily practice log.
- English and Spanish.

[Unreleased]: https://github.com/rdiecv/paradiddle/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/rdiecv/paradiddle/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/rdiecv/paradiddle/releases/tag/v1.0.0
