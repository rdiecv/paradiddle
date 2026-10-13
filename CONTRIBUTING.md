# Contributing to Paradiddle

Thanks for wanting to help! Paradiddle is a small, friendly project, and every kind of help
counts: reporting a bug, fixing a rudiment, translating, or writing code.

By taking part you agree to follow our [Code of Conduct](CODE_OF_CONDUCT.md).

## Ways to help

| I want to… | How |
|---|---|
| Report a bug | Open a [bug report](https://github.com/rdiecv/paradiddle/issues/new?template=bug_report.yml) |
| Suggest an idea | Open a [feature request](https://github.com/rdiecv/paradiddle/issues/new?template=feature_request.yml) |
| Fix a rudiment | Open a [rudiment correction](https://github.com/rdiecv/paradiddle/issues/new?template=rudiment_correction.yml), or send a pull request (see below) |
| Translate | See [Translations](#translations) |
| Write code | Pick an issue (look for `good first issue`), say you're on it, then follow [Pull requests](#pull-requests) |
| Report a security problem | **Don't open a public issue.** See [SECURITY.md](SECURITY.md) |

For bigger changes, please open an issue first so we can agree on the approach before you
spend time on it.

## Development setup

You need **JDK 17** and the **Android SDK** (platform 35). Android Studio brings both, or use the
command line tools.

```sh
git clone https://github.com/rdiecv/paradiddle.git
cd paradiddle
./gradlew test lint assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk   # optional, on a phone or emulator
```

If Gradle can't find the SDK, create `local.properties` with `sdk.dir=/path/to/Android/Sdk`.

## How the code is organised

| Module | Contents | Rules |
|---|---|---|
| `core/` | Plain Kotlin, no Android: rudiment model and notation parser, timeline, scorer, calibration, sound synthesis, mixer | All logic goes here, with unit tests |
| `app/` | Android: audio engine (`AudioTrack`), Room database, settings (DataStore), Jetpack Compose UI | Keep it thin: UI and platform glue |

The **timeline** is the single source of truth: audio, drawing and scoring all read it, so they
can't drift apart. The full design is in
[`docs/superpowers/specs/2026-10-13-paradiddle-design.md`](docs/superpowers/specs/2026-10-13-paradiddle-design.md).

## Project rules

These keep Paradiddle welcome on F-Droid and respectful of its users:

- **No proprietary dependencies** (no Google Play Services, Firebase, Crashlytics…).
- **No network access, ads, analytics or tracking.**
- **No new Android permissions** without discussing it in an issue first.
- Everything the user sees is translated: add new strings to `values/strings.xml` **and**
  `values-es/strings.xml`.
- New logic comes with unit tests in `:core`.

## Code style

- [Kotlin official code style](https://kotlinlang.org/docs/coding-conventions.html); the
  `.editorconfig` file sets it up in Android Studio and most editors.
- Match the surrounding code: naming, comment density, small focused files.
- New source files start with the license header:

  ```kotlin
  // SPDX-FileCopyrightText: 2026 The Paradiddle authors
  // SPDX-License-Identifier: GPL-3.0-or-later
  ```

## Rudiment corrections

Rudiments live in [`app/src/main/assets/rudiments.json`](app/src/main/assets/rudiments.json). The
notation is explained in the [README](README.md#rudiment-data). After editing, run
`./gradlew :core:test`: it checks that every pattern is valid, that all 40 PAS numbers are there,
and that each rudiment scores 100 % when played perfectly. Please mention your source (PAS sheet,
book, video) in the pull request.

## Translations

To add a language, for example French (`fr`):

1. Copy `app/src/main/res/values/strings.xml` to `app/src/main/res/values-fr/strings.xml` and
   translate the texts. Leave the `%1$d`-style placeholders as they are. Rudiment names stay in
   English (it's what drummers use everywhere).
2. Add `<locale android:name="fr" />` to `app/src/main/res/xml/locales_config.xml`.
3. Optionally, translate the store texts: copy `fastlane/metadata/android/en-US/` to
   `fastlane/metadata/android/fr-FR/`.

## Commits

- One topic per commit, with a short first line (about 70 characters max) like
  `Scorer: count flat flams as hits`, plus a few lines of detail if needed.
- English or Spanish are both fine.

## Pull requests

1. Fork the repo and create a branch from `main` (e.g. `fix/flam-drag-pattern`).
2. Make your change, with tests where it makes sense.
3. Run `./gradlew test lint assembleDebug`. CI runs the same and must pass.
4. Open the pull request and fill in the template. Link the issue it fixes (`Fixes #12`).
5. A maintainer will review it. Small follow-up changes are normal; please don't take them
   personally. PRs are usually squash-merged.

## License of contributions

Paradiddle is licensed under the [GPL-3.0-or-later](LICENSE). By sending a contribution, you agree
that it's released under the same license, and that you have the right to contribute it.

## Releasing (maintainers)

1. Update `versionCode` (+1) and `versionName` in `app/build.gradle.kts`
   ([Semantic Versioning](https://semver.org/)).
2. Move the "Unreleased" notes in `CHANGELOG.md` to the new version, and write the store
   changelog in `fastlane/metadata/android/{en-US,es-ES}/changelogs/<versionCode>.txt`
   (max 500 characters).
3. Commit, then tag and push: `git tag v1.2.0 && git push origin v1.2.0`. A tag with a suffix
   such as `v1.2.0-beta1` is published as a pre-release.
4. The **Release** workflow tests, builds and signs the APK and publishes the GitHub release with
   its SHA-256 checksum. F-Droid picks up the new tag on its own and builds its own copy.

> **Current status:** no release key has been set up yet. Until it is, the workflow skips signing
> and publishing with a warning, and test builds are uploaded by hand, signed with the
> maintainer's debug key.

The signing key lives only in the repository secrets (`RELEASE_KEYSTORE_BASE64`,
`RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`) and in the
maintainer's offline backup. **Never commit a keystore or its passwords.** If the key is lost,
existing installs can't be updated.
