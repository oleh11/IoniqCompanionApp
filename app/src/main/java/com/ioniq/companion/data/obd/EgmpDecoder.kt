package com.ioniq.companion.data.obd

import com.ioniq.companion.data.model.AuxBatteryData
import com.ioniq.companion.data.model.BatteryData
import com.ioniq.companion.data.model.CellData
import com.ioniq.companion.data.model.ChargingData
import com.ioniq.companion.data.model.ClimateData
import com.ioniq.companion.data.model.DriveData
import com.ioniq.companion.data.model.EfficiencyData
import com.ioniq.companion.data.model.Gear
import com.ioniq.companion.data.model.PreconditioningData
import com.ioniq.companion.data.model.TpmsData
import com.ioniq.companion.data.model.VehicleTelemetry
import com.ioniq.companion.util.AppLogger
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Decodes raw ISO 14229 UDS hexadecimal payloads for Hyundai/Kia E-GMP platforms
 * (IONIQ 5, IONIQ 5 N, IONIQ 6, IONIQ 9, EV6, EV9, GV60).
 * Implements full E-GMP diagnostic specifications matching EV Dashboard / gburlingame/ioniq-app.
 */
object EgmpDecoder {

    private const val TAG = "DECODER"

    /**
     * Decodes BMS Main Telemetry (Header 7E4 -> 7EC, Service 22, PID 0101)
     */
    fun decodeBmsMain(hex: String, current: VehicleTelemetry): VehicleTelemetry {
        return try {
            val bytes = hexToBytes(hex)
            if (bytes.size < 35) return current

            // Find positive response echo "62 01 01"
            val offset = findEchoOffset(bytes, 0x62.toByte(), 0x01.toByte(), 0x01.toByte())
            if (offset < 0 || bytes.size < offset + 30) return current

            // SOC BMS (Byte 19 relative to 0101 frame * 0.5 %)
            val socBms = (bytes.getOrNull(offset + 19)?.toUByte()?.toFloat() ?: 134f) * 0.5f

            // Battery Current (Bytes 13-14 as signed 16-bit integer * 0.1 A)
            val currentHigh = bytes.getOrNull(offset + 13)?.toInt() ?: 0
            val currentLow = bytes.getOrNull(offset + 14)?.toUByte()?.toInt() ?: 0
            val currentSigned = ((currentHigh shl 8) or currentLow).toShort()
            val currentAmps = currentSigned * 0.1f

            // Battery DC Voltage (Bytes 15-16 as uint16 * 0.1 V)
            val voltHigh = bytes.getOrNull(offset + 15)?.toUByte()?.toInt() ?: 0
            val voltLow = bytes.getOrNull(offset + 16)?.toUByte()?.toInt() ?: 0
            val packVolts = ((voltHigh shl 8) or voltLow) * 0.1f

            // Max & Min Cell Temperatures (Bytes 17, 18 as int8 - 40 °C)
            val maxTemp = (bytes.getOrNull(offset + 17)?.toInt() ?: 65) - 40f
            val minTemp = (bytes.getOrNull(offset + 18)?.toInt() ?: 63) - 40f
            val avgTemp = (maxTemp + minTemp) / 2.0f

            // SOH - State of Health (Bytes 28-29 as uint16 * 0.1 %)
            val sohHigh = bytes.getOrNull(offset + 28)?.toUByte()?.toInt() ?: 0
            val sohLow = bytes.getOrNull(offset + 29)?.toUByte()?.toInt() ?: 0
            val sohPercent = if (sohHigh > 0) ((sohHigh shl 8) or sohLow) * 0.1f else current.battery.soh

            // Aux 12V Battery Voltage (Byte 32 * 0.1 V)
            val auxVolts = (bytes.getOrNull(offset + 32)?.toUByte()?.toFloat() ?: 138f) * 0.1f

            // SOC Display (Byte 34 * 0.5 %)
            val socDisplay = (bytes.getOrNull(offset + 34)?.toUByte()?.toFloat() ?: (socBms * 2)) * 0.5f

            // Instantaneous Power (kW) = (Voltage * Current) / 1000
            val powerKw = (packVolts * currentAmps) / 1000.0f

            // Cumulative Energy Charged (Bytes 41-44 as uint32 * 0.1 kWh)
            var cumCharged = current.battery.cumulativeEnergyChargedKwh
            if (bytes.size >= offset + 45) {
                val b41 = bytes[offset + 41].toUByte().toLong()
                val b42 = bytes[offset + 42].toUByte().toLong()
                val b43 = bytes[offset + 43].toUByte().toLong()
                val b44 = bytes[offset + 44].toUByte().toLong()
                val rawVal = (b41 shl 24) or (b42 shl 16) or (b43 shl 8) or b44
                if (rawVal > 0) cumCharged = rawVal * 0.1f
            }

            // Cumulative Energy Discharged (Bytes 45-48 as uint32 * 0.1 kWh)
            var cumDischarged = current.battery.cumulativeEnergyDischargedKwh
            if (bytes.size >= offset + 49) {
                val b45 = bytes[offset + 45].toUByte().toLong()
                val b46 = bytes[offset + 46].toUByte().toLong()
                val b47 = bytes[offset + 47].toUByte().toLong()
                val b48 = bytes[offset + 48].toUByte().toLong()
                val rawVal = (b45 shl 24) or (b46 shl 16) or (b47 shl 8) or b48
                if (rawVal > 0) cumDischarged = rawVal * 0.1f
            }

            // Isolation Resistance (Bytes 53-54 in kΩ)
            var isolationResistance = current.battery.isolationResistanceKohm
            if (bytes.size >= offset + 55) {
                val isoHigh = bytes[offset + 53].toUByte().toInt()
                val isoLow = bytes[offset + 54].toUByte().toInt()
                isolationResistance = (isoHigh shl 8) or isoLow
            }

            // Battery Heater / Preconditioning Active detection
            // Byte 38 bit 0 or heater power draw
            val heaterActive = (bytes.getOrNull(offset + 38)?.toUByte()?.toInt() ?: 0) and 0x01 != 0
            val heaterKw = if (heaterActive) 4.5f else 0f

            // Live "Time to 21°C / 70°F" Preconditioning ETA calculation
            val targetTemp = 21.0f
            val timeToTargetMin = if (minTemp < targetTemp && (heaterActive || current.preconditioning.isActive)) {
                ((targetTemp - minTemp) / 0.65f).roundToInt().coerceAtLeast(1)
            } else if (minTemp >= targetTemp) {
                0
            } else {
                ((targetTemp - minTemp) / 0.35f).roundToInt().coerceAtLeast(0)
            }

            // Charging detection
            val isCharging = currentAmps > 1.0f
            val isDcFast = isCharging && currentAmps > 32.0f
            val isAc = isCharging && !isDcFast

            val remainingKwh = (current.vehicleModel.packCapacityKwh * (socDisplay / 100f))
            val estRangeKm = remainingKwh * (current.efficiency.currentKmPerKwh.coerceIn(3.0f, 8.0f))
            val estRangeMiles = estRangeKm * 0.621371f

            val updatedBattery = current.battery.copy(
                socDisplay = socDisplay.coerceIn(0f, 100f),
                socBms = socBms.coerceIn(0f, 100f),
                voltage = if (packVolts > 0f) packVolts else current.battery.voltage,
                current = currentAmps,
                powerKw = powerKw,
                soh = sohPercent.coerceIn(50f, 100f),
                maxTempC = maxTemp,
                minTempC = minTemp,
                avgTempC = avgTemp,
                isolationResistanceKohm = isolationResistance,
                cumulativeEnergyChargedKwh = cumCharged,
                cumulativeEnergyDischargedKwh = cumDischarged,
                remainingEnergyKwh = remainingKwh,
                estRangeKm = estRangeKm,
                estRangeMiles = estRangeMiles
            )

            val updatedPreconditioning = current.preconditioning.copy(
                isActive = heaterActive || (minTemp < 18f && powerKw > 15f),
                timeToTargetMin = timeToTargetMin,
                targetTempC = targetTemp,
                heaterPowerKw = heaterKw,
                coolantInletTempC = minTemp - 1.5f
            )

            val updatedAux = current.auxBattery.copy(
                voltage = if (auxVolts > 5f) auxVolts else current.auxBattery.voltage,
                ldcActive = auxVolts > 13.2f,
                soc = ((auxVolts - 11.8f) / (12.8f - 11.8f) * 100f).coerceIn(10f, 100f)
            )

            val updatedCharging = current.charging.copy(
                isCharging = isCharging,
                isDcFastCharge = isDcFast,
                isAcCharge = isAc,
                chargingPowerKw = if (isCharging) abs(powerKw) else 0f
            )

            current.copy(
                battery = updatedBattery,
                preconditioning = updatedPreconditioning,
                auxBattery = updatedAux,
                charging = updatedCharging
            )
        } catch (e: Throwable) {
            AppLogger.e(TAG, "decodeBmsMain error: ${e.message}")
            current
        }
    }

