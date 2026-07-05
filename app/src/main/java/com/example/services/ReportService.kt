package com.example.services

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import com.example.presentation.viewmodel.AnalyticsMetrics
import java.io.File
import java.io.FileOutputStream

class ReportService(private val context: Context) {

    fun generatePdfReport(metrics: AnalyticsMetrics, timeframe: String) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        // Background
        canvas.drawColor(Color.WHITE)

        // Header
        paint.color = Color.parseColor("#FF6D00") // OrangePrimary
        canvas.drawRect(0f, 0f, 595f, 100f, paint)

        paint.color = Color.WHITE
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("OPTIX BILLING SOLUTION", 40f, 50f, paint)
        
        paint.textSize = 14f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Business Analytics Report - $timeframe", 40f, 80f, paint)

        // Summary Boxes
        paint.color = Color.parseColor("#F5F5F5")
        canvas.drawRoundRect(40f, 130f, 280f, 220f, 10f, 10f, paint)
        canvas.drawRoundRect(315f, 130f, 555f, 220f, 10f, 10f, paint)

        paint.color = Color.BLACK
        paint.textSize = 12f
        canvas.drawText("TOTAL REVENUE", 60f, 160f, paint)
        canvas.drawText("TOTAL ORDERS", 335f, 160f, paint)

        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Rs.${metrics.totalSales.toInt()}", 60f, 195f, paint)
        canvas.drawText("${metrics.numBills}", 335f, 195f, paint)

        // Top Products Table
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f
        canvas.drawText("Top Performing Products", 40f, 270f, paint)
        
        paint.textSize = 12f
        paint.color = Color.GRAY
        canvas.drawText("Product Name", 40f, 300f, paint)
        canvas.drawText("Qty Sold", 400f, 300f, paint)
        canvas.drawText("Revenue", 480f, 300f, paint)
        
        paint.color = Color.BLACK
        var yPos = 330f
        metrics.topSellingItems.forEach { item ->
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(item.name, 40f, yPos, paint)
            canvas.drawText("${item.quantity}", 400f, yPos, paint)
            canvas.drawText("Rs.${item.totalRevenue.toInt()}", 480f, yPos, paint)
            yPos += 25f
        }

        // Trends Placeholder (since full charts are hard in raw canvas)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f
        canvas.drawText("Sales Insights", 40f, 500f, paint)
        
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 12f
        canvas.drawText("Peak Selling Hour: ${metrics.peakHour}", 40f, 530f, paint)
        canvas.drawText("Average Order Value: Rs.${metrics.averageOrderValue.toInt()}", 40f, 555f, paint)

        pdfDocument.finishPage(page)

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Optix_Report_${System.currentTimeMillis()}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(context, "Report saved to Documents", Toast.LENGTH_LONG).show()
            openPdf(file)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun openPdf(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No app found to open PDF", Toast.LENGTH_SHORT).show()
        }
    }
}
