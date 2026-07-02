package com.example.services

import com.example.data.entity.OrderItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PrinterDevice(
    val name: String,
    val address: String,
    val signalStrength: Int,
    val isConnected: Boolean = false
)

class PrinterManager {
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectedDevice = MutableStateFlow<PrinterDevice?>(null)
    val connectedDevice: StateFlow<PrinterDevice?> = _connectedDevice.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<PrinterDevice>>(emptyList())
    val scannedDevices: StateFlow<List<PrinterDevice>> = _scannedDevices.asStateFlow()

    private val _printedReceipts = MutableStateFlow<List<String>>(emptyList())
    val printedReceipts: StateFlow<List<String>> = _printedReceipts.asStateFlow()

    init {
        // Default scanned list
        _scannedDevices.value = listOf(
            PrinterDevice("POS-58 Printer", "00:11:22:33:44:55", 5),
            PrinterDevice("BT Printer 2", "66:77:88:99:AA:BB", 4),
            PrinterDevice("Thermal Printer", "11:22:33:44:55:66", 3)
        )
    }

    suspend fun scanDevices() {
        _isScanning.value = true
        _scannedDevices.value = emptyList()
        delay(1500) // Simulate scanning
        _scannedDevices.value = listOf(
            PrinterDevice("POS-58 Printer", "00:11:22:33:44:55", 5),
            PrinterDevice("BT Printer 2", "66:77:88:99:AA:BB", 4),
            PrinterDevice("Thermal Printer", "11:22:33:44:55:66", 3)
        )
        _isScanning.value = false
    }

    suspend fun connect(device: PrinterDevice): Boolean {
        delay(800) // Simulate connecting
        _connectedDevice.value = device.copy(isConnected = true)
        return true
    }

    suspend fun disconnect() {
        delay(400)
        _connectedDevice.value = null
    }

    fun printRawText(text: String): Boolean {
        val currentList = _printedReceipts.value.toMutableList()
        currentList.add(0, text)
        _printedReceipts.value = currentList
        return true
    }

    fun generateReceiptText(
        businessName: String,
        address: String,
        phone: String,
        gstNumber: String?,
        tokenNumber: String,
        items: List<OrderItem>,
        subtotal: Double,
        discount: Double,
        total: Double,
        footerMessage: String,
        currency: String = "₹",
        invoiceNumber: String = "",
        cashierName: String = "Admin",
        paymentMethod: String = "Cash"
    ): String {
        val line = "--------------------------------" // 32 chars wide for 58mm printer
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateTime = sdf.format(Date())

        val sb = java.lang.StringBuilder()
        sb.append("[C]<b>$businessName</b>\n")
        sb.append("[C]$address\n")
        sb.append("[C]Ph: $phone\n")
        if (!gstNumber.isNullOrBlank()) {
            sb.append("[C]GST: $gstNumber\n")
        }
        sb.append("[C]$line\n")
        sb.append("[C]<b>TOKEN : $tokenNumber</b>\n")
        sb.append("[C]$line\n")
        if (invoiceNumber.isNotEmpty()) {
            sb.append("[L]Inv No: $invoiceNumber\n")
        }
        sb.append("[L]Cashier: $cashierName\n")
        sb.append("[L]Date: $dateTime\n")
        sb.append("[L]Payment: $paymentMethod\n")
        sb.append("[C]$line\n")
        
        // Items header
        sb.append("[L]Item               Qty  Price  Total\n")
        sb.append("[C]$line\n")
        
        for (item in items) {
            val namePart = if (item.itemName.length > 15) item.itemName.substring(0, 15) else item.itemName.padEnd(15)
            val qtyPart = item.quantity.toString().padStart(3)
            val priceStr = String.format(Locale.US, "%.0f", item.price).padStart(5)
            val totalStr = String.format(Locale.US, "%.0f", item.price * item.quantity).padStart(6)
            sb.append("[L]$namePart$qtyPart $priceStr $totalStr\n")
        }
        
        sb.append("[C]$line\n")
        sb.append("[R]Subtotal: $currency${String.format(Locale.US, "%.2f", subtotal)}\n")
        if (discount > 0) {
            sb.append("[R]Discount: -$currency${String.format(Locale.US, "%.2f", discount)}\n")
        }
        sb.append("[R]<b>GRAND TOTAL: $currency${String.format(Locale.US, "%.2f", total)}</b>\n")
        sb.append("[C]$line\n")
        sb.append("[C]$footerMessage\n")
        sb.append("[C]<b>Powered by Zaddy Billing</b>\n\n\n\n")

        val receiptText = sb.toString()
        printRawText(receiptText)
        return receiptText
    }

    companion object {
        @Volatile
        private var INSTANCE: PrinterManager? = null

        fun getInstance(): PrinterManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PrinterManager()
                INSTANCE = instance
                instance
            }
        }
    }
}