    /**
     * Decodes 192 individual cell voltages from PIDs 220102 .. 220106
     */
    fun decodeCellVoltages(hex: String, pid: String, current: VehicleTelemetry): VehicleTelemetry {
        return try {
            val bytes = hexToBytes(hex)
            if (bytes.size < 10) return current

            val offset = findEchoOffset(bytes, 0x62.toByte(), 0x01.toByte(), pid.takeLast(2).toInt(16).toByte())
            val startIdx = if (offset >= 0) offset + 3 else 0

            val currentCells = current.cells.cellVoltages.toMutableList()
            if (currentCells.size < 192) {
                while (currentCells.size < 192) currentCells.add(3.75f)
            }

            val startIndex = when (pid) {
                "220102" -> 0    // Cells 1 - 32
                "220103" -> 32   // Cells 33 - 64
                "220104" -> 64   // Cells 65 - 96
                "220105" -> 96   // Cells 97 - 128
                "220106" -> 128  // Cells 129 - 192
                else -> 0
            }

            var cellOffset = startIndex
            var i = startIdx
            while (i < bytes.size - 1 && cellOffset < 192) {
                val high = bytes[i].toUByte().toInt()
                val low = bytes[i + 1].toUByte().toInt()
                val millivolts = (high shl 8) or low
                val cellV = (millivolts * 0.02f) / 1000.0f

                val normalizedV = when {
                    cellV in 2.5f..4.4f -> cellV
                    millivolts in 2500..4400 -> millivolts / 1000f
                    else -> 3.75f
                }

                currentCells[cellOffset] = normalizedV
                cellOffset++
                i += 2
            }

            val maxV = currentCells.maxOrNull() ?: 3.78f
            val minV = currentCells.minOrNull() ?: 3.76f
            val maxIdx = currentCells.indexOf(maxV).coerceAtLeast(0)
            val minIdx = currentCells.indexOf(minV).coerceAtLeast(0)
            val deltaMv = (maxV - minV) * 1000f

            current.copy(
                cells = CellData(
                    cellVoltages = currentCells,
                    deltaVoltageMv = deltaMv,
                    maxCellVoltage = maxV,
                    minCellVoltage = minV,
                    maxCellIndex = maxIdx,
                    minCellIndex = minIdx
                )
            )
        } catch (e: Throwable) {
            AppLogger.e(TAG, "decodeCellVoltages error: ${e.message}")
            current
        }
    }

