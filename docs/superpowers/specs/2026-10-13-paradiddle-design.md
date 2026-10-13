# Paradiddle — design spec (v1)

Date: 2026-10-13 · Status: approved in chat (sections 1–2 reviewed; the user asked to proceed with the rest)

## 1. Goal

A free/open source Android app for learning the **40 PAS drum rudiments**, for the author,
friends and F-Droid users. You study each rudiment (notation, sticking, example sound),
then practise it by tapping two on-screen pads (left/right hand) against a metronome, and
the app scores timing and sticking and remembers your progress.

Decisions taken with the user:

| Topic | Decision |
|---|---|
| v1 focus | Rudiments guide. Song recommendations and other exercises → later versions |
| Feedback | Guide + on-screen tapping with timing/sticking score (no microphone) |
| Content | All 40 PAS rudiments |
| Grace notes / buzz | Tolerant scoring (see §5) |
| Progress | Local only: best clean BPM, accuracy history, practice time, practice log |
| Languages | English + Spanish |
| License | GPL-3.0-or-later |
| Name / package | Paradiddle · `io.github.rdiecv.paradiddle` · repo `github.com/rdiecv/paradiddle` |
| Stack | Kotlin + Jetpack Compose, audio in pure Kotlin (`AudioTrack`), Room, DataStore |
| Min SDK | 26 (Android 8.0). Target/compile SDK 35 |
| Sounds | Synthesised in code (no sample files) |

Constraints: F-Droid inclusion → no Google Play Services, Firebase, analytics, ads or
network access. The app requests **no permissions**.

Out of scope for v1: songs, non-rudiment exercises, microphone input, strict grace-note
mode, accounts/sync, cloud backup.

## 2. Architecture

Two Gradle modules:

- **`:core`** — plain Kotlin/JVM, no Android. All musical logic, fast unit tests.
  - `model`: `Hand`, `StrokeKind`, `Stroke`, `Rudiment`, `Category`
  - `StrokeNotation`: parses the compact sticking notation (§3)
  - `RudimentCatalog`: parses `rudiments.json` (kotlinx.serialization)
  - `TimelineBuilder`: rudiment + BPM + repetitions → `Timeline` (count-in clicks + `NoteEvent`s)
  - `Scorer`: matches taps to expected notes → live `Judgement`s and a final `SessionResult`
  - `audio.Synth`: synthesises click / snare / grace / buzz sounds as `FloatArray`
  - `audio.Mixer`: mixes timed sound events into output buffers, sample-accurately
  - `Calibration`: computes the latency offset from calibration taps
- **`:app`** — Android.
  - `audio/AudioEngine`: streams `Mixer` output to a low-latency float `AudioTrack` on a
    dedicated thread; maps audio frames ↔ `System.nanoTime` with `AudioTimestamp`
  - `data/`: rudiments loader (assets), Room DB (`practice_session`), DataStore settings
  - `ui/`: Compose screens + custom `NotationView` and `LanesView`

The **Timeline** is the single source of truth: audio, visuals and scoring all read the
same list of events.

```
rudiments.json → Rudiment → TimelineBuilder → Timeline
                                              ├→ Mixer/AudioEngine (clicks + sounds)
                                              ├→ LanesView         (falling notes)
                                              └→ Scorer ← TapEvent(hand, sessionTime − calibration)
                                                   ↓
                                    live feedback + SessionResult → Room
```

## 3. Rudiment data

`app/src/main/assets/rudiments.json`, one entry per rudiment:

```json
{ "id": "flam_tap", "pas": 22, "name": "Flam Tap", "category": "flam",
  "grid": 4, "strokes": "lR> R rL> L" }
```

- `category`: `roll | diddle | flam | drag`
- `grid`: slots per beat — 1 (quarters), 2 (8ths), 3 (8th triplets), 4 (16ths),
  6 (16th sextuplets), 8 (32nds)
