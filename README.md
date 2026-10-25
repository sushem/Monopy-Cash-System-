# Monopy Cash System

An Android app for tracking the bank and every player's cash during a game of Monopoly —
no paper money, no arguments about who paid whom.

## What it does

- **One row per account.** The bank sits at the top, players below, each row showing the
  token colour, name and current cash.
- **Tap a row** to open that account: big balance, add/subtract controls and the full
  history of every movement.
- **Add or subtract cash** with a numeric field plus quick-tap amounts (50, 100, 500 out
  of the box, editable in Settings).
- **Optional counterparty.** Leave it on "Nobody" for a plain balance adjustment, or pick
  the bank/another player and the same amount is recorded on both sides in one tap.
- **Loans from the bank**, counted separately from cash. "Take loan" moves cash out of the
  bank and adds the same amount to what the player owes; "Repay" does the reverse and is
  capped at the outstanding balance. The account screen shows the loan and the net position
  (cash minus loan), and the list row flags anyone who owes money.
- **Properties per player**, each with a name and the houses standing on it (0-4 houses,
  or a hotel at 5). Add, rename and remove them from the player's own screen, or step the
  house count up and down straight from the row.
- **Editable name and colour** for players and for the bank, from the pencil icon in the
  account screen. Players can also be removed there.
- **Undo** any entry from the history — it reverses the amount and drops the row, including
  the loan side of a borrow or repayment.
- **Add players** with a starting amount (default $1,500), optionally withdrawn from the
  bank so the totals stay honest.
- **Quick transfer** from the button beside "Add player": two scroll wheels pick who pays
  and who gets paid, the amount takes the same quick shortcuts, and one tap sends it.
- **Settings** (overflow menu) chooses the currency symbol used everywhere in the app, and
  adds or removes the quick cash shortcuts.
- **New game** from the overflow menu resets the bank ($20,580) and clears all players.
- Everything is stored on the device, so a game survives closing the app.

## The board

A second tab plays the Marathi **Business — नवा व्यापार** set from the box photos.

- **40 squares** around the board: eight colour groups of Indian cities, four transports
  (रेल्वे, मोटर बोट, बस, एअर इंडिया), two utilities (इलेक्ट्रिक कंपनी, पाणी खाते), the two
  resorts (SPORTS CLUB, WATER WORLD), two taxes, GO, तुरुंग, विश्रांती and तुरुंगात जा.
- **Title-deed cards** — tap any square for its card: price, the full rent ladder up to a
  hotel, what a house costs, and who owns it.
- **सुवर्ण संधी** — a 16-card deck, drawn when you land on a "?" square and applied for you.
- **Dice** with real pip faces. Rolling moves the token and settles everything that
  follows: salary for passing GO, buy-or-pass on an unowned square, rent paid straight to
  the owner, doubles rolling again, three doubles sending you to jail, bail, and taxes.
- **Houses** go up from the title-deed card once you hold a whole colour group.
- Every move is written to the table log, and all of it is cash in the same accounts the
  Cash tab tracks, so the two always agree.

### A note on the numbers

The square names come from your photos. The rent ladders printed on the cards are too
small to read in them, so they are **derived on Monopoly's proportions** and rounded —
they play sensibly but they are not your set's exact figures. Everything (names, prices,
rents, house costs) sits in one file, `app/src/main/java/com/monopy/cash/model/Board.kt`;
correct a value there and the board, the cards and rent collection all follow.

## Playing together

One device hosts and the others join it over the same Wi-Fi — no account, no internet, no
server to run.

1. On the host: **Wi-Fi icon → Host the game**. It shows an address like `192.168.1.5:8742`.
2. On every other device: **Wi-Fi icon →** type your name and that address **→ Join**.

A joining device is seated as a player (with ₹1,500 from the bank) and can play its own
turn; the host applies every rule and hands the whole game back out, so no device drifts
from the others. While joined, players, properties and the board are managed by the host;
your own cash actions and turns still work from your device.

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
