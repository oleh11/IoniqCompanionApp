package com.ioniq.companion.auto.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ioniq.companion.data.repository.TelemetryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * In-Car Android Auto Charging Screen
 */
class AutoChargingScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    private val repository = TelemetryRepository.getInstance(carContext)
    private var telemetryJob: Job? = null

    init {
        lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        telemetryJob = CoroutineScope(Dispatchers.Main).launch {
            repository.telemetry.collect {
                invalidate()
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        telemetryJob?.cancel()
        telemetryJob = null
    }

    override fun onGetTemplate(): Template {
        val t = repository.telemetry.value

        if (!t.isConnected && !t.isDemoMode) {
            return MessageTemplate.Builder("OBD-II Adapter Disconnected\n\nPlease connect your Veepeak Bluetooth adapter in the phone app to stream charging telemetry.")
                .setTitle("Charging • Disconnected")
                .setHeaderAction(Action.BACK)
                .addAction(
                    Action.Builder()
                        .setTitle("Refresh")
                        .setOnClickListener { invalidate() }
                        .build()
                )
                .build()
        }

        val isCharging = t.charging.isCharging || t.battery.powerKw > 0.5f
        val kw = if (t.charging.isCharging) t.charging.chargingPowerKw else abs(t.battery.powerKw)

        val paneBuilder = Pane.Builder()

        paneBuilder.addRow(
            Row.Builder()
                .setTitle("⚡ Charging Speed: ${String.format("%.1f", kw)} kW")
                .addText("Pack: ${t.battery.voltage.toInt()} V  •  Delivered: ${String.format("%.1f", abs(t.battery.current))} A")
                .build()
        )

        paneBuilder.addRow(
            Row.Builder()
                .setTitle("⏱️ Time to 80%: ${if (isCharging) "${t.charging.estMinutesTo80} min" else "--"}")
                .addText("Energy Added: ${String.format("%.2f", t.charging.sessionKwhDelivered)} kWh  •  SOC: ${t.battery.socDisplay.toInt()}%")
                .build()
        )

        paneBuilder.addRow(
            Row.Builder()
                .setTitle("🔌 EVSE Station Limit: ${t.charging.evseMaxPowerKw.toInt()} kW (${t.charging.evseMaxCurrentA.toInt()} A)")
                .addText("Requested Current: ${t.charging.requestedCurrentA.toInt()} A")
                .build()
        )

        paneBuilder.addRow(
            Row.Builder()
                .setTitle("🌡️ Battery Temp: ${String.format("%.1f", t.battery.avgTempC)} °C")
                .addText(if (t.battery.avgTempC >= 21.0f) "Battery at optimal fast-charge temperature" else "Warming to optimal 21°C / 70°F")
                .build()
        )

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("Charging Telemetry")
            .setHeaderAction(Action.BACK)
            .build()
    }
}
