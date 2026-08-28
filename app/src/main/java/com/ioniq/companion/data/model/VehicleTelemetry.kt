package com.ioniq.companion.data.model

/**
 * Complete immutable snapshot of vehicle telemetry for E-GMP vehicles
 * (Hyundai IONIQ 5, IONIQ 5 N, IONIQ 6, IONIQ 9, Kia EV6, EV9, Genesis GV60)
 * Modeled after gburlingame/ioniq-app EV Dashboard architecture.
 */
data class VehicleTelemetry(
    val vehicleModel: VehicleModel = VehicleModel.E_GMP_UNIVERSAL,
    val timestampMs: Long = System.currentTimeMillis(),
    val battery: BatteryData = BatteryData(),
    val cells: CellData = CellData(),
    val drive: DriveData = DriveData(),
    val efficiency: EfficiencyData = EfficiencyData(),
    val preconditioning: PreconditioningData = PreconditioningData(),
    val tpms: TpmsData = TpmsData(),
    val climate: ClimateData = ClimateData(),
    val auxBattery: AuxBatteryData = AuxBatteryData(),
    val charging: ChargingData = ChargingData(),
    val diagnostics: DtcData = DtcData(),
    val isDemoMode: Boolean = false,
    val isConnected: Boolean = false
)

enum class VehicleModel(val displayName: String, val packCapacityKwh: Float, val cellCount: Int) {
    E_GMP_UNIVERSAL("E-GMP EV Platform", 77.4f, 192),
    IONIQ_5("Hyundai IONIQ 5", 77.4f, 192),
    IONIQ_5_N("Hyundai IONIQ 5 N", 84.0f, 192),
    IONIQ_6("Hyundai IONIQ 6", 77.4f, 192),
    IONIQ_9("Hyundai IONIQ 9", 99.8f, 192),
    KIA_EV6("Kia EV6", 77.4f, 192),
    KIA_EV9("Kia EV9", 99.8f, 192),
    GENESIS_GV60("Genesis GV60", 77.4f, 192)
}

data class BatteryData(
    val socDisplay: Float = 0f,           // Display SOC % (0 when disconnected)
    val socBms: Float = 0f,               // Real BMS SOC %
    val voltage: Float = 0f,              // Pack DC Voltage (V)
    val current: Float = 0f,              // Battery Current (A)
    val powerKw: Float = 0f,              // Instantaneous Power (kW, + for Draw, - for Regen)
    val soh: Float = 100f,                // State of Health %
    val maxTempC: Float = 0f,             // Max Cell Temp (°C)
    val minTempC: Float = 0f,             // Min Cell Temp (°C)
    val avgTempC: Float = 0f,             // Average Battery Temp (°C)
    val moduleTemps: List<Float> = emptyList(),
    val isolationResistanceKohm: Int = 0, // Isolation Resistance (kΩ)
    val cumulativeEnergyChargedKwh: Float = 0f,
    val cumulativeEnergyDischargedKwh: Float = 0f,
    val remainingEnergyKwh: Float = 0f,
    val estRangeKm: Float = 0f,
    val estRangeMiles: Float = 0f
)

data class EfficiencyData(
    val currentMiPerKwh: Float = 3.6f,           // Current rolling mi/kWh
    val currentKmPerKwh: Float = 5.8f,           // Current rolling km/kWh
    val rollingAverageMiPerKwh: Float = 3.4f,    // 15-minute moving average benchmark line
    val tripEfficiencyMiPerKwh: Float = 3.5f,    // Trip net efficiency
    val trendHistory: List<Float> = emptyList(), // 15-minute rolling history samples
    val isBetterThanAverage: Boolean = true      // Green (better than avg) vs Amber (worse than avg)
)

data class PreconditioningData(
    val isActive: Boolean = false,               // Battery heater / preconditioning active
    val timeToTargetMin: Int = 0,                // Minutes until pack reaches 21°C (70°F) for max DC speed
    val targetTempC: Float = 21.0f,              // Optimal DC fast charge target temp
    val heaterPowerKw: Float = 0f,               // High-voltage PTC battery heater power draw
    val coolantInletTempC: Float = 0f            // Battery cooling/heating circuit inlet temp
)

data class CellData(
    val cellVoltages: List<Float> = emptyList(),
    val maxCellVoltage: Float = 0f,
    val minCellVoltage: Float = 0f,
    val maxCellIndex: Int = 0,
    val minCellIndex: Int = 0,
    val deltaVoltageMv: Float = 0f               // ΔV in millivolts
)

data class DriveData(
    val gear: Gear = Gear.PARK,
    val speedKmh: Float = 0f,
    val speedMph: Float = 0f,
    val frontMotorRpm: Int = 0,
    val rearMotorRpm: Int = 0,
    val motorTempC: Float = 0f,
    val inverterTempC: Float = 0f,
    val acceleratorPercent: Float = 0f,
    val regenPaddleLevel: Int = 0,               // 0, 1, 2, 3, 4 (i-Pedal)
    val brakePedalActive: Boolean = false,
    val headLightsActive: Boolean = false,
    val motorPowerDrawKw: Float = 0f
)

enum class Gear(val label: String) {
    PARK("P"),
    REVERSE("R"),
    NEUTRAL("N"),
    DRIVE("D")
}

data class TpmsData(
    val frontLeftPsi: Float = 0f,
    val frontRightPsi: Float = 0f,
    val rearLeftPsi: Float = 0f,
    val rearRightPsi: Float = 0f,
    val frontLeftTempC: Float = 0f,
    val frontRightTempC: Float = 0f,
    val rearLeftTempC: Float = 0f,
    val rearRightTempC: Float = 0f,
    val recommendedPsi: Float = 36.0f,
    val isWarningActive: Boolean = false
)

data class ClimateData(
    val cabinTempC: Float = 0f,
    val ambientTempC: Float = 0f,
    val driverVentTempC: Float = 0f,
    val passengerVentTempC: Float = 0f,
    val floorVentTempC: Float = 0f,
    val hvacPowerDrawKw: Float = 0f,
    val batteryWarmerActive: Boolean = false,
    val batteryWarmerPowerKw: Float = 0f
)

data class AuxBatteryData(
    val voltage: Float = 0f,                     // 12V Battery Voltage
    val soc: Float = 0f,                         // 12V SOC %
    val current: Float = 0f,                     // Amperes from LDC (DC-DC Converter)
    val temperatureC: Float = 0f,
    val ldcActive: Boolean = false
)

data class ChargingData(
    val isCharging: Boolean = false,
    val isDcFastCharge: Boolean = false,
    val isAcCharge: Boolean = false,
    val chargingPowerKw: Float = 0f,
    val evseMaxCurrentA: Float = 0f,
    val evseMaxPowerKw: Float = 0f,
    val requestedCurrentA: Float = 0f,
    val deliveredCurrentA: Float = 0f,
    val sessionKwhDelivered: Float = 0f,
    val sessionDurationSec: Long = 0L,
    val estMinutesTo80: Int = 0,
    val estMinutesTo100: Int = 0,
    val pilotDutyCyclePercent: Float = 0f,
    val timeToPreheatTempMin: Int = 0
)

data class DtcData(
    val activeDtcs: List<DtcItem> = emptyList(),
    val scannedModulesCount: Int = 0,
    val isScanInProgress: Boolean = false
)

data class DtcItem(
    val code: String,
    val module: String,
    val description: String,
    val severity: DtcSeverity
)

enum class DtcSeverity {
    INFO,
    WARNING,
    CRITICAL
}
