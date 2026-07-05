package com.example.services

import android.annotation.SuppressLint
import android.content.Context
import com.example.data.entity.OrderItem
import com.example.data.repository.PrinterConfigRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PrinterDevice(
    val name: String,
    val address: String,
    val signalStrength: Int = 0,
    val isConnected: Boolean = false
)

class PrinterManager private constructor(private val context: Context) {
    private val btService = BluetoothPrinterService(context)
    private val generator = ReceiptGenerator()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectedDevice = MutableStateFlow<PrinterDevice?>(null)
    val connectedDevice: StateFlow<PrinterDevice?> = _connectedDevice.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<PrinterDevice>>(emptyList())
    val scannedDevices: StateFlow<List<PrinterDevice>> = _scannedDevices.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    @SuppressLint("MissingPermission")
    suspend fun scanDevices() {
        _isScanning.value = true
        _error.value = null
        try {
            val paired = btService.getPairedDevices()
            _scannedDevices.value = paired.map { 
                PrinterDevice(it.name ?: "Unknown", it.address) 
            }
        } catch (e: Exception) {
            _error.value = "Failed to scan: ${e.message}"
        } finally {
            _isScanning.value = false
        }
    }

    suspend fun connect(device: PrinterDevice): Boolean {
        _error.value = null
        val success = btService.connect(device.address)
        if (success) {
            _connectedDevice.value = device.copy(isConnected = true)
        } else {
            _error.value = "Could not connect to ${device.name}"
        }
        return success
    }

    suspend fun disconnect() {
        btService.disconnect()
        _connectedDevice.value = null
    }

    suspend fun printReceipt(
        businessName: String,
        address: String,
        phone: String,
        gstNumber: String?,
        tokenNumber: String,
        invoiceNumber: String,
        items: List<OrderItem>,
        subtotal: Double,
        discount: Double,
        total: Double,
        paymentMethod: String,
        cashierName: String,
        footerMessage: String,
        currency: String = "Rs",
        shouldPrint: Boolean = true
    ): String {
        val bytes = generator.generateEscPosBytes(
            businessName, address, phone, gstNumber, tokenNumber, 
            invoiceNumber, items, subtotal, discount, total, 
            paymentMethod, cashierName, footerMessage, currency
        )
        
        if (shouldPrint) {
            if (btService.isConnected()) {
                btService.print(bytes)
            } else {
                _error.value = "Printer not connected. Saved to history."
            }
        }

        // Return a clean string version for the UI preview
        return buildString {
            append("$businessName\n")
            append("$address\n")
            append("Ph: $phone\n")
            if (!gstNumber.isNullOrBlank()) append("GST: $gstNumber\n")
            append("--------------------------------\n")
            append("TOKEN NO: $tokenNumber\n")
            append("--------------------------------\n")
            append("Inv: $invoiceNumber\n")
            append("Mode: $paymentMethod\n")
            append("--------------------------------\n")
            for (item in items) {
                append("${item.itemName.padEnd(18)} ${item.quantity}  ${(item.price * item.quantity).toInt()}\n")
            }
            append("--------------------------------\n")
            append("TOTAL: $currency ${total.toInt()}\n")
            append("--------------------------------\n")
            append("Thank You!\n")
        }
    }

    suspend fun testPrint(): Boolean {
        if (!btService.isConnected()) return false
        val testBytes = "Optix Billing Solution\nTest Print Successful\n\n\n\n".toByteArray()
        return btService.print(testBytes)
    }

    companion object {
        @Volatile
        private var INSTANCE: PrinterManager? = null

        fun getInstance(context: Context): PrinterManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PrinterManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

