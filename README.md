# Iskra EV Companion (Іскра) for Android & Android Auto ⚡🚗

An advanced OBD-II diagnostic and live telemetry application built with **Jetpack Compose**, **Material 3**, and **Android Auto** for the Hyundai E-GMP platform (**IONIQ 5, IONIQ 5 N, IONIQ 6, IONIQ 9, Kia EV6, EV9, Genesis GV60**), specifically engineered for **Veepeak OBD-II Bluetooth adapters**.

Inspired by the iOS *IONIQ 5 Companion* application, this project delivers high-fidelity visual graphics, glowing bi-directional power/regen arc dials, real-time 192-cell voltage heatmaps, 4-wheel TPMS monitoring, 800V DC fast charging curves, and in-car Android Auto display integration.

---

## Key Features

### 1. 📱 Modern EV Dashboard (Jetpack Compose)
- **Bi-Directional Power Dial**: Real-time smooth needle and arc animation showing instantaneous motor draw (+kW) and regenerative braking (-kW).
- **800V SOC Ring**: Glowing battery arc displaying Display SOC %, BMS SOC %, Pack Voltage, Amperage, and remaining kWh.
- **Consumption Breakdown**: Live power distribution between Drivetrain, HVAC/Climate, Battery Pre-warmer, and 12V electronics.

### 2. 🔬 Battery & Cell Diagnostics
- **192-Cell Voltage Heatmap**: Full color-graded matrix grid of all cell voltages with minimum/maximum cell identification and $\Delta$V (cell delta) warning thresholds.
- **Module Thermals**: Battery temperature sensor distribution.
- **12V Auxiliary System**: Deep telemetry on 12V lead-acid/AGM auxiliary battery, LDC DC-DC converter output, and state of health (SOH).

### 3. ⚡ 800V DC Fast Charging Telemetry
- **Peak Charging Telemetry**: Real-time kW speed (up to 240 kW), delivered vs. requested current, and voltage curve.
- **Session Tracking**: Energy added (kWh), session timer, and estimated minutes to 80% and 100%.
- **Pre-Conditioning Indicator**: Time to reach optimal 21°C / 70°F battery fast-charge temperature.

### 4. 🚗 Vehicle Systems & TPMS
- **4-Wheel Tire Health**: Overhead vehicle chassis schematic displaying live PSI and °C for Front-Left, Front-Right, Rear-Left, and Rear-Right wheels with low-pressure alerts.
- **Dual Motor & Inverter Thermals**: Front and Rear motor RPM, inverter temperature, and coolant health.

### 5. 🚘 Android Auto In-Car Integration
- Full `androidx.car.app` integration for vehicle infotainment displays:
  - **Driving Dashboard**: Power draw/regen status, SOC %, range, gear, motor RPM, and tire pressures.
  - **Charging Screen**: Live charging speed (kW), target %, and EVSE station limits.
  - **Cell Diagnostics Screen**: Pack balance, $\Delta$V, and SOH.

### 6. 🔌 Veepeak Bluetooth Engine & Offline Simulator
- **Dual Connection Stack**: Supports both **Bluetooth Low Energy (BLE GATT)** and **Bluetooth Classic (RFCOMM SPP)** with auto-discovery for Veepeak OBDCheck BLE, BLE+, VP11, VP30, and compatible ELM327 dongles.
- **Interactive Simulator (Demo Mode)**: 4 realistic presets (*City Driving & Regen, Highway 75 MPH Cruise, 800V DC Fast Charging, Cold Weather Pre-Conditioning*) so the app can be fully tested and demonstrated without a vehicle or dongle.
- **CSV Exporter**: Export complete session logs to CSV and share via standard Android share sheet.

---

## Project Structure

```
Ioniq5Companion/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml              # Bluetooth permissions & CarAppService
│   │   ├── java/com/ioniq/companion/
│   │   │   ├── MainActivity.kt              # Main Compose Activity & Bottom Navigation
│   │   │   ├── auto/                        # Android Auto (CarAppService & Screens)
│   │   │   │   ├── IoniqCarAppService.kt
│   │   │   │   ├── IoniqCarSession.kt
│   │   │   │   └── screens/
│   │   │   │       ├── AutoDrivingScreen.kt
│   │   │   │       ├── AutoChargingScreen.kt
│   │   │   │       └── AutoCellDiagnosticsScreen.kt
│   │   │   ├── data/
│   │   │   │   ├── model/                   # VehicleTelemetry, ConnectionState, Enums
│   │   │   │   ├── bluetooth/               # Veepeak BLE & Classic SPP Manager
│   │   │   │   ├── obd/                     # ELM327 Engine & E-GMP PID Decoder
│   │   │   │   ├── simulator/               # Offline Drive/Charging Simulator
│   │   │   │   └── repository/              # TelemetryRepository Singleton
│   │   │   └── ui/
│   │   │       ├── components/              # PowerDialGauge, BatterySocRing, CellHeatmap, TPMS
│   │   │       ├── screens/                 # Dashboard, Battery, Charging, Vehicle, DTC, Settings
│   │   │       └── theme/                   # Cyber-EV Dark Aurora Material 3 Theme
│   │   └── res/                             # Icons, themes, and automotive_app_desc.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## Getting Started in Android Studio

1. **Open the Project**:
   - Open Android Studio -> `File -> Open` -> Select `C:\Users\oleha\.gemini\antigravity\scratch\Ioniq5Companion`.
   - Let Gradle sync download dependencies automatically.

2. **Run on Device or Emulator**:
   - Select your target Android device or emulator (API 26+) and click **Run (Shift + F10)**.
   - The app launches in **Interactive Demo Mode** immediately, giving you animated gauges, cell heatmaps, and dynamic driving power flow.

3. **Connecting a Physical Veepeak Adapter**:
   - Plug your Veepeak adapter into your car's OBD-II port under the steering column.
   - In the app, switch to the **Setup (Settings)** tab.
   - Tap **Scan** -> Select your **Veepeak** adapter from the list -> Tap **Connect**.
   - Toggle **Demo Mode** off to stream live telemetry from your car's BMS, VMCU, TPMS, and HVAC ECUs.

4. **Testing Android Auto**:
   - Launch the Android Auto **Desktop Head Unit (DHU)** via Android Studio or connect your phone to your car's USB/Wireless Android Auto.
   - Open the **IONIQ 5 Auto** app icon on the head unit display.
