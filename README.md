# Monopy Cash System

An Android app for tracking the bank and every player's cash during a game of Monopoly —
no paper money, no arguments about who paid whom.

## What it does

- **One row per account.** The bank sits at the top, players below, each row showing the
  token colour, name and current cash.
- **Tap a row** to open that account: big balance, add/subtract controls and the full
  history of every movement.
- **Add or subtract cash** with a numeric field plus quick-tap Monopoly denominations
  (1, 5, 10, 20, 50, 100, 500).
- **Optional counterparty.** Leave it on "Nobody" for a plain balance adjustment, or pick
  the bank/another player and the same amount is recorded on both sides in one tap.
- **Editable name and colour** for players and for the bank, from the pencil icon in the
  account screen. Players can also be removed there.
- **Undo** any entry from the history — it reverses the amount and drops the row.
- **Add players** with a starting amount (default $1,500), optionally withdrawn from the
  bank so the totals stay honest.
- **New game** from the overflow menu resets the bank ($20,580) and clears all players.
- Everything is stored on the device, so a game survives closing the app.

## Building an APK

Builds run through GitHub Actions — **Actions → Build APK → Run workflow**, then choose
`debug`, `release` or `both`. The workflow uploads the APKs as run artifacts and publishes
them to a GitHub Release tagged `build-<run number>`:

- `MonopyCash.apk` — release build, ready to install
- `MonopyCash-debug.apk` — debuggable build for testing

### Signing

The release APK is signed so it can be installed directly. By default the workflow
generates a throwaway key for each run, which means **a new build won't install over an
older one** — uninstall first, or set repository secrets to sign every build with the same
key:

| Secret | Purpose |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | Base64 of your `.jks`/`.p12` keystore (`base64 -w0 release.jks`) |
| `MONOPY_KEYSTORE_PASSWORD` | Keystore password (default `monopycash`) |
| `MONOPY_KEY_ALIAS` | Key alias (default `monopy`) |
| `MONOPY_KEY_PASSWORD` | Key password (default `monopycash`) |

When no keystore is present at all (for example a local `gradle assembleRelease`), the
release build falls back to the debug signing key so the APK is still installable.

### Building locally

Requires JDK 17 and the Android SDK (compileSdk 34), with Gradle 8.7:

```bash
gradle assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
gradle assembleRelease   # app/build/outputs/apk/release/app-release.apk
```

## Tech

Kotlin, Jetpack Compose (Material 3), single activity, no database — accounts are
serialised to JSON in shared preferences, exposed as a `StateFlow` from `GameStore`.
