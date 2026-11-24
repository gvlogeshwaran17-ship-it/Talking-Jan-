# Talking Jan (Android App)

A native Android (Kotlin) replica of Talking Tom starring Jan.

## Features
- **Voice Mimicry**: Listens automatically via microphone and repeats everything in a classic high-pitched Talking Tom voice.
- **Touch Zones**:
  - Pat head -> Happy purr & floating hearts
  - Poke face -> Dizzy wobble, boing sound & stars
  - Tickle lower area -> Laughing bounce & giggles
- **Interactive Action Buttons**: Coffee, Cake, Cymbal Prank, Dance Mode, and Mic Toggle.

## How to Build the APK
### Option A: Android Studio (Local)
1. Unzip this folder and open it in **Android Studio**.
2. Wait for Gradle sync to finish.
3. Click **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Copy `app/build/outputs/apk/debug/app-debug.apk` to your Android phone and install it.

### Option B: Cloud Build via GitHub (No Android Studio Needed)
1. Upload the unzipped folder to a new GitHub repository.
2. Open the **Actions** tab and run **Build Talking Jan APK**.
3. Download the `TalkingJan-APK` artifact directly to your Android phone!
