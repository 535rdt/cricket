# 🏏 Cricket Scoreboard Android App

A comprehensive, professional, modern Jetpack Compose Cricket Scoring & Team Management application for Android.

---

## 📦 APK Download
Install the latest build on your Android phone:
- 🚀 **[Download Latest APK from GitHub Releases (CricketScoreboard.apk)](https://github.com/535rdt/cricket/releases/download/v1.2.0/CricketScoreboard.apk)**
- 🏷️ **[View GitHub Release v1.2.0](https://github.com/535rdt/cricket/releases/tag/v1.2.0)**
- 📁 **[View CricketScoreboard.apk in Repository Code](https://github.com/535rdt/cricket/blob/main/CricketScoreboard.apk)**

---

## 🚀 Key Features

### 1. 🌐 Live Scoreboard Sharing & Spectator Hub ("Sharing" Bottom Bar Tab)
- **Dedicated "Sharing" Tab in Bottom Bar**:
  - Convenient 1-tap bottom bar navigation item to watch live games from other scoreboards or broadcast your own.
  - Active visual badges (`🔴` for live spectator mode, `⭐` for active broadcasting).
- **Game Shared Window of Another Scoreboard**:
  - Enter the Match Code (e.g. `CRIC-8492`) or Host IP to connect directly to any scorer's scoreboard.
  - Displays real-time streaming game window:
    - Giant live score (`148/3 in 16.2 ov`), CRR, Target, and Required Run Rate.
    - Live striker (`*`) & non-striker cards with runs, balls, 4s, 6s, and avatars.
    - Active bowler with overs, maidens, runs conceded, wickets, and economy.
    - Delivery strip for current over (`[0] [1] [4] [Wd] [6] [W]`).
    - Full batting and bowling scorecards.
    - Auto-syncs every 1.8 seconds in real time!
    - 1-tap disconnect / refresh controls.
- **⚡ Supabase Cloud Integration** *(New!)*:
  - Backed by **Supabase PostgreSQL & Realtime**:
    - **`live_matches`**: Ultra low-latency ball-by-ball match broadcasting.
    - **`saved_teams`**: Cloud team rosters backup & cross-device sync.
    - **`match_history`**: Permanent completed match scorecard archive.
  - **In-App Cloud Settings Dialog**: Enter your own Supabase Project URL & Anon Key with built-in connection tester.
  - **Turnkey SQL Script**: Included [`supabase_schema.sql`](supabase_schema.sql) ready to run in Supabase SQL Editor.
- **Dual Connectivity Modes**:
  1. **Local Wi-Fi / Hotspot Server**: Built-in embedded HTTP server (`http://<device-ip>:8080`) works offline with zero internet access over hotspot.
  2. **Supabase & Cloud Broadcast**: Instant real-time live sync over internet with automated auto-refresh polling every 1.8 seconds.
- **Broadcast Your Own Scoreboard**:
  - Dedicated tab to start/stop broadcasting your active game.
  - Generates 6-character Match Code, browser web link, and WhatsApp share messages.

---

### 2. 📸 Player Photos & Avatars
- **Photo Upload & Camera Capture**:
  - Add photos to any player via phone Gallery or Camera.
  - Automatic circular crop and compression with persistent local device storage.
- **Visual Player Identification**:
  - Player photos appear on the **Live Striker & Non-Striker card**.
  - Avatars shown on **Bowler cards**, **Scorecard Batting/Bowling tables**, and **Team Squad lists**.
  - Fallback to stylish initial initials avatar if no photo is attached.

---

### 3. 👥 Dedicated Teams & Squads Management
- **Saved Teams Library**:
  - Save custom teams with custom player rosters, Captain (**C**), and Wicketkeeper (**WK**) roles.
  - Clean library interface to view, edit, update, or delete saved teams.
- **Instant Rematch ("🔁 Use Teams from Last Match")**:
  - 1-tap button to load both teams and rosters directly from the previous match.
  - Seamless match recreation without re-typing player names.

---

### 4. ⚙️ Flexible Match Setup
- **Squad Customization**:
  - Support for 2 to 30 players per side (ideal for 2v2 box cricket, gully/turf cricket, and standard 11-player matches).
  - Quick **"⇄ Swap Team 1 & 2"** home/away toggle.
- **Match Format & Overs**:
  - Quick presets: 5, 10, 15, 20 (T20), 50 (ODI), or custom overs.
  - Automatic maximum overs per bowler limit calculation (`overs / 5`).
- **Toss Engine**:
  - Select toss winner and decision (Bat First / Bowl First).
  - Automatically configures batting and bowling sides for 1st and 2nd innings.
- **Configurable Extras**:
  - Custom Wide runs (1 or 2).
  - Custom No-Ball runs (1 or 2).

---

### 5. 🏏 Live Scoring Engine
- **Hero Dashboard Banner**:
  - Large live score display (`148/3` in `16.2 / 20.0` ov).
  - Real-time **Current Run Rate (CRR)** & **Projected Score**.
  - For 2nd Innings: Live **Target**, runs needed, balls remaining, and **Required Run Rate (RRR)**.
- **Active Batsmen & Bowler Cards**:
  - Striker badge (`🏏 *`), Runs, Balls, 4s, 6s, and Strike Rate.
  - Bowler figures: Overs, Maidens, Runs conceded, Wickets, and Economy Rate.
  - Strike rotation button (`⇄ Strike Swap`) and mid-over bowler substitution support.
- **Over Deliveries Strip**:
  - Color-coded badges for every delivery in the current over (`[0] [1] [4] [Wd] [6] [W]`).
- **Interactive Scoring Keypad**:
  - Run buttons: `0` (Dot), `1`, `2`, `3`, `4` (Gold boundary), `6` (Emerald maximum).
  - Extras: `WD` (Wide), `NB` (No Ball), `LB` (Leg Bye), `BYE` (Bye).
  - Support for combined events (e.g. Wide + overthrows, No-ball + boundary).
- **Dismissal Engine (Wicket Dialog)**:
  - Modes: Bowled, Caught (with fielder selector), Run Out, LBW, Stumped, Hit Wicket, Retired Hurt.
  - Automatic bench player selector for incoming batsman.
- **Multi-Level Undo (`↩️ Undo`)**:
  - Safely revert accidental balls, restoring batsman runs, bowler stats, and strike positions.
- **Auto-Transitions & Result Calculation**:
  - Automatic strike change at end of over (6 legal deliveries).
  - Prompt for new bowler (preventing consecutive overs by the same bowler).
  - Innings break and target calculator.
  - Match winner calculation and automated **Player of the Match** award.

---

### 6. 📋 Full Scorecard & Match Archive
- Toggle between **1st Innings** and **2nd Innings** scorecards.
- Complete Batting stats (Runs, Balls, 4s, 6s, SR, dismissal info) and Bowling stats (O, M, R, W, Econ).
- Fall of Wickets (FOW) timeline and Did Not Bat list.
- **Match History**: Persistent match storage across app restarts.
- **🖼️ High-Res PNG & JPG Match Report Sharing** *(New!)*:
  - Generates broadcast-quality, high-resolution (1080px) graphic scorecards with dark stadium gradients, gold & emerald accents, and player avatars.
  - Choose between **PNG (Lossless HD)** and **JPG (Compact)** formats.
  - Interactive dialog with real-time thumbnail preview.
  - 1-Tap **Share Image** to WhatsApp, Instagram, Telegram, SMS, or email with scorecard caption attached.
  - **Save to Gallery**: Saves directly to your phone's `Pictures/CricketScoreboard` album.
  - Optional fallback to share plain text summary.

---

## 🛠️ Tech Stack & Architecture
- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose with Material 3 Design
- **Architecture**: MVVM (Model-View-ViewModel) + StateFlow
- **Async & Networking**: Kotlin Coroutines, Ktor Client, NanoHTTPD / embedded local server
- **Storage**: SharedPreferences & Android Internal Storage (JSON serialization)
- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 15 (API 35)

---

## 📲 How to Install & Run

### Option 1: Direct APK Install
1. Copy `CricketScoreboard.apk` to your Android device via USB cable, WhatsApp, Google Drive, or Bluetooth.
2. Tap the APK file on your device and select **Install** (allow installation from unknown sources if prompted).

### Option 2: Install via ADB
```powershell
adb install -r CricketScoreboard.apk
```

### Option 3: Build from Source
```powershell
cd CricketScoreboard
./gradlew assembleDebug
```
The output APK will be located at `CricketScoreboard/app/build/outputs/apk/debug/app-debug.apk`.
