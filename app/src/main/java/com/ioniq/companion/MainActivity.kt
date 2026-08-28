package com.ioniq.companion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ioniq.companion.data.repository.TelemetryRepository
import com.ioniq.companion.ui.screens.BatteryCellsScreen
import com.ioniq.companion.ui.screens.ChargingScreen
import com.ioniq.companion.ui.screens.DashboardScreen
import com.ioniq.companion.ui.screens.DiagnosticsScreen
import com.ioniq.companion.ui.screens.SettingsScreen
import com.ioniq.companion.ui.screens.VehicleTpmsScreen
import com.ioniq.companion.ui.theme.CockpitBg
import com.ioniq.companion.ui.theme.IoniqCompanionTheme
import com.ioniq.companion.ui.theme.IoniqCyan
import com.ioniq.companion.ui.theme.SurfaceCard
import com.ioniq.companion.ui.theme.SurfaceCardSecondary
import com.ioniq.companion.ui.theme.TextMuted
import com.ioniq.companion.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {

    private val repository by lazy { TelemetryRepository.getInstance(this) }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permissions handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkBluetoothPermissions()

        setContent {
            IoniqCompanionTheme {
                MainApp(repository = repository)
            }
        }
    }

    private fun checkBluetoothPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Drive", Icons.Default.Speed)
    data object Battery : Screen("battery", "Cells", Icons.Default.GridOn)
    data object Charging : Screen("charging", "Charge", Icons.Default.EvStation)
    data object Vehicle : Screen("vehicle", "TPMS", Icons.Default.DirectionsCar)
    data object Diagnostics : Screen("diagnostics", "DTC", Icons.Default.Build)
    data object Settings : Screen("settings", "Setup", Icons.Default.Settings)
}

@Composable
fun MainApp(repository: TelemetryRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val telemetry by repository.telemetry.collectAsStateWithLifecycle()
    val powerHistory by repository.powerHistory.collectAsStateWithLifecycle()
    val connectionState by repository.bluetoothManager.connectionState.collectAsStateWithLifecycle()
    val discoveredDevices by repository.bluetoothManager.discoveredDevices.collectAsStateWithLifecycle()
    val isDemoMode by repository.isDemoMode.collectAsStateWithLifecycle()
    val selectedVehicleModel by repository.selectedVehicleModel.collectAsStateWithLifecycle()

    val screens = listOf(
        Screen.Dashboard,
        Screen.Battery,
        Screen.Charging,
        Screen.Vehicle,
        Screen.Diagnostics,
        Screen.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CockpitBg,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceCard,
                tonalElevation = 4.dp
            ) {
                screens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        selected = isSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IoniqCyan,
                            selectedTextColor = IoniqCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = SurfaceCardSecondary
                        ),
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    telemetry = telemetry,
                    powerHistory = powerHistory,
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Battery.route) {
                BatteryCellsScreen(
                    telemetry = telemetry
                )
            }
            composable(Screen.Charging.route) {
                ChargingScreen(
                    telemetry = telemetry
                )
            }
            composable(Screen.Vehicle.route) {
                VehicleTpmsScreen(
                    telemetry = telemetry
                )
            }
            composable(Screen.Diagnostics.route) {
                DiagnosticsScreen(
                    telemetry = telemetry,
                    onExportCsv = { repository.generateCsvExport() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    connectionState = connectionState,
                    discoveredDevices = discoveredDevices,
                    isDemoMode = isDemoMode,
                    selectedVehicleModel = selectedVehicleModel,
                    onStartScan = { repository.bluetoothManager.startScan() },
                    onConnectDevice = { device -> repository.connectToDevice(device) },
                    onToggleDemoMode = { enabled -> repository.setDemoMode(enabled) },
                    onSelectScenario = { scenario -> repository.setSimulationScenario(scenario) },
                    onSelectVehicleModel = { model -> repository.selectedVehicleModel.value = model }
                )
            }
        }
    }
}
