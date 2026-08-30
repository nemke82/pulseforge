# 🩺 PulseForge (GalaxyBP)

**PulseForge** je open-source sistem za telemetriju zdravstvenih senzora i procenu krvnog pritiska namenjen za **Samsung Galaxy Watch 4 / 5 / 6 / 7 (Wear OS)** i **Android / Samsung telefone**.

Aplikacija zaobilazi regionalna ograničenja fabričkih aplikacija direktnim pristupom sirovim hardverskim kanalima sata (**Optički PPG + ECG elektrode + Puls**) kako bi izračunala **Pulse Transit Time (PTT)** i procenila sistolni/dijastolni pritisak (**SYS / DIA**) kalibrisan pomoću pravog aparata sa manžetnom (npr. Omron).

---

## 📅 CalVer Verzije i GitHub Releases

Projekat koristi **[Calendar Versioning (CalVer)](https://calver.org/)** šemu:
$$\mathbf{vYYYY.0M.MICRO} \quad \text{(primer: } \mathbf{v2026.08.1}\text{)}$$

Svaki put kada se u repozitorijum pošalje tag (npr. `v2026.08.1`), GitHub Actions automatski gradi i objavljuje novi **GitHub Release** sa priloženim APK paketima spremnim za preuzimanje:

👉 **[Preuzmi najnoviji Release (GitHub Releases)](https://github.com/nemke82/pulseforge/releases)**

---

## 📐 Kako sistem funkcioniše

```
Galaxy Watch (Wear OS)
        │
        ├── Samsung PPG (Zeleni / Infracrveni optički senzor)
        ├── ECG elektrode (mV QRS impuls komora)
        └── Heart Rate & IBI (Puls)
        │
        ▼ (Bluetooth Wearable Data Layer)
Samsung Phone Companion Aplikacija
        │
        ├── Osciloskop uživo za PPG i ECG talase
        ├── 3-Point Kalibracija manžetnom (SYS = a·PTT + b·HR + c)
        ├── Matematička procena SYS / DIA
        └── Istorija merenja i Health Connect sinhronizacija
```

### Princip Pulse Transit Time (PTT) metode:
1. **ECG R-pik ($t_{\text{ECG\_R}}$)**: Beleži trenutak električne depolarizacije srčanih komora.
2. **PPG dolazak pulsa ($t_{\text{PPG\_foot}}$)**: Optički senzor na zglobu beleži trenutak nailaska talasa krvi.
3. **PTT ($\Delta t$)**: Razlika $\Delta t = t_{\text{PPG\_foot}} - t_{\text{ECG\_R}}$.
4. **Model pritiska**: Kada je krvni pritisak viši, arterije su zategnutije i pulsni talas putuje brže ($\Delta t$ opada). Kalibracijom pomoću 3 merenja manžetnom dobija se personalizovana regresiona formula.

---

## 📲 Uputstvo za Instalaciju

### 1. Instalacija na Android Telefon (`pulseforge-phone-app.apk`)
1. Preuzmi `pulseforge-phone-app.apk` iz [Releases](https://github.com/nemke82/pulseforge/releases).
2. Otvori preuzeti APK fajl na telefonu i klikni **Install** (ako pita, dozvoli *"Install unknown apps"*).
3. Pokreni aplikaciju i odobri Bluetooth i Permission dozvole.

---

### 2. Instalacija na Galaxy Watch (`pulseforge-galaxy-watch-app.apk`)

Galaxy Watch koristi Wear OS, pa se APK instalira putem **Wireless ADB (bežični debugging)** direktno sa računara ili sa telefona (koristeći aplikaciju *Bugjaeger* ili *GeminiMan Wear OS Manager*).

#### Korak po korak preko računara (PC / Mac / Linux):

1. **Aktiviraj Developer Options na satu**:
   - Na Galaxy Watch-u otvori **Settings** $\to$ **About watch** $\to$ **Software info**.
   - Dodirni polje **Software version** 7 puta uzastopno dok se ne pojavi poruka *"Developer mode turned on"*.

2. **Uključi Wireless Debugging na satu**:
   - Vrati se u **Settings** $\to$ **Developer options**.
   - Uključi **ADB debugging**.
   - Uključi **Wireless debugging** (sat i računar moraju biti na istoj Wi-Fi mreži).
   - Obrati pažnju na prikazanu **IP adresu i port** (npr. `192.168.1.45:5555` ili port za uparivanje).

3. **Poveži se i instaliraj aplikaciju**:
   Otvori terminal na računaru i pokreni:
   ```bash
   # 1. Povezivanje sa satom (zameni sa IP adresom tvog sata)
   adb connect 192.168.1.45:5555

   # 2. Instalacija PulseForge Wear aplikacije
   adb install pulseforge-galaxy-watch-app.apk
   ```

4. **Dozvole na satu**:
   - Pokreni **PulseForge** na satu i odobri dozvolu za senzore tela (**Body Sensors**).

---

## 🧪 Protokol za Testiranje (Korak po Korak)

Kada su obe aplikacije instalirane, isprati sledeći postupak za kompletno testiranje:

### Faza 1: Provera Bluetooth veze
1. Otvori **PulseForge** na telefonu.
2. Na početnom ekranu (**Dashboard**) u gornjem desnom uglu proveri da li stoji zeleni bedž **`Galaxy Watch (Connected)`**.
3. Otvori aplikaciju na satu — status treba da prikaže **`Phone Paired`**.

### Faza 2: Testiranje prenosa signala uživo (Live Oscilloscope)
1. U aplikaciji na telefonu pređi na tab **`Signal`**.
2. Na satu dodirni dugme **`START BP & ECG`** i prisloni prst druge ruke na gornje dugme/elektrodu sata.
3. Na telefonu posmatraj:
   - Zeleni talas: **Optički PPG signal pulsa**.
   - Crveni talas: **ECG električni impuls**.
   - Prikaz **Pulse Transit Time ($\Delta t$)** u milisekundama.

### Faza 3: Personalizovana 3-Point Kalibracija (sa Omron manžetnom)
Za tačne rezultate potrebno je izvršiti 3 kalibraciona merenja:
1. U aplikaciji na telefonu otvori tab **`Calibrate`**.
2. Sedite mirno 5 minuta.
3. Izmeri pritisak pravim aparatom na nadlaktici (npr. Omron) i istovremeno pokreni merenje na satu.
4. Upiši dobijene vrednosti (npr. `122` za SYS i `81` za DIA) u formu i klikni **`Save Calibration Point`**.
5. Ponovi postupak još dva puta u toku dana (npr. nakon pauze ili lagane šetnje kako bi postojala prirodna varijacija pritiska).
6. Nakon 3. tačke profil prelazi u status **`CALIBRATED PROFILE ACTIVE`**.

### Faza 4: Redovno merenje i istorija
1. Pokreni merenje pritiskom na dugme na satu ili tapom na **`START WATCH MEASUREMENT`** sa telefona.
2. Merenje traje 30 sekundi.
3. Po završetku, sat vibrira i prikazuje procenjeni **SYS / DIA** i puls.
4. Rezultat se automatski sinhronizuje u tab **`History`** na telefonu sa grafikonom i statistikom.

---

## 🛠 Tehnologije

- **Wear OS Module**: Jetpack Compose for Wear OS, Android SensorManager, Samsung Health Sensors raw PPG/ECG, Play Services Wearable.
- **Mobile Module**: Jetpack Compose Material 3, Dark Mode Cyber-Medical dizajn, Custom Canvas Oscilloscope, StateFlow.
- **Shared Module**: Digital Signal Processing (DSP), Pan-Tompkins R-peak algoritam, linearna i nelinearna regresija.
- **CI/CD**: GitHub Actions sa automatskim generisanjem i potpisivanjem APK fajlova i objavljivanjem GitHub Releases.

---

## ⚠️ Napomena i Odricanje Odgovornosti
*PulseForge je eksperimentalni projekat namenjen za istraživanje i ličnu telemetriju zdravstvenih senzora. Nije sertifikovano medicinsko sredstvo i ne sme se koristiti za postavljanje medicinske dijagnoze ili određivanje terapije.*
