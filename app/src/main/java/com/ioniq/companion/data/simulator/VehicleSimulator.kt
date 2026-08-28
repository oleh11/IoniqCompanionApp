package com.ioniq.companion.data.simulator

import com.ioniq.companion.data.model.AuxBatteryData
import com.ioniq.companion.data.model.BatteryData
import com.ioniq.companion.data.model.CellData
import com.ioniq.companion.data.model.ChargingData
import com.ioniq.companion.data.model.ClimateData
import com.ioniq.companion.data.model.DriveData
import com.ioniq.companion.data.model.EfficiencyData
import com.ioniq.companion.data.model.Gear
import com.ioniq.companion.data.model.PreconditioningData
import com.ioniq.companion.data.model.SimulationScenario
import com.ioniq.companion.data.model.TpmsData
import com.ioniq.companion.data.model.VehicleModel
import com.ioniq.companion.data.model.VehicleTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Realistic E-GMP Vehicle Simulator for offline testing, UI demonstration,
 * and Android Auto verification.
 * Modeled with live 15-min rolling efficiency curves and preconditioning ETA.
 */
class VehicleSimulator {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var simulationJob: Job? = null

    private val _telemetry = MutableStateFlow(VehicleTelemetry(isDemoMode = true))
    val telemetry: StateFlow<VehicleTelemetry> = _telemetry.asStateFlow()

    private var currentScenario = SimulationScenario.CITY_DRIVE
    private var tick = 0L
    private var currentSoc = 68.5f
    private var sessionChargedKwh = 0.0f
    private var sessionSeconds = 0L

    // 15-minute rolling efficiency history buffer (samples every few ticks)
    private val efficiencyHistory = mutableListOf<Float>().apply {
        // Pre-populate with typical E-GMP driving samples (around 3.2 - 4.1 mi/kWh)
        addAll(listOf(3.2f, 3.4f, 3.6f, 3.8f, 3.5f, 3.3f, 3.7f, 4.0f, 3.9f, 3.6f, 3.4f, 3.8f, 4.1f, 3.7f, 3.5f))
    }

    fun setScenario(scenario: SimulationScenario) {
        currentScenario = scenario
        tick = 0L
        if (scenario == SimulationScenario.DC_FAST_CHARGE) {
            currentSoc = 22.0f
            sessionChargedKwh = 0.0f
            sessionSeconds = 0L
        }
    }

    fun start() {
        stop()
        simulationJob = scope.launch {
            while (isActive) {
                tick++
                val updated = generateTelemetryFrame(tick, currentScenario)
                _telemetry.value = updated
                delay(100) // 10 Hz smooth UI refresh
            }
        }
    }

    fun stop() {
        simulationJob?.cancel()
        simulationJob = null
    }

    private fun generateTelemetryFrame(t: Long, scenario: SimulationScenario): VehicleTelemetry {
        val timeSec = t * 0.1f
        return when (scenario) {
            SimulationScenario.CITY_DRIVE -> generateCityDrive(timeSec)
            SimulationScenario.HIGHWAY_CRUISE -> generateHighwayCruise(timeSec)
            SimulationScenario.DC_FAST_CHARGE -> generateFastCharge(timeSec)
            SimulationScenario.COLD_BATTERY_PREHEAT -> generateColdPreheat(timeSec)
        }
    }

