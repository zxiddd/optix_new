package com.example.services

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.example.data.entity.BusinessProfile
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
    private val TAG = "PrinterManager"

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectedDevice = MutableStateFlow<PrinterDevice?>(null)
    val connectedDevice: StateFlow<PrinterDevice?> = _connectedDevice.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<PrinterDevice>>(emptyList())
    val scannedDevices: StateFlow<List<PrinterDevice>> = _scannedDevices.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var lastUsedAddress: String? = null

    @SuppressLint("MissingPermission")
    suspend fun scanDevices() {
        _isScanning.value = true
        _error.value = null
        try {
            val paired = btService.getPairedDevices()
            if (paired.isEmpty()) {
                _error.value = "No paired printers found. Please pair in system settings first."
            }
            _scannedDevices.value = paired.map { 
                PrinterDevice(it.name ?: "Unknown", it.address) 
            }
        } catch (e: Exception) {
            _error.value = "Scan failed: ${e.message}"
        } finally {
            _isScanning.value = false
        }
    }

    suspend fun connect(device: PrinterDevice): Boolean {
        _error.value = null
        val success = btService.connect(device.address)
        if (success) {
            _connectedDevice.value = device.copy(isConnected = true)
            lastUsedAddress = device.address
        } else {
            _error.value = "Could not connect to ${device.name}. Ensure it's on and in range."
        }
        return success
    }

    suspend fun disconnect() {
        btService.disconnect()
        _connectedDevice.value = null
    }

    fun reset() {
        _connectedDevice.value = null
        _scannedDevices.value = emptyList()
        _error.value = null
        lastUsedAddress = null
    }

    private suspend fun ensureConnection(): Boolean {
        if (btService.isConnected()) return true
        
        val address = lastUsedAddress ?: return false
        Log.d(TAG, "Attempting automatic reconnection to $address")
        _error.value = "Attempting reconnection..."
        
        return if (btService.connect(address)) {
            _error.value = null
            true
        } else {
            _error.value = "Printer disconnected. Please check connection."
            _connectedDevice.value = null
            false
        }
    }

    suspend fun printReceipt(
        profile: BusinessProfile,
        tokenNumber: String,
        invoiceNumber: String,
        items: List<OrderItem>,
        subtotal: Double,
        discount: Double,
        total: Double,
        paymentMethod: String,
        cashierName: String,
        qrImagePath: String? = null,
        shouldPrint: Boolean = true
    ): String {
        val bytes = generator.generateEscPosBytes(
            profile, tokenNumber, invoiceNumber, items, 
            subtotal, discount, total, paymentMethod, cashierName, qrImagePath
        )
        
        if (shouldPrint) {
            if (ensureConnection()) {
                val success = btService.print(bytes)
                if (!success) {
                    _error.value = "Printing failed. Retrying..."
                    delay(1000)
                    if (ensureConnection()) {
                        btService.print(bytes)
                    } else {
                        _error.value = "Printing failed. Saved to history."
                    }
                } else {
                    _error.value = null
                }
            } else {
                _error.value = "Printer disconnected. Saved to history."
            }
        }

        // Return a clean string version for the UI preview
        return buildString {
            if (profile.showBusinessName) append("${profile.name}\n")
            if (profile.showAddress) append("${profile.address}\n")
            if (profile.showPhone) append("Ph: ${profile.phone}\n")
            if (profile.showGst && !profile.gstNumber.isNullOrBlank()) append("GST: ${profile.gstNumber}\n")
            append("--------------------------------\n")
            if (profile.showOrderNumber) append("TOKEN NO: $tokenNumber\n")
            append("--------------------------------\n")
            append("Inv: $invoiceNumber\n")
            if (profile.showDateTime) append("Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}\n")
            if (profile.showCashierName) append("Cashier: $cashierName\n")
            append("Mode: $paymentMethod\n")
            append("--------------------------------\n")
            for (item in items) {
                append("${item.itemName.padEnd(18)} ${item.quantity}  ${(item.price * item.quantity).toInt()}\n")
            }
            append("--------------------------------\n")
            append("TOTAL: ${profile.currency} ${total.toInt()}\n")
            append("--------------------------------\n")
            append("${profile.footerMessage}\n")
            if (profile.showVisitAgain) append("Visit Again!\n")
        }
    }

    suspend fun testPrint(): Boolean {
        if (!ensureConnection()) return false
        val testBytes = "Optix Billing Solution\nTest Print Successful\n\n\n\n".toByteArray()
        return btService.print(testBytes)
    }

    suspend fun printSalesSummary(
        businessName: String,
        timeframe: String,
        items: List<Pair<String, Double>>,
        itemQuantities: Map<String, Int>,
        totalSales: Double,
        numBills: Int,
        shouldPrint: Boolean = true,
        currency: String = "₹"
    ): String {
        val bytes = generator.generateSalesSummaryEscPosBytes(businessName, timeframe, items, itemQuantities, totalSales, numBills, currency)
        
        if (shouldPrint) {
            if (ensureConnection()) {
                btService.print(bytes)
            }
        }

        return buildString {
            append("SALES SUMMARY\n")
            append("$businessName\n")
            append("Period: $timeframe\n")
            append("--------------------------------\n")
            append("Total Bills: $numBills\n")
            append("Total Sales: $currency${totalSales.toInt()}\n")
            append("--------------------------------\n")
            for (item in items) {
                append("${item.first.padEnd(18)} ${itemQuantities[item.first]}  ${item.second.toInt()}\n")
            }
            append("--------------------------------\n")
        }
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
