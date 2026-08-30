# 🩺 PulseForge (GalaxyBP)

**PulseForge** is an open-source health telemetry and Blood Pressure estimation suite designed specifically for **Samsung Galaxy Watch 4 / 5 / 6 / 7 (Wear OS)** and **Android / Samsung Smartphones**.

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

Install the Wear OS APK onto your Galaxy Watch via **Wireless ADB (Wi-Fi Debugging)** using your computer (PC / Mac / Linux) or a phone-based sideload app (e.g., *Bugjaeger* or *GeminiMan Wear OS Manager*).

#### Step-by-Step via Computer (ADB):

1. **Enable Developer Options on Galaxy Watch**:
   - On your Galaxy Watch, go to **Settings** $\to$ **About watch** $\to$ **Software info**.
   - Tap **Software version** 7 times continuously until you see the notification *"Developer mode turned on"*.

2. **Enable Wireless Debugging**:
   - Go back to **Settings** $\to$ **Developer options**.
   - Turn **ON** **ADB debugging**.
   - Turn **ON** **Wireless debugging** (make sure your watch and PC are connected to the same Wi-Fi network).
   - Note the **IP address and Port** displayed (e.g., `192.168.1.45:5555`).

3. **Connect and Install**:
   Open your computer terminal and execute:
   ```bash
   # 1. Connect to the watch (replace with your watch's IP address and port)
   adb connect 192.168.1.45:5555

   # 2. Install the PulseForge Wear OS APK
   adb install pulseforge-galaxy-watch-app.apk
   ```

4. **Permissions on Watch**:
   - Open **PulseForge** on your Galaxy Watch and grant the **Body Sensors** permission when prompted.

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
