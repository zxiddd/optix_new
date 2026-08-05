package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.repository.*
import com.example.services.AuthManager
import com.example.services.PrinterManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import android.content.Context
import java.io.File

class OptixApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }

    val businessProfileRepository by lazy { BusinessProfileRepository(database.businessProfileDao()) }
    val billingItemRepository by lazy { BillingItemRepository(database.billingItemDao()) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val billOrderRepository by lazy { BillOrderRepository(database.billOrderDao()) }
    val printerConfigRepository by lazy { PrinterConfigRepository(database.printerConfigDao()) }
    val staffRepository by lazy { StaffRepository(database.staffDao()) }
    val dailyReportRepository by lazy { DailyReportRepository(database.dailyReportDao()) }
    val subscriptionRepository by lazy { SubscriptionRepository(database.subscriptionDao()) }
    val paymentQrRepository by lazy { PaymentQrRepository(database.paymentQrDao()) }
    val supportTicketRepository by lazy { SupportTicketRepository(database.supportTicketDao()) }
    val staffActivityLogRepository by lazy { StaffActivityLogRepository(database.staffActivityLogDao()) }
    val staffSessionRepository by lazy { StaffSessionRepository(database.staffSessionDao()) }
    val notificationRepository by lazy { NotificationRepository(database.notificationDao()) }

    val authManager by lazy { AuthManager.getInstance(this) }
    val printerManager by lazy { PrinterManager.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // --- DATA ISOLATION & CLEANUP ---
        authManager.onLogout = {
            applicationScope.launch(Dispatchers.IO) {
                try {
                    // 1. Clear Room Database (Critical for isolation)
                    database.clearAllTables()
                    
                    // 2. Reset In-memory managers
                    printerManager.reset()

                    // 3. Clear Token SharedPreferences
                    getSharedPreferences("zaddy_token_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                    
                    // 3. Clear auth related fields if any other exist
                    // (zaddy_auth_prefs is cleared in AuthManager.logout)

                    // 4. Clear Internal Files (Logos, QRs, item images)
                    listOf("business", "payment_qr", "items").forEach { folder ->
                        val dir = File(filesDir, folder)
                        if (dir.exists()) {
                            dir.deleteRecursively()
                        }
                    }

                    // 5. Clear External Files (Reports)
                    val externalDir = getExternalFilesDir(null)
                    if (externalDir?.exists() == true) {
                        externalDir.deleteRecursively()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        lateinit var instance: OptixApplication
            private set
    }
}
