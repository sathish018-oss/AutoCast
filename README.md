# AutoCast Pro - Android Auto Screen Mirroring & YouTube Streaming App

AutoCast Pro is a native Kotlin Android application designed to stream video content and mirror your phone screen directly onto your Android Auto head unit display.

---

## 🌟 Key Features

1. **Dual Streaming Modes**:
   - **YouTube Widescreen Player Mode**: An optimized YouTube HTML5 IFrame interface designed specifically for head unit aspect ratios (16:9 widescreen and 21:9 ultra-wide).
   - **Full Phone Screen Mirroring Mode**: Uses Android's low-latency `MediaProjection` API to mirror your entire device screen, apps, and audio to the car screen.

2. **Touch Passthrough Engine**:
   - Uses an Android `AccessibilityService` gesture dispatch engine to route touch, swipe, and scroll events from the head unit display back to your phone.

3. **Non-Rooted Android Compatibility**:
   - Works on non-rooted Android 10 to 15+ devices using Android Auto Developer Mode ("Unknown Sources").

---

## 🏗️ Architecture Overview

```
+-----------------------------------------------------------------------+
|                         ANDROID PHONE DEVICE                          |
|                                                                       |
|  +-----------------------------------------------------------------+  |
|  |                     MainActivity (Companion UI)                 |  |
|  |  - YouTube Player Mode / Screen Mirror Mode selector             |  |
|  |  - Permission setup (MediaProjection, Overlay, Accessibility)   |  |
|  +-----------------------------------------------------------------+  |
|                                  |                                    |
|         +------------------------+------------------------+          |
|         |                                                 |          |
|  +--------------+                                  +--------------+  |
|  | ScreenCast   |                                  | YouTube      |  |
|  | Service      |                                  | Player       |  |
|  | (Media-      |                                  | Manager      |  |
|  | Projection)  |                                  | (HTML5 API)  |  |
|  +--------------+                                  +--------------+  |
|         |                                                 |          |
|         +------------------------+------------------------+          |
|                                  |                                    |
|                 +---------------------------------+                   |
|                 | AutoCastCarAppService           |                   |
|                 | (AndroidX Car App Library)       |                   |
|                 +---------------------------------+                   |
+----------------------------------|------------------------------------+
                                   | Android Auto Protocol (USB / Wireless)
                                   v
+-----------------------------------------------------------------------+
|                       ANDROID AUTO HEAD UNIT                          |
|                                                                       |
|  +-----------------------------------------------------------------+  |
|  |                     AutoCastCarScreen                           |  |
|  |  - Touch-optimized YouTube Control Deck                         |  |
|  |  - High-fps Screen Stream Surface / Canvas View                  |  |
|  |  - Touch Event Passthrough Dispatcher                           |  |
|  +-----------------------------------------------------------------+  |
+-----------------------------------------------------------------------+
```

---

## 📲 Setup & Installation Guide

### Step 1: Enable Android Auto Developer Mode
Google Play restricts video apps on Android Auto by default. You must enable **Developer Settings** in Android Auto on your phone:
1. Open **Settings** on your Android phone.
2. Search for **Android Auto**.
3. Scroll to the very bottom and locate **Version**.
4. Tap the **Version** box **10 times** continuously until a popup prompts: *"Enable developer settings?"*. Tap **OK**.
5. Tap the **3 vertical dots menu** (top right corner of Android Auto settings).
6. Select **Developer settings**.
7. Scroll down and check **Unknown sources**.
8. Set **Application Mode** to **Developer**.

---

### Step 2: Build & Sideload APK
1. Open the project folder in **Android Studio**.
2. Build the signed or debug APK (`./gradlew assembleDebug`).
3. Sideload the APK onto your phone using one of the following methods:
   - **Method A (ADB Direct Install)**:
     ```bash
     adb install -r app/build/outputs/apk/debug/app-debug.apk
     ```
   - **Method B (AAAD / KingInstaller)**:
     Use an Android Auto installer utility like AAAD (Android Auto Apps Downloader) or KingInstaller to register `com.autocast.app` as installed via Google Play Store package manager (`com.android.vending`).

---

### Step 3: Grant Permissions on Phone
1. Open the **AutoCast Pro** companion app on your phone.
2. Select your desired streaming mode (**YouTube** or **Screen Cast**).
3. Tap **Start Screen Mirroring** and approve the `MediaProjection` prompt (*"Start recording or casting with AutoCast"*).
4. Tap **Enable Touch Passthrough** to activate the Accessibility Service for full touch control from the car display.

---

### Step 4: Connect to Vehicle
1. Connect your phone to your vehicle or head unit via USB cable or Wireless Android Auto.
2. In the Android Auto app drawer on your head unit, select **AutoCast**.
3. Enjoy YouTube video playback and full screen mirroring directly on your car screen!

---

## 🛠️ Tech Stack
- **Language**: Kotlin 1.9
- **Target SDK**: Android 34 (Android 14/15)
- **Min SDK**: Android 26 (Android 8.0)
- **UI & Auto Libraries**: `androidx.car.app:app`, `androidx.appcompat`, Google Material Components
- **Media Pipeline**: `MediaProjectionManager`, `VirtualDisplay`, `WebView` (HTML5 YouTube IFrame API)
