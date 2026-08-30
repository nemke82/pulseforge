# 🩺 PulseForge (GalaxyBP)

**PulseForge** is an open-source health telemetry and Blood Pressure estimation suite designed specifically for **Samsung Galaxy Watch 4 / 5 / 6 / 7 (Wear OS)** and **Android / Samsung Phones**.

It bypasses region/country restrictions of proprietary apps by directly accessing raw sensor channels (**Optical PPG + ECG + Heart Rate**) to compute **Pulse Transit Time (PTT)** and estimate systolic/diastolic blood pressure (**SYS / DIA**) calibrated against reference cuff readings (e.g. Omron).

---

## 📐 How It Works

```
Galaxy Watch (Wear OS)
        │
        ├── Samsung PPG (Green / IR Photoplethysmogram)
        ├── ECG Electrodes (mV QRS complex)
        └── Heart Rate & IBI
        │
        ▼ (Bluetooth Wearable Data Layer)
Samsung Phone Companion App
        │
        ├── Real-time Oscilloscope Waveforms
        ├── 3-Point Omron Cuff Calibration (SYS = a·PTT + b·HR + c)
        ├── Dynamic SYS / DIA Estimation
        └── Health Connect & Measurement History
```

### Pulse Transit Time (PTT) Math
1. **ECG R-Peak**: Identifies ventricular depolarization ($t_{ECG\_R}$).
2. **PPG Pulse Arrival**: Detects the systolic wave foot ($t_{PPG\_foot}$).
3. **PTT ($\Delta t$)**: $\Delta t = t_{PPG\_foot} - t_{ECG\_R}$.
4. **Blood Pressure Model**: As vascular tone and blood pressure increase, pulse waves travel faster (PTT decreases). Using a 3-point cuff calibration, a personalized regression model translates $(PTT, HR) \to (SYS, DIA)$.

---

## 🚀 Automated APK Builds via GitHub Actions

This repository includes a preconfigured GitHub Actions workflow [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml) that builds APKs automatically.

### 📥 Downloading the APKs
1. Go to the **Actions** tab on your GitHub repository: `https://github.com/nemke82/pulseforge/actions`
2. Click on the latest workflow run.
3. Scroll down to **Artifacts** to download:
   - `pulseforge-phone-app`: APK for your Android / Samsung smartphone.
   - `pulseforge-galaxy-watch-app`: APK for your Galaxy Watch (Wear OS).
   - `pulseforge-apks-bundle`: ZIP containing both APKs.

---

## 📲 Installation Guide

### 1. Phone App (`pulseforge-phone-app.apk`)
- Transfer `pulseforge-phone-app.apk` to your phone or download directly from GitHub.
- Open the APK on your phone and tap **Install** (allow "Install unknown apps" if prompted).

---

### 2. Galaxy Watch App (`pulseforge-galaxy-watch-app.apk`)

Install the Wear OS APK directly onto your Galaxy Watch via **Wireless ADB**:

1. **Enable Developer Options on Galaxy Watch**:
   - On your Galaxy Watch, go to **Settings** $\to$ **About watch** $\to$ **Software info**.
   - Tap **Software version** 7 times until you see *"Developer mode turned on"*.
2. **Enable Wireless Debugging**:
   - Go back to **Settings** $\to$ **Developer options**.
   - Turn **ON** **ADB debugging** and **Wireless debugging**.
   - Note the **IP address and port** shown (e.g. `192.168.1.50:5555` or pairing port on Wear OS 4/5).
3. **Connect and Install from your PC / Terminal**:
   ```bash
   # Connect to watch
   adb connect 192.168.1.50:5555

   # Install the Wear APK
   adb install pulseforge-galaxy-watch-app.apk
   ```

---

## 🎯 3-Point Cuff Calibration Protocol

For optimal accuracy:
1. **Rest**: Sit quietly for 5 minutes with feet flat on the floor.
2. **First Point**: Take a simultaneous reading with your arm cuff (e.g. Omron) and the Galaxy Watch. Enter the cuff SYS/DIA into the **Calibration Wizard** in the phone app.
3. **Second Point**: Repeat 15–20 minutes later.
4. **Third Point**: Take a third measurement at a different time of day or after mild activity (to capture dynamic pressure variance).
5. Once 3 points are saved, your custom mathematical calibration profile will automatically be applied to all future watch readings.

---

## 🛠 Project Structure

```
pulseforge/
├── .github/workflows/
│   └── build-apk.yml           # GitHub Actions CI/CD workflow
├── shared/                     # Shared models, PTT algorithm, regression solver
│   └── src/main/java/com/pulseforge/shared/
│       ├── algorithm/          # PttBpEstimator (peak detection, calibration math)
│       └── model/              # SensorSample, BloodPressureMeasurement, CalibrationProfile
├── mobile/                     # Android Phone Companion App (Jetpack Compose Material 3)
│   └── src/main/java/com/pulseforge/mobile/
│       ├── datalayer/          # Wearable Data Layer receiver service
│       ├── data/               # Repository and history state management
│       └── ui/                 # Dashboard, Live Signal, Calibration, History, Settings
└── wear/                       # Galaxy Watch Wear OS App (Wear Compose)
    └── src/main/java/com/pulseforge/wear/
        ├── datalayer/          # Data Layer stream sender
        ├── sensor/             # Samsung PPG/ECG & Heart Rate SensorManager
        └── presentation/       # Circular UI, animated pulse wave, countdown
```

---

## ⚠️ Disclaimer
*PulseForge is an experimental research and personal health telemetry tool. It is not a certified medical device and should not be used as a substitute for professional medical diagnosis or clinical treatment.*
