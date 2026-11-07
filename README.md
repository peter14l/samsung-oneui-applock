# Samsung One UI App Locker 🛡️

A production-ready, zero-bloat native Android App Locker built in 100% Kotlin and Jetpack Compose, strictly adhering to the Samsung One UI design philosophy. Built specifically for Samsung Galaxy devices, supporting **Android 11 (One UI 3.x / API 30) through Android 15+ (One UI 7.x / API 35)**.

---

## ✨ Features & Highlights

- **Native Samsung Biometric Authentication:** Utilizes `androidx.biometric.BiometricPrompt` with `BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL` to natively trigger Samsung's Fingerprint scanner, Face Unlock, and device PIN/Pattern/Password without custom PIN pads.
- **One UI Design System:**
  - True AMOLED Black (`#010101`) dark theme and Samsung Light (`#F6F6F8`).
  - Signature Samsung Blue accent (`#0381FE`).
  - `26.dp` grouped card containers with `1.dp` hairline dividers (inset `68.dp`).
  - Collapsible One UI Expanding App Bar (top 35% title header with one-handed reachable bottom 65%).
  - Squircle `44.dp` app icons and pill-shaped toggles.
- **App Launch Interception Engine:** Battery-optimized `AccessibilityService` listening strictly to `TYPE_WINDOW_STATE_CHANGED` with in-memory `Set<String>` synced via Kotlin Flow for $O(1)$ instantaneous interception.
- **Window Blur & Graceful Android 11 Fallback:** Leverages Android 12+ (API 31+) hardware `window.setBackgroundBlurRadius(80)` with graceful degradation on Android 11 (API 30) using high-opacity frosted glass (`Color.Black.copy(0.88f)` / `Color.White.copy(0.92f)`), preventing `NoSuchMethodError` crashes.
- **Notification Guard & Content Masking:** Intercepts notifications for locked applications using `NotificationListenerService`, suppressing sensitive previews and replacing them with One UI masked alerts (*"Contents hidden • Unlock app to view"*), preserving underlying `PendingIntent`s.
- **Hardware-Backed Encryption:** Stores lock preferences and session states inside `EncryptedSharedPreferences` via Jetpack Security (AES-256 GCM + AES-256 SIV).
- **Split-per-ABI Signed Builds:** Native ABI splits for `arm64-v8a`, `armeabi-v7a`, `x86_64`, and Universal APKs with automated GitHub Releases CI/CD.

---

## 📱 Compatibility Matrix

| OS Version | One UI Version | API Level | Native Blur | Notification Runtime Perm |
|---|---|---|---|---|
| **Android 11** | One UI 3.0 / 3.1 / 3.5 | API 30 | Frosted Fallback Surface | Auto-Granted via Channel |
| **Android 12 / 12L** | One UI 4.0 / 4.1 / 4.1.1 | API 31 / 32 | Native Window Blur (80px) | Auto-Granted via Channel |
| **Android 13** | One UI 5.0 / 5.1 / 5.1.1 | API 33 | Native Window Blur (80px) | `POST_NOTIFICATIONS` Runtime Check |
| **Android 14** | One UI 6.0 / 6.1 / 6.1.1 | API 34 | Native Window Blur (80px) | `POST_NOTIFICATIONS` Runtime Check |
| **Android 15+** | One UI 7.0+ | API 35 | Native Window Blur (80px) | Predictive Back & SDK 35 Insets |

---

## 🚀 Setup & Installation Guide on Samsung Galaxy

### 1. Download Release APK
Download the signed APK matching your phone's architecture from the [GitHub Releases](../../releases) section:
- **`app-arm64-v8a-release.apk`** (Recommended for almost all modern Samsung Galaxy S, Z, A, and M series phones).
- **`app-universal-release.apk`** (Universal fallback APK compatible with all devices).

### 2. Enable Accessibility Service (App Interception)
1. Open the App Lock app.
2. Under **Required Permissions**, tap **Enable** next to *App Interception Service*.
3. Tap **Installed apps** > **App Lock Interception Service**.
4. Toggle **On** and tap **Allow**.

