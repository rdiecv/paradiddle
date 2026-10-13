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

## Update: kit view and named beats

- **Kit view**: each style screen shows a top view of the Nitro Mesh kit (crash, ride, hi-hat
  and pedal, snare, toms 1–3, kick and pedal) on a dark stage. Pads used by the beat are
  outlined; while playing, the pads hit on the current 16th light up (accent > normal > ghost,
  fading over the step) with a badge for the limb (right-handed: cymbals = R, snare and toms =
  L, kick = RF). Cross-stick lights the snare rim, an open hi-hat shows a lifted ring, the
  cowbell is played on the ride pad and the tom track on tom 3.
- **Named beats**: the data file is now `styles.json`: styles (id, family, name, kit, setup
  text) with a list of beats (id, name, tempo, how-to text, tracks). Texts are per language in
  the file, with English as the fallback; the `groove_*` strings left `strings.xml`. The style
  screen picks a beat with chips; changing beat stops playback and resets the tempo.
- Beats added only where sources agree: rock half-time and open hi-hat, pop four on the floor
  and ballad, punk driving 8ths, metal gallop and blast beat, bossa nova 2-3 clave, reggae
  rockers and steppers. D-beat, songo variations (cáscara) and cumbia variations were left out
  for lack of reliable sources.

## Update: songs and a learning guide

- Each beat may list `songs` (title, artist, optional per-language note). They are listening
  examples only: no recordings, transcriptions or per-song arrangements ship with the app
  (copyright, and arrangements written without listening would be unreliable). A song is
  listed only when a source confirms the beat; tempos are left out because sources disagree.
- Every beat shows a "Songs that use it" card (or "no suggestion yet") and a collapsible,
  generic "How to learn a whole song" guide: listen and name the parts, match the tempo with
  the loop, count bars into a road map, practise slowly, play along with fills and a crash
  on the next 1, listen for hi-hat/ride changes, play through via Aux In.

## Update: kit legend

The text under the kit drawing became an ⓘ button in the kit's empty bottom-right corner. It
opens a dialog explaining the symbols: R/L/RF, bright vs faint, lit rim = cross-stick, ring =
open hi-hat, amber outline = pads used by the beat.

## Update: listening to a song

Each song has a play button that sends Android's standard `MEDIA_PLAY_FROM_SEARCH` intent (title,
artist, query) so whichever music app the user has finds and plays it; if none answers, it falls
back to `ACTION_WEB_SEARCH`, and then to a short message. No service (YouTube, Spotify…) is named,
which keeps the app clear of F-Droid's NonFreeNet anti-feature and needs no permission.

## Update: song lessons and per-style sound

The generic "How to learn a whole song" guide is gone. Instead, a song can carry a `lesson`, and
its card gets a "Learn this song" button that opens a song page (route `song/{id}`):

- **Data.** `lesson` = `bpm`, ordered `sections` and learning `steps` (EN/ES). A section has a
  `name`, `bars`, an optional `note` and either a `beat` (id of any beat in the file), inline
  `tracks` (a short pattern only that song uses, e.g. stop hits or a build-up) or neither (the
  drums rest). The parser rejects unknown beats, both beat and tracks, bars < 1, a lesson with no
  drums and an out-of-range tempo.
- **Sources.** The user chose researched plans: each lesson is written only from published drum
  guides and bar-by-bar song charts, listed in the song's `sources` (kept in the file, ignored by
  the parser). Bar counts can be off by a bar or two where charts disagree, and the steps say so
  when a detail is uncertain (e.g. the clave direction in The Girl from Ipanema). Songs with no
  reliable form (La sandunguera, One Drop, La pollera colorá, Cumbia sampuesana) keep only the
  listen button.
- **Song page.** Header with artist and tempo, the kit drawing (crash lit at each new section),
  a status line (count-in / section · bar x of y · next section), tempo ±4 with a reset to the
  song's tempo, play the whole song, the learning path and a song map: one row per section with
  its beat, its note and a "play from here" button; the playing row is highlighted.
- **Playback.** `SongTimeline` maps a step to (section, bar, groove step). `GrooveSound`
  renders a one-bar count-in, the grooves section by section, quarter clicks where the drums
  rest and a crash at the start of each later section.
- **New beats** needed by the lessons: "Open hi-hat chorus" (rock) and "Disco (closed hi-hat)"
  (pop); Stayin' Alive and Another One Bites the Dust moved to the disco beat.

Each style also names a drum `sound`: a `KitSound` preset of synthesis parameters (kick pitch and
decay, snare tone and noise, hi-hat darkness, ride, room reflections…) so rock, pop, punk,
metal, bossa nova, songo, reggae and cumbia each sound like their genre. The rudiments keep the
standard sound.
