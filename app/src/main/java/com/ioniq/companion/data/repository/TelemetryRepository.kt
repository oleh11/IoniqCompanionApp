package com.ioniq.companion.data.repository

import android.content.Context
import com.ioniq.companion.data.bluetooth.BluetoothManager
import com.ioniq.companion.data.model.ConnectionState
import com.ioniq.companion.data.model.DiscoveredBluetoothDevice
import com.ioniq.companion.data.model.DistanceUnit
import com.ioniq.companion.data.model.PowerUnit
import com.ioniq.companion.data.model.PressureUnit
import com.ioniq.companion.data.model.SimulationScenario
import com.ioniq.companion.data.model.TempUnit
import com.ioniq.companion.data.model.VehicleModel
import com.ioniq.companion.data.model.VehicleTelemetry
import com.ioniq.companion.data.obd.EgmpDecoder
import com.ioniq.companion.data.obd.Elm327Engine
import com.ioniq.companion.data.simulator.VehicleSimulator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Singleton repository coordinating Bluetooth OBD telemetry, E-GMP decoding,
 * optional offline simulation, and user display preferences.
 */
class TelemetryRepository private constructor(context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val bluetoothManager = BluetoothManager(context.applicationContext)
    val elm327Engine = Elm327Engine(bluetoothManager)
    val simulator = VehicleSimulator()

    // Demo mode is OFF by default: Real OBD data is expected unless user explicitly enables demo mode in settings
    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _liveTelemetry = MutableStateFlow(VehicleTelemetry(isDemoMode = false, isConnected = false))

    // Active telemetry flow (observable by Phone UI & Android Auto)
    val telemetry: StateFlow<VehicleTelemetry> = MutableStateFlow(VehicleTelemetry(isDemoMode = false, isConnected = false))

    // Chart history (rolling 60 points)
    private val _powerHistory = MutableStateFlow<List<Pair<Long, Float>>>(emptyList())
    val powerHistory: StateFlow<List<Pair<Long, Float>>> = _powerHistory.asStateFlow()

    // User settings
    val selectedVehicleModel = MutableStateFlow(VehicleModel.E_GMP_UNIVERSAL)
    val powerUnit = MutableStateFlow(PowerUnit.KW)
    val pressureUnit = MutableStateFlow(PressureUnit.PSI)
    val tempUnit = MutableStateFlow(TempUnit.FAHRENHEIT)
    val distanceUnit = MutableStateFlow(DistanceUnit.MILES)

    init {
        // Collect simulator data ONLY when demo mode is explicitly enabled
        scope.launch {
            simulator.telemetry.collect { simData ->
                if (_isDemoMode.value) {
                    (telemetry as MutableStateFlow).value = simData.copy(
                        vehicleModel = selectedVehicleModel.value,
                        isDemoMode = true,
                        isConnected = true
                    )
                    recordHistoryPoint(simData.battery.powerKw)
                }
            }
        }

        // Collect live OBD telemetry
        scope.launch {
            _liveTelemetry.collect { liveData ->
                if (!_isDemoMode.value) {
                    val isConnected = bluetoothManager.connectionState.value is ConnectionState.Connected
                    (telemetry as MutableStateFlow).value = liveData.copy(
                        vehicleModel = selectedVehicleModel.value,
                        isDemoMode = false,
                        isConnected = isConnected
                    )
                    if (isConnected) {
                        recordHistoryPoint(liveData.battery.powerKw)
                    }
                }
            }
        }

        // Process incoming OBD responses from vehicle ECUs
        scope.launch {
            elm327Engine.rawResponses.collect { resp ->
                val current = _liveTelemetry.value
                val updated = when (resp.header) {
                    "7E4", "7EC" -> {
                        when (resp.command) {
                            "220101" -> EgmpDecoder.decodeBmsMain(resp.hexPayload, current)
                            "220102", "220103", "220104", "220105", "220106" ->
                                EgmpDecoder.decodeCellVoltages(resp.hexPayload, resp.command, current)
                            else -> current
                        }
                    }
                    "7E2", "7EA" -> EgmpDecoder.decodeVmcu(resp.hexPayload, current)
                    "7A0", "7A8" -> EgmpDecoder.decodeTpms(resp.hexPayload, current)
                    "7B3", "7BB" -> EgmpDecoder.decodeClimate(resp.hexPayload, current)
                    else -> current
                }
                _liveTelemetry.value = updated.copy(isConnected = true, isDemoMode = false)
            }
        }

        // Manage connection lifecycle
        scope.launch {
            bluetoothManager.connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        if (!_isDemoMode.value) {
                            _liveTelemetry.value = _liveTelemetry.value.copy(isConnected = true)
                            elm327Engine.startPolling()
                        }
                    }
                    is ConnectionState.Disconnected -> {
                        _liveTelemetry.value = _liveTelemetry.value.copy(isConnected = false)
                        elm327Engine.stopPolling()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun recordHistoryPoint(powerKw: Float) {
        val current = _powerHistory.value.toMutableList()
        current.add(Pair(System.currentTimeMillis(), powerKw))
        if (current.size > 60) {
            current.removeAt(0)
        }
        _powerHistory.value = current
    }

    fun setDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
        if (enabled) {
            elm327Engine.stopPolling()
            bluetoothManager.disconnect()
            simulator.start()
        } else {
            simulator.stop()
            (telemetry as MutableStateFlow).value = VehicleTelemetry(
                vehicleModel = selectedVehicleModel.value,
                isDemoMode = false,
                isConnected = bluetoothManager.connectionState.value is ConnectionState.Connected
            )
            if (bluetoothManager.connectionState.value is ConnectionState.Connected) {
                elm327Engine.startPolling()
            }
        }
    }

    fun setSimulationScenario(scenario: SimulationScenario) {
        simulator.setScenario(scenario)
    }

    fun connectToDevice(device: DiscoveredBluetoothDevice) {
        setDemoMode(false)
        bluetoothManager.connectToDevice(device)
    }

    fun generateCsvExport(): String {
        val current = telemetry.value
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        sb.append("Timestamp,Vehicle,Connected,SOC Display %,SOC BMS %,Voltage V,Current A,Power kW,Min Temp C,Max Temp C,Delta V mV,Gear,Speed kmh,TPMS FL psi,TPMS FR psi,TPMS RL psi,TPMS RR psi,Aux 12V V\n")
        sb.append("${dateFormat.format(Date(current.timestampMs))},")
        sb.append("${current.vehicleModel.displayName},")
        sb.append("${current.isConnected},")
        sb.append("${current.battery.socDisplay},")
        sb.append("${current.battery.socBms},")
        sb.append("${current.battery.voltage},")
        sb.append("${current.battery.current},")
        sb.append("${current.battery.powerKw},")
        sb.append("${current.battery.minTempC},")
        sb.append("${current.battery.maxTempC},")
        sb.append("${current.cells.deltaVoltageMv},")
        sb.append("${current.drive.gear.label},")
        sb.append("${current.drive.speedKmh},")
        sb.append("${current.tpms.frontLeftPsi},")
        sb.append("${current.tpms.frontRightPsi},")
        sb.append("${current.tpms.rearLeftPsi},")
        sb.append("${current.tpms.rearRightPsi},")
        sb.append("${current.auxBattery.voltage}\n")
        return sb.toString()
    }

    companion object {
        @Volatile
        private var instance: TelemetryRepository? = null

        fun getInstance(context: Context): TelemetryRepository {
            return instance ?: synchronized(this) {
                instance ?: TelemetryRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
