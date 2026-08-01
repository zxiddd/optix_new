package com.example.services

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.*

class BluetoothPrinterService(private val context: Context) {
    private val TAG = "BTPrinterService"
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (bluetoothAdapter?.isEnabled == false) return emptyList()
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(address: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isConnected()) return@withContext true
            if (bluetoothAdapter?.isEnabled == false) {
                Log.e(TAG, "Bluetooth is disabled")
                return@withContext false
            }
            
            val device = bluetoothAdapter?.getRemoteDevice(address) ?: return@withContext false
            
            // Close any existing socket before trying to connect
            closeSocket()

            bluetoothSocket = try {
                device.createRfcommSocketToServiceRecord(SPP_UUID)
            } catch (e: Exception) {
                device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
            }
            
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            Log.d(TAG, "Connected to $address")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed: ${e.message}")
            
            // Fallback: Try insecure socket if secure failed
            try {
                val device = bluetoothAdapter?.getRemoteDevice(address)
                bluetoothSocket = device?.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                Log.d(TAG, "Insecure connection successful")
                return@withContext true
            } catch (e2: Exception) {
                Log.e(TAG, "Insecure connection failed: ${e2.message}")
            }

            disconnect()
            false
        }
    }

    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true && outputStream != null
    }

    @SuppressLint("MissingPermission")
    suspend fun print(bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected()) {
            Log.e(TAG, "Not connected to printer")
            return@withContext false
        }
        try {
            outputStream?.write(bytes)
            outputStream?.flush()
            Log.d(TAG, "Data sent to printer")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Print failed: ${e.message}")
            // Connection might be lost, trigger disconnect to cleanup
            disconnect()
            false
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Disconnecting from printer")
        closeSocket()
    }

    private fun closeSocket() {
        try {
            outputStream?.close()
        } catch (e: Exception) {}
        try {
            bluetoothSocket?.close()
        } catch (e: Exception) {}
        outputStream = null
        bluetoothSocket = null
    }
}