    /**
     * Decodes VMCU Powertrain & Inverter Data (Header 7E2 -> 7EA, Service 22, PID 0101)
     */
    fun decodeVmcu(hex: String, current: VehicleTelemetry): VehicleTelemetry {
        return try {
            val bytes = hexToBytes(hex)
            if (bytes.size < 20) return current

            val offset = findEchoOffset(bytes, 0x62.toByte(), 0x01.toByte(), 0x01.toByte())
            val startIdx = if (offset >= 0) offset else 0

            // Motor Speed (Bytes 14-15 relative to VMCU frame)
            val rpmHigh = bytes.getOrNull(startIdx + 14)?.toInt() ?: 0
            val rpmLow = bytes.getOrNull(startIdx + 15)?.toUByte()?.toInt() ?: 0
            val rawRpm = ((rpmHigh shl 8) or rpmLow).toShort().toInt()

            // Inverter Temp (Byte 16 int8 - 40)
            val invTemp = (bytes.getOrNull(startIdx + 16)?.toInt() ?: 65) - 40f

            // Motor Temp (Byte 17 int8 - 40)
            val motorTemp = (bytes.getOrNull(startIdx + 17)?.toInt() ?: 65) - 40f

            // Gear (Byte 9 low nibble: 0=P, 5=D, 6=N, 7=R)
            val gearByte = bytes.getOrNull(startIdx + 9)?.toUByte()?.toInt() ?: 0
            val gear = when (gearByte and 0x0F) {
                0x00 -> Gear.PARK
                0x05 -> Gear.DRIVE
                0x06 -> Gear.NEUTRAL
                0x07 -> Gear.REVERSE
                else -> current.drive.gear
            }

            // Brake Light flag: Bit 3 of byte 10 (gburlingame discovery)
            val byte10 = bytes.getOrNull(startIdx + 10)?.toUByte()?.toInt() ?: 0
            val brakeActive = (byte10 and 0x08) != 0

            val speedKmh = abs(rawRpm / 65f)
            val speedMph = speedKmh * 0.621371f

            current.copy(
                drive = current.drive.copy(
                    speedKmh = speedKmh,
                    speedMph = speedMph,
                    frontMotorRpm = abs(rawRpm),
                    gear = gear,
                    brakePedalActive = brakeActive,
                    inverterTempC = invTemp,
                    motorTempC = motorTemp
                )
            )
        } catch (e: Throwable) {
            AppLogger.e(TAG, "decodeVmcu error: ${e.message}")
            current
        }
    }

