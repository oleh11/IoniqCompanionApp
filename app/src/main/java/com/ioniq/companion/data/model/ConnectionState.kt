package com.ioniq.companion.data.model

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Scanning : ConnectionState
    data class Connecting(val deviceName: String) : ConnectionState
    data class Handshaking(val deviceName: String, val step: String) : ConnectionState
    data class Connected(val deviceName: String, val protocol: String) : ConnectionState
    data class Error(val message: String) : ConnectionState
}

data class DiscoveredBluetoothDevice(
    val name: String,
    val address: String,
    val isBle: Boolean,
    val isVeepeak: Boolean,
    val rssi: Int = -50
)

enum class SimulationScenario(val displayName: String, val description: String) {
    CITY_DRIVE("City Driving & Regen", "Active stop-and-go with high regen paddle deceleration and dynamic power bursts."),
    HIGHWAY_CRUISE("Highway Cruise 75 MPH", "Sustained high speed power draw, inverter/motor thermal rise, and aerodynamic drag."),
    DC_FAST_CHARGE("800V DC Fast Charging", "Peak 235 kW charge curve, battery warming up to 21°C, and EVSE telemetry."),
    COLD_BATTERY_PREHEAT("Cold Weather Pre-Conditioning", "Sub-zero battery pack with battery heater firing and low cell balancing.")
}

enum class PowerUnit(val symbol: String) {
    KW("kW"),
    HP("hp"),
    PS("PS")
}

enum class PressureUnit(val symbol: String, val toPsiMultiplier: Float) {
    PSI("psi", 1.0f),
    BAR("bar", 0.0689476f),
    KPA("kPa", 6.89476f)
}

enum class TempUnit(val symbol: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F")
}

enum class DistanceUnit(val symbol: String) {
    KILOMETERS("km"),
    MILES("mi")
}