- `strokes` tokens (space separated):
  - `R` / `L` — stroke, 1 slot
  - suffix `>` — accent
  - lowercase prefix `r`/`l` (one or more) — grace notes attached to the stroke, no slot time
  - suffix `z` — buzz stroke
  - suffix `:n` — lasts n slots
  - `-` (optionally `-:n`) — rest
- The whole pattern must last a whole number of beats (validated).
- Rudiment names stay in English in both languages (standard usage). Practice tips are per
  category (roll, diddle, flam, drag) and translated in `strings.xml` (`tip_<category>`).
- Patterns are written in their usual "open" (slow) rudimental form. They are data: a
  drummer can correct them without touching code.

## 4. Timing and audio

- Practice length is counted in **repetitions** of the pattern (4 / 8 / 16, default 8)
  because patterns have different lengths (1–6 beats). One count-in of 4 clicks precedes.
- Session time 0 = first audio frame of the session. A note at beat position `b` plays at
  `countIn + b · 60/BPM` seconds.
- Metronome click on every beat; higher pitch on the first beat of each repetition.
- **Listen** mode: clicks + synthesised snare playing the pattern (accent 1.0, normal 0.55,
  grace 0.3, buzz = bursts every ~25 ms). **Practice** mode: clicks, plus optional quiet
  guide sound (setting).
- `AudioEngine`: `AudioTrack` float PCM at the device's native rate,
  `PERFORMANCE_MODE_LOW_LATENCY`, written in small blocking chunks by a render thread.
  Frame→time mapping from `AudioTrack.getTimestamp`: a frame `f` is heard at
  `ts.nanoTime + (f − ts.framePosition)·1e9/sampleRate`. Before the first timestamp is
  available the engine estimates it from the write position and buffer size.
- Taps: Compose pointer input, `PointerInputChange.uptimeMillis` converted to the
  `System.nanoTime` base (offset measured once per session), then to session time, then
  minus the calibration offset.
- **Calibration**: 16 clicks at 90 BPM, user taps along; the first 4 are ignored; offset =
  median(tap − click) of the rest, if at least 8 taps matched within ±250 ms. Stored in
  settings; the list screen shows a banner until calibration has been done once.
- Visuals: `LanesView` redraws every frame (`withFrameNanos`), computing "now" in session
  time from the same mapping, so notes reach the hit line exactly when they sound.

## 5. Scoring

Expected notes = primary strokes (with hand, time, accent, buzz) + grace notes (hand,
attached to a primary). Grace notes do not have scored timing (tolerant mode).

- Primary window `W = min(100 ms, 0.45 · slotDuration)`. Grace window: from
  `t_main − 180 ms` to `t_main + 10 ms`.
- Taps are processed in time order. For a tap of hand `h`, candidates are unmatched notes
  whose window contains the tap:
  1. same-hand notes (primary or grace) — pick the one with the earliest expected time;
  2. otherwise the nearest other-hand primary → **wrong hand** (note consumed);
  3. otherwise → **extra tap**.
- When a primary's window closes unmatched → **missed**. Unmatched grace → missed grace.
- Primary judgement: **perfect** `|dt| ≤ min(25 ms, W/2)`, **good** `|dt| ≤ min(50 ms, W)`,
  **ok** otherwise inside the window.
- Points: perfect 1.0, good 0.75, ok 0.5, wrong hand 0.25, missed 0. Grace: hit 0.5 / 0
  (max 0.5). Extra tap: −0.25. Accuracy = max(0, Σ points) / Σ max points · 100.
- Session stats: accuracy, counts per judgement, mean offset (ms, tap − expected:
  negative = rushing, positive = dragging; |mean| < 10 ms = steady), spread (std dev of
  offsets), wrong-hand, missed, extra taps.
- Missed-note detection lags 60 ms behind "now" so taps delivered slightly late by the
  input system can still match. Taps during the count-in are ignored.
- **Clean** = accuracy ≥ 90 %. Suggestion: ≥ 90 % → try +4 BPM; < 70 % → try −8 BPM;
  otherwise keep the tempo.

## 6. Screens

