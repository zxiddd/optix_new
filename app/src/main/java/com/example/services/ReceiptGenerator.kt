package com.example.services

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.data.entity.BusinessProfile
import com.example.data.entity.OrderItem
import java.text.SimpleDateFormat
import java.util.*

class ReceiptGenerator {

    fun generateEscPosBytes(
        profile: BusinessProfile,
        tokenNumber: String,
        invoiceNumber: String,
        items: List<OrderItem>,
        subtotal: Double,
        discount: Double,
        total: Double,
        paymentMethod: String,
        cashierName: String,
        qrImagePath: String? = null
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
        
        // 1. Business Header
        add(EscPosConstants.ALIGN_CENTER)
        
        if (profile.showBusinessName) {
            add(EscPosConstants.FONT_SIZE_BIG)
            add(EscPosConstants.BOLD_ON)
            addText("${profile.name}\n")
        }
        
        add(EscPosConstants.FONT_SIZE_NORMAL)
        add(EscPosConstants.BOLD_OFF)
        
        if (profile.showAddress) {
            addText("${profile.address}\n")
        }
        
        if (profile.showPhone) {
            addText("Ph: ${profile.phone}\n")
        }
        
        if (profile.showGst && !profile.gstNumber.isNullOrBlank()) {
            addText("GST: ${profile.gstNumber}\n")
        }
        
        if (profile.showBusinessName || profile.showAddress || profile.showPhone) {
            addLine()
        }

        // 2. Token
        if (profile.showOrderNumber) {
            add(EscPosConstants.FONT_SIZE_DOUBLE_HEIGHT)
            add(EscPosConstants.BOLD_ON)
            addText("TOKEN NO: $tokenNumber\n")
            add(EscPosConstants.FONT_SIZE_NORMAL)
            add(EscPosConstants.BOLD_OFF)
            addLine()
        }

        // 3. Order Details
        add(EscPosConstants.ALIGN_LEFT)
        addText("Invoice No: $invoiceNumber\n")
        
        if (profile.showDateTime) {
            addText("Date: $dateStr    Time: $timeStr\n")
        }
        
        if (profile.showCashierName) {
            addText("Cashier: $cashierName\n")
        }
        addLine()

        // 4. Items
        add(EscPosConstants.BOLD_ON)
        addText(String.format("%-18s %3s %8s\n", "Item", "Qty", "Price"))
        add(EscPosConstants.BOLD_OFF)
        addLine()

        for (item in items) {
            val name = if (item.itemName.length > 18) item.itemName.substring(0, 15) + "..." else item.itemName
            addText(String.format("%-18s %3d %8.2f\n", name, item.quantity, item.price * item.quantity))
        }
        addLine()

        // 5. Totals
        add(EscPosConstants.ALIGN_RIGHT)
        addText("Subtotal:  ${profile.currency}${String.format("%.2f", subtotal)}\n")
        
        if (profile.showDiscounts && discount > 0) {
            addText("Discount: -${profile.currency}${String.format("%.2f", discount)}\n")
        }

        if (profile.showTaxes && profile.taxPercentage > 0) {
            val taxAmount = total * (profile.taxPercentage / 100)
            addText("Tax (${profile.taxPercentage}%): ${profile.currency}${String.format("%.2f", taxAmount)}\n")
        }
        
        add(EscPosConstants.BOLD_ON)
        add(EscPosConstants.FONT_SIZE_DOUBLE_WIDTH)
        addText("TOTAL: ${profile.currency}${String.format("%.2f", total)}\n")
        add(EscPosConstants.FONT_SIZE_NORMAL)
        add(EscPosConstants.BOLD_OFF)
        addLine()

        // 6. Footer
        add(EscPosConstants.ALIGN_CENTER)
        addText("Payment Mode: $paymentMethod\n")
        
        if (profile.qrEnabled && qrImagePath != null) {
            addLine()
            try {
                val bitmap = BitmapFactory.decodeFile(qrImagePath)
                if (bitmap != null) {
                    val scaled = scaleBitmap(bitmap, 300)
                    add(EscPosConstants.ALIGN_CENTER)
                    add(decodeBitmap(scaled))
                    addText("\n")
                }
            } catch (e: Exception) {
                addText("[QR ERROR]\n")
            }
        }
        
        addLine()
        addText("${profile.footerMessage}\n")
        
        if (profile.showVisitAgain) {
            addText("Visit Again! 🙏\n")
        }
        
        add(EscPosConstants.BOLD_ON)
        addText("Optix Billing Solution\n")
        add(EscPosConstants.BOLD_OFF)
        
        add(EscPosConstants.FEED_LINE)
        add(EscPosConstants.FEED_LINE)
        add(EscPosConstants.PAPER_CUT)

        return output.toByteArray()
    }

