<div align="center">

# Gamyam

**Live TSRTC bus tracking — a "Where is my Train" experience, rebuilt for Telangana State buses.**

Jetpack Compose · Material 3 · Navigation 3 · Kotlin

</div>

---

> **⚖️ Unofficial clone — educational project.**
> Gamyam is an independent, open-source re-imagining of a live transit tracker. It is **not affiliated with, endorsed by, or connected to TSRTC, MapmyIndia, or the "Where is my Train" app.**
>
> This app **only reads public, authenticated API data the same way the official service does** — bus positions, routes, stops and schedules. It does **not** modify, scrape-private, bypass, or harm any system, database, or service. All requests are standard read-only lookups made with a normal client token. Built for learning and personal use.

---

## What is this?

Gamyam answers one question instantly: **"where is my bus right now, and when does it reach my stop?"**

It's a clean, one-hand, tap-minimal Android client for TSRTC city, district and airport bus services — with a live route timeline, arrival states, and pre-arrival reminders, in the spirit of the popular *Where is my Train* UX but for buses.

## Features

**Live bus tracker**
- Real-time position drawn on a custom Compose route timeline (no heavy map SDK).
- Current bus, next stop, ETA, and states: *Approaching · Arrived · Departed · Before time · No delay · Delay unknown*.
- Trip detail with stops, driver, depot, destination and duration.

**Smart search** — three tabs: **District · City · Airport**
- Find buses by service ID, bus/vehicle number, route number, reservation, or **From → To** stops.
- **Nearby stops** using device location, with instant offline suggestions from bundled stop/city datasets.
- Search history saved locally for quick re-access.

**Reminders**
- Schedule a **"Gamyam bus reminder"** alarm a few minutes before arrival via the system clock.

**Feedback**
- In-app feedback delivered straight to the developer's Telegram.

**Polish**
- Splash onboarding, Light / Dark / System theme, Material 3 throughout, helpline & about.

## Tech stack

| Area | Choice |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3, custom `Canvas` timeline |
| Navigation | AndroidX **Navigation 3** (`NavDisplay` + `entryProvider`) |
| Async | Coroutines + Flow |
| Networking | Retrofit + OkHttp, kotlinx.serialization |
| Auth | OAuth2 client-credentials token with auto-refresh + 401 retry |
| Storage | Jetpack DataStore (preferences) + bundled JSON assets |
| Data source | TSRTC public transit API (via MapmyIndia gateway) |
| Feedback | Telegram Bot API |
| Build | Gradle (Kotlin DSL) · minSdk 24 · target/compileSdk 36 |

## Architecture

```
app/src/main/java/com/gamyam/app/
├── data/        ApiClient · AuthManager · DataRepository · StopRepository
│                AppSettingsRepository · SearchHistoryRepository · FeedbackSender
├── ui/
│   ├── splash/  SplashScreen + SplashViewModel
│   ├── main/    MainScreen (search + tabs) + MainScreenViewModel + SearchViewModel
│   ├── search/  Service / Route / Vehicle / Reservation search result screens
│   └── tracker/ LiveBusTrackerScreen · trip detail · from-to & route results
├── theme/       Color · Type · Theme (M3, light/dark/system)
└── Navigation.kt, MainActivity.kt
```

## Getting started

```bash
git clone https://github.com/anjan-budige/gamyam.git
cd gamyam
# open in Android Studio (Ladybug+) and sync
```

Network features need credentials that are **not** in the repo (kept out on purpose). Create `local.properties` at the project root:

```properties
# --- MapmyIndia / TSRTC gateway (OAuth) ---
CLIENT_ID=your_client_id
CLIENT_SECRET=your_c...e
# --- optional ---
MAP_SDK_KEY=***
# --- Telegram feedback (leave blank to disable feedback) ---
TELEGRAM_BOT_TOKEN=***
TELEGRAM_CHAT_ID=your_chat_id
# --- release signing (optional, omit for debug) ---
RELEASE_STORE_FILE=
RELEASE_STORE_PASSWORD=
RELEASE_KE...LAS=
RELEASE_KEY_PASSWORD=
***

Network features need these credentials, and the release build also needs the signing block. `local.properties`, `*.jks` and `*.keystore` are gitignored so secrets never reach the repo.

## Build

```bash
./gradlew assembleRelease      # signed APK → app/build/outputs/apk/release/app-release.apk
```

This produces the signed **release** build that users install.

## Download

**App:** <https://github.com/anjan-budige/gamyam>

**APK (direct):** <https://github.com/anjan-budige/gamyam/raw/main/gamyam.apk>

## Credits

Developed by **Anjan** · UI concept inspired by *Where is my Train* · transit data courtesy of TSRTC's public API.

## Contact

If you have any queries, please email **anjanbudige@gmail.com**.
