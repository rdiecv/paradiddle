# Real-drum input: microphone and USB-MIDI

Date: 2026-10-13 · Status: approved product decisions, technical design by Claude (auto mode)

## Goal

The user practises on a practice pad and on an Alesis Nitro Mesh kit, not on the phone screen.
The app must hear those hits and score them, for rudiments and for style beats.

## Product decisions (from the user)

- **Two inputs**, chosen in Settings next to the existing on-screen pads:
  - **Microphone**: any practice pad or acoustic kit. Hears *that* a hit happened and how loud.
  - **Electronic kit (USB-MIDI)**: the Nitro Mesh module plugged into the phone with a USB OTG
    cable. Exact timing, velocity and which pad was hit.
- **What is scored**: timing of every stroke, plus accents (loud vs soft). Neither input can tell
  the left hand from the right, so the sticking is shown as a guide but not scored.
- **Where**: rudiment practice, and a new "Practise on my drums" mode for style beats (timing of
  every hit; with MIDI also the right pad).

## Facts this design relies on

- Nitro module default MIDI notes (official *Nitro Drum Module User Guide* v1.2, appendix "Pad
  MIDI Note Numbers"): kick 36, snare 38, snare rim 40, tom 1 48 (rim 50), tom 2 45 (rim 47),
  tom 3 43 (rim 58), tom 4 41 (rim 39), ride 51, crash 1 49, crash 2 57, hi-hat open 46,
  half-open 23, closed 42, pedal 44, splash 21. Drums are on channel 10. USB carries MIDI only.
  Notes may be remapped on the module, so unknown notes count as "some pad" (timing only).
- Android `android.media.midi` (API 23+, minSdk is 26) sees class-compliant USB-MIDI devices
  without any permission. Message timestamps are in the `System.nanoTime()` base.
- `AudioRecord.getTimestamp(…, TIMEBASE_MONOTONIC)` (API 24+) maps a recorded frame to
  `System.nanoTime()`, so mic onsets land on the same clock as the playback.

## Design

### Core (pure Kotlin, unit-tested)

- `input/DrumHit(timeNanos, level, instrument?)`: one hit in session time; `level` 0–1 (MIDI
  velocity / 127, or mic peak); `instrument` null when unknown (mic, unmapped note).
- `input/NitroMidi`: MIDI note → `Instrument` (table above; rims of toms → TOM, snare rim →
  CROSS_STICK, ride → RIDE, crash/splash → CRASH, hi-hat → HIHAT; pedal 44 → ignored).
- `input/MidiParser`: turns raw MIDI bytes (with running status) into note-on events
  (velocity 0 = note off, ignored). Real-time bytes are skipped.
- `input/OnsetDetector(sampleRate)`: fed consecutive mono float chunks; high-pass filter
  (~60 Hz) → 1 ms peak envelope → adaptive noise floor. An onset fires when the envelope
  crosses `max(minLevel, floor × 4)` and has risen ≥ 2× over the last 10 ms; its level is the
  peak in the next 10 ms; then 35 ms of refractory time (a flam's two notes merge into one hit).
- `input/ClickGate`: drops metronome bleed in mic mode. It learns the loudest onset heard within
  ±30 ms of each count-in click (the player is told not to play during the count-in); later, an
  onset within ±30 ms of a click must be louder than 1.5 × that level. With headphones the
  learnt level is 0 and nothing is dropped.
- `Scorer` gets a **real-drum mode** (`Scorer(timeline, realDrums = true)`):
  - hand is ignored: a hit matches the earliest open note in the window; no WRONG_HAND;
  - with an `instrument`, the note's `pad` must match (pad groups: hi-hat, ride/cowbell, crash,
    snare/cross-stick, tom, kick) or it is judged `WRONG_PAD` (same points as wrong hand);
  - grace notes are not scored; hits during a matched note's grace span or buzz length are
    absorbed (not extra). A flam's offset is the distance to the nearest of its grace and main
    times;
  - each matched note keeps its level; `finish()` judges accents: threshold = 1.25 × median
    level of the matched unaccented notes; an accented note is right if ≥ threshold, an
    unaccented one if below. Result: `accentsRight / accentsTotal` (null when the pattern has
    no accents or no unaccented notes). Accents are reported, not mixed into the accuracy.
- `NoteEvent` gets `pad: Instrument? = null`. `GrooveTimeline.build(groove, bpm, bars, perPad)`
  turns a beat into a `Timeline`: one-bar count-in, a click per beat, notes for every hit
  (`perPad`, MIDI) or one note per step with any hit (mic). Ghost notes are kept: they are part
  of the beat.

### App

- **Settings**: `inputSource` (SCREEN, MICROPHONE, MIDI) and one calibration offset per source
  (the existing key stays the screen one). The Settings screen gets a "Play on" choice, the MIDI
  device status and the microphone permission request.
- `input/DrumInput`: `start(onHit: (nanoTime, level, instrument?) -> Unit)`, `stop()`.
  - `MidiDrumInput`: opens the first MIDI device with an output port, parses with `MidiParser`
    and `NitroMidi`, uses the message timestamp.
  - `MicDrumInput`: `AudioRecord` (UNPROCESSED when supported, else VOICE_RECOGNITION, so no
    automatic gain), 48 kHz-ish native rate, float mono, `OnsetDetector` on a reading thread,
    frame → `nanoTime` through `AudioRecord` timestamps.
  - Hits are posted to the main thread and turned into session time with
    `playback.sessionTimeAt(nanoTime) − offset(source)`.
- `RECORD_AUDIO` is declared and requested only when the user picks the microphone.
  `android.software.midi` is declared as an optional feature.
- **Rudiment practice**: with a real-drum source, the on-screen pads are replaced by a panel
  showing what is listening and the last hit; lane flashes use the matched note's hand; the
  results add "Accents: x of y". The mic panel reminds the player to use headphones and to stay
  silent during the count-in.
- **Calibration**: the same screen, played on the chosen source (tap, hit the pad or the kit);
  the offset is saved for that source.
- **Style beats**: a "Practise on my drums" button on each beat (shown when the source is mic or
  MIDI; otherwise it explains how to choose one). Route `groove-practice/{styleId}/{beat}/{bpm}`:
  count-in, 8 bars, the lit kit, bar counter, live accuracy, flashes "early / late / wrong pad",
  then the same results card (not saved to the practice log, which is per rudiment).
- My kit guide text: explain the two ways to connect.

### Errors

No MIDI device → message "Connect the module with a USB OTG cable" and the start button stays
off. Mic permission denied → message and a button to settings. Mic can't open → the existing
audio-error text.

## Tests

Core unit tests for `NitroMidi`, `MidiParser`, `OnsetDetector` (synthetic clicks, noise, flams,
fast 16ths), `ClickGate`, the real-drum `Scorer` (any hand, wrong pad, absorbed graces and
buzzes, accents) and `GrooveTimeline`. On-device checks are the user's (no emulator here).

## Build order

1. Core: NitroMidi, MidiParser · 2. OnsetDetector, ClickGate · 3. Scorer real-drum mode and
accents · 4. GrooveTimeline · 5. Settings, DrumInput (MIDI, mic), manifest · 6. Rudiment practice
and calibration · 7. Style beat practice · 8. Strings, guide, docs, release as a test build.
