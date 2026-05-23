# 🏆 Trash Patrol — World Cup Edition

## ⚽ Tactical Overview
**Trash Patrol - World Cup Edition** is a native Android application built with Kotlin and Jetpack Compose. Styled as an digital football stadium tactics board, it serves as a high-fidelity field tool to report litter, coordinate virtual referee reviews, dispatch a "Robot Striker" to dispose of the target, and confirm goals (cleanups) on the pitch.

With a midnight-turf color palette, referee penalty cards, and a custom-painted 2D soccer field tracker, this tool transforms municipal garbage cleanup into a competitive sport.

---

## 🎨 Visual Identity & Theme
- **Midnight Dark Arena background** (`#0D0D0D` / `#0F1411`) providing deep, high-contrast visual focus.
- **Glaring Pitch Green accents** (`#00FF7F`) for active goals, successful sweeps, and clean marks.
- **Vibrant Referee Colors**:
  - 🟥 **Red Cards** (`#FF4444`) for immediate rejections, fouls, or network connection timeouts.
  - 🟨 **Yellow Cards** (`#FF8C00`) to highlight caution states, timeouts, and crucial confirmation requests.
- **World Cup Gold Trophy** (`#FFD700`) styling for headings, status banners, and robot indicators.
- **Typography & Icons**: Consistent, bold Material 3 indicators, responsive typography layout, and customized inline sports symbols.

---

## 📱 Core Layouts & Screens

### 1️⃣ The Pitch Playground (Report Screen)
The home kickoff of the app lets field agents snapshot new litter incidents:
- **Glowing Shutter Button**: A large, interactive pulsing soccer ball that opens the default camera.
- **Match Snapshot**: Displays a rounded photo preview bordered in stadium-green trim with quick cancellation triggers.
- **Scouting Kickoff**: Sends a `multipart/form-data` packet to the coordinator including compressed JPEGs (85% quality) and device-level GPS coordinates.
- **VAR Analysis Panel**: Shows a loading indicator tracking "Analyzing photo...".
- **Referee Red Card Penalty**: If rejected, a distinct red player card displays the infraction reason (e.g. *"Image too blurry"* or *"No litter detected"*) alongside a "Try Again" setup.

### 2️⃣ The Match Tracker (Mission Screen)
Once confirmed, the app transitions into the Live Match Tracker to track field activity:
- **VAR State Monitor**: Displays raw server states (e.g., `dispatching`, `awaiting_bridge_callback`) in a monospace console chip to keep developer scouts updated.
- **Live Match Scorecard**: Houses the primary human-readable status, detailing real-time robot operations.
- **2D Custom Football Pitch Progress Canvas**: 
  - A custom-drawn green soccer field featuring crisp white boundary lines, kickoff center-circle, and penalty boxes.
  - Animates a **Robot Striker** (golden cup avatar) and a miniature rolling soccer ball dynamically along the field x-axis using bouncy spring physics as the match climbs from kickoff to goal completion.
- **Scouting Verdict**: Displays classifier evaluations indicating **Litter Type**, **Scouting Confidence %**, and textual **Visual Evidence** details.
- **Match Point (Confirmation Screen)**: When the robot completes its sweep, the state shifts to `awaiting_confirmation_photo`. A flashing yellow button prompts the user to take a cleanup confirmation photo. Once uploaded and certified, a large green success scorecard reports **GOAL DETECTED — CLEANUP VALIDATED ✓**.

### 3️⃣ Tactical Settings (Settings Screen)
Accessible via the gold gear icon in the stadium taskbar:
- **Coordinator URL Configuration**: Customize the backend target URL to adapt to local machines or live ngrok tunnels (default: `https://pachydermatous-alena-hyperbarbarous.ngrok-free.dev`).
- **Test Connection**: Executes an asynchronous ping against the saved coordinator's `/health` endpoint and updates the scorecard dynamically with a green **Connected ✓** or red **Unreachable ✗** status.
- **ngrok Interstitial Bypass**: Self-heals and bypasses ngrok browser warnings automatically.

---

## 🛠️ API & Network Protocols

The app connects to your coordinator backend using optimized OkHttp / Retrofit configurations with a **30-second connection timeout**:

### 1. `POST /android/report`
Submits a multipart request with location coordinates:
- `image`: JPEG file compressed dynamically to `85%` quality.
- `mission_id`: Generated UUID prefix (`cleanup-` followed by 8 characters).
- `location`: Optional JSON coordinates if fine/coarse GPS scopes are accepted:
  ```json
  {"lat": 37.123, "lng": -122.456}
  ```

### 2. `GET /missions/{mission_id}`
Active multi-threaded polling running at **2-second intervals** while the match is live.
- **Network Alert System**: Displays a referee-themed alert at the bottom of the screen if connection drops, continuing attempt routines automatically.
- **Timeout Mitigation**: Detects the lack of state variations for over 30 seconds and transitions to a custom timeout deck with an manual "Retry Sync Kick-off" prompt.

### 3. `POST /android/confirmation`
Submits the final post-cleanup scene:
- `image`: Multipart JPEG validation file.
- `mission_id`: Unique active ID.

---

## 🚀 Sideloading, Debugging, and Safe Installation

If you encounter installation issues during development (such as standard code `255 Exception` security warnings on modern Xiaomi, Oppo, or corporate devices):

1. **Enable USB Debugging (Security Settings)** under Developer Options if using MIUI/HyperOS, Oppo ColorOS, or Realme UI ROMs to authorize ADB to manage runtime policies.
2. **Remove package install pre-grant parameters** (`-g` flag) if restricted by enterprise MDM profiles, letting the app's standard Compose runtime prompt users for dynamic Camera and GPS permissions upon initial launch.

---

## 🏆 Development Dependencies
These standard components are bundled using central version catalogs:
- **Jetpack Compose & Material 3** — Stadium UI and responsive layout elements
- **OkHttp & Retrofit** — Multiparts and high-speed network polling
- **Coil** — Matches snapshot rendering and previews
- **Google Play Services Location** — Smart coordinate tracking
- **Accompanist Permissions** — Secure Camera and GPS runtime checks
- **Kotlin Coroutines / StateFlow** — Multi-threaded state sync and loop handling
