package com.example.services

import com.example.data.entity.OrderItem
import java.text.SimpleDateFormat
import java.util.*

class ReceiptGenerator {

    fun generateEscPosBytes(
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
        currency: String = "Rs"
    ): ByteArray {
        val output = mutableListOf<Byte>()

        fun add(bytes: ByteArray) = output.addAll(bytes.toList())
        fun addText(text: String) = add(text.toByteArray(Charsets.US_ASCII))
        fun addLine() = addText("--------------------------------\n")

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateStr = sdf.format(Date())
        val timeStr = timeSdf.format(Date())

        add(EscPosConstants.INIT)
        
        // Business Header
        add(EscPosConstants.ALIGN_CENTER)
        add(EscPosConstants.FONT_SIZE_BIG)
        add(EscPosConstants.BOLD_ON)
        addText("$businessName\n")
        add(EscPosConstants.FONT_SIZE_NORMAL)
        add(EscPosConstants.BOLD_OFF)
        addText("$address\n")
        addText("Ph: $phone\n")
        if (!gstNumber.isNullOrBlank()) {
            addText("GST: $gstNumber\n")
        }
        addLine()

        // Token
        add(EscPosConstants.FONT_SIZE_DOUBLE_HEIGHT)
        add(EscPosConstants.BOLD_ON)
        addText("TOKEN NO: $tokenNumber\n")
        add(EscPosConstants.FONT_SIZE_NORMAL)
        add(EscPosConstants.BOLD_OFF)
        addLine()

        // Order Details
        add(EscPosConstants.ALIGN_LEFT)
        addText("Invoice No: $invoiceNumber\n")
        addText("Date: $dateStr    Time: $timeStr\n")
        addText("Cashier: $cashierName\n")
        addLine()

        // Items
        add(EscPosConstants.BOLD_ON)
        addText(String.format("%-18s %3s %8s\n", "Item", "Qty", "Price"))
        add(EscPosConstants.BOLD_OFF)
        addLine()

        for (item in items) {
            val name = if (item.itemName.length > 18) item.itemName.substring(0, 15) + "..." else item.itemName
            addText(String.format("%-18s %3d %8.2f\n", name, item.quantity, item.price * item.quantity))
        }
        addLine()

        // Totals
        add(EscPosConstants.ALIGN_RIGHT)
        addText("Subtotal:  Rs.${String.format("%.2f", subtotal)}\n")
        if (discount > 0) {
            addText("Discount: -Rs.${String.format("%.2f", discount)}\n")
        }
        add(EscPosConstants.BOLD_ON)
        add(EscPosConstants.FONT_SIZE_DOUBLE_WIDTH)
        addText("TOTAL: Rs.${String.format("%.2f", total)}\n")
        add(EscPosConstants.FONT_SIZE_NORMAL)
        add(EscPosConstants.BOLD_OFF)
        addLine()

        // Footer
        add(EscPosConstants.ALIGN_CENTER)
        addText("Payment Mode: $paymentMethod\n")
        addLine()
        addText("Thank You! Visit Again\n")
        add(EscPosConstants.BOLD_ON)
        addText("Optix Billing Solution\n")
        add(EscPosConstants.BOLD_OFF)
        
        add(EscPosConstants.FEED_LINE)
        add(EscPosConstants.FEED_LINE)
        add(EscPosConstants.FEED_LINE)
        add(EscPosConstants.PAPER_CUT)

        return output.toByteArray()
    }
}
