package com.example.services

import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.util.Log
import com.example.OptixApplication
import com.example.data.entity.*
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject


class RealtimeSyncManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var socket: Socket? = null
    @Volatile private var mySocketId: String? = null

    companion object {
        @Volatile
        private var INSTANCE: RealtimeSyncManager? = null

        fun getInstance(context: Context): RealtimeSyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RealtimeSyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun getSocketId(): String? = mySocketId

    @Synchronized
    fun connect() {
        if (socket?.connected() == true) {
            Log.d("OPTIX_FLOW", "[SOCKET CONNECTED] Socket already active. Skipping duplicate connection.")
            return
        }

        try {
            val app = OptixApplication.instance
            val token = app.authManager.getAccessToken() ?: run {
                Log.w("OPTIX_FLOW", "[SOCKET CONNECT FAILED] No access token available.")
                return
            }

            val opts = IO.Options().apply {
                forceNew = true
                reconnection = true
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                extraHeaders = mapOf("Authorization" to listOf("Bearer $token"))
                auth = mapOf("token" to token)
            }

            socket = IO.socket("https://api.optixapp.in/events", opts)

            socket?.on(Socket.EVENT_CONNECT) {
                mySocketId = socket?.id()
                Log.d("OPTIX_FLOW", "[SOCKET CONNECTED] Socket connected cleanly (ID: $mySocketId)")
                SyncManager.getInstance(appContext).triggerSyncNow()
            }

            socket?.on("authenticated") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    mySocketId = data.optString("socketId", socket?.id())
                    Log.d("OPTIX_FLOW", "[JOINED ROOM] Socket $mySocketId joined room: ${data.optString("room")}")
                }
            }

            // --- GRANULAR REAL-TIME EVENT LISTENERS ---

            // 1. Logo Updated
            socket?.on("logo.updated") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    val sender = data.optString("senderSocketId")
                    if (sender.isNotEmpty() && sender == mySocketId) {
                        Log.d("OPTIX_FLOW", "[ECHO IGNORED] logo.updated originating from self")
                        return@on
                    }

                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] logo.updated -> Updating Room profile & Coil cache")
                    scope.launch {
                        try {
                            val profileRepo = app.businessProfileRepository
                            val existing = profileRepo.getProfileSync() ?: BusinessProfile()
                            val newUrl = if (data.isNull("logoUrl")) null else data.optString("logoUrl")

                            if (existing.logoPath != null && existing.logoPath != newUrl) {
                                Log.d("OPTIX_FLOW", "[COIL CACHE PURGED] Purging old logo URL cache: ${existing.logoPath}")
                                val imageLoader = coil.Coil.imageLoader(appContext)
                                imageLoader.diskCache?.clear()
                                imageLoader.memoryCache?.clear()
                            }

                            val showLogo = data.optBoolean("showLogo", newUrl != null)
                            val updated = existing.copy(logoPath = newUrl, showLogo = showLogo)
                            profileRepo.saveProfile(updated)
                            Log.d("OPTIX_FLOW", "[ROOM UPDATED] logo.updated applied live (<200ms)")
                        } catch (e: Exception) {
                            Log.e("OPTIX_FLOW", "[EVENT ERR] logo.updated error: ${e.message}")
                        }
                    }
                }
            }

            // 2. Receipt Toggle Updated
            socket?.on("receipt.toggle.updated") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    val sender = data.optString("senderSocketId")
                    if (sender.isNotEmpty() && sender == mySocketId) {
                        Log.d("OPTIX_FLOW", "[ECHO IGNORED] receipt.toggle.updated originating from self")
                        return@on
                    }

                    val key = data.optString("key")
                    val value = data.optBoolean("value")
                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] receipt.toggle.updated -> $key = $value")

                    scope.launch {
                        try {
                            val profileRepo = app.businessProfileRepository
                            val existing = profileRepo.getProfileSync() ?: BusinessProfile()
                            val updated = when (key) {
                                "showLogo" -> existing.copy(showLogo = value)
                                "showBusinessName" -> existing.copy(showBusinessName = value)
                                "showAddress" -> existing.copy(showAddress = value)
                                "showPhone" -> existing.copy(showPhone = value)
                                "showGst" -> existing.copy(showGst = value)
                                "showDateTime" -> existing.copy(showDateTime = value)
                                "showOrderNumber" -> existing.copy(showOrderNumber = value)
                                "showCashierName" -> existing.copy(showCashierName = value)
                                "showDiscounts" -> existing.copy(showDiscounts = value)
                                "showTaxes" -> existing.copy(showTaxes = value)
                                "qrEnabled" -> existing.copy(qrEnabled = value)
                                "showVisitAgain" -> existing.copy(showVisitAgain = value)
                                else -> existing
                            }
                            profileRepo.saveProfile(updated)
                            Log.d("OPTIX_FLOW", "[ROOM UPDATED] receipt.toggle.updated ($key = $value) applied live (<200ms)")
                        } catch (e: Exception) {
                            Log.e("OPTIX_FLOW", "[EVENT ERR] receipt.toggle.updated error: ${e.message}")
                        }
                    }
                }
            }

            // 3. Receipt Updated (Full object update)
            socket?.on("receipt.updated") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    val sender = data.optString("senderSocketId")
                    if (sender.isNotEmpty() && sender == mySocketId) {
                        Log.d("OPTIX_FLOW", "[ECHO IGNORED] receipt.updated originating from self")
                        return@on
                    }

                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] receipt.updated -> Updating Room receipt settings")
                    scope.launch {
                        try {
                            val profileRepo = app.businessProfileRepository
                            val existing = profileRepo.getProfileSync() ?: BusinessProfile()

                            val updated = existing.copy(
                                footerMessage = data.optString("footerMessage", existing.footerMessage),
                                showBusinessName = data.optBoolean("showBusinessName", existing.showBusinessName),
                                showAddress = data.optBoolean("showAddress", existing.showAddress),
                                showPhone = data.optBoolean("showPhone", existing.showPhone),
                                showGst = data.optBoolean("showGst", existing.showGst),
                                showDateTime = data.optBoolean("showDateTime", existing.showDateTime),
                                showOrderNumber = data.optBoolean("showOrderNumber", existing.showOrderNumber),
                                showCashierName = data.optBoolean("showCashierName", existing.showCashierName),
                                showDiscounts = data.optBoolean("showDiscounts", existing.showDiscounts),
                                showTaxes = data.optBoolean("showTaxes", existing.showTaxes),
                                taxPercentage = data.optDouble("taxPercentage", existing.taxPercentage),
                                qrEnabled = data.optBoolean("qrEnabled", existing.qrEnabled),
                                showVisitAgain = data.optBoolean("showVisitAgain", existing.showVisitAgain)
                            )
                            profileRepo.saveProfile(updated)
                            Log.d("OPTIX_FLOW", "[ROOM UPDATED] receipt.updated applied live (<200ms)")
                        } catch (e: Exception) {
                            Log.e("OPTIX_FLOW", "[EVENT ERR] receipt.updated error: ${e.message}")
                        }
                    }
                }
            }


            // 4. Payment QR Created / Updated
            val handleQrUpsert: (JSONObject) -> Unit = { data ->
                val sender = data.optString("senderSocketId")
                if (sender.isEmpty() || sender != mySocketId) {
                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] paymentQr created/updated -> Name: ${data.optString("name")}")
                    scope.launch {
                        try {
                            val qrDao = app.database.paymentQrDao()
                            val newPath = data.optString("imageUrl", data.optString("imagePath"))
                            
                            val imageLoader = coil.Coil.imageLoader(appContext)
                            imageLoader.diskCache?.clear()
                            imageLoader.memoryCache?.clear()

                            val entity = PaymentQrEntity(
                                id = data.optString("id"),
                                businessId = data.optString("businessId"),
                                name = data.optString("name", "UPI QR"),
                                imagePath = newPath,
                                isActive = data.optBoolean("isActive", true)
                            )
                            if (entity.isActive) {
                                qrDao.setActiveQr(entity.id)
                            }
                            qrDao.insertQr(entity)
                            Log.d("OPTIX_FLOW", "[ROOM UPDATED] Payment QR saved live in Room (<200ms)")
                        } catch (e: Exception) {
                            Log.e("OPTIX_FLOW", "[EVENT ERR] paymentQr save error: ${e.message}")
                        }
                    }
                }
            }

            socket?.on("paymentQr.created") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) handleQrUpsert(args[0] as JSONObject)
            }

            socket?.on("paymentQr.updated") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) handleQrUpsert(args[0] as JSONObject)
            }

            // 5. Payment QR Selected
            socket?.on("paymentQr.selected") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    val sender = data.optString("senderSocketId")
                    if (sender.isNotEmpty() && sender == mySocketId) return@on

                    val id = data.optString("id")
                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] paymentQr.selected -> ID: $id")
                    scope.launch {
                        try {
                            app.database.paymentQrDao().setActiveQr(id)
                            Log.d("OPTIX_FLOW", "[ROOM UPDATED] Default Payment QR selected live (<200ms)")
                        } catch (e: Exception) {}
                    }
                }
            }

            // 6. Payment QR Deleted
            socket?.on("paymentQr.deleted") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    val sender = data.optString("senderSocketId")
                    if (sender.isNotEmpty() && sender == mySocketId) return@on

                    val id = data.optString("id")
                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] paymentQr.deleted -> ID: $id")
                    scope.launch {
                        try {
                            app.database.paymentQrDao().deleteById(id)
                            Log.d("OPTIX_FLOW", "[ROOM UPDATED] Payment QR deleted live (<200ms)")
                        } catch (e: Exception) {}
                    }
                }
            }

            // 6. Business Updated (Name / Phone / Address)
            socket?.on("business.updated") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    val sender = data.optString("senderSocketId")
                    if (sender.isNotEmpty() && sender == mySocketId) {
                        Log.d("OPTIX_FLOW", "[ECHO IGNORED] business.updated originating from self")
                        return@on
                    }

                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] business.updated -> Name: ${data.optString("name")}")
                    scope.launch {
                        try {
                            val profileRepo = app.businessProfileRepository
                            val existing = profileRepo.getProfileSync() ?: BusinessProfile()
                            val updated = existing.copy(
                                name = data.optString("name", existing.name),
                                phone = data.optString("phone", existing.phone),
                                address = data.optString("address", existing.address),
                                openingTime = data.optString("openingTime", existing.openingTime),
                                closingTime = data.optString("closingTime", existing.closingTime),
                                timezone = data.optString("timezone", existing.timezone)
                            )
                            profileRepo.saveProfile(updated)
                            Log.d("OPTIX_FLOW", "[ROOM UPDATED] business.updated applied live (<200ms)")
                        } catch (e: Exception) {
                            Log.e("OPTIX_FLOW", "[EVENT ERR] business.updated error: ${e.message}")
                        }
                    }
                }
            }

            // 6.5. Business Reset Event
            socket?.on("business.reset") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    val sender = data.optString("senderSocketId")
                    if (sender.isNotEmpty() && sender == mySocketId) {
                        Log.d("OPTIX_FLOW", "[ECHO IGNORED] business.reset originating from self")
                        return@on
                    }

                    val resetDate = data.optString("lastResetBusinessDate")
                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] business.reset -> Date: $resetDate")
                    scope.launch {
                        try {
                            val profileRepo = app.businessProfileRepository
                            val existing = profileRepo.getProfileSync() ?: BusinessProfile()
                            val updated = existing.copy(lastResetBusinessDate = resetDate)
                            profileRepo.saveProfile(updated)
                            Log.d("OPTIX_FLOW", "[ROOM UPDATED] business.reset applied live (<200ms)")
                        } catch (e: Exception) {
                            Log.e("OPTIX_FLOW", "[EVENT ERR] business.reset error: ${e.message}")
                        }
                    }
                }
            }

            // 6.6. Feature Flags Updated Real-Time
            socket?.on("feature_flags_updated") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] feature_flags_updated -> $obj")
                    val map = mutableMapOf<String, String>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        if (k != "senderSocketId") {
                            map[k] = obj.optString(k)
                        }
                    }
                    FeatureGate.updateFeatureFlags(map)
                }
            }

            // 6.7. Remote Commands Real-Time
            socket?.on("remote_command") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    val action = obj.optString("action")
                    val deviceId = obj.optString("deviceId")

                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] remote_command -> Action: $action, Target Device: ${if (deviceId.isEmpty()) "ALL" else deviceId}")

                    scope.launch {
                        try {
                            when (action) {
                                "FORCE_SYNC", "FORCE_FULL_SYNC", "REFRESH_SUBSCRIPTION" -> {
                                    SyncManager.getInstance(appContext).triggerSyncNow()
                                }
                                "LOGOUT_ALL_DEVICES" -> {
                                    app.authManager.logout()
                                }
                                "RESTART_SOCKET" -> {
                                    socket?.disconnect()
                                    socket?.connect()
                                }
                                "RECONNECT_WEBSOCKET" -> {
                                    socket?.connect()
                                }
                                "CLEAR_CACHE" -> {
                                    val imageLoader = coil.Coil.imageLoader(appContext)
                                    imageLoader.diskCache?.clear()
                                    imageLoader.memoryCache?.clear()
                                }
                                "REBUILD_LOCAL_DB" -> {
                                    app.database.clearAllTables()
                                    SyncManager.getInstance(appContext).triggerSyncNow()
                                }
                                "SEND_TEST_NOTIFICATION" -> {
                                    Log.d("OPTIX_FLOW", "[TEST NOTIFICATION] Remote test notification received cleanly")
                                }
                                else -> {
                                    Log.d("OPTIX_FLOW", "[UNKNOWN REMOTE COMMAND] $action")
                                }
                            }

                        } catch (e: Exception) {
                            Log.e("OPTIX_FLOW", "[REMOTE COMMAND ERR] Action $action failed: ${e.message}")
                        }
                    }
                }
            }

            // 6.8. Admin Broadcast Notification
            socket?.on("admin_notification") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    val title = obj.optString("title", "Super Admin Notification")
                    val message = obj.optString("message", "")
                    val type = obj.optString("type", "ADMIN_BROADCAST")
                    val severity = obj.optString("severity", "INFO")

                    Log.d("OPTIX_FLOW", "[ADMIN NOTIFICATION RECEIVED] Title: $title | Message: $message | Type: $type")

                    // 1. Trigger OS Heads-Up Notification
                    showAdminNotification(appContext, title, message)

                    // 2. Toast alert on main thread
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(appContext, "🔔 $title: $message", Toast.LENGTH_LONG).show()
                    }

                    // 3. Save to Room database
                    scope.launch {
                        try {
                            app.notificationRepository.insert(
                                com.example.data.entity.NotificationEntity(
                                    id = java.util.UUID.randomUUID().toString(),
                                    businessId = app.authManager.getBusinessId() ?: "",
                                    title = title,
                                    message = message,
                                    type = type,
                                    severity = severity,
                                    isRead = false,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("OPTIX_FLOW", "[NOTIFICATION INSERT ERR] ${e.message}")
                        }
                    }
                }
            }





            // 7. Core Entity Events (Order / Product / Category)
            socket?.on("order.created") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    val sender = obj.optString("senderSocketId")
                    if (sender.isNotEmpty() && sender == mySocketId) return@on

                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] order.created -> Token: ${obj.optString("tokenNumber")}")
                    scope.launch {
                        try {
                            val orderRepo = app.billOrderRepository
                            val itemsArr = obj.optJSONArray("items")
                            val orderItemsJson = itemsArr?.toString() ?: "[]"

                            val order = BillOrder(
                                id = obj.optString("id"),
                                tokenNumber = obj.optString("tokenNumber"),
                                invoiceNumber = obj.optString("invoiceNumber"),
                                status = obj.optString("status", "PAID"),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                subtotal = obj.optDouble("subtotal", obj.optDouble("total")),
                                discount = obj.optDouble("discount", 0.0),
                                tax = obj.optDouble("tax", 0.0),
                                total = obj.optDouble("total"),
                                orderItemsJson = orderItemsJson,
                                paymentMethod = obj.optString("paymentMethod", "CASH"),
                                cashierName = obj.optString("cashierName", "Admin"),
                                customerName = if (obj.isNull("customerName")) null else obj.optString("customerName"),
                                isSynced = true
                            )
                            orderRepo.insert(order)
                            Log.d("OPTIX_FLOW", "[ROOM UPDATED] Order inserted live into Room (<200ms)")
                        } catch (e: Exception) {
                            Log.e("OPTIX_FLOW", "[EVENT ERR] order.created error: ${e.message}")
                        }
                    }
                }
            }

            socket?.on("product.updated") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    val sender = obj.optString("senderSocketId")
                    if (sender.isNotEmpty() && sender == mySocketId) return@on

                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] product.updated -> Name: ${obj.optString("name")}")
                    scope.launch {
                        try {
                            val itemRepo = app.billingItemRepository
                            val item = BillingItem(
                                id = obj.optString("id"),
                                name = obj.optString("name"),
                                description = if (obj.isNull("description")) null else obj.optString("description"),
                                barcode = if (obj.isNull("barcode")) null else obj.optString("barcode"),
                                sku = if (obj.isNull("sku")) null else obj.optString("sku"),
                                categoryId = obj.optString("categoryId"),
                                price = obj.optDouble("price"),
                                imageUrl = if (obj.isNull("imageUrl")) null else obj.optString("imageUrl"),
                                isOutOfStock = obj.optBoolean("isOutOfStock", false),
                                pricingType = obj.optString("pricingType", "FIXED"),
                                unit = obj.optString("unit", "Piece"),
                                isSynced = true
                            )
                            itemRepo.insert(item)
                            Log.d("OPTIX_FLOW", "[ROOM UPDATED] Product updated live in Room (<200ms)")
                        } catch (e: Exception) {
                            Log.e("OPTIX_FLOW", "[EVENT ERR] product.updated error: ${e.message}")
                        }
                    }
                }
            }

            socket?.on("product.deleted") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    val sender = obj.optString("senderSocketId")
                    if (sender.isNotEmpty() && sender == mySocketId) return@on

                    val id = obj.optString("id")
                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] product.deleted -> ID: $id")
                    scope.launch {
                        try {
                            app.billingItemRepository.deleteById(id)
                        } catch (e: Exception) {}
                    }
                }
            }

            socket?.on("category.updated") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    val sender = obj.optString("senderSocketId")
                    if (sender.isNotEmpty() && sender == mySocketId) return@on

                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] category.updated -> Name: ${obj.optString("name")}")
                    scope.launch {
                        try {
                            val catRepo = app.categoryRepository
                            val cat = Category(
                                id = obj.optString("id"),
                                name = obj.optString("name"),
                                sortOrder = obj.optInt("sortOrder", 0)
                            )
                            catRepo.insert(cat)
                        } catch (e: Exception) {}
                    }
                }
            }

            // ─── STAFF REAL-TIME EVENTS ─────────────────────────────────────────

            // Staff Created
            socket?.on("staff.created") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    val sender = obj.optString("senderSocketId")
                    if (sender.isNotEmpty() && sender == mySocketId) { Log.d("OPTIX_FLOW", "[ECHO IGNORED] staff.created from self"); return@on }
                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] staff.created -> Inserting staff into Room: ${obj.optString("name")}")
                    scope.launch {
                        try {
                            val permsArr = try { obj.getJSONArray("permissions") } catch (e: Exception) { null }
                            val permsList = mutableListOf<String>()
                            if (permsArr != null) for (j in 0 until permsArr.length()) permsList.add(permsArr.optString(j))
                            val staff = com.example.data.entity.Staff(
                                id = obj.optString("id"),
                                name = obj.optString("name"),
                                username = obj.optString("username"),
                                password = "",
                                role = obj.optString("role", "staff"),
                                isDisabled = obj.optBoolean("isDisabled", false),
                                businessId = obj.optString("businessId", ""),
                                lastModified = System.currentTimeMillis(),
                                permissionsJson = org.json.JSONArray(permsList).toString(),
                                canBillWeightBased = permsList.contains("WEIGHT_BILLING") || permsList.isEmpty(),
                                canEditWeight = permsList.contains("EDIT_WEIGHT") || permsList.isEmpty(),
                                canEnterAmount = permsList.contains("ENTER_AMOUNT") || permsList.isEmpty(),
                                canChangeProductPrice = permsList.contains("CHANGE_PRICE")
                            )
                            app.staffRepository.insert(staff)
                            Log.d("OPTIX_FLOW", "[STAFF CREATED] Room updated for staff: ${staff.name}")
                        } catch (e: Exception) { Log.e("OPTIX_FLOW", "[STAFF CREATED ERR] ${e.message}") }
                    }
                }
            }

            // Staff Updated
            socket?.on("staff.updated") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    val sender = obj.optString("senderSocketId")
                    if (sender.isNotEmpty() && sender == mySocketId) { Log.d("OPTIX_FLOW", "[ECHO IGNORED] staff.updated from self"); return@on }
                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] staff.updated -> Updating Room staff: ${obj.optString("name")}")
                    scope.launch {
                        try {
                            val id = obj.optString("id")
                            val existing = app.staffRepository.getStaffById(id)
                            val permsArr = try { obj.getJSONArray("permissions") } catch (e: Exception) { null }
                            val permsList = mutableListOf<String>()
                            if (permsArr != null) for (j in 0 until permsArr.length()) permsList.add(permsArr.optString(j))
                            val staff = (existing ?: com.example.data.entity.Staff()).copy(
                                id = id,
                                name = obj.optString("name", existing?.name ?: ""),
                                username = obj.optString("username", existing?.username ?: ""),
                                role = obj.optString("role", existing?.role ?: "staff"),
                                isDisabled = obj.optBoolean("isDisabled", existing?.isDisabled ?: false),
                                businessId = obj.optString("businessId", existing?.businessId ?: ""),
                                lastModified = System.currentTimeMillis(),
                                permissionsJson = if (permsArr != null) org.json.JSONArray(permsList).toString() else (existing?.permissionsJson ?: "[]"),
                                canBillWeightBased = if (permsArr != null) permsList.contains("WEIGHT_BILLING") else (existing?.canBillWeightBased ?: true),
                                canEditWeight = if (permsArr != null) permsList.contains("EDIT_WEIGHT") else (existing?.canEditWeight ?: true),
                                canEnterAmount = if (permsArr != null) permsList.contains("ENTER_AMOUNT") else (existing?.canEnterAmount ?: true),
                                canChangeProductPrice = if (permsArr != null) permsList.contains("CHANGE_PRICE") else (existing?.canChangeProductPrice ?: false)
                            )
                            app.staffRepository.insert(staff)
                            Log.d("OPTIX_FLOW", "[STAFF UPDATED] Room updated for staff: ${staff.name}")
                        } catch (e: Exception) { Log.e("OPTIX_FLOW", "[STAFF UPDATED ERR] ${e.message}") }
                    }
                }
            }

            // Staff Deleted
            socket?.on("staff.deleted") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    val sender = obj.optString("senderSocketId")
                    if (sender.isNotEmpty() && sender == mySocketId) { Log.d("OPTIX_FLOW", "[ECHO IGNORED] staff.deleted from self"); return@on }
                    val id = obj.optString("id")
                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] staff.deleted -> Removing staff from Room: $id")
                    scope.launch {
                        try {
                            app.staffRepository.deleteById(id)
                            Log.d("OPTIX_FLOW", "[STAFF DELETED] Room entry removed for staffId: $id")
                        } catch (e: Exception) { Log.e("OPTIX_FLOW", "[STAFF DELETED ERR] ${e.message}") }
                    }
                }
            }

            // Staff Disabled
            socket?.on("staff.disabled") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    val sender = obj.optString("senderSocketId")
                    if (sender.isNotEmpty() && sender == mySocketId) { Log.d("OPTIX_FLOW", "[ECHO IGNORED] staff.disabled from self"); return@on }
                    val id = obj.optString("id")
                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] staff.disabled -> Disabling staff in Room: $id")
                    scope.launch {
                        try {
                            app.staffRepository.setDisabled(id, true)
                            Log.d("OPTIX_FLOW", "[STAFF DISABLED] Room updated for staffId: $id")
                        } catch (e: Exception) { Log.e("OPTIX_FLOW", "[STAFF DISABLED ERR] ${e.message}") }
                    }
                }
            }

            // Staff Enabled
            socket?.on("staff.enabled") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    val sender = obj.optString("senderSocketId")
                    if (sender.isNotEmpty() && sender == mySocketId) { Log.d("OPTIX_FLOW", "[ECHO IGNORED] staff.enabled from self"); return@on }
                    val id = obj.optString("id")
                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] staff.enabled -> Enabling staff in Room: $id")
                    scope.launch {
                        try {
                            app.staffRepository.setDisabled(id, false)
                            Log.d("OPTIX_FLOW", "[STAFF ENABLED] Room updated for staffId: $id")
                        } catch (e: Exception) { Log.e("OPTIX_FLOW", "[STAFF ENABLED ERR] ${e.message}") }
                    }
                }
            }

            // Staff Last Active Updated
            socket?.on("staff.lastActive.updated") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    val staffId = obj.optString("staffId")
                    val lastActStr = obj.optString("lastActivityAt")
                    val ts = try {
                        if (lastActStr.isNotEmpty()) java.time.Instant.parse(lastActStr).toEpochMilli() else System.currentTimeMillis()
                    } catch (e: Exception) { System.currentTimeMillis() }
                    scope.launch {
                        try {
                            app.staffRepository.updateLastActivityAt(staffId, ts)
                            Log.d("OPTIX_FLOW", "[LAST ACTIVE UPDATED] StaffId $staffId updated to $ts live (<200ms)")
                        } catch (e: Exception) {}
                    }
                }
            }

            // Staff Permissions Updated
            socket?.on("staff.permissions.updated") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    val sender = obj.optString("senderSocketId")
                    if (sender.isNotEmpty() && sender == mySocketId) { Log.d("OPTIX_FLOW", "[ECHO IGNORED] staff.permissions.updated from self"); return@on }
                    val id = obj.optString("id")
                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] staff.permissions.updated -> Updating permissions in Room: $id")
                    scope.launch {
                        try {
                            val permsArr = try { obj.getJSONArray("permissions") } catch (e: Exception) { null }
                            if (permsArr != null) {
                                val permsList = mutableListOf<String>()
                                for (j in 0 until permsArr.length()) permsList.add(permsArr.optString(j))
                                val permJson = org.json.JSONArray(permsList).toString()
                                app.staffRepository.updatePermissionsJson(id, permJson)
                                if (id == app.authManager.getUserId()) {
                                    app.authManager.updatePermissions(permsList)
                                }
                                Log.d("OPTIX_FLOW", "[PERMISSIONS UPDATED] Room & AuthManager permissions updated live (<200ms) for staffId: $id, perms: $permsList")
                            }
                        } catch (e: Exception) { Log.e("OPTIX_FLOW", "[PERMISSIONS UPDATED ERR] ${e.message}") }
                    }
                }
            }

            // Suspicious Activity — notify owner
            socket?.on("staff.suspicious_activity") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    val staffId = obj.optString("staffId")
                    val action = obj.optString("action")
                    Log.w("OPTIX_FLOW", "[SUSPICIOUS ACTIVITY ALERT] StaffId: $staffId, Action: $action")
                    scope.launch {
                        try {
                            val log = com.example.data.entity.StaffActivityLog(
                                id = java.util.UUID.randomUUID().toString(),
                                staffId = staffId,
                                businessId = "",
                                action = action,
                                entityType = obj.optString("entityType"),
                                isSuspicious = true,
                                createdAt = System.currentTimeMillis()
                            )
                            app.staffActivityLogRepository.insert(log)
                            Log.w("OPTIX_FLOW", "[SUSPICIOUS ACTIVITY STORED] Logged to Room for owner visibility")
                        } catch (e: Exception) { Log.e("OPTIX_FLOW", "[SUSPICIOUS ACTIVITY ERR] ${e.message}") }
                    }
                }
            }

            // Staff Activity Created
            socket?.on("staff.activity.created") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    scope.launch {
                        try {
                            val log = com.example.data.entity.StaffActivityLog(
                                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                                staffId = obj.optString("staffId"),
                                businessId = obj.optString("businessId"),
                                action = obj.optString("action"),
                                entityType = obj.optString("entityType"),
                                entityId = obj.optString("entityId"),
                                deviceId = obj.optString("deviceId"),
                                isSuspicious = obj.optBoolean("isSuspicious", false),
                                severity = obj.optString("severity", "NORMAL"),
                                createdAt = System.currentTimeMillis()
                            )
                            app.staffActivityLogRepository.insert(log)
                            Log.d("OPTIX_FLOW", "[ACTIVITY LOG STORED] Logged action: ${log.action}")
                        } catch (e: Exception) {}
                    }
                }
            }

            // Staff Session Started / Ended
            socket?.on("staff.session.started") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    scope.launch {
                        try {
                            val session = com.example.data.entity.StaffSession(
                                id = obj.optString("id"),
                                staffId = obj.optString("staffId"),
                                businessId = obj.optString("businessId"),
                                deviceId = obj.optString("deviceId"),
                                deviceName = obj.optString("deviceName"),
                                loginAt = System.currentTimeMillis(),
                                isActive = true
                            )
                            app.staffSessionRepository.insertSession(session)
                            Log.d("OPTIX_FLOW", "[SESSION STARTED] Logged session for staffId: ${session.staffId}")
                        } catch (e: Exception) {}
                    }
                }
            }

            socket?.on("staff.session.ended") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    val sessionId = obj.optString("sessionId")
                    scope.launch {
                        try {
                            app.staffSessionRepository.closeSession(sessionId)
                            Log.d("OPTIX_FLOW", "[SESSION ENDED] Closed session in Room: $sessionId")
                        } catch (e: Exception) {}
                    }
                }
            }

            // Notification Created
            socket?.on("staff.notification.created") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    scope.launch {
                        try {
                            val notif = com.example.data.entity.NotificationEntity(
                                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                                businessId = obj.optString("businessId"),
                                title = obj.optString("title"),
                                message = obj.optString("message"),
                                type = obj.optString("type", "INFO"),
                                severity = obj.optString("severity", "INFO"),
                                isRead = false,
                                createdAt = System.currentTimeMillis()
                            )
                            app.notificationRepository.insert(notif)
                            Log.d("OPTIX_FLOW", "[NOTIFICATION RECEIVED] Title: ${notif.title}")
                        } catch (e: Exception) {}
                    }
                }
            }

            // 8. Subscription Updated
            socket?.on("subscription_updated") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    Log.d("OPTIX_FLOW", "[EVENT RECEIVED] subscription_updated -> Refreshing FeatureGate")
                    scope.launch {
                        try {
                            // Trigger a full sync pull to get latest subscription details
                            SyncManager.getInstance(appContext).triggerSyncNow()
                        } catch (e: Exception) {}
                    }
                }
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.w("OPTIX_FLOW", "[SOCKET DISCONNECT] Socket disconnected from server")
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("OPTIX_FLOW", "[SOCKET CONNECT ERROR] Socket error: ${if (args.isNotEmpty()) args[0] else ""}")
            }

            socket?.connect()
        } catch (e: Exception) {
            Log.e("OPTIX_FLOW", "[REALTIME MANAGER ERR] Socket init error: ${e.message}")
        }
    }

    @Synchronized
    fun disconnect() {
        try {
            socket?.disconnect()
            socket?.off()
            socket = null
            mySocketId = null
            Log.d("OPTIX_FLOW", "[SOCKET DISCONNECTED] Socket closed cleanly")
        } catch (e: Exception) {
            Log.e("OPTIX_FLOW", "[SOCKET DISCONNECT ERR] ${e.message}")
        }
    }

    private fun showAdminNotification(context: Context, title: String, message: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "optix_admin_notifications"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Optix Admin Broadcasts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "High priority notifications from Optix Super Admin"
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, com.example.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
            Log.d("OPTIX_FLOW", "[SYSTEM NOTIFICATION DISPLAYED] $title")
        } catch (e: Exception) {
            Log.e("OPTIX_FLOW", "[SYSTEM NOTIFICATION ERR] ${e.message}")
        }
    }

    fun isConnected(): Boolean {
        return socket?.connected() == true
    }
}
