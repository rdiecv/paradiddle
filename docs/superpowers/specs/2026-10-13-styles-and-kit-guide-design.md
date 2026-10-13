# Styles and Nitro Mesh kit guide — design

Date: 2026-10-13. Adds two sections to Paradiddle next to the rudiments.

## Goal

1. **Kit setup**: a read-only guide on configuring the Alesis Nitro Mesh kit for practice with
   the app (connections, kit choice, metronome, pad sensitivity, Learning Mode).
2. **Styles**: the basic groove of each music style, taught with a drum grid, a short text and
   a looping playback, plus a recommended Nitro Mesh setup for that style.

Decisions taken with the user: guide is read-only (no USB-MIDI input); grooves are
read + listen (no scoring); first styles are the rock family (rock, pop, punk, metal) and
Latin & world (bossa nova, songo, reggae one drop, cumbia).

Out of scope: scoring grooves, MIDI input, swing/triplet grooves, fills, a click track under
the groove.

## Navigation

A bottom navigation bar with three tabs on the top-level screens:
**Rudiments** (current list, keeps Log and Settings in its top bar), **Styles**, **My kit**.
Detail screens (rudiment, practice, groove, calibration, settings, log) hide the bar.
(The "Kit setup guide" button in Settings from the first draft was dropped: the tab replaces it.)

## Groove data (`core`)

`assets/grooves.json`, parsed by `GrooveCatalog` (same style as `RudimentCatalog`):

```json
{"id": "rock", "family": "rock", "name": "Rock", "bpm": 110, "minBpm": 90, "maxBpm": 140,
 "kit": 10, "tracks": {"hihat": "x.x.x.x.x.x.x.x.", "snare": "....X.......X...", "kick": "x.......x.x....."}}
```

- One character per 16th note; a track is 16 × bars characters (1 or 2 bars). All tracks of a
  groove have the same length.
- Symbols: `.` rest, `x` normal, `X` accent, `g` ghost note, `o` open hi-hat (hi-hat only).
- Instruments: `hihat`, `ride`, `cowbell`, `snare`, `cross_stick`, `tom`, `kick`.
- `family`: `rock` or `latin`. `kit`: Nitro Mesh preset kit number (1–24).
- Validation (throws with the groove id): unknown family/instrument/symbol, `o` on a
  non-hi-hat track, length not a multiple of 16 or different between tracks, empty groove,
  tempo outside 30–280 or `minBpm ≤ bpm ≤ maxBpm` broken, kit outside 1–24, duplicate ids.
- Texts (how to play it, kit setup tips) live in `strings.xml` (EN + ES), mapped by id in the app.

## Sound

- `Synth` gains `kick`, `hihat(open)`, `ride`, `cowbell`, `crossStick`, `tom`; snare reused.
  Gains: accent 1.0, normal 0.65, ghost 0.3.
- `GrooveSound.voices(groove, bpm, loops)` builds mixer voices for `loops` repetitions.
  Playback uses the existing `AudioEngine.play` with enough loops for ~5 minutes; Stop,
  leaving the screen or the app going to the background stops it. Changing the tempo while
  playing restarts the loop at the new tempo (when the slider is released).
- The grid highlights the current step from `Playback.sessionTimeAt`.

## Screens

- **Styles list**: grouped by family, each row: name, typical tempo range, Nitro kit number.
- **Groove detail**: grid (one block per bar, rows = instruments, columns = 1 e & a …),
  tempo control (−4 / +4 / slider within 30–280, starts at the groove's `bpm`), Play/Stop,
  "How to play it" card, "Nitro Mesh setup" card (kit number + name, suggested voices,
  click settings and tips).
- **My kit**: the guide, sections Connect, Choose a kit, Metronome, Snare pad feel,
  Practising with Paradiddle, Learning Mode, and a note that it follows the official user guide.

Kit names, voice numbers and menu steps come from the official Nitro Mesh user guide and
Kit & Voice List v1.0.

## Content caveat

Rock, pop, punk, metal, bossa nova and reggae are standard textbook grooves. Songo and cumbia
are simplified kit adaptations; they are marked as such in the text and, like
`rudiments.json`, can be corrected in `grooves.json` without touching code.

## Tests

`GrooveCatalogTest` (bundled file parses; each validation error), `GrooveSoundTest` (voice
count and frame positions for a known groove, loops, instrument mapping), synth sounds finite
and peak ≤ 1. Then `./gradlew test lint assembleDebug`; device check by the user.