    private fun generateCityDrive(t: Float): VehicleTelemetry {
        // Accelerating and regenerative braking cycle (period 16s)
        val cycle = sin(t * (2f * PI.toFloat() / 16f))
        val powerKw: Float
        val speedKmh: Float
        val gear = Gear.DRIVE
        val regenPaddle = 3

        if (cycle >= -0.2f) {
            // Power Draw (Acceleration / Cruising)
            powerKw = -((cycle + 0.2f) * 65f + 8f) // negative is discharge in BMS convention
            speedKmh = (cycle + 0.2f) * 55f + 15f
        } else {
            // Regenerative Braking (Green power return)
            powerKw = (cycle.coerceAtMost(-0.2f) + 0.2f) * -52f // positive is charge
            speedKmh = (1f + cycle) * 35f
        }

        val voltage = 720f + (cycle * 8f)
        val currentAmps = (powerKw * 1000f) / voltage
        val rpm = (speedKmh * 65f).toInt()
        val speedMph = speedKmh * 0.621371f

        currentSoc -= 0.0003f
        if (currentSoc < 10f) currentSoc = 85f

        val cellBase = 3.75f + (cycle * 0.03f)
        val cells = List(192) { i ->
            cellBase + (sin(i * 0.15f + t * 0.2f) * 0.008f)
        }
        val maxCell = cells.maxOrNull() ?: 3.78f
        val minCell = cells.minOrNull() ?: 3.76f

        // Dynamic 15-min efficiency calculation (mi/kWh)
        val currentEfficiency = (3.6f + cycle * 0.6f).coerceIn(2.4f, 5.2f)
        if (tick % 50 == 0L) {
            if (efficiencyHistory.size >= 25) efficiencyHistory.removeAt(0)
            efficiencyHistory.add(currentEfficiency)
        }
        val rollingAvg = efficiencyHistory.average().toFloat()
        val isBetter = currentEfficiency >= rollingAvg

        val estRangeKm = currentSoc * (currentEfficiency * 1.60934f * 0.774f)
        val estRangeMiles = estRangeKm * 0.621371f

        return VehicleTelemetry(
            vehicleModel = VehicleModel.E_GMP_UNIVERSAL,
            timestampMs = System.currentTimeMillis(),
            battery = BatteryData(
                socDisplay = currentSoc,
                socBms = currentSoc - 1.2f,
                voltage = voltage,
                current = currentAmps,
                powerKw = powerKw,
                soh = 99.6f,
                maxTempC = 27.5f,
                minTempC = 25.0f,
                avgTempC = 26.2f,
                moduleTemps = listOf(26f, 27f, 27f, 28f, 26f, 26f, 25f, 26f),
                isolationResistanceKohm = 3450,
                cumulativeEnergyChargedKwh = 4520.4f,
                cumulativeEnergyDischargedKwh = 4210.8f,
                remainingEnergyKwh = (77.4f * (currentSoc / 100f)),
                estRangeKm = estRangeKm,
                estRangeMiles = estRangeMiles
            ),
            efficiency = EfficiencyData(
                currentMiPerKwh = currentEfficiency,
                currentKmPerKwh = currentEfficiency * 1.60934f,
                rollingAverageMiPerKwh = rollingAvg,
                tripEfficiencyMiPerKwh = 3.65f,
                trendHistory = efficiencyHistory.toList(),
                isBetterThanAverage = isBetter
            ),
            preconditioning = PreconditioningData(
                isActive = false,
                timeToTargetMin = 0,
                targetTempC = 21.0f,
                heaterPowerKw = 0f,
                coolantInletTempC = 24.5f
            ),
            cells = CellData(
                cellVoltages = cells,
                maxCellVoltage = maxCell,
                minCellVoltage = minCell,
                maxCellIndex = 18,
                minCellIndex = 142,
                deltaVoltageMv = ((maxCell - minCell) * 1000f)
            ),
            drive = DriveData(
                gear = gear,
                speedKmh = speedKmh.coerceAtLeast(0f),
                speedMph = speedMph.coerceAtLeast(0f),
                frontMotorRpm = rpm,
                rearMotorRpm = rpm,
                motorTempC = 52.0f + (cycle * 3f),
                inverterTempC = 44.0f + (cycle * 2f),
                acceleratorPercent = if (powerKw < 0) (cycle * 45f).coerceIn(0f, 100f) else 0f,
                regenPaddleLevel = regenPaddle,
                brakePedalActive = powerKw > 5f,
                motorPowerDrawKw = if (powerKw < 0) -powerKw else 0f
            ),
            tpms = TpmsData(
                frontLeftPsi = 36.2f + (sin(t * 0.05f) * 0.2f),
                frontRightPsi = 36.0f + (cos(t * 0.05f) * 0.2f),
                rearLeftPsi = 36.5f,
                rearRightPsi = 36.3f,
                frontLeftTempC = 30.5f,
                frontRightTempC = 31.0f,
                rearLeftTempC = 29.0f,
                rearRightTempC = 28.5f,
                recommendedPsi = 36.0f
            ),
            climate = ClimateData(
                cabinTempC = 21.5f,
                ambientTempC = 19.0f,
                driverVentTempC = 20.0f,
                passengerVentTempC = 20.0f,
                floorVentTempC = 21.5f,
                hvacPowerDrawKw = 1.1f
            ),
            auxBattery = AuxBatteryData(
                voltage = 13.8f,
                soc = 92.0f,
                current = 3.2f,
                temperatureC = 24.0f,
                ldcActive = true
            ),
            charging = ChargingData(isCharging = false),
            isDemoMode = true
        )
    }