> [!IMPORTANT]
> **Android 13+ "Restricted Settings" Bypass:**
> If Samsung Knox or Android 13+ greys out the Accessibility switch with a *"Restricted setting: For your security, this setting is currently unavailable"* alert:
> 1. Long-press the **App Lock** icon on your home screen or app drawer and tap the **(i)** Info icon (or go to **Settings > Apps > App Lock**).
> 2. In the top-right corner, tap the **⋮ (three dots)** menu.
> 3. Select **Allow restricted settings**.
> 4. Verify with your fingerprint or PIN, then return to Accessibility settings and toggle the service on.

### 3. Enable Notification Guard (Content Masking)
1. Tap **Enable** next to *Notification Guard*.
2. Locate **App Lock Notification Guard** in Device & App Notifications and toggle it **On**.
3. Allow Notification Access.

### 4. Background Protection (Disable Battery Optimization)
1. Tap **Enable** next to *Background Protection*.
2. Select **Allow** when prompted to ignore battery optimizations, ensuring the One UI system memory manager does not stop the background accessibility interceptor.

---

## 🏛️ Architecture Overview

```
com.samsung.oneui.applock
├── AppLockApplication.kt          # Global Application & Notification Channel bootstrapping
├── data
│   ├── model
│   │   ├── AppInfo.kt             # UI model for installed applications & lock states
│   │   └── LockSettings.kt        # Master toggles, notification scope, relock policies
│   ├── pref
│   │   └── EncryptedPreferencesManager.kt # Jetpack Security EncryptedSharedPreferences & Flow
│   └── repository
│       └── AppLockRepository.kt   # Clean Repository coordinating PackageManager & EncryptedPrefs
├── receiver
│   └── BootReceiver.kt            # Warmed-up cache initialization on device reboot
├── service
│   ├── AppLockAccessibilityService.kt # Window state change interceptor (O(1) in-memory lookup)
│   └── NotificationGuardService.kt    # Sensitive notification interception & content masking
├── ui
│   ├── components
│   │   ├── OneUICard.kt           # 26.dp rounded container with 68.dp inset dividers
│   │   ├── OneUIExpandingAppBar.kt# Collapsible One UI 35% header with standard top bar
│   │   ├── OneUISearchBar.kt      # One UI pill search input
│   │   └── OneUISwitch.kt         # Custom One UI pill switch
│   ├── lock
│   │   └── LockOverlayActivity.kt # Native BiometricPrompt with Android 11/12+ blur handling
│   ├── main
│   │   ├── MainActivity.kt        # Root launcher activity
│   │   ├── MainScreen.kt          # One UI Master dashboard & application list
│   │   ├── MainViewModel.kt       # MVVM StateFlow coordinator
│   │   ├── PermissionCard.kt      # Permissions onboarding with deep links
│   │   └── PermissionState.kt     # Permission status evaluation
│   └── theme
│       ├── Color.kt               # AMOLED #010101, Light #F6F6F8, Samsung Blue #0381FE
│       ├── Shape.kt               # 26.dp cards, 13.dp squircles, 50% pill buttons
│       ├── Theme.kt               # One UI Compose theme provider
│       └── Type.kt                # Typography scale matching Samsung One UI guidelines
└── util
    ├── PermissionHelper.kt        # System permission checkers & setting navigation intents
    └── SystemPackageFilter.kt     # SystemUI and launcher package resolution
```

---

## ⚙️ Building Locally

Ensure you have **JDK 17** installed and configured:

```bash
# Clone the repository
git clone https://github.com/peter14l/samsung-oneui-applock.git
cd samsung-oneui-applock

# Build Debug APK
./gradlew assembleDebug

# Build Release Split APKs (using release-keystore.jks)
export RELEASE_KEYSTORE_PASSWORD="<password>"
export RELEASE_KEY_ALIAS="applock-release-key"
export RELEASE_KEY_PASSWORD="<password>"
./gradlew assembleRelease
```

Generated APKs will be located under `app/build/outputs/apk/release/`.

---

## 🔒 Security & Privacy

- **No Network Permissions:** `android.permission.INTERNET` is **NOT** declared in `AndroidManifest.xml`. Zero telemetry, zero analytics, zero data leaving your device.
- **Hardware Keystore:** AES keys are bound to the Android Keystore hardware security module (StrongBox / TEE).
- **Direct Biometrics:** Biometric credentials never pass through app memory; authentication is handled solely by the OS `BiometricPrompt`.

---

## 📄 License
Distributed under the Apache 2.0 License. Built with ❤️ for Samsung One UI lovers.
