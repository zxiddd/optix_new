package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.repository.*
import com.example.services.AuthManager
import com.example.services.PrinterManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ZaddyApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }

    val businessProfileRepository by lazy { BusinessProfileRepository(database.businessProfileDao()) }
    val billingItemRepository by lazy { BillingItemRepository(database.billingItemDao()) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val billOrderRepository by lazy { BillOrderRepository(database.billOrderDao()) }
    val printerConfigRepository by lazy { PrinterConfigRepository(database.printerConfigDao()) }
    val staffRepository by lazy { StaffRepository(database.staffDao()) }

    val authManager by lazy { AuthManager.getInstance(this) }
    val printerManager by lazy { PrinterManager.getInstance() }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ZaddyApplication
            private set
    }
}
