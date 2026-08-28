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

/**
 * In-Car Android Auto Battery Cells & Health Diagnostics Screen
 */
class AutoCellDiagnosticsScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

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
            return MessageTemplate.Builder("OBD-II Adapter Disconnected\n\nPlease connect your Veepeak Bluetooth adapter in the phone app to stream cell diagnostics.")
                .setTitle("Cell Diagnostics • Disconnected")
                .setHeaderAction(Action.BACK)
                .addAction(
                    Action.Builder()
                        .setTitle("Refresh")
                        .setOnClickListener { invalidate() }
                        .build()
                )
                .build()
        }

        val paneBuilder = Pane.Builder()

        paneBuilder.addRow(
            Row.Builder()
                .setTitle("📊 Cell Delta: ΔV ${t.cells.deltaVoltageMv.toInt()} mV")
                .addText("Max: ${String.format("%.3f", t.cells.maxCellVoltage)} V (#${t.cells.maxCellIndex + 1})  •  Min: ${String.format("%.3f", t.cells.minCellVoltage)} V (#${t.cells.minCellIndex + 1})")
                .build()
        )

        paneBuilder.addRow(
            Row.Builder()
                .setTitle("🏥 Pack Health (SOH): ${String.format("%.1f", t.battery.soh)}%")
                .addText("Isolation Resistance: ${t.battery.isolationResistanceKohm} kΩ")
                .build()
        )

        paneBuilder.addRow(
            Row.Builder()
                .setTitle("🔋 Aux 12V Battery: ${String.format("%.2f", t.auxBattery.voltage)} V (${t.auxBattery.soc.toInt()}%)")
                .addText("LDC Converter: ${String.format("%.1f", t.auxBattery.current)} A  •  Temp: ${t.auxBattery.temperatureC.toInt()} °C")
                .build()
        )

        paneBuilder.addRow(
            Row.Builder()
                .setTitle("⚡ Total Lifetime Charged: ${String.format("%.1f", t.battery.cumulativeEnergyChargedKwh)} kWh")
                .addText("Total Discharged: ${String.format("%.1f", t.battery.cumulativeEnergyDischargedKwh)} kWh")
                .build()
        )

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("Cell & Battery Diagnostics")
            .setHeaderAction(Action.BACK)
            .build()
    }
}