    private fun scaleBitmap(src: Bitmap, width: Int): Bitmap {
        val height = (src.height * (width.toFloat() / src.width)).toInt()
        return Bitmap.createScaledBitmap(src, width, height, true)
    }

    private fun decodeBitmap(bmp: Bitmap): ByteArray {
        val width = (bmp.width + 7) / 8 * 8
        val height = bmp.height
        val data = ByteArray(width * height / 8)
        
        var k = 0
        for (i in 0 until height) {
            for (j in 0 until width step 8) {
                var temp: Byte = 0
                for (m in 0 until 8) {
                    if (j + m < bmp.width) {
                        val pixel = bmp.getPixel(j + m, i)
                        val r = (pixel shr 16) and 0xff
                        val g = (pixel shr 8) and 0xff
                        val b = pixel and 0xff
                        val gray = (r * 0.3 + g * 0.59 + b * 0.11).toInt()
                        if (gray < 128) {
                            temp = (temp.toInt() or (1 shl (7 - m))).toByte()
                        }
                    }
                }
                data[k++] = temp
            }
        }
        
        val xl = (width / 8) % 256
        val xh = (width / 8) / 256
        val yl = height % 256
        val yh = height / 256
        
        val header = byteArrayOf(0x1D, 0x76, 0x30, 0x00, xl.toByte(), xh.toByte(), yl.toByte(), yh.toByte())
        return header + data
    }

    fun generateSalesSummaryEscPosBytes(
        businessName: String,
        timeframe: String,
        items: List<Pair<String, Double>>,
        itemQuantities: Map<String, Int>,
        totalSales: Double,
        numBills: Int
    ): ByteArray {
        val output = mutableListOf<Byte>()

        fun add(bytes: ByteArray) = output.addAll(bytes.toList())
        fun addText(text: String) = add(text.toByteArray(Charsets.US_ASCII))
        fun addLine() = addText("--------------------------------\n")

        add(EscPosConstants.INIT)
        add(EscPosConstants.ALIGN_CENTER)
        add(EscPosConstants.BOLD_ON)
        addText("SALES SUMMARY\n")
        addText("$businessName\n")
        add(EscPosConstants.BOLD_OFF)
        addText("Period: $timeframe\n")
        addLine()

        add(EscPosConstants.ALIGN_LEFT)
        addText("Total Sales: Rs.${String.format("%.2f", totalSales)}\n")
        addText("Total Bills: $numBills\n")
        addLine()

        add(EscPosConstants.BOLD_ON)
        addText(String.format("%-18s %3s %8s\n", "Item", "Qty", "Total"))
        add(EscPosConstants.BOLD_OFF)
        addLine()

        for (item in items) {
            val name = if (item.first.length > 18) item.first.substring(0, 15) + "..." else item.first
            val qty = itemQuantities[item.first] ?: 0
            addText(String.format("%-18s %3d %8.2f\n", name, qty, item.second))
        }
        addLine()

        add(EscPosConstants.ALIGN_CENTER)
        addText("Optix Billing Solution\n")
        add(EscPosConstants.FEED_LINE)
        add(EscPosConstants.FEED_LINE)
        add(EscPosConstants.PAPER_CUT)

        return output.toByteArray()
    }
}
