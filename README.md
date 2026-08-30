# 🩺 PulseForge (GalaxyBP)

**PulseForge** is an open-source health telemetry and Blood Pressure estimation suite designed specifically for **Samsung Galaxy Watch (Watch 4 / 5 / 6 / 7 / Ultra / Ultra 2 & FE on Wear OS)** and **Android / Samsung Smartphones**.

It bypasses region and country restrictions of proprietary software by directly accessing raw sensor channels (**Optical PPG + ECG Electrodes + Heart Rate**) to compute **Pulse Transit Time (PTT)** and estimate systolic/diastolic blood pressure (**SYS / DIA**) calibrated against reference arm cuff measurements (e.g., Omron).

---

## 📅 CalVer Versioning & GitHub Releases

This project follows the **[Calendar Versioning (CalVer)](https://calver.org/)** convention:
$$\mathbf{vYYYY.0M.MICRO} \quad \text{(e.g., } \mathbf{v2026.08.1}\text{)}$$

Whenever a release tag (e.g. `v2026.08.1`) is pushed, GitHub Actions automatically compiles both apps and publishes an official **GitHub Release** with the APK binaries attached:

👉 **[Download Latest Release (GitHub Releases)](https://github.com/nemke82/pulseforge/releases)**

---

## 📐 Architecture & How It Works

```
Galaxy Watch (Wear OS)
        │
        ├── Samsung PPG (Green / Infrared Photoplethysmogram)
        ├── ECG Electrodes (mV QRS ventricular complex)
        └── Heart Rate & IBI (Inter-beat interval)
        │
        ▼ (Bluetooth Wearable Data Layer)
Samsung / Android Phone Companion App
        │
        ├── Real-Time Dual Oscilloscope (PPG & ECG)
        ├── 3-Point Omron Cuff Calibration (SYS = a·PTT + b·HR + c)
        ├── Dynamic SYS / DIA Mathematical Estimation
        └── Measurement History & Health Connect Sync
```

### Pulse Transit Time (PTT) Math
1. **ECG R-Peak ($t_{\text{ECG\_R}}$)**: Captures the precise electrical depolarization moment of the heart ventricles.
2. **PPG Pulse Arrival ($t_{\text{PPG\_foot}}$)**: The optical sensor on the wrist registers the systolic pulse wave inflection.
3. **PTT ($\Delta t$)**: Calculated as $\Delta t = t_{\text{PPG\_foot}} - t_{\text{ECG\_R}}$.
4. **Blood Pressure Model**: As vascular tone and blood pressure rise, arterial stiffness increases, causing pulse waves to propagate faster ($\Delta t$ decreases). A personalized regression model trained on 3 reference cuff measurements maps $(PTT, HR) \to (SYS, DIA)$.

---

## 📲 Installation Guide

### 1. Phone App (`pulseforge-phone-app.apk`)
1. Download `pulseforge-phone-app.apk` from [GitHub Releases](https://github.com/nemke82/pulseforge/releases).
2. Open the downloaded APK on your phone and tap **Install** (enable *"Install unknown apps"* if prompted).
3. Open the app and grant the necessary Bluetooth and notification permissions.

---

### 2. Galaxy Watch App (`pulseforge-galaxy-watch-app.apk`)

#### 💡 Why is ADB / Sideloading required for Smartwatches?
Unlike Android phones, **Wear OS (Galaxy Watch) does not have a built-in file manager or package installer UI** that allows tapping an `.apk` file to install it directly on the watch. Google and Samsung intentionally designed Wear OS to install consumer apps exclusively through the Google Play Store.

For third-party and open-source applications, **Wireless ADB (Wi-Fi sideloading)** is the standard and official Android developer method to install APKs onto your watch. Once installed, the app runs permanently and natively on the watch.

You can install the watch APK using either your **Android Phone (No PC needed)** or your **Computer**.

---

#### 📱 Method A: Install from your Phone (Recommended — No PC Needed)

You can use your Android smartphone as the installer using free, graphical Wear OS manager apps:

1. **Install a Sideload App on your Phone**:
   - Install **[GeminiMan Wear OS Manager](https://play.google.com/store/apps/details?id=com.geminiman.wearosmanager)** (or *Easy Fire Tools* / *Bugjaeger*) from the Google Play Store.
2. **Download Watch APK on your Phone**:
   - Download `pulseforge-galaxy-watch-app.apk` from [GitHub Releases](https://github.com/nemke82/pulseforge/releases) directly onto your phone.
3. **Enable Wireless Debugging on Watch**:
   - On your Galaxy Watch: **Settings** $\to$ **About watch** $\to$ **Software info** $\to$ tap **Software version** 7 times until Developer mode is enabled.
   - Go to **Settings** $\to$ **Developer options** $\to$ turn **ON** **ADB debugging** and **Wireless debugging**.
   - Note the **IP address and Port** shown on the watch screen (ensure watch and phone are on the same Wi-Fi).
4. **Sideload to Watch**:
   - Open *GeminiMan Wear OS Manager* on your phone, enter the watch's IP/Port to pair/connect.
   - Tap **Sideload APK / Install APK File**, select `pulseforge-galaxy-watch-app.apk`, and tap **Install**.
   - The app will transfer and install onto your Galaxy Watch automatically.

---

#### 💻 Method B: Install via Computer Terminal (ADB)

1. **Enable Wireless Debugging on Watch**:
   - Go to **Settings** $\to$ **Developer options** on your watch and enable **ADB debugging** + **Wireless debugging**.
   - Note the IP address and Port (e.g., `192.168.1.45:5555`).
2. **Connect and Install from Terminal**:
   ```bash
   # Connect to watch over Wi-Fi (replace with your watch's IP and port)
   adb connect 192.168.1.45:5555

   # Install the watch APK
   adb install pulseforge-galaxy-watch-app.apk
   ```

3. **Grant Sensor Permissions**:
   - Open **PulseForge** on your watch and accept the **Body Sensors** permission prompt.

---

## 🧪 End-to-End Testing & Verification Protocol

Follow this step-by-step verification protocol after installing both applications:

### Phase 1: Verify Bluetooth Data Layer Connectivity
1. Launch **PulseForge** on your phone.
2. Check the top-right connection badge on the **Dashboard** — it should show a green indicator with **`Galaxy Watch`**.
3. Launch the app on your watch — the subheader should read **`Phone Paired`**.

### Phase 2: Test Real-Time Sensor Stream (Live Oscilloscope)
1. On the phone app, navigate to the **`Signal`** tab.
2. On your watch, tap the **`START`** button and lightly place the index finger of your opposite hand on the watch's upper electrode/button (for ECG contact).
3. On the phone screen, observe:
   - **Green Waveform**: Live optical PPG pulse wave.
   - **Red Waveform**: ECG electrical QRS spikes.
   - **Pulse Transit Time ($\Delta t$)**: Live calculated millisecond delay indicator.

### Phase 3: Personalized 3-Point Cuff Calibration (Omron)
To train the regression formula for accurate Blood Pressure estimation:
1. Open the **`Calibrate`** tab in the phone app.
2. Rest in a seated position for 5 minutes with feet flat on the ground.
3. Take a simultaneous reading using your upper-arm cuff (e.g. Omron) and the watch.
4. Input the reference values (e.g., `122` SYS, `81` DIA) and click **`Save Calibration Point`**.
5. Repeat twice more at different times (e.g., after light movement or a break) to capture natural physiological pressure variation.
6. Once 3 points are recorded, the profile status changes to **`CALIBRATED PROFILE ACTIVE`**.

### Phase 4: Regular Measurement & History Logging
1. Trigger a measurement by tapping **`START WATCH MEASUREMENT`** on your phone or pressing **`START`** on your watch.
2. Keep still during the 30-second measurement window.
3. Once completed, the watch vibrates and displays your estimated **SYS / DIA** and heart rate.
4. The reading is automatically synchronized into the phone's **`History`** tab with statistical summaries.

---

## 🛠 Tech Stack

- **Wear OS**: Jetpack Compose for Wear OS, Android SensorManager, Samsung Health raw PPG/ECG sensor telemetry, Google Play Services Wearable Data Layer.
- **Mobile**: Jetpack Compose Material 3, Dark Mode Cyber-Medical theme, Custom Canvas Real-Time Oscilloscopes, StateFlow.
- **Shared Core**: Digital Signal Processing (DSP), Pan-Tompkins ECG R-peak detector, PPG foot slope detection, multi-variable linear regression.
- **CI/CD**: GitHub Actions automated pipeline with CalVer releases and artifact generation.

---

## ⚠️ Disclaimer
*PulseForge is an experimental research and personal health telemetry project. It is not a certified medical device and should not be used as a substitute for professional clinical diagnosis or medical treatment.*