    private fun generateHighwayCruise(t: Float): VehicleTelemetry {
        val speedKmh = 120.0f + (sin(t * 0.2f) * 3f)
        val speedMph = speedKmh * 0.621371f
        val powerKw = -28.5f + (cos(t * 0.3f) * 2.5f)
        val voltage = 712.0f
        val currentAmps = (powerKw * 1000f) / voltage
        val rpm = (speedKmh * 65f).toInt()

        currentSoc -= 0.0006f
        val cells = List(192) { i -> 3.71f + (sin(i.toFloat()) * 0.004f) }

        val currentEfficiency = 3.1f + (cos(t * 0.2f) * 0.3f)
        val rollingAvg = 3.25f

        val estRangeKm = currentSoc * 3.8f
        val estRangeMiles = estRangeKm * 0.621371f

        return VehicleTelemetry(
            vehicleModel = VehicleModel.E_GMP_UNIVERSAL,
            timestampMs = System.currentTimeMillis(),
            battery = BatteryData(
                socDisplay = currentSoc,
                socBms = currentSoc - 1.1f,
                voltage = voltage,
                current = currentAmps,
                powerKw = powerKw,
                soh = 99.4f,
                maxTempC = 33.0f,
                minTempC = 30.0f,
                avgTempC = 31.5f,
                remainingEnergyKwh = (77.4f * (currentSoc / 100f)),
                estRangeKm = estRangeKm,
                estRangeMiles = estRangeMiles
            ),
            efficiency = EfficiencyData(
                currentMiPerKwh = currentEfficiency,
                currentKmPerKwh = currentEfficiency * 1.60934f,
                rollingAverageMiPerKwh = rollingAvg,
                tripEfficiencyMiPerKwh = 3.2f,
                trendHistory = listOf(3.4f, 3.3f, 3.2f, 3.1f, 3.0f, 3.1f, 3.2f, 3.3f),
                isBetterThanAverage = currentEfficiency >= rollingAvg
            ),
            preconditioning = PreconditioningData(
                isActive = false,
                timeToTargetMin = 0,
                targetTempC = 21.0f,
                heaterPowerKw = 0f,
                coolantInletTempC = 29.5f
            ),
            cells = CellData(
                cellVoltages = cells,
                maxCellVoltage = 3.725f,
                minCellVoltage = 3.708f,
                maxCellIndex = 44,
                minCellIndex = 112,
                deltaVoltageMv = 17.0f
            ),
            drive = DriveData(
                gear = Gear.DRIVE,
                speedKmh = speedKmh,
                speedMph = speedMph,
                frontMotorRpm = rpm,
                rearMotorRpm = rpm,
                motorTempC = 64.0f,
                inverterTempC = 55.0f,
                acceleratorPercent = 24.0f,
                regenPaddleLevel = 1,
                motorPowerDrawKw = 28.5f
            ),
            tpms = TpmsData(
                frontLeftPsi = 38.0f,
                frontRightPsi = 37.8f,
                rearLeftPsi = 38.2f,
                rearRightPsi = 38.1f,
                frontLeftTempC = 38.0f,
                frontRightTempC = 38.5f,
                rearLeftTempC = 36.0f,
                rearRightTempC = 36.2f,
                recommendedPsi = 36.0f
            ),
            climate = ClimateData(cabinTempC = 21.0f, ambientTempC = 22.0f, hvacPowerDrawKw = 1.4f),
            auxBattery = AuxBatteryData(voltage = 14.1f, soc = 95.0f, current = 4.0f, ldcActive = true),
            charging = ChargingData(isCharging = false),
            isDemoMode = true
        )
    }

