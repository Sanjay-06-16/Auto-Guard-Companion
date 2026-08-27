package com.autoguard.companion.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.util.UUID

enum class BluetoothState {
    DISCONNECTED, CONNECTING, CONNECTED, UNAVAILABLE, ERROR
}

class BluetoothManager(private val context: Context) {
    private val TAG = "BluetoothManager"
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val DEVICE_NAME = "AUTO_GUARD"
    
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    
    // Observable states for UI
    private val _connectionState = MutableStateFlow(BluetoothState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _esp32GpsAvailable = MutableStateFlow(false)
    val esp32GpsAvailable = _esp32GpsAvailable.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var readerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private var onAccidentReceived: ((AccidentEvent) -> Unit)? = null

    init {
        if (bluetoothAdapter == null) {
            _connectionState.value = BluetoothState.UNAVAILABLE
            _errorMessage.value = "Bluetooth is not supported on this device"
        }
    }

    fun setAccidentListener(listener: (AccidentEvent) -> Unit) {
        onAccidentReceived = listener
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice() {
        val adapter = bluetoothAdapter ?: run {
            _connectionState.value = BluetoothState.UNAVAILABLE
            return
        }

        if (!adapter.isEnabled) {
            _connectionState.value = BluetoothState.ERROR
            _errorMessage.value = "Bluetooth is disabled. Please enable it."
            return
        }

        _connectionState.value = BluetoothState.CONNECTING
        _errorMessage.value = null

        scope.launch {
            try {
                // Requirement 3: Look for a paired device named "AUTO_GUARD"
                val pairedDevices: Set<BluetoothDevice>? = adapter.bondedDevices
                val device = pairedDevices?.find { it.name == DEVICE_NAME }

                if (device == null) {
                    _connectionState.value = BluetoothState.ERROR
                    _errorMessage.value = "Device '$DEVICE_NAME' not found. Please pair the ESP32 in Android Bluetooth settings first."
                    return@launch
                }

                // Requirement 4 & 5: Connect using Classic Bluetooth RFCOMM/SPP and SPP UUID
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                adapter.cancelDiscovery()
                
                socket?.connect()
                
                _connectionState.value = BluetoothState.CONNECTED
                startReading()
                Log.d(TAG, "Connected to $DEVICE_NAME")
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                _connectionState.value = BluetoothState.ERROR
                _errorMessage.value = "Connection to $DEVICE_NAME failed: ${e.localizedMessage}"
                closeSocket()
            }
        }
    }

    private fun startReading() {
        readerJob?.cancel()
        readerJob = scope.launch {
            val inputStream: InputStream = socket?.inputStream ?: return@launch
            val buffer = ByteArray(1024)
            val stringBuilder = StringBuilder()

            while (socket?.isConnected == true) {
                try {
                    val bytes = inputStream.read(buffer)
                    if (bytes > 0) {
                        val chunk = String(buffer, 0, bytes)
                        stringBuilder.append(chunk)

                        if (stringBuilder.contains("\n")) {
                            val packets = stringBuilder.toString().split("\n")
                            // Process all complete newline-terminated packets
                            for (i in 0 until packets.size - 1) {
                                val packet = packets[i].trim()
                                if (packet.isNotEmpty()) {
                                    processIncomingPacket(packet)
                                }
                            }
                            // Retain the last potentially incomplete chunk
                            stringBuilder.setLength(0)
                            stringBuilder.append(packets.last())
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Bluetooth read error", e)
                    handleDisconnection("Connection lost with ESP32")
                    break
                }
            }
        }
    }

    private fun processIncomingPacket(data: String) {
        Log.d(TAG, "Bluetooth packet received: $data")
        val event = AccidentParser.parse(data)
        if (event != null) {
            // Requirement 8: Update UI state safely
            _esp32GpsAvailable.value = event.esp32GpsAvailable
            // Notify ViewModel to trigger the accident flow
            onAccidentReceived?.invoke(event)
        }
    }

    private fun handleDisconnection(reason: String) {
        closeSocket()
        // Requirement 10: Automatically update the UI when the connection is lost
        _connectionState.value = BluetoothState.DISCONNECTED
        _errorMessage.value = reason
        _esp32GpsAvailable.value = false
    }

    private fun closeSocket() {
        readerJob?.cancel()
        try {
            socket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing socket", e)
        }
        socket = null
    }

    fun disconnect() {
        handleDisconnection("Disconnected by user")
    }
}
