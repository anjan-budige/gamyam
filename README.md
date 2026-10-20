<div align="center">

<img src="https://github.com/anjan-budige/gamyam/raw/main/app/src/main/res/drawable/gamyam.png" width="120" alt="Gamyam logo" />

# Gamyam

### 🚌 Live TSRTC bus tracking — a *Where-is-my-Train* experience, rebuilt for Telangana State buses.

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white&style=for-the-badge)](#download)
[![Language](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white&style=for-the-badge)](#tech-stack)
[![UI](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](#tech-stack)
[![minSdk](https://img.shields.io/badge/minSdk-24-22C55E?style=for-the-badge)](#build)

<a href="https://github.com/anjan-budige/gamyam/raw/main/gamyam.apk">
  <img src="https://img.shields.io/badge/⬇%20Download-APK-1448A6?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" />
</a>

</div>

---

> **⚖️ Unofficial clone — educational project.**
> Gamyam is an independent, open-source re-imagining of a live transit tracker. It is **not affiliated with or endorsed by TSRTC, MapmyIndia, or the "Where is my Train" app.**
> It **only reads public transit data the same way the official client does** — bus positions, routes, stops and schedules — and does **not** modify, scrape-private, bypass or harm any system or service.

---

## ✨ Features

<table>
<tr>
<td width="50%" valign="top">

### 📍 Live bus tracker
Real-time position on a custom route timeline — **current bus, next stop, ETA** and states like *Approaching · Arrived · Departed · Before time*.

</td>
<td width="50%" valign="top">

### 🔎 Smart search
Find buses by **service ID, vehicle / route number, reservation, or From → To** stops across **District · City · Airport** tabs.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 🗺️ Nearby stops
Uses your location to surface the closest stops, with **instant offline suggestions** from bundled city & stop datasets.

</td>
<td width="50%" valign="top">

### ⏰ Bus reminders
Schedule a **"Gamyam bus reminder"** alarm a few minutes before your bus arrives, via the system clock.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 🧭 Trip details
Full stop list, driver, depot, destination and duration for any planned or running service.

</td>
<td width="50%" valign="top">

### 💬 Feedback & polish
In-app feedback straight to the developer, **Light / Dark / System** themes, splash onboarding, Material 3 throughout.

</td>
</tr>
</table>

##  Tech stack

| Area | Choice |
| :-- | :-- |
| **Language** | Kotlin |
| **UI** | Jetpack Compose · Material 3 · custom `Canvas` timeline |
| **Navigation** | AndroidX Navigation 3 |
| **Async** | Coroutines + Flow |
| **Network** | Retrofit · OkHttp · kotlinx.serialization |
| **Auth** | OAuth2 client-credentials with auto-refresh + 401 retry |
| **Storage** | Jetpack DataStore + bundled JSON assets |
| **Data** | TSRTC public transit API (MapmyIndia gateway) |
| **Build** | Gradle (Kotlin DSL) · minSdk 24 · target 36 |

## ❓ FAQ

**Is this the official TSRTC app?**
No. Gamyam is an independent, unofficial client built for learning. It just reads the same public transit data.

**Does it track trains?**
No — it's for **buses**. The *experience* is inspired by *Where is my Train*, reimagined for TSRTC city, district and airport services.

**Do I need an account or API key to use it?**
No. Install the APK and start searching. Credentials are only needed if you build the app from source.

**Will it drain my battery or data?**
It only polls the network when you open a live tracker, and stop/city data ships inside the app for fast, low-data search.

**Is my location safe?**
Your location is used only to fetch nearby stops and isn't stored or shared beyond that request.

## 📥 Download

**App:** <https://github.com/anjan-budige/gamyam>

**APK (direct):** <https://github.com/anjan-budige/gamyam/raw/main/gamyam.apk>

<div align="center">
<a href="https://github.com/anjan-budige/gamyam/raw/main/gamyam.apk">
  <img src="https://img.shields.io/badge/⬇%20Download-APK-1448A6?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" />
</a>
</div>

## 🔨 Build from source

```bash
git clone https://github.com/anjan-budige/gamyam.git
cd gamyam
# add your credentials to local.properties (gitignored), then:
./gradlew assembleRelease      # → app/build/outputs/apk/release/app-release.apk
```

`local.properties`, `*.jks` and `*.keystore` are gitignored, so keys and signing material never reach the repo.

## 👤 Credits & contact

Developed by **Anjan** · UI concept inspired by *Where is my Train* · transit data courtesy of TSRTC's public API.

If you have any queries, please email **anjanbudige@gmail.com**.