    private fun generateFastCharge(t: Float): VehicleTelemetry {
        // High power 800V DC Fast Charging up to ~235 kW!
        val chargingKw = 232.0f - (cos(t * 0.05f) * 8f)
        val packVoltage = 710f + (currentSoc * 0.8f)
        val chargeAmps = (chargingKw * 1000f) / packVoltage

        sessionSeconds++
        sessionChargedKwh += (chargingKw * (0.1f / 3600f))
        currentSoc += (chargingKw * (0.1f / 3600f) / 77.4f) * 100f
        if (currentSoc > 90f) currentSoc = 20f

        val cellBase = 3.70f + ((currentSoc / 100f) * 0.45f)
        val cells = List(192) { i -> cellBase + (sin(i * 0.4f) * 0.006f) }
        val estMin80 = ((80f - currentSoc).coerceAtLeast(0f) / (chargingKw / 77.4f * 60f)).toInt()

        return VehicleTelemetry(
            vehicleModel = VehicleModel.E_GMP_UNIVERSAL,
            timestampMs = System.currentTimeMillis(),
            battery = BatteryData(
                socDisplay = currentSoc,
                socBms = currentSoc - 0.8f,
                voltage = packVoltage,
                current = chargeAmps,
                powerKw = chargingKw,
                soh = 99.8f,
                maxTempC = 34.0f,
                minTempC = 31.0f,
                avgTempC = 32.5f,
                moduleTemps = listOf(32f, 33f, 34f, 34f, 33f, 32f, 32f, 33f),
                remainingEnergyKwh = (77.4f * (currentSoc / 100f)),
                estRangeKm = (currentSoc * 4.6f),
                estRangeMiles = (currentSoc * 4.6f) * 0.621371f
            ),
            efficiency = EfficiencyData(
                currentMiPerKwh = 3.8f,
                currentKmPerKwh = 6.1f,
                rollingAverageMiPerKwh = 3.6f
            ),
            preconditioning = PreconditioningData(
                isActive = false,
                timeToTargetMin = 0,
                targetTempC = 21.0f,
                heaterPowerKw = 0f,
                coolantInletTempC = 31.0f
            ),
            cells = CellData(
                cellVoltages = cells,
                maxCellVoltage = cellBase + 0.007f,
                minCellVoltage = cellBase - 0.006f,
                deltaVoltageMv = 13.0f
            ),
            drive = DriveData(gear = Gear.PARK, speedKmh = 0f, speedMph = 0f, frontMotorRpm = 0, rearMotorRpm = 0),
            tpms = TpmsData(),
            climate = ClimateData(cabinTempC = 21.0f, ambientTempC = 20.0f, batteryWarmerActive = false),
            auxBattery = AuxBatteryData(voltage = 14.4f, soc = 98.0f, current = 8.0f, ldcActive = true),
            charging = ChargingData(
                isCharging = true,
                isDcFastCharge = true,
                isAcCharge = false,
                chargingPowerKw = chargingKw,
                evseMaxCurrentA = 350.0f,
                evseMaxPowerKw = 240.0f,
                requestedCurrentA = 325.0f,
                deliveredCurrentA = chargeAmps,
                sessionKwhDelivered = sessionChargedKwh,
                sessionDurationSec = sessionSeconds / 10,
                estMinutesTo80 = estMin80,
                estMinutesTo100 = estMin80 + 15,
                pilotDutyCyclePercent = 95.0f
            ),
            isDemoMode = true
        )
    }

    private fun generateColdPreheat(t: Float): VehicleTelemetry {
        val batteryTemp = -4.0f + (t * 0.02f).coerceAtMost(25.0f)
        val heaterActive = batteryTemp < 20.0f
        val heaterKw = if (heaterActive) 4.8f else 0.0f
        val powerKw = -heaterKw - 1.2f // HVAC + Warmer

        val timeToPreheatMin = ((21.0f - batteryTemp).coerceAtLeast(0f) * 1.5f).toInt()
        val cells = List(192) { i -> 3.65f + (sin(i.toFloat()) * 0.015f) }

        return VehicleTelemetry(
            vehicleModel = VehicleModel.E_GMP_UNIVERSAL,
            timestampMs = System.currentTimeMillis(),
            battery = BatteryData(
                socDisplay = 54.0f,
                socBms = 52.5f,
                voltage = 698.0f,
                current = (powerKw * 1000f) / 698.0f,
                powerKw = powerKw,
                maxTempC = batteryTemp + 1.5f,
                minTempC = batteryTemp - 1.5f,
                avgTempC = batteryTemp,
                moduleTemps = List(8) { batteryTemp },
                remainingEnergyKwh = 41.5f,
                estRangeKm = 210f,
                estRangeMiles = 130.5f
            ),
            efficiency = EfficiencyData(
                currentMiPerKwh = 2.8f,
                currentKmPerKwh = 4.5f,
                rollingAverageMiPerKwh = 3.0f,
                isBetterThanAverage = false
            ),
            preconditioning = PreconditioningData(
                isActive = heaterActive,
                timeToTargetMin = timeToPreheatMin,
                targetTempC = 21.0f,
                heaterPowerKw = heaterKw,
                coolantInletTempC = batteryTemp + 3.0f
            ),
            cells = CellData(
                cellVoltages = cells,
                maxCellVoltage = 3.672f,
                minCellVoltage = 3.638f,
                deltaVoltageMv = 34.0f
            ),
            drive = DriveData(gear = Gear.PARK, speedKmh = 0f, speedMph = 0f),
            tpms = TpmsData(frontLeftPsi = 32.5f, frontRightPsi = 32.2f, rearLeftPsi = 32.8f, rearRightPsi = 32.4f),
            climate = ClimateData(
                cabinTempC = 18.5f,
                ambientTempC = -8.0f,
                hvacPowerDrawKw = 2.4f,
                batteryWarmerActive = heaterActive,
                batteryWarmerPowerKw = heaterKw
            ),
            auxBattery = AuxBatteryData(voltage = 13.6f, soc = 80.0f),
            charging = ChargingData(
                isCharging = false,
                timeToPreheatTempMin = timeToPreheatMin
            ),
            isDemoMode = true
        )
    }
}