1. **Rudiments** (home): grouped by category, each row: PAS number, name, best clean BPM.
   Actions: Log, Settings. Calibration banner until calibrated.
2. **Rudiment detail**: notation (`NotationView`), sticking, tip, stats (best clean BPM,
   last accuracy, total time), tempo (40–240, ±4 buttons + slider), repetitions (4/8/16),
   buttons Listen / Practice. Listen opens the practice screen in listen mode (pattern sound,
   lanes and notation highlight, no pads, no scoring, nothing saved).
3. **Practice** (portrait): top bar BPM · repetition n/N · live accuracy; notation strip
   with current note highlighted; two falling-note lanes (L left, R right) toward the hit
   line; two big pads. Grace notes = small dots; accents = taller blocks; buzz = "z".
   Hit feedback flashes on the hit line (green perfect, yellow good/ok with early/late
   arrow, red wrong hand/missed). Stop button.
4. **Results** (shown in place on the practice screen when the session ends): big accuracy, counts, tendency (rushing/dragging/steady, consistency),
   suggestion; buttons Retry, Try suggested BPM, Done. The session is saved when it
   completes (a stopped session is not saved).
5. **Calibration** (§4).
6. **Settings**: calibration (run/reset), click volume, guide sound on/off, default
   repetitions, about (version, GPL-3.0, source link text).
7. **Log**: sessions grouped by day with total time per day and overall.

`NotationView` draws a single-line percussion staff: note heads, stems up, beams per beat
according to duration (grid 4/6 → 16ths, 8 → 32nds, 2/3 → 8ths), tuplet number for grid 3
and 6, accent marks, small slashed grace notes, "z" on buzz stems, sticking letters below.

## 7. Data storage

- Room `practice_session`: id, rudimentId, startedAt (epoch ms), bpm, repetitions,
  accuracy, meanOffsetMs, spreadMs, perfect, good, ok, wrongHand, missed, extra,
  durationSec.
- Derived by queries: best clean BPM per rudiment, last accuracy, total time, daily log.
- DataStore: calibrationOffsetMs (nullable = not calibrated), clickVolume, guideSound,
  defaultRepetitions.

## 8. Error handling

- Invalid `rudiments.json` can't ship: a `:core` unit test parses the real file and checks
  all 40 PAS numbers, unique ids, whole-beat lengths.
- Audio: if `AudioTrack` creation fails, the practice screen shows an error message and a
  back button; the rest of the app keeps working.
- App going to background during practice → session stops, not saved.
- Calibration with too few taps → message "try again", nothing stored.

## 9. Testing

- `:core` JUnit tests: notation parser, catalog (real file), timeline times, scorer
  (perfect/early/late/wrong hand/missed/extra/flam/drag/buzz, accuracy formula), mixer
  (sample-exact placement, chunk boundaries), synth (no NaN, peak ≤ 1), calibration.
- `:app`: build + lint + JVM tests for pure helpers. Device testing by hand (APK).
- Command: `./gradlew test lint assembleDebug`.

## 10. F-Droid / repo

- `LICENSE` (GPL-3.0), `README.md` (EN, with build instructions), fastlane metadata in
  `fastlane/metadata/android/{en-US,es-ES}` (title, short/full description, changelog).
- No proprietary dependencies; versionCode/versionName in `app/build.gradle.kts`.
- Release builds unsigned for F-Droid (F-Droid signs).

## 11. Build order

1. Gradle skeleton (wrapper, modules, version catalog), git init.
2. `:core` model + notation parser + catalog + tests.
3. `rudiments.json` with all 40 rudiments + catalog test.
4. Timeline + scorer + calibration + tests.
5. Synth + mixer + tests.
6. `:app` data layer (assets loader, Room, DataStore).
7. AudioEngine.
8. UI: theme, navigation, list, detail + NotationView, practice + LanesView, results,
   calibration, settings, log. Strings EN/ES.
9. F-Droid metadata, README, LICENSE, icon. `./gradlew test lint assembleDebug`.
