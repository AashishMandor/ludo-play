# Ludo Play — Android Studio Project

A native Android offline Ludo game built with Kotlin and a custom Canvas view. It supports 2, 3, or 4 local players taking turns on one phone.

## Gameplay included

- 2–4 local players (pass-and-play)
- Four coloured home bases and four tokens per player
- Dice rolls from 1 to 6
- A token leaves the yard on a 6
- Shared track, coloured home lanes, exact finish, and winner detection
- Captures on non-safe track cells
- Safe cells
- Extra turn after rolling a 6, capturing, or reaching home
- New Game and player-count controls

## Build online without Android Studio

See [ONLINE_BUILD_GUIDE.md](ONLINE_BUILD_GUIDE.md). The included `.github/workflows/android-build.yml` builds a debug APK using GitHub Actions in the cloud. You only need a GitHub account and a browser to trigger the build and download the APK.

## Open and run locally (optional)

1. Install Android Studio.
2. Open this `ludo_android` folder.
3. Allow Gradle sync. Android Studio may prompt you to install Android SDK Platform 35.
4. Connect an Android phone with USB debugging enabled, or create an emulator.
5. Press **Run** (green play button).

## Notes

- The app uses package `com.example.ludoplay`, supports Android 6.0 (API 23) and above, and is locked to portrait orientation.
- This first version is offline local multiplayer. It does not include online matchmaking, accounts, chat, ads, or an online server.
- The implementation uses a Ludo-style board and is not affiliated with or a reproduction of the Ludo King brand.
