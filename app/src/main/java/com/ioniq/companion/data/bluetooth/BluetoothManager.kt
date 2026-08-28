package com.ioniq.companion.data.bluetooth

import android.annotation.SuppressLint
import android.content.Context
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import com.ioniq.companion.data.model.ConnectionState
import com.ioniq.companion.data.model.DiscoveredBluetoothDevice
import com.ioniq.companion.util.AppLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * High-reliability Bluetooth engine for Veepeak OBD-II adapters.
 * Fully instrumented with AppLogger and crash-proof CompletableDeferred async state machines.
 */
class BluetoothManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val TAG = "BLUETOOTH"

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        manager?.adapter
    }

    private val _incomingDataFlow = MutableSharedFlow<String>(extraBufferCapacity = 128)
    val incomingDataFlow: SharedFlow<String> = _incomingDataFlow.asSharedFlow()

    // Classic Bluetooth SPP
    private var sppSocket: BluetoothSocket? = null
    private var sppOutputStream: OutputStream? = null
    private var sppInputStream: InputStream? = null

    // BLE GATT
    private var bleGatt: BluetoothGatt? = null
    private var bleTxCharacteristic: BluetoothGattCharacteristic? = null
    private var bleRxCharacteristic: BluetoothGattCharacteristic? = null

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

        private val KNOWN_BLE_SERVICES = listOf(
            UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB"), // Veepeak / Vgate / ELM327
            UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"), // Nordic UART NUS (Veepeak BLE+, vLinker)
            UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB"), // Carista / Veepeak BLE+
            UUID.fromString("000018F0-0000-1000-8000-00805F9B34FB"), // Veepeak iOS/Android Dual
            UUID.fromString("E7810A71-73AE-499D-8C15-FAA9AEF0C3F2")  // vLinker MC/FD
        )
    }

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: "Unknown Bluetooth Device"
            val isVeepeak = isVeepeakDeviceName(name) || isOBDDeviceName(name)

            val discovered = DiscoveredBluetoothDevice(
                name = name,
                address = device.address,
                isBle = true,
                isVeepeak = isVeepeak,
                rssi = result.rssi
            )

            updateAndSortDevices(discovered)
        }

        override fun onScanFailed(errorCode: Int) {
            AppLogger.e(TAG, "BLE Scan failed with errorCode: $errorCode")
        }
    }

    @Synchronized
    private fun updateAndSortDevices(newDevice: DiscoveredBluetoothDevice) {
        val current = _discoveredDevices.value.toMutableList()
        val index = current.indexOfFirst { it.address == newDevice.address }

        if (index >= 0) {
            current[index] = current[index].copy(
                rssi = newDevice.rssi,
                name = if (newDevice.name != "Unknown Bluetooth Device") newDevice.name else current[index].name,
                isVeepeak = newDevice.isVeepeak || current[index].isVeepeak
            )
        } else {
            AppLogger.d(TAG, "Discovered device: '${newDevice.name}' (${newDevice.address}) RSSI: ${newDevice.rssi} dBm (Veepeak: ${newDevice.isVeepeak})")
            current.add(newDevice)
        }

        // Sort: Veepeak/OBD devices first, then sort by highest RSSI (-35 dBm > -70 dBm)
        _discoveredDevices.value = current.sortedWith(
            compareByDescending<DiscoveredBluetoothDevice> { it.isVeepeak }
                .thenByDescending { it.rssi }
        )
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        _discoveredDevices.value = emptyList()
        _connectionState.value = ConnectionState.Scanning
        AppLogger.i(TAG, "Starting Bluetooth Scan...")

        val adapter = bluetoothAdapter
        if (adapter == null) {
            AppLogger.e(TAG, "BluetoothAdapter is NULL or disabled on this phone!")
            _connectionState.value = ConnectionState.Error("Bluetooth not available")
            return
        }

        if (!adapter.isEnabled) {
            AppLogger.w(TAG, "Bluetooth is turned OFF. Please enable Bluetooth.")
            _connectionState.value = ConnectionState.Error("Bluetooth is turned off")
            return
        }

        // 1. Immediately load already paired/bonded devices
        try {
            val bonded = adapter.bondedDevices
            AppLogger.i(TAG, "Checking bonded/paired devices (Found: ${bonded?.size ?: 0})")
            bonded?.forEach { b ->
                val name = b.name ?: "Paired OBD Device"
                val isVeepeak = isVeepeakDeviceName(name) || isOBDDeviceName(name)
                AppLogger.d(TAG, "Found paired device: '$name' (${b.address})")
                updateAndSortDevices(
                    DiscoveredBluetoothDevice(
                        name = name,
                        address = b.address,
                        isBle = false,
                        isVeepeak = isVeepeak,
                        rssi = -45 // Strong default RSSI for paired device
                    )
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error querying bonded devices", e)
        }

        // 2. Start BLE Scanner with low latency
        try {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            adapter.bluetoothLeScanner?.startScan(null, settings, scanCallback)
            AppLogger.d(TAG, "BLE Low Latency Scanner active")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start BLE scanner", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        AppLogger.d(TAG, "Stopping Bluetooth scan")
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error stopping scan", e)
        }
        if (_connectionState.value is ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    private fun isVeepeakDeviceName(name: String): Boolean {
        val upper = name.uppercase()
        return upper.contains("VEEPEAK") ||
               upper.contains("OBDCHECK") ||
               upper.contains("VP11") ||
               upper.contains("VP30") ||
               upper.contains("IOS-VLINK") ||
               upper.contains("ANDROID-VLINK") ||
               upper.contains("VLINKER") ||
               upper.contains("CARISTA") ||
               upper.contains("OBDLINK")
    }

    private fun isOBDDeviceName(name: String): Boolean {
        val upper = name.uppercase()
        return upper.contains("OBD") ||
               upper.contains("ELM") ||
               upper.contains("VGATE") ||
               upper.contains("LINK")
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceInfo: DiscoveredBluetoothDevice) {
        stopScan()
        AppLogger.i(TAG, "Attempting connection to '${deviceInfo.name}' (${deviceInfo.address})...")

        val adapter = bluetoothAdapter ?: run {
            AppLogger.e(TAG, "BluetoothAdapter is null")
            _connectionState.value = ConnectionState.Error("Bluetooth adapter not available")
            return
        }

        val device = try {
            adapter.getRemoteDevice(deviceInfo.address)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Invalid device address: ${deviceInfo.address}", e)
            _connectionState.value = ConnectionState.Error("Invalid device address: ${e.message}")
            return
        }

        _connectionState.value = ConnectionState.Connecting(deviceInfo.name)

        scope.launch {
            try {
                adapter.cancelDiscovery()
            } catch (_: Exception) {}

            try {
                if (deviceInfo.isBle) {
                    AppLogger.d(TAG, "Mode: BLE GATT connection first...")
                    val bleSuccess = connectBle(device, deviceInfo.name)
                    if (!bleSuccess) {
                        AppLogger.w(TAG, "BLE connection failed. Triggering Classic SPP fallback...")
                        _connectionState.value = ConnectionState.Handshaking(deviceInfo.name, "Retrying via Bluetooth SPP...")
                        val sppSuccess = connectClassicSpp(device, deviceInfo.name)
                        if (!sppSuccess) {
                            _connectionState.value = ConnectionState.Error("Connection failed via BLE & SPP")
                        }
                    }
                } else {
                    AppLogger.d(TAG, "Mode: Classic SPP connection first...")
                    val sppSuccess = connectClassicSpp(device, deviceInfo.name)
                    if (!sppSuccess) {
                        AppLogger.w(TAG, "Classic SPP connection failed. Triggering BLE fallback...")
                        _connectionState.value = ConnectionState.Handshaking(deviceInfo.name, "Retrying via Bluetooth BLE...")
                        val bleSuccess = connectBle(device, deviceInfo.name)
                        if (!bleSuccess) {
                            _connectionState.value = ConnectionState.Error("Connection failed via SPP & BLE")
                        }
                    }
                }
            } catch (e: Throwable) {
                AppLogger.e(TAG, "Uncaught error during connection: ${e.message}", e)
                _connectionState.value = ConnectionState.Error("Connect error: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectClassicSpp(device: BluetoothDevice, name: String?): Boolean = withContext(Dispatchers.IO) {
        disconnect()
        AppLogger.i(TAG, "Connecting to '${device.address}' via Classic RFCOMM SPP...")

        var socket: BluetoothSocket? = try {
            AppLogger.d(TAG, "SPP Try 1: Insecure RFCOMM socket (UUID: $SPP_UUID)")
            device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Insecure RFCOMM creation failed: ${e.message}")
            null
        }

        if (socket == null) {
            socket = try {
                AppLogger.d(TAG, "SPP Try 2: Secure RFCOMM socket")
                device.createRfcommSocketToServiceRecord(SPP_UUID)
            } catch (e: Exception) {
                AppLogger.w(TAG, "Secure RFCOMM creation failed: ${e.message}")
                null
            }
        }

        if (socket == null) {
            socket = try {
                AppLogger.d(TAG, "SPP Try 3: Reflection createRfcommSocket(1)")
                val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                method.invoke(device, 1) as? BluetoothSocket
            } catch (e: Exception) {
                AppLogger.w(TAG, "Reflection socket creation failed: ${e.message}")
                null
            }
        }

        if (socket == null) {
            AppLogger.e(TAG, "Failed to create any RFCOMM socket")
            return@withContext false
        }

        return@withContext try {
            socket.connect()
            sppSocket = socket
            sppOutputStream = socket.outputStream
            sppInputStream = socket.inputStream

            AppLogger.i(TAG, "✓ Classic SPP Socket CONNECTED successfully!")
            _connectionState.value = ConnectionState.Connected(name ?: "Veepeak Adapter", "Bluetooth SPP")
            startSppReadLoop()
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Socket connect() threw exception: ${e.message}")
            try { socket.close() } catch (_: Exception) {}
            false
        }
    }

    private fun startSppReadLoop() {
        scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            val lineBuilder = StringBuilder()

            try {
                val input = sppInputStream ?: return@launch
                AppLogger.d(TAG, "SPP Stream Reader loop started")
                while (sppSocket?.isConnected == true && _connectionState.value is ConnectionState.Connected) {
                    val count = input.read(buffer)
                    if (count > 0) {
                        val chunk = String(buffer, 0, count, Charsets.US_ASCII)
                        for (c in chunk) {
                            if (c == '\r' || c == '\n' || c == '>') {
                                val line = lineBuilder.toString().trim()
                                if (line.isNotEmpty()) {
                                    AppLogger.d("SPP-RX", "< $line")
                                    _incomingDataFlow.emit(line)
                                }
                                lineBuilder.clear()
                            } else {
                                lineBuilder.append(c)
                            }
                        }
                    } else if (count == -1) {
                        AppLogger.w(TAG, "SPP End of Stream (-1)")
                        break
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "SPP Read Loop error: ${e.message}")
                if (_connectionState.value is ConnectionState.Connected) {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectBle(device: BluetoothDevice, name: String?): Boolean = withContext(Dispatchers.Main) {
        disconnect()
        AppLogger.i(TAG, "Connecting to '${device.address}' via BLE GATT...")

        val deferred = CompletableDeferred<Boolean>()

        try {
            bleGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                    AppLogger.d(TAG, "BLE onConnectionStateChange: status=$status, newState=$newState")
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        AppLogger.i(TAG, "BLE GATT Connected! Discovering services...")
                        _connectionState.value = ConnectionState.Handshaking(name ?: "Veepeak BLE", "Negotiating High Speed Link...")
                        try {
                            gatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                            gatt?.discoverServices()
                        } catch (e: Exception) {
                            AppLogger.e(TAG, "discoverServices error: ${e.message}")
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        AppLogger.w(TAG, "BLE GATT Disconnected (status: $status)")
                        deferred.complete(false)
                        _connectionState.value = ConnectionState.Disconnected
                        try { gatt?.close() } catch (_: Exception) {}
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    AppLogger.d(TAG, "BLE onServicesDiscovered: status=$status, total services=${gatt?.services?.size ?: 0}")
                    if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                        val success = configureBleGattServices(gatt, name)
                        deferred.complete(success)
                    } else {
                        AppLogger.e(TAG, "BLE Service Discovery failed with status $status")
                        deferred.complete(false)
                    }
                }

                override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                    @Suppress("DEPRECATION")
                    val value = characteristic.value ?: return
                    handleBleBytes(value)
                }

                override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                    handleBleBytes(value)
                }
            })
        } catch (e: Exception) {
            AppLogger.e(TAG, "connectGatt error: ${e.message}", e)
            return@withContext false
        }

        val result = withTimeoutOrNull(10000) {
            deferred.await()
        } ?: false

        return@withContext result
    }

    private val bleLineBuilder = StringBuilder()

    private fun handleBleBytes(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        val chunk = String(bytes, Charsets.US_ASCII)
        scope.launch {
            for (c in chunk) {
                if (c == '\r' || c == '\n' || c == '>') {
                    val line = bleLineBuilder.toString().trim()
                    if (line.isNotEmpty()) {
                        AppLogger.d("BLE-RX", "< $line")
                        _incomingDataFlow.emit(line)
                    }
                    bleLineBuilder.clear()
                } else {
                    bleLineBuilder.append(c)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun configureBleGattServices(gatt: BluetoothGatt, name: String?): Boolean {
        // 1. Check Known Services
        for (serviceUuid in KNOWN_BLE_SERVICES) {
            val service = gatt.getService(serviceUuid) ?: continue
            AppLogger.d(TAG, "Matching known OBD service: $serviceUuid")
            for (char in service.characteristics) {
                val hasNotify = (char.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 ||
                                (char.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                val hasWrite = (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                               (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0

                if (hasNotify && hasWrite) {
                    bleTxCharacteristic = char
                    bleRxCharacteristic = char
                    enableBleNotification(gatt, char)
                    AppLogger.i(TAG, "✓ Configured OBD BLE Characteristic: ${char.uuid}")
                    _connectionState.value = ConnectionState.Connected(name ?: "Veepeak BLE", "BLE (GATT $serviceUuid)")
                    return true
                }
            }
        }

        // 2. Generic Scanner across ALL services if known ones didn't match
        AppLogger.d(TAG, "Scanning all ${gatt.services.size} GATT services for Notify/Write characteristics...")
        for (service in gatt.services) {
            for (char in service.characteristics) {
                val hasNotify = (char.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 ||
                                (char.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                val hasWrite = (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                               (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0

                if (hasNotify && hasWrite) {
                    bleTxCharacteristic = char
                    bleRxCharacteristic = char
                    enableBleNotification(gatt, char)
                    AppLogger.i(TAG, "✓ Auto-detected OBD Characteristic: ${char.uuid} in Service: ${service.uuid}")
                    _connectionState.value = ConnectionState.Connected(name ?: "Veepeak BLE", "BLE (Auto-Detected)")
                    return true
                }
            }
        }

        AppLogger.e(TAG, "No compatible Notify/Write characteristic found in GATT table")
        return false
    }

    @SuppressLint("MissingPermission")
    private fun enableBleNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        try {
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(CCCD_UUID)
            if (descriptor != null) {
                val isIndicate = (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                val value = if (isIndicate) BluetoothGattDescriptor.ENABLE_INDICATION_VALUE else BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

                AppLogger.d(TAG, "Writing CCCD Notification descriptor for ${characteristic.uuid}")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(descriptor, value)
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = value
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(descriptor)
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "enableBleNotification error: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun sendCommand(command: String): Boolean {
        AppLogger.d("TX", "> $command")
        val payload = (command + "\r").toByteArray(Charsets.US_ASCII)

        sppOutputStream?.let { stream ->
            return try {
                stream.write(payload)
                stream.flush()
                true
            } catch (e: Exception) {
                AppLogger.e(TAG, "SPP Write error: ${e.message}")
                false
            }
        }

        bleTxCharacteristic?.let { char ->
            bleGatt?.let { gatt ->
                return try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val writeType = if ((char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                        } else {
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        }
                        gatt.writeCharacteristic(char, payload, writeType) == BluetoothGatt.GATT_SUCCESS
                    } else {
                        @Suppress("DEPRECATION")
                        char.value = payload
                        if ((char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                        }
                        @Suppress("DEPRECATION")
                        gatt.writeCharacteristic(char)
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "BLE Write error: ${e.message}")
                    false
                }
            }
        }

        AppLogger.w(TAG, "Cannot send '$command' - no active output stream")
        return false
    }

    fun disconnect() {
        try {
            AppLogger.d(TAG, "Closing previous connections...")
            sppOutputStream?.close()
            sppInputStream?.close()
            sppSocket?.close()
            bleGatt?.disconnect()
            bleGatt?.close()

            sppOutputStream = null
            sppInputStream = null
            sppSocket = null
            bleGatt = null
            bleTxCharacteristic = null
            bleRxCharacteristic = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
