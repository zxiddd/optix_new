package com.example.services

import android.content.Context
import android.util.Log
import com.example.OptixApplication
import com.example.data.repository.CloudRepository
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class SyncManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isSyncing = AtomicBoolean(false)
    private val prefs = appContext.getSharedPreferences("zaddy_sync_prefs", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var INSTANCE: SyncManager? = null

        fun getInstance(context: Context): SyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun startSyncLoop() {
        scope.launch {
            while (isActive) {
                try {
                    val isWebSocketConnected = RealtimeSyncManager.getInstance(appContext).isConnected()
                    if (!isWebSocketConnected) {
                        Log.d("OPTIX_FLOW", "[FALLBACK POLLING] WebSocket disconnected. Performing 30s fallback poll...")
                        performSync()
                    } else {
                        Log.d("OPTIX_FLOW", "[WEBSOCKET ACTIVE] Skipping polling sync loop (Real-Time active)")
                    }
                } catch (e: Exception) {
                    Log.e("SyncManager", "Sync loop error: ${e.message}")
                }
                delay(30000)
            }
        }
    }

    fun triggerSyncNow() {
        scope.launch {
            performSync()
        }
    }

    suspend fun performSync() = withContext(Dispatchers.IO) {
        if (!isSyncing.compareAndSet(false, true)) {
            Log.d("OPTIX_FLOW", "[SYNC MANAGER] Sync already running. Skipping duplicate trigger.")
            return@withContext
        }
        try {
            val app = OptixApplication.instance
            val token = app.authManager.getAccessToken() ?: return@withContext
            val userId = app.authManager.getUserId() ?: return@withContext

            val cloudRepo = CloudRepository(userId, token)
            val catRepo = app.categoryRepository
            val itemRepo = app.billingItemRepository
            val orderRepo = app.billOrderRepository
            val profileRepo = app.businessProfileRepository
            val qrRepo = app.paymentQrRepository
            val staffRepo = app.staffRepository

            // 1. Push pending offline orders
            cloudRepo.pushPendingOfflineOrders(orderRepo)

            // 2. Perform incremental pull using persistent last_sync_ts
            val lastSyncTs = prefs.getLong("last_sync_ts", 0L)
            Log.d("OPTIX_FLOW", "[SYNC MANAGER PULL] Pulling since: $lastSyncTs")
            
            if (lastSyncTs == 0L) {
                cloudRepo.syncCloudToLocal(catRepo, itemRepo, orderRepo, profileRepo, qrRepo, staffRepo)
            } else {
                cloudRepo.syncPullIncremental(catRepo, itemRepo, orderRepo, profileRepo, lastSyncTs, qrRepo, staffRepo)
            }
            
            prefs.edit().putLong("last_sync_ts", System.currentTimeMillis()).apply()
        } catch (e: Exception) {
            Log.e("OPTIX_FLOW", "[SYNC MANAGER ERR] ${e.message}")
        } finally {
            isSyncing.set(false)
        }
    }
}