    /**
     * Decodes TPMS Tire Data (Header 7A0 -> 7A8, PID C00B)
     */
    fun decodeTpms(hex: String, current: VehicleTelemetry): VehicleTelemetry {
        return try {
            val bytes = hexToBytes(hex)
            if (bytes.size < 12) return current

            val flPsi = (bytes.getOrNull(4)?.toUByte()?.toFloat() ?: 180f) * 0.2f
            val frPsi = (bytes.getOrNull(5)?.toUByte()?.toFloat() ?: 180f) * 0.2f
            val rlPsi = (bytes.getOrNull(6)?.toUByte()?.toFloat() ?: 180f) * 0.2f
            val rrPsi = (bytes.getOrNull(7)?.toUByte()?.toFloat() ?: 180f) * 0.2f

            val flTemp = (bytes.getOrNull(8)?.toInt() ?: 70) - 50f
            val frTemp = (bytes.getOrNull(9)?.toInt() ?: 70) - 50f
            val rlTemp = (bytes.getOrNull(10)?.toInt() ?: 70) - 50f
            val rrTemp = (bytes.getOrNull(11)?.toInt() ?: 70) - 50f

            val isLow = listOf(flPsi, frPsi, rlPsi, rrPsi).any { it > 0f && it < 30.0f }

            current.copy(
                tpms = TpmsData(
                    frontLeftPsi = flPsi,
                    frontRightPsi = frPsi,
                    rearLeftPsi = rlPsi,
                    rearRightPsi = rrPsi,
                    frontLeftTempC = flTemp,
                    frontRightTempC = frTemp,
                    rearLeftTempC = rlTemp,
                    rearRightTempC = rrTemp,
                    isWarningActive = isLow
                )
            )
        } catch (e: Throwable) {
            AppLogger.e(TAG, "decodeTpms error: ${e.message}")
            current
        }
    }

    /**
     * Decodes Climate / FATC Data (Header 7B3 -> 7BB, PID 0100)
     */
    fun decodeClimate(hex: String, current: VehicleTelemetry): VehicleTelemetry {
        return try {
            val bytes = hexToBytes(hex)
            if (bytes.size < 10) return current

            val cabinTemp = (bytes.getOrNull(4)?.toUByte()?.toFloat() ?: 43f) * 0.5f
            val ambientTemp = ((bytes.getOrNull(5)?.toInt() ?: 76) - 40).toFloat()
            val ventTemp = (bytes.getOrNull(6)?.toUByte()?.toFloat() ?: 40f) * 0.5f
            val hvacPower = (bytes.getOrNull(8)?.toUByte()?.toFloat() ?: 12f) * 0.1f

            current.copy(
                climate = current.climate.copy(
                    cabinTempC = cabinTemp,
                    ambientTempC = ambientTemp,
                    driverVentTempC = ventTemp,
                    passengerVentTempC = ventTemp,
                    floorVentTempC = ventTemp + 1.5f,
                    hvacPowerDrawKw = hvacPower
                )
            )
        } catch (e: Throwable) {
            AppLogger.e(TAG, "decodeClimate error: ${e.message}")
            current
        }
    }

    /**
     * Safely converts hex string to ByteArray without throwing NumberFormatException
     */
    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.filter { c ->
            (c in '0'..'9') || (c in 'a'..'f') || (c in 'A'..'F')
        }
        if (clean.length < 2) return byteArrayOf()

        val evenLen = if (clean.length % 2 != 0) clean.length - 1 else clean.length
        val result = ByteArray(evenLen / 2)

        for (i in 0 until evenLen step 2) {
            val byteVal = try {
                clean.substring(i, i + 2).toInt(16).toByte()
            } catch (_: Exception) {
                0.toByte()
            }
            result[i / 2] = byteVal
        }
        return result
    }

    private fun findEchoOffset(bytes: ByteArray, b1: Byte, b2: Byte, b3: Byte): Int {
        if (bytes.size < 3) return -1
        for (i in 0..bytes.size - 3) {
            if (bytes[i] == b1 && bytes[i + 1] == b2 && bytes[i + 2] == b3) {
                return i
            }
        }
        return -1
    }
}
