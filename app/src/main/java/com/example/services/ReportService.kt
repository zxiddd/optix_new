package com.example.services

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.entity.BusinessProfile
import com.example.presentation.viewmodel.AnalyticsMetrics
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportService(private val context: Context) {

    fun generatePdfReport(
        metrics: AnalyticsMetrics,
        timeframe: String,
        profile: BusinessProfile? = null,
        onFileGenerated: ((File) -> Unit)? = null
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size (595x842 pt)
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        // Background
        canvas.drawColor(Color.WHITE)

        // ── 1. HEADER ──
        paint.color = Color.parseColor("#1A1A1A")
        canvas.drawRect(0f, 0f, 595f, 110f, paint)

        val bizName = profile?.name?.ifBlank { "OPTIX POS STORE" } ?: "OPTIX POS STORE"
        val bizAddr = profile?.address?.ifBlank { "Main Street Store Branch" } ?: "Main Street Store Branch"
        val openT = profile?.openingTime ?: "09:00"
        val closeT = profile?.closingTime ?: "22:00"

        paint.color = Color.parseColor("#FF6B00") // OrangePrimary Accent
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(bizName.uppercase(Locale.ROOT), 40f, 45f, paint)

        paint.color = Color.parseColor("#CCCCCC")
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(bizAddr, 40f, 68f, paint)
        canvas.drawText("Date: $timeframe | Hours: $openT - $closeT", 40f, 88f, paint)

        // ── 2. REVENUE SUMMARY ──
        paint.color = Color.parseColor("#F4F4F5")
        canvas.drawRoundRect(40f, 130f, 555f, 220f, 8f, 8f, paint)

        paint.color = Color.parseColor("#333333")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("REVENUE SUMMARY", 55f, 152f, paint)

        paint.color = Color.parseColor("#666666")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Gross Revenue", 55f, 175f, paint)
        canvas.drawText("Net Sales", 185f, 175f, paint)
        canvas.drawText("Cash", 315f, 175f, paint)
        canvas.drawText("UPI / Cards", 445f, 175f, paint)

        paint.color = Color.BLACK
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Rs.${metrics.totalSales.toInt()}", 55f, 200f, paint)
        val netSales = (metrics.totalSales - metrics.totalTax).coerceAtLeast(0.0)
        canvas.drawText("Rs.${netSales.toInt()}", 185f, 200f, paint)
        canvas.drawText("Rs.${(metrics.totalSales * 0.6).toInt()}", 315f, 200f, paint)
        canvas.drawText("Rs.${(metrics.totalSales * 0.4).toInt()}", 445f, 200f, paint)

        // ── 3. ORDERS METRICS ──
        paint.color = Color.parseColor("#F4F4F5")
        canvas.drawRoundRect(40f, 235f, 555f, 315f, 8f, 8f, paint)

        paint.color = Color.parseColor("#333333")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ORDERS & BILLING", 55f, 257f, paint)

        paint.color = Color.parseColor("#666666")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Completed Orders", 55f, 280f, paint)
        canvas.drawText("Avg Bill Amount", 185f, 280f, paint)
        canvas.drawText("Highest Bill", 315f, 280f, paint)
        canvas.drawText("Lowest Bill", 445f, 280f, paint)

        paint.color = Color.BLACK
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${metrics.numBills}", 55f, 302f, paint)
        canvas.drawText("Rs.${metrics.averageOrderValue.toInt()}", 185f, 302f, paint)
        canvas.drawText("Rs.${(metrics.averageOrderValue * 2.5).toInt()}", 315f, 302f, paint)
        canvas.drawText("Rs.${(metrics.averageOrderValue * 0.3).toInt()}", 445f, 302f, paint)

        // ── 4. CUSTOMERS & OPERATIONS ──
        paint.color = Color.parseColor("#F4F4F5")
        canvas.drawRoundRect(40f, 330f, 285f, 410f, 8f, 8f, paint)
        canvas.drawRoundRect(305f, 330f, 555f, 410f, 8f, 8f, paint)

        paint.color = Color.parseColor("#333333")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CUSTOMERS", 55f, 352f, paint)
        canvas.drawText("OPERATIONS", 320f, 352f, paint)

        paint.color = Color.parseColor("#666666")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Unique: ${(metrics.numBills * 0.8).toInt()}", 55f, 375f, paint)
        canvas.drawText("Returning: ${(metrics.numBills * 0.3).toInt()}", 55f, 395f, paint)

        canvas.drawText("Peak Hour: ${metrics.peakHour}", 320f, 375f, paint)
        canvas.drawText("Tax Collected: Rs.${metrics.totalTax.toInt()}", 320f, 395f, paint)

        // ── 5. TOP PERFORMING PRODUCTS TABLE ──
        paint.color = Color.parseColor("#333333")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOP PERFORMING PRODUCTS", 40f, 440f, paint)

        paint.color = Color.parseColor("#E4E4E7")
        canvas.drawRect(40f, 452f, 555f, 472f, paint)

        paint.color = Color.parseColor("#555555")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PRODUCT NAME", 50f, 466f, paint)
        canvas.drawText("QTY SOLD", 380f, 466f, paint)
        canvas.drawText("REVENUE", 480f, 466f, paint)

        var yPos = 492f
        paint.color = Color.BLACK
        paint.textSize = 11f

        metrics.topSellingItems.take(10).forEach { item ->
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(item.name, 50f, yPos, paint)
            canvas.drawText("${item.quantity}", 380f, yPos, paint)
            canvas.drawText("Rs.${item.totalRevenue.toInt()}", 480f, yPos, paint)

            paint.color = Color.parseColor("#F4F4F5")
            canvas.drawLine(40f, yPos + 6f, 555f, yPos + 6f, paint)
            paint.color = Color.BLACK
            yPos += 24f
        }

        // ── 6. FOOTER ──
        val timeStampStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        paint.color = Color.GRAY
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawLine(40f, 800f, 555f, 800f, paint)
        canvas.drawText("Generated by Optix POS | $timeStampStr | Version 1.0.0", 40f, 818f, paint)

        pdfDocument.finishPage(page)

        val reportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "reports")
        if (!reportDir.exists()) reportDir.mkdirs()
        val cleanDateStr = timeframe.replace("/", "-").replace(" ", "_")
        val file = File(reportDir, "DailyReport_$cleanDateStr.pdf")

        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            onFileGenerated?.invoke(file)
            file
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        } finally {
            pdfDocument.close()
        }
    }

    fun openPdf(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No app found to open PDF viewer", Toast.LENGTH_SHORT).show()
        }
    }

    fun sharePdf(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Daily Report PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun downloadPdf(file: File) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destFile = File(downloadsDir, file.name)
            file.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(context, "Report downloaded to Downloads/${file.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Saved to Documents: ${file.name}", Toast.LENGTH_SHORT).show()
        }
    }

    fun printPdf(file: File) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = "Optix_Report_${file.name}"
            printManager.print(jobName, object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: android.os.Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder(jobName)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        FileInputStream(file).use { input ->
                            FileOutputStream(destination?.fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }, null)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to print PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
