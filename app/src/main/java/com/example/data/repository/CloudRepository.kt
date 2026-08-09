package com.example.data.repository

import android.net.Uri
import android.util.Log
import com.example.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CloudRepository(private val userId: String, private val authToken: String? = null) {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val baseUrl = "https://api.optixapp.in/api/v1"

    companion object {
        private val isSyncing = java.util.concurrent.atomic.AtomicBoolean(false)
        @Volatile private var lastSyncedProfile: BusinessProfile? = null
    }

    private fun buildRequest(path: String): Request.Builder {
        val builder = Request.Builder().url("$baseUrl$path")
        val token = try {
            com.example.OptixApplication.instance.authManager.getAccessToken() ?: authToken
        } catch (e: Exception) {
            authToken
        }
        if (!token.isNullOrEmpty()) {
            builder.addHeader("Authorization", "Bearer $token")
        } else {
            Log.w("OPTIX_FLOW", "[401 PREVENTED] No access token available for request to $path")
        }
        val socketId = try {
            com.example.services.RealtimeSyncManager.getInstance(com.example.OptixApplication.instance).getSocketId()
        } catch (e: Exception) { null }
        if (!socketId.isNullOrEmpty()) {
            builder.addHeader("X-Socket-Id", socketId)
        }
        return builder
    }

    private fun formatUrl(u: String?): String? {
        if (u.isNullOrEmpty()) return null
        if (u.contains("/data/user/0/") || u.contains("/storage/emulated/")) {
            Log.w("OPTIX_FLOW", "[LOCAL PATH REJECTED] Cleansed legacy local path from cloud/room: $u")
            return null
        }
        return if (u.startsWith("/")) "https://api.optixapp.in$u" else u
    }

    // --- Business Profile ---
    val profile: Flow<BusinessProfile?> = flow {
        try {
            val req = buildRequest("/business/profile").get().build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val str = resp.body?.string() ?: ""
                val obj = JSONObject(str)
                emit(
                    BusinessProfile(
                        name = obj.optString("name", "Optix Store"),
                        phone = obj.optString("phone", ""),
                        address = obj.optString("address", ""),
                        setupCompleted = true
                    )
                )
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun syncCloudToLocal(
        categoryRepo: CategoryRepository,
        itemRepo: BillingItemRepository,
        orderRepo: BillOrderRepository,
        profileRepo: BusinessProfileRepository,
        qrRepo: PaymentQrRepository? = null,
        staffRepo: StaffRepository? = null,
        subRepo: SubscriptionRepository? = null
    ) = withContext(Dispatchers.IO) {
        if (!isSyncing.compareAndSet(false, true)) {
            Log.d("OPTIX_FLOW", "[SYNC SKIPPED] Concurrent sync operation prevented")
            return@withContext
        }
        try {
            val req = buildRequest("/sync/full-dump").get().build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val str = resp.body?.string() ?: "{}"
                val root = JSONObject(str)
                parseAndStoreDump(root, categoryRepo, itemRepo, orderRepo, profileRepo, qrRepo, staffRepo, subRepo)
            }
        } catch (e: Exception) {
            Log.e("CloudRepository", "syncCloudToLocal error: ${e.message}")
        } finally {
            isSyncing.set(false)
        }
    }

    suspend fun syncPullIncremental(
        categoryRepo: CategoryRepository,
        itemRepo: BillingItemRepository,
        orderRepo: BillOrderRepository,
        profileRepo: BusinessProfileRepository,
        sinceTimestamp: Long = 0L,
        qrRepo: PaymentQrRepository? = null,
        staffRepo: StaffRepository? = null,
        subRepo: SubscriptionRepository? = null
    ) = withContext(Dispatchers.IO) {
        if (!isSyncing.compareAndSet(false, true)) {
            Log.d("OPTIX_FLOW", "[SYNC SKIPPED] Concurrent sync operation prevented")
            return@withContext
        }
        try {
            val req = buildRequest("/sync/pull?since=$sinceTimestamp").get().build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val str = resp.body?.string() ?: "{}"
                val root = JSONObject(str)
                parseAndStoreDump(root, categoryRepo, itemRepo, orderRepo, profileRepo, qrRepo, staffRepo, subRepo)
            }
        } catch (e: Exception) {
            Log.e("CloudRepository", "syncPullIncremental error: ${e.message}")
        } finally {
            isSyncing.set(false)
        }
    }

    suspend fun pushPendingOfflineOrders(
        orderRepo: BillOrderRepository
    ) = withContext(Dispatchers.IO) {
        try {
            val allOrders = orderRepo.getOrdersSync()
            val pending = allOrders.filter { !it.isSynced || it.tokenNumber.startsWith("TEMP-") || it.tokenNumber.startsWith("LOCAL-") }
            Log.d("OPTIX_SYNC", "[OFFLINE QUEUE CHECK] Total Room Orders: ${allOrders.size}, Unsynced Pending: ${pending.size}")
            for (order in pending) {
                Log.d("OPTIX_SYNC", "[OFFLINE QUEUE UPLOADING] Uploading pending order ${order.id} (token: ${order.tokenNumber}, invoice: ${order.invoiceNumber})...")
                val serverToken = insertOrder(order)
                if (serverToken.isNotEmpty()) {
                    val finalToken = if (!serverToken.startsWith("TEMP-") && !serverToken.startsWith("LOCAL-")) serverToken else order.tokenNumber
                    Log.d("OPTIX_SYNC", "[OFFLINE QUEUE SUCCESS] Order ${order.id} synced. Assigned Official Token: $finalToken")
                    val updated = order.copy(tokenNumber = finalToken, isSynced = true)
                    orderRepo.insert(updated)
                }
            }
        } catch (e: Exception) {
            Log.e("OPTIX_SYNC", "[OFFLINE QUEUE ERROR] Push error: ${e.message}")
        }
    }

    private suspend fun parseAndStoreDump(
        root: JSONObject,
        categoryRepo: CategoryRepository,
        itemRepo: BillingItemRepository,
        orderRepo: BillOrderRepository,
        profileRepo: BusinessProfileRepository,
        qrRepo: PaymentQrRepository? = null,
        staffRepo: StaffRepository? = null,
        subRepo: SubscriptionRepository? = null
    ) = withContext(Dispatchers.IO) {
        // 1. Business & Settings
        if (root.has("business") && !root.isNull("business")) {
            val bObj = root.getJSONObject("business")
            val bName = bObj.optString("name")
            val bPhone = bObj.optString("phone", "")
            val bAddr = bObj.optString("address", "")
            val bCountry = bObj.optString("country", "India")
            val bCurrency = bObj.optString("currency", "₹")

            var rShowLogo = false
            var rLogoUrl: String? = null
            var rFooter = "Thank You! Visit Again 🙏"
            var rShowName = true
            var rShowAddr = true
            var rShowPhone = true
            var rShowGst = false
            var rShowDateTime = true
            var rShowOrderNumber = true
            var rShowCashierName = true
            var rShowDiscounts = true
            var rShowTaxes = false
            var rTaxPercentage = 0.0
            var rQrEnabled = false
            var rShowVisitAgain = true

            if (bObj.has("receiptSettings") && !bObj.isNull("receiptSettings")) {
                val rObj = bObj.getJSONObject("receiptSettings")
                val urlStr = rObj.optString("logoUrl", "")
                rLogoUrl = if (urlStr.isNotEmpty()) urlStr else null
                rShowLogo = rObj.optBoolean("showLogo", rLogoUrl != null)
                rFooter = rObj.optString("footerMessage", "Thank You! Visit Again 🙏")
                rShowName = rObj.optBoolean("showBusinessName", true)
                rShowAddr = rObj.optBoolean("showAddress", true)
                rShowPhone = rObj.optBoolean("showPhone", true)
                rShowGst = rObj.optBoolean("showGst", false)
                rShowDateTime = rObj.optBoolean("showDateTime", true)
                rShowOrderNumber = rObj.optBoolean("showOrderNumber", true)
                rShowCashierName = rObj.optBoolean("showCashierName", true)
                rShowDiscounts = rObj.optBoolean("showDiscounts", true)
                rShowTaxes = rObj.optBoolean("showTaxes", false)
                rTaxPercentage = rObj.optDouble("taxPercentage", 0.0)
                rQrEnabled = rObj.optBoolean("qrEnabled", false)
                rShowVisitAgain = rObj.optBoolean("showVisitAgain", true)
            }

            val finalLogoUrl = formatUrl(rLogoUrl)
            Log.d("OPTIX_FLOW", "[FULL DUMP] Raw logoUrl: $rLogoUrl -> Sanitized: $finalLogoUrl")

            var bOpeningTime = "09:00"
            var bClosingTime = "22:00"
            var bTimezone = "Asia/Riyadh"
            var bLastResetDate: String? = null

            if (bObj.has("settings") && !bObj.isNull("settings")) {
                val sObj = bObj.getJSONObject("settings")
                bOpeningTime = sObj.optString("openingTime", "09:00")
                bClosingTime = sObj.optString("closingTime", "22:00")
                bTimezone = sObj.optString("timezone", "Asia/Riyadh")
                val rDate = sObj.optString("lastResetBusinessDate", "")
                if (rDate.isNotEmpty()) bLastResetDate = rDate
            }

            if (bName.isNotEmpty() || bObj.has("receiptSettings") || bObj.has("settings")) {
                val existing = profileRepo.getProfileSync() ?: BusinessProfile()
                val targetLogo = finalLogoUrl ?: formatUrl(existing.logoPath)
                Log.d("OPTIX_FLOW", "[ROOM RESTORE] Storing Logo logoPath: $targetLogo, showLogo: $rShowLogo, qrEnabled: $rQrEnabled")
                val updatedProfile = existing.copy(
                    name = if (bName.isNotEmpty()) bName else existing.name,
                    phone = if (bPhone.isNotEmpty()) bPhone else existing.phone,
                    address = if (bAddr.isNotEmpty()) bAddr else existing.address,
                    currency = bCurrency,
                    country = bCountry,
                    openingTime = bOpeningTime,
                    closingTime = bClosingTime,
                    timezone = bTimezone,
                    lastResetBusinessDate = bLastResetDate ?: existing.lastResetBusinessDate,
                    setupCompleted = true,
                    showLogo = rShowLogo,
                    logoPath = targetLogo,
                    footerMessage = rFooter,
                    showBusinessName = rShowName,
                    showAddress = rShowAddr,
                    showPhone = rShowPhone,
                    showGst = rShowGst,
                    showDateTime = rShowDateTime,
                    showOrderNumber = rShowOrderNumber,
                    showCashierName = rShowCashierName,
                    showDiscounts = rShowDiscounts,
                    showTaxes = rShowTaxes,
                    taxPercentage = rTaxPercentage,
                    qrEnabled = rQrEnabled,
                    showVisitAgain = rShowVisitAgain
                )
                profileRepo.saveProfile(updatedProfile)
                lastSyncedProfile = updatedProfile
            }
        }

        // 1.5 Subscriptions
        if (root.has("subscription") && !root.isNull("subscription") && subRepo != null) {
            val sObj = root.getJSONObject("subscription")
            val pObj = if (sObj.has("plan") && !sObj.isNull("plan")) sObj.getJSONObject("plan") else null
            
            val expiryStr = sObj.optString("expiryDate", "")
            val expiryTs = try {
                if (expiryStr.isEmpty()) 0L
                else if (expiryStr.all { it.isDigit() }) expiryStr.toLong()
                else {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    sdf.parse(expiryStr)?.time ?: 0L
                }
            } catch (e: Exception) { 0L }

            val sub = UserSubscription(
                uid = sObj.optString("businessId", ""),
                planId = sObj.optString("planId", "TRIAL"),
                planName = pObj?.optString("name", "Trial Plan") ?: sObj.optString("planName", "Trial Plan"),
                amount = sObj.optDouble("amount", 0.0),
                currency = sObj.optString("currency", "₹"),
                country = sObj.optString("country", "India"),
                billingCycle = sObj.optString("billingCycle", "MONTHLY"),
                status = sObj.optString("status", "ACTIVE").uppercase(),
                billsUsed = sObj.optInt("billsUsed", 0),
                productsUsed = sObj.optInt("productsUsed", 0),
                activationCode = sObj.optString("activationCode", null),
                expiryDate = expiryTs
            )
            subRepo.saveSubscription(sub)
            
            // Critical: Force FeatureGate update after sync
            val bills = orderRepo.getOrdersSync().size
            val prods = itemRepo.getAllItemsSync().size
            val updatedSub = if (sub.planId == "TRIAL") {
                sub.copy(billsUsed = bills, productsUsed = prods)
            } else sub
            com.example.services.FeatureGate.updateSubscription(updatedSub)
        }

        // 2. Categories
        if (root.has("categories")) {
            val arr = root.getJSONArray("categories")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                categoryRepo.insert(
                    Category(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        sortOrder = obj.optInt("sortOrder", 0)
                    )
                )
            }
        }

        // 3. Products
        if (root.has("products")) {
            val arr = root.getJSONArray("products")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val rawImg = obj.optString("imageUrl", "")
                val imgUrl = formatUrl(rawImg)
                itemRepo.insert(
                    BillingItem(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        price = obj.optDouble("price", 0.0),
                        pricingType = obj.optString("pricingType", "FIXED"),
                        unit = obj.optString("unit", "Piece"),
                        categoryId = obj.optString("categoryId"),
                        imageUrl = imgUrl,
                        isOutOfStock = obj.optBoolean("isOutOfStock", false)
                    )
                )
            }
        }

        // 4. Orders
        if (root.has("orders")) {
            val arr = root.getJSONArray("orders")
            Log.d("OPTIX_FLOW", "[TOTAL ORDERS IN POSTGRES / API] Orders count in full dump: ${arr.length()}")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                orderRepo.insert(
                    BillOrder(
                        id = obj.optString("id"),
                        tokenNumber = obj.optString("tokenNumber", "001"),
                        invoiceNumber = obj.optString("invoiceNumber", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        subtotal = obj.optDouble("subtotal", 0.0),
                        discount = obj.optDouble("discount", 0.0),
                        tax = obj.optDouble("tax", 0.0),
                        total = obj.optDouble("total", 0.0),
                        orderItemsJson = obj.optString("orderItemsJson", "[]"),
                        paymentMethod = obj.optString("paymentMethod", "Cash"),
                        cashierName = obj.optString("cashierName", "Admin"),
                        isSynced = true
                    )
                )
            }
            Log.d("OPTIX_FLOW", "[ORDERS PARSED & INSERTED INTO ROOM] Inserted ${arr.length()} orders into Room")
        }

        // 5. Payment QRs
        if (root.has("paymentQrs") && qrRepo != null) {
            val arr = root.getJSONArray("paymentQrs")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val rawQr = obj.optString("imageUrl", obj.optString("imagePath", ""))
                val qrPath = formatUrl(rawQr)
                if (qrPath != null) {
                    Log.d("OPTIX_FLOW", "[ROOM RESTORE] Storing Payment QR Name: ${obj.optString("name")}, COIL URL: $qrPath")
                    qrRepo.insert(
                        PaymentQrEntity(
                            id = obj.optString("id"),
                            name = obj.optString("name", "UPI QR"),
                            imagePath = qrPath,
                            isActive = obj.optBoolean("isActive", true)
                        )
                    )
                }
            }
        }

        // 6. Staff
        if (root.has("staff")) {
            val arr = root.getJSONArray("staff")
            Log.d("OPTIX_FLOW", "[STAFF RESTORE] Restoring ${arr.length()} staff members into Room")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val permsArr = try { obj.getJSONArray("permissions") } catch (e: Exception) { null }
                val permsList = mutableListOf<String>()
                if (permsArr != null) {
                    for (j in 0 until permsArr.length()) permsList.add(permsArr.optString(j))
                }
                val permJson = org.json.JSONArray(permsList).toString()
                val existing = try { staffRepo?.getStaffById(obj.optString("id")) } catch (e: Exception) { null }
                val staff = com.example.data.entity.Staff(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    username = obj.optString("username"),
                    password = existing?.password ?: "",
                    role = obj.optString("role", "staff"),
                    isDisabled = obj.optBoolean("isDisabled", false),
                    businessId = obj.optString("businessId", ""),
                    phone = if (obj.isNull("phone")) null else obj.optString("phone"),
                    email = if (obj.isNull("email")) null else obj.optString("email"),
                    lastModified = System.currentTimeMillis(),
                    isDeleted = false,
                    permissionsJson = permJson,
                    canBillWeightBased = permsList.contains("WEIGHT_BILLING"),
                    canEditWeight = permsList.contains("EDIT_WEIGHT"),
                    canEnterAmount = permsList.contains("ENTER_AMOUNT"),
                    canChangeProductPrice = permsList.contains("CHANGE_PRICE")
                )
                staffRepo?.insert(staff)
                Log.d("OPTIX_FLOW", "[STAFF RESTORED] Staff: ${staff.name} (${staff.username}), isDisabled=${staff.isDisabled}, perms=$permJson")
            }
        }

        // 7. Sessions
        if (root.has("sessions")) {
            val arr = root.getJSONArray("sessions")
            val db = com.example.OptixApplication.instance.database
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val loginAtTs = try {
                    val str = obj.optString("loginAt")
                    if (str.isNotEmpty()) java.time.Instant.parse(str).toEpochMilli() else System.currentTimeMillis()
                } catch (e: Exception) { System.currentTimeMillis() }

                val session = StaffSession(
                    id = obj.optString("id"),
                    staffId = obj.optString("staffId"),
                    businessId = obj.optString("businessId"),
                    deviceId = if (obj.isNull("deviceId")) null else obj.optString("deviceId"),
                    deviceName = if (obj.isNull("deviceName")) null else obj.optString("deviceName"),
                    loginAt = loginAtTs,
                    isActive = obj.optBoolean("isActive", true)
                )
                db.staffSessionDao().insertSession(session)
            }
            Log.d("OPTIX_FLOW", "[SESSIONS RESTORED] Inserted ${arr.length()} sessions into Room")
        }

        // 8. Activity Logs
        if (root.has("activityLogs")) {
            val arr = root.getJSONArray("activityLogs")
            val db = com.example.OptixApplication.instance.database
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val createdTs = try {
                    val str = obj.optString("createdAt")
                    if (str.isNotEmpty()) java.time.Instant.parse(str).toEpochMilli() else System.currentTimeMillis()
                } catch (e: Exception) { System.currentTimeMillis() }

                val log = com.example.data.entity.StaffActivityLog(
                    id = obj.optString("id"),
                    staffId = obj.optString("staffId"),
                    businessId = obj.optString("businessId"),
                    action = obj.optString("action"),
                    entityType = if (obj.isNull("entityType")) null else obj.optString("entityType"),
                    entityId = if (obj.isNull("entityId")) null else obj.optString("entityId"),
                    deviceId = if (obj.isNull("deviceId")) null else obj.optString("deviceId"),
                    isSuspicious = obj.optBoolean("isSuspicious", false),
                    severity = obj.optString("severity", "NORMAL"),
                    createdAt = createdTs
                )
                db.staffActivityLogDao().insertLog(log)
            }
            Log.d("OPTIX_FLOW", "[ACTIVITY LOGS RESTORED] Inserted ${arr.length()} activity logs into Room")
        }
    }

    suspend fun saveProfile(profile: BusinessProfile) = suspendCoroutine<Unit> { cont ->
        val currentLast = lastSyncedProfile
        if (currentLast != null &&
            currentLast.name == profile.name &&
            currentLast.phone == profile.phone &&
            currentLast.address == profile.address &&
            currentLast.openingTime == profile.openingTime &&
            currentLast.closingTime == profile.closingTime &&
            currentLast.timezone == profile.timezone &&
            currentLast.showLogo == profile.showLogo &&
            currentLast.logoPath == profile.logoPath &&
            currentLast.footerMessage == profile.footerMessage &&
            currentLast.showBusinessName == profile.showBusinessName &&
            currentLast.showAddress == profile.showAddress &&
            currentLast.showPhone == profile.showPhone &&
            currentLast.showGst == profile.showGst &&
            currentLast.showDateTime == profile.showDateTime &&
            currentLast.showOrderNumber == profile.showOrderNumber &&
            currentLast.showCashierName == profile.showCashierName &&
            currentLast.showDiscounts == profile.showDiscounts &&
            currentLast.showTaxes == profile.showTaxes &&
            currentLast.taxPercentage == profile.taxPercentage &&
            currentLast.qrEnabled == profile.qrEnabled &&
            currentLast.showVisitAgain == profile.showVisitAgain
        ) {
            Log.d("OPTIX_FLOW", "[PROFILE UNCHANGED] Skipping network POST /business/profile (no changes detected)")
            return@suspendCoroutine cont.resume(Unit)
        }

        val json = JSONObject().apply {
            put("name", profile.name)
            put("phone", profile.phone)
            put("address", profile.address)
            put("openingTime", profile.openingTime)
            put("closingTime", profile.closingTime)
            put("timezone", profile.timezone)
            put("currency", profile.currency)
            put("receiptSettings", JSONObject().apply {
                put("showLogo", profile.showLogo)
                val cleanLogo = formatUrl(profile.logoPath)
                if (cleanLogo != null) {
                    put("logoUrl", cleanLogo)
                }
                put("footerMessage", profile.footerMessage)
                put("showBusinessName", profile.showBusinessName)
                put("showAddress", profile.showAddress)
                put("showPhone", profile.showPhone)
                put("showGst", profile.showGst)
                put("showDateTime", profile.showDateTime)
                put("showOrderNumber", profile.showOrderNumber)
                put("showCashierName", profile.showCashierName)
                put("showDiscounts", profile.showDiscounts)
                put("showTaxes", profile.showTaxes)
                put("taxPercentage", profile.taxPercentage)
                put("qrEnabled", profile.qrEnabled)
                put("showVisitAgain", profile.showVisitAgain)
            })
        }
        val req = buildRequest("/business/profile").post(json.toString().toRequestBody(jsonMediaType)).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { cont.resume(Unit) }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    lastSyncedProfile = profile
                    Log.d("OPTIX_FLOW", "[BUSINESS PROFILE SAVED TO CLOUD SUCCESS]")
                }
                cont.resume(Unit)
            }
        })
    }

    suspend fun resetBusinessDay(targetBusinessDate: String) = suspendCoroutine<Unit> { cont ->
        val json = JSONObject().apply {
            put("targetBusinessDate", targetBusinessDate)
        }
        val req = buildRequest("/business/reset").post(json.toString().toRequestBody(jsonMediaType)).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OPTIX_FLOW", "[CLOUD RESET ERR] POST /business/reset failed: ${e.message}")
                cont.resume(Unit)
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("OPTIX_FLOW", "[CLOUD RESET SUCCESS] POST /business/reset confirmed for $targetBusinessDate")
                }
                cont.resume(Unit)
            }
        })
    }

    suspend fun saveReceiptToggle(key: String, value: Boolean) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("receiptSettings", JSONObject().apply {
                    put("toggleKey", key)
                    put("toggleValue", value)
                    put(key, value)
                })
            }
            val req = buildRequest("/business/profile").post(json.toString().toRequestBody(jsonMediaType)).build()
            val resp = client.newCall(req).execute()
            Log.d("OPTIX_FLOW", "[RECEIPT TOGGLE POST SUCCESS] key: $key, val: $value, code: ${resp.code}")
        } catch (e: Exception) {
            Log.e("OPTIX_FLOW", "[TOGGLE POST ERR] ${e.message}")
        }
    }

    suspend fun postPaymentQr(qr: PaymentQrEntity) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("id", qr.id)
                put("name", qr.name)
                put("imageUrl", qr.imagePath)
                put("isActive", qr.isActive)
            }
            val req = buildRequest("/payment-qrs").post(json.toString().toRequestBody(jsonMediaType)).build()
            val resp = client.newCall(req).execute()
            Log.d("OPTIX_FLOW", "[POST PAYMENT QR SUCCESS] Code: ${resp.code}, id: ${qr.id}")
        } catch (e: Exception) {
            Log.e("OPTIX_FLOW", "[POST PAYMENT QR ERR] ${e.message}")
        }
    }

    suspend fun selectPaymentQr(id: String) = withContext(Dispatchers.IO) {
        try {
            val req = buildRequest("/payment-qrs/$id/select").put("".toRequestBody(jsonMediaType)).build()
            val resp = client.newCall(req).execute()
            Log.d("OPTIX_FLOW", "[SELECT PAYMENT QR SUCCESS] Code: ${resp.code}, id: $id")
        } catch (e: Exception) {
            Log.e("OPTIX_FLOW", "[SELECT PAYMENT QR ERR] ${e.message}")
        }
    }

    suspend fun deletePaymentQrCloud(id: String) = withContext(Dispatchers.IO) {
        try {
            val req = buildRequest("/payment-qrs/$id").delete().build()
            val resp = client.newCall(req).execute()
            Log.d("OPTIX_FLOW", "[DELETE PAYMENT QR SUCCESS] Code: ${resp.code}, id: $id")
        } catch (e: Exception) {
            Log.e("OPTIX_FLOW", "[DELETE PAYMENT QR ERR] ${e.message}")
        }
    }

    suspend fun pushPaymentQrs(qrs: List<PaymentQrEntity>, isDeleted: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            val qrArray = JSONArray()
            for (q in qrs) {
                qrArray.put(JSONObject().apply {
                    put("id", q.id)
                    put("name", q.name)
                    put("imageUrl", q.imagePath)
                    put("isActive", q.isActive)
                    if (isDeleted) put("isDeleted", true)
                })
            }
            val json = JSONObject().apply {
                put("paymentQrs", qrArray)
            }
            val req = buildRequest("/sync/push").post(json.toString().toRequestBody(jsonMediaType)).build()
            val resp = client.newCall(req).execute()
            Log.d("OPTIX_FLOW", "[PUSH QRS] isDeleted=$isDeleted, Response Code: ${resp.code}")
        } catch (e: Exception) {
            Log.e("OPTIX_FLOW", "[PUSH QRS ERR] ${e.message}")
        }
    }

    // --- Items ---
    val allItems: Flow<List<BillingItem>> = flow {
        try {
            val req = buildRequest("/products").get().build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val str = resp.body?.string() ?: "[]"
                val arr = JSONArray(str)
                val items = mutableListOf<BillingItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    items.add(
                        BillingItem(
                            id = obj.optString("id"),
                            name = obj.optString("name"),
                            price = obj.optDouble("price", 0.0),
                            categoryId = obj.optString("categoryId"),
                            imageUrl = obj.optString("imageUrl", "")
                        )
                    )
                }
                emit(items)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    suspend fun insertItem(item: BillingItem) = suspendCoroutine<Unit> { cont ->
        val json = JSONObject().apply {
            if (item.id.isNotEmpty()) put("id", item.id)
            put("name", item.name)
            put("price", item.price)
            put("pricingType", item.pricingType)
            put("unit", item.unit)
            put("categoryId", item.categoryId)
            put("imageUrl", item.imageUrl)
            put("isOutOfStock", item.isOutOfStock)
            put("description", item.description)
            put("barcode", item.barcode)
            put("sku", item.sku)
        }
        val req = buildRequest("/products").post(json.toString().toRequestBody(jsonMediaType)).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { cont.resume(Unit) }
            override fun onResponse(call: Call, response: Response) { cont.resume(Unit) }
        })
    }

    suspend fun deleteItem(itemId: String) = suspendCoroutine<Unit> { cont ->
        val req = buildRequest("/products/$itemId").delete().build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { cont.resume(Unit) }
            override fun onResponse(call: Call, response: Response) { cont.resume(Unit) }
        })
    }

    suspend fun uploadImage(uri: Uri, context: android.content.Context, fileName: String, category: String = "products"): String = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: return@withContext ""
            
            Log.d("OPTIX_SYNC", "[UPLOAD MULTIPART START] Category: $category, File: $fileName, Size: ${bytes.size} bytes")

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, bytes.toRequestBody("image/*".toMediaType()))
                .addFormDataPart("category", if (category == "business") "businesses" else "products")
                .build()
                
            val req = buildRequest("/uploads").post(requestBody).build()
            val resp = client.newCall(req).execute()
            val str = resp.body?.string() ?: ""
            Log.d("OPTIX_SYNC", "[UPLOAD MULTIPART RESPONSE] Code: ${resp.code}, Body: $str")

            if (resp.isSuccessful) {
                val json = JSONObject(str)
                val rawUrl = json.optString("url", "")
                val finalUrl = if (rawUrl.startsWith("/")) "https://api.optixapp.in$rawUrl" else rawUrl
                Log.d("OPTIX_SYNC", "[UPLOAD SUCCESS] Returned Public URL: $finalUrl")
                finalUrl
            } else {
                Log.e("OPTIX_SYNC", "[UPLOAD FAILED] ${resp.code} ${resp.message}")
                ""
            }
        } catch (e: Exception) {
            Log.e("OPTIX_SYNC", "[UPLOAD EXCEPTION] error: ${e.message}")
            ""
        }
    }

    // --- Categories ---
    val allCategories: Flow<List<Category>> = flow {
        try {
            val req = buildRequest("/categories").get().build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val str = resp.body?.string() ?: "[]"
                val arr = JSONArray(str)
                val list = mutableListOf<Category>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        Category(
                            id = obj.optString("id"),
                            name = obj.optString("name"),
                            sortOrder = obj.optInt("sortOrder", 0)
                        )
                    )
                }
                emit(list)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    suspend fun insertCategory(category: Category) = suspendCoroutine<Unit> { cont ->
        val json = JSONObject().apply {
            if (category.id.isNotEmpty()) put("id", category.id)
            put("name", category.name)
            put("sortOrder", category.sortOrder)
        }
        val req = buildRequest("/categories").post(json.toString().toRequestBody(jsonMediaType)).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { cont.resume(Unit) }
            override fun onResponse(call: Call, response: Response) { cont.resume(Unit) }
        })
    }

    suspend fun deleteCategory(categoryId: String) = suspendCoroutine<Unit> { cont ->
        val req = buildRequest("/categories/$categoryId").delete().build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { cont.resume(Unit) }
            override fun onResponse(call: Call, response: Response) { cont.resume(Unit) }
        })
    }

    // --- Orders ---
    val allOrders: Flow<List<BillOrder>> = flow {
        try {
            val req = buildRequest("/orders").get().build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val str = resp.body?.string() ?: "[]"
                val arr = JSONArray(str)
                val list = mutableListOf<BillOrder>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        BillOrder(
                            id = obj.optString("id"),
                            invoiceNumber = obj.optString("invoiceNumber"),
                            total = obj.optDouble("total", 0.0),
                            paymentMethod = obj.optString("paymentMethod", "CASH")
                        )
                    )
                }
                emit(list)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    suspend fun insertOrder(order: BillOrder): String = suspendCoroutine { cont ->
        val json = JSONObject().apply {
            if (order.id.isNotEmpty()) put("id", order.id)
            put("tokenNumber", order.tokenNumber)
            put("invoiceNumber", order.invoiceNumber)
            put("subtotal", order.subtotal)
            put("discount", order.discount)
            put("tax", order.tax)
            put("total", order.total)
            put("paymentMethod", order.paymentMethod)
            put("cashierName", order.cashierName)
            put("orderItemsJson", order.orderItemsJson)

            try {
                val arr = JSONArray(order.orderItemsJson)
                val itemsArr = JSONArray()
                for (i in 0 until arr.length()) {
                    val itemObj = arr.getJSONObject(i)
                    itemsArr.put(JSONObject().apply {
                        put("productId", itemObj.optString("itemId", itemObj.optString("id")))
                        put("productName", itemObj.optString("itemName", itemObj.optString("name")))
                        put("price", itemObj.optDouble("price", 0.0))
                        put("quantity", itemObj.optInt("quantity", 1))
                        if (itemObj.has("weight")) put("weight", itemObj.optDouble("weight"))
                        if (itemObj.has("unit")) put("unit", itemObj.optString("unit"))
                    })
                }
                put("items", itemsArr)
            } catch (e: Exception) {}
        }
        val req = buildRequest("/orders").post(json.toString().toRequestBody(jsonMediaType)).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { cont.resume(order.tokenNumber) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val str = response.body?.string() ?: "{}"
                    val obj = JSONObject(str)
                    val serverToken = obj.optString("tokenNumber", order.tokenNumber)
                    cont.resume(serverToken)
                } catch (e: Exception) {
                    cont.resume(order.tokenNumber)
                }
            }
        })
    }

    suspend fun deleteOrder(orderId: String) = suspendCoroutine<Unit> { cont ->
        val req = buildRequest("/orders/$orderId").delete().build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { cont.resume(Unit) }
            override fun onResponse(call: Call, response: Response) { cont.resume(Unit) }
        })
    }

    // --- Staff ---
    val allStaff: Flow<List<Staff>> = flow {
        try {
            val req = buildRequest("/staff").get().build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val str = resp.body?.string() ?: "[]"
                val arr = JSONArray(str)
                val list = mutableListOf<Staff>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        Staff(
                            id = obj.optString("id"),
                            name = obj.optString("name"),
                            username = obj.optString("username"),
                            role = obj.optString("role", "staff")
                        )
                    )
                }
                emit(list)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    suspend fun insertStaff(staff: com.example.data.entity.Staff) = suspendCoroutine<Unit> { cont ->
        val permsList = try {
            val arr = org.json.JSONArray(staff.permissionsJson)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) list.add(arr.optString(i))
            list
        } catch (e: Exception) {
            val list = mutableListOf<String>()
            if (staff.canBillWeightBased) list.add("WEIGHT_BILLING")
            if (staff.canEditWeight) list.add("EDIT_WEIGHT")
            if (staff.canEnterAmount) list.add("ENTER_AMOUNT")
            if (staff.canChangeProductPrice) list.add("CHANGE_PRICE")
            list
        }
        val permsArray = org.json.JSONArray(permsList)
        val json = JSONObject().apply {
            if (staff.id.isNotEmpty()) put("id", staff.id)
            put("name", staff.name)
            put("username", staff.username)
            put("role", staff.role)
            if (staff.password.isNotEmpty()) put("password", staff.password)
            put("isDisabled", staff.isDisabled)
            if (!staff.phone.isNullOrEmpty()) put("phone", staff.phone)
            if (!staff.email.isNullOrEmpty()) put("email", staff.email)
            put("permissions", permsArray)
        }
        val req = buildRequest("/staff").post(json.toString().toRequestBody(jsonMediaType)).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { cont.resume(Unit) }
            override fun onResponse(call: Call, response: Response) { cont.resume(Unit) }
        })
    }

    suspend fun updateStaff(staff: com.example.data.entity.Staff) = insertStaff(staff)

    suspend fun deleteStaff(staffId: String) = suspendCoroutine<Unit> { cont ->
        val req = buildRequest("/staff/$staffId").delete().build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { cont.resume(Unit) }
            override fun onResponse(call: Call, response: Response) { cont.resume(Unit) }
        })
    }

    suspend fun disableStaff(staffId: String) = withContext(Dispatchers.IO) {
        try {
            val req = buildRequest("/staff/$staffId/disable").put("".toRequestBody(jsonMediaType)).build()
            val resp = client.newCall(req).execute()
            Log.d("OPTIX_FLOW", "[DISABLE STAFF] Code: ${resp.code}, staffId: $staffId")
        } catch (e: Exception) {
            Log.e("OPTIX_FLOW", "[DISABLE STAFF ERR] ${e.message}")
        }
    }

    suspend fun enableStaff(staffId: String) = withContext(Dispatchers.IO) {
        try {
            val req = buildRequest("/staff/$staffId/enable").put("".toRequestBody(jsonMediaType)).build()
            val resp = client.newCall(req).execute()
            Log.d("OPTIX_FLOW", "[ENABLE STAFF] Code: ${resp.code}, staffId: $staffId")
        } catch (e: Exception) {
            Log.e("OPTIX_FLOW", "[ENABLE STAFF ERR] ${e.message}")
        }
    }

    suspend fun updateStaffPermissions(staffId: String, permissions: List<String>) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("permissions", org.json.JSONArray(permissions))
            }
            val req = buildRequest("/staff/$staffId/permissions").put(json.toString().toRequestBody(jsonMediaType)).build()
            val resp = client.newCall(req).execute()
            Log.d("OPTIX_FLOW", "[UPDATE PERMISSIONS] Code: ${resp.code}, staffId: $staffId, perms: $permissions")
        } catch (e: Exception) {
            Log.e("OPTIX_FLOW", "[UPDATE PERMISSIONS ERR] ${e.message}")
        }
    }

    suspend fun terminateStaffSession(sessionId: String) = withContext(Dispatchers.IO) {
        try {
            val req = buildRequest("/staff/sessions/$sessionId/terminate").post("".toRequestBody(jsonMediaType)).build()
            val resp = client.newCall(req).execute()
            Log.d("OPTIX_FLOW", "[TERMINATE SESSION] Code: ${resp.code}, sessionId: $sessionId")
        } catch (e: Exception) {
            Log.e("OPTIX_FLOW", "[TERMINATE SESSION ERR] ${e.message}")
        }
    }

    suspend fun markNotificationsRead() = withContext(Dispatchers.IO) {
        try {
            val req = buildRequest("/staff/notifications/read").put("".toRequestBody(jsonMediaType)).build()
            client.newCall(req).execute()
        } catch (e: Exception) {}
    }

    // --- Subscriptions ---
    val subscription: Flow<UserSubscription?> = flow {
        try {
            val req = buildRequest("/subscriptions").get().build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val str = resp.body?.string() ?: ""
                val obj = JSONObject(str)
                val pObj = if (obj.has("plan") && !obj.isNull("plan")) obj.getJSONObject("plan") else null

                val expiryStr = obj.optString("expiryDate", "")
                val expiryTs = try {
                    if (expiryStr.isEmpty()) 0L
                    else if (expiryStr.all { it.isDigit() }) expiryStr.toLong()
                    else {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        sdf.parse(expiryStr)?.time ?: 0L
                    }
                } catch (e: Exception) { 0L }

                emit(
                    UserSubscription(
                        uid = obj.optString("businessId", ""),
                        planId = obj.optString("planId", "TRIAL"),
                        planName = pObj?.optString("name", "Trial Plan") ?: obj.optString("planName", "Trial Plan"),
                        amount = obj.optDouble("amount", 0.0),
                        currency = obj.optString("currency", "₹"),
                        country = obj.optString("country", "India"),
                        billingCycle = obj.optString("billingCycle", "MONTHLY"),
                        status = obj.optString("status", "ACTIVE").uppercase(),
                        billsUsed = obj.optInt("billsUsed", 0),
                        productsUsed = obj.optInt("productsUsed", 0),
                        activationCode = obj.optString("activationCode", null),
                        expiryDate = expiryTs
                    )
                )
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun activateSubscriptionCode(code: String): ActivationResult = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply { put("code", code) }
            val req = buildRequest("/subscription/activate").post(json.toString().toRequestBody(jsonMediaType)).build()
            val resp = client.newCall(req).execute()
            val str = resp.body?.string() ?: ""
            val obj = JSONObject(str)
            ActivationResult(
                success = resp.isSuccessful && obj.optBoolean("success", true),
                message = obj.optString("message", null)
            )
        } catch (e: Exception) {
            ActivationResult(false, "Connection error")
        }
    }

    data class ActivationResult(val success: Boolean, val message: String? = null)

    suspend fun saveSubscription(sub: UserSubscription) {
        // This is usually handled by the backend after payment or code activation, 
        // but we might want to sync local trial state
        try {
            val json = JSONObject().apply {
                put("planId", sub.planId)
                put("country", sub.country)
                put("currency", sub.currency)
                put("billingCycle", sub.billingCycle)
                put("expiryDate", sub.expiryDate)
            }
            val req = buildRequest("/subscription/sync-local").post(json.toString().toRequestBody(jsonMediaType)).build()
            client.newCall(req).execute()
        } catch (e: Exception) {}
    }

    suspend fun getAvailablePlans(): List<SubscriptionPlan> {
        return listOf(
            SubscriptionPlan("STARTER", "Starter", 499.0, 30),
            SubscriptionPlan("GROWTH", "Growth", 999.0, 30)
        )
    }

    // --- Payments ---
    suspend fun createRazorpayOrder(planId: String, billingCycle: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("planId", planId)
                put("billingCycle", billingCycle)
            }
            val req = buildRequest("/payments/create-order").post(json.toString().toRequestBody(jsonMediaType)).build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: "{}"
            Log.d("OPTIX_FLOW", "[PAYMENT ORDER] Response Code: ${resp.code}, Body: $body")
            if (resp.isSuccessful) {
                JSONObject(body)
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun verifyRazorpayPayment(orderId: String, paymentId: String, signature: String): UserSubscription? = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("razorpay_order_id", orderId)
                put("razorpay_payment_id", paymentId)
                put("razorpay_signature", signature)
            }
            val req = buildRequest("/payments/verify").post(json.toString().toRequestBody(jsonMediaType)).build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val str = resp.body?.string() ?: ""
                android.util.Log.d("OPTIX_FLOW", "[VERIFY RESPONSE] Body: $str")
                val root = JSONObject(str)
                if (root.has("subscription") && !root.isNull("subscription")) {
                    val sObj = root.getJSONObject("subscription")
                    val pObj = if (sObj.has("plan") && !sObj.isNull("plan")) sObj.getJSONObject("plan") else null
                    
                    val expiryStr = sObj.optString("expiryDate", "")
                    val expiryTs = try {
                        if (expiryStr.isEmpty()) 0L
                        else if (expiryStr.all { it.isDigit() }) expiryStr.toLong()
                        else {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                            sdf.parse(expiryStr)?.time ?: 0L
                        }
                    } catch (e: Exception) { 0L }

                    UserSubscription(
                        uid = sObj.optString("businessId", ""),
                        planId = sObj.optString("planId", "TRIAL"),
                        planName = pObj?.optString("name", "Trial Plan") ?: sObj.optString("planName", "Trial Plan"),
                        amount = sObj.optDouble("amount", 0.0),
                        currency = sObj.optString("currency", "₹"),
                        country = sObj.optString("country", "India"),
                        billingCycle = sObj.optString("billingCycle", "MONTHLY"),
                        status = sObj.optString("status", "ACTIVE").uppercase(),
                        billsUsed = sObj.optInt("billsUsed", 0),
                        productsUsed = sObj.optInt("productsUsed", 0),
                        activationCode = sObj.optString("activationCode", null),
                        expiryDate = expiryTs
                    )
                } else null
            } else null
        } catch (e: Exception) { null }
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()
}
