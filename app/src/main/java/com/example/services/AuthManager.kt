package com.example.services

import android.content.Context
import com.example.OptixApplication
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class AuthManager(private val context: Context) {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val baseUrl = "https://api.optixapp.in/api/v1"

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "optix_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("optix_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    private val _userRole = MutableStateFlow<String>(prefs.getString("user_role", "admin") ?: "admin")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _staffName = MutableStateFlow<String?>(prefs.getString("staff_name", null))
    val staffName: StateFlow<String?> = _staffName.asStateFlow()

    private val _userPermissions = MutableStateFlow<Set<String>>(
        prefs.getStringSet("staff_permissions", emptySet()) ?: emptySet()
    )
    val userPermissions: StateFlow<Set<String>> = _userPermissions.asStateFlow()

    private val _isLoggedIn = MutableStateFlow<Boolean>(prefs.getString("access_token", null) != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(prefs.getString("user_email", null))
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _userId = MutableStateFlow<String?>(prefs.getString("user_id", null))
    val userId: StateFlow<String?> = _userId.asStateFlow()

    fun getAccessToken(): String? = prefs.getString("access_token", null)
    fun getBusinessId(): String? = prefs.getString("business_id", null)
    fun getUserId(): String? = prefs.getString("user_id", null)

    fun hasPermission(action: String): Boolean {
        val role = _userRole.value.lowercase()
        if (role == "admin" || role == "owner") return true
        return _userPermissions.value.contains(action)
    }

    fun updatePermissions(perms: List<String>) {
        val permSet = perms.toSet()
        prefs.edit().putStringSet("staff_permissions", permSet).apply()
        _userPermissions.value = permSet
    }

    fun loginAsStaff(username: String, pass: String?, onComplete: (Boolean, String?) -> Unit) {
        val devId = try {
            android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "device_1"
        } catch (e: Exception) { "device_1" }
        val devName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()

        val json = JSONObject().apply {
            put("username", username)
            if (!pass.isNull_or_empty()) put("password", pass)
            put("deviceId", devId)
            put("deviceName", devName)
        }
        val body = json.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("$baseUrl/auth/staff/signin")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onComplete(false, e.message ?: "Network failure")
            }

            override fun onResponse(call: Call, response: Response) {
                val respStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    try {
                        val obj = JSONObject(respStr)
                        val at = obj.optString("access_token")
                        val rt = obj.optString("refresh_token")
                        val name = obj.optString("name", username)
                        val bId = obj.optString("businessId", "")
                        val sId = obj.optString("staffId", "")
                        val role = obj.optString("role", "staff")

                        val permsArr = obj.optJSONArray("permissions")
                        val permsList = mutableListOf<String>()
                        if (permsArr != null) {
                            for (i in 0 until permsArr.length()) permsList.add(permsArr.getString(i))
                        }
                        val permSet = permsList.toSet()

                        // Reset sync timestamp so full dump is fetched for staff
                        context.getSharedPreferences("zaddy_sync_prefs", Context.MODE_PRIVATE)
                            .edit().putLong("last_sync_ts", 0L).apply()

                        prefs.edit()
                            .putString("access_token", at)
                            .putString("refresh_token", rt)
                            .putString("user_role", role.lowercase())
                            .putString("staff_name", name)
                            .putString("business_id", bId)
                            .putString("user_id", sId)
                            .putStringSet("staff_permissions", permSet)
                            .apply()

                        _userRole.value = role.lowercase()
                        _staffName.value = name
                        _userId.value = sId
                        _userPermissions.value = permSet
                        _isLoggedIn.value = true

                        // Hydrate Room DB synchronously on staff login
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val cloudRepo = com.example.data.repository.CloudRepository(sId, at)
                                val db = OptixApplication.instance.database
                                cloudRepo.syncCloudToLocal(
                                    com.example.data.repository.CategoryRepository(db.categoryDao()),
                                    com.example.data.repository.BillingItemRepository(db.billingItemDao()),
                                    com.example.data.repository.BillOrderRepository(db.billOrderDao()),
                                    com.example.data.repository.BusinessProfileRepository(db.businessProfileDao()),
                                    com.example.data.repository.PaymentQrRepository(db.paymentQrDao()),
                                    com.example.data.repository.StaffRepository(db.staffDao())
                                )
                                android.util.Log.d("OPTIX_FLOW", "[STAFF ROOM HYDRATED] Successfully hydrated Room for staff $name")
                            } catch (e: Exception) {
                                android.util.Log.e("OPTIX_FLOW", "[STAFF HYDRATION ERR] ${e.message}")
                            } finally {
                                RealtimeSyncManager.getInstance(context).connect()
                                mainHandler.post { onComplete(true, null) }
                            }
                        }
                    } catch (e: Exception) {
                        onComplete(false, "Invalid server response")
                    }
                } else {
                    val serverMsg = try {
                        JSONObject(respStr).optString("message", "Staff authentication failed")
                    } catch (e: Exception) {
                        "Staff authentication failed"
                    }
                    onComplete(false, serverMsg)
                }
            }
        })
    }

    fun signIn(email: String, pass: String, onComplete: (Boolean, String?) -> Unit) {
        val json = JSONObject().apply {
            put("email", email)
            put("password", pass)
        }
        val body = json.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("$baseUrl/auth/local/signin")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { onComplete(false, e.message ?: "Network error") }
            }

            override fun onResponse(call: Call, response: Response) {
                val respStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    try {
                        val obj = JSONObject(respStr)
                        val at = obj.optString("access_token")
                        val rt = obj.optString("refresh_token")
                        val uId = obj.optString("userId", "")
                        val bId = obj.optString("businessId", "")
                        val setupCompleted = obj.optBoolean("setupCompleted", false)

                        prefs.edit()
                            .putString("access_token", at)
                            .putString("refresh_token", rt)
                            .putString("user_email", email)
                            .putString("user_role", "admin")
                            .putString("user_id", uId)
                            .putString("business_id", bId)
                            .putBoolean("setup_completed", setupCompleted)
                            .apply()

                        android.util.Log.d("OPTIX_FLOW", "[SIGNIN SUCCESS] Token saved for user: $uId, business: $bId")

                        CoroutineScope(Dispatchers.IO).launch {
                            android.util.Log.d("OPTIX_FLOW", "[FULL DUMP START] Hydrating Room from cloud...")
                            try {
                                val repo = com.example.data.repository.CloudRepository(uId, at)
                                val db = OptixApplication.instance.database
                                repo.syncCloudToLocal(
                                    com.example.data.repository.CategoryRepository(db.categoryDao()),
                                    com.example.data.repository.BillingItemRepository(db.billingItemDao()),
                                    com.example.data.repository.BillOrderRepository(db.billOrderDao()),
                                    com.example.data.repository.BusinessProfileRepository(db.businessProfileDao()),
                                    com.example.data.repository.PaymentQrRepository(db.paymentQrDao()),
                                    com.example.data.repository.StaffRepository(db.staffDao())
                                )
                                val pCount = db.billingItemDao().getAllItemsSync().size
                                val cCount = db.categoryDao().getAllCategoriesSync().size
                                val oCount = db.billOrderDao().getAllOrdersSync().size
                                android.util.Log.d("OPTIX_FLOW", "[ROOM POPULATED] Products=$pCount, Categories=$cCount, Orders=$oCount")
                            } catch (e: Exception) {
                                android.util.Log.e("OPTIX_FLOW", "[FULL DUMP ERR] ${e.message}")
                            }

                            _userEmail.value = email
                            _userRole.value = "admin"
                            _userId.value = uId
                            _isLoggedIn.value = true
                            mainHandler.post { onComplete(true, null) }
                        }
                    } catch (e: Exception) {
                        mainHandler.post { onComplete(false, "Invalid response payload") }
                    }
                } else {
                    val errMsg = try {
                        val obj = JSONObject(respStr)
                        obj.optString("message", "Invalid email or password")
                    } catch (e: Exception) {
                        "Invalid email or password"
                    }
                    mainHandler.post { onComplete(false, errMsg) }
                }
            }
        })
    }

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    fun signInWithGoogle(email: String, name: String?, googleId: String?, onComplete: (Boolean, String?) -> Unit) {
        val json = JSONObject().apply {
            put("email", email)
            if (!name.isNull_or_empty()) put("name", name)
            if (!googleId.isNull_or_empty()) put("googleId", googleId)
        }
        val body = json.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("$baseUrl/auth/google")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { onComplete(false, e.message ?: "Network failure") }
            }

            override fun onResponse(call: Call, response: Response) {
                val respStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    try {
                        val obj = JSONObject(respStr)
                        val at = obj.optString("access_token")
                        val rt = obj.optString("refresh_token")
                        val uId = obj.optString("userId", "")
                        val bId = obj.optString("businessId", "")
                        val setupCompleted = obj.optBoolean("setupCompleted", false)

                        prefs.edit()
                            .putString("access_token", at)
                            .putString("refresh_token", rt)
                            .putString("user_email", email)
                            .putString("user_role", "admin")
                            .putString("user_id", uId)
                            .putString("business_id", bId)
                            .putBoolean("setup_completed", setupCompleted)
                            .apply()

                        _userEmail.value = email
                        _userRole.value = "admin"
                        _userId.value = uId
                        _isLoggedIn.value = true
                        mainHandler.post { onComplete(true, null) }
                    } catch (e: Exception) {
                        mainHandler.post { onComplete(false, "Invalid Google Auth response") }
                    }
                } else {
                    mainHandler.post { onComplete(false, "Google authentication failed") }
                }
            }
        })
    }

    fun isSetupCompleted(): Boolean = prefs.getBoolean("setup_completed", false)
    fun setSetupCompleted(completed: Boolean) {
        prefs.edit().putBoolean("setup_completed", completed).apply()
    }

    fun signUp(
        email: String,
        pass: String,
        businessName: String,
        phone: String = "",
        address: String = "",
        onComplete: (Boolean, String?) -> Unit
    ) {
        val json = JSONObject().apply {
            put("email", email)
            put("password", pass)
            put("businessName", businessName)
            put("phone", phone)
            put("address", address)
        }
        val body = json.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("$baseUrl/auth/local/signup")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { onComplete(false, e.message ?: "Network failure") }
            }

            override fun onResponse(call: Call, response: Response) {
                val respStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    try {
                        val obj = JSONObject(respStr)
                        val at = obj.optString("access_token")
                        val rt = obj.optString("refresh_token")
                        val uId = obj.optString("userId", "")
                        val bId = obj.optString("businessId", "")
                        val setupCompleted = obj.optBoolean("setupCompleted", false)

                        prefs.edit()
                            .putString("access_token", at)
                            .putString("refresh_token", rt)
                            .putString("user_email", email)
                            .putString("user_role", "admin")
                            .putString("user_id", uId)
                            .putString("business_id", bId)
                            .putBoolean("setup_completed", setupCompleted)
                            .apply()

                        android.util.Log.d("OPTIX_FLOW", "[LOGIN SUCCESS] Token saved for user: $uId, business: $bId")
                        
                        CoroutineScope(Dispatchers.IO).launch {
                            android.util.Log.d("OPTIX_FLOW", "[FULL DUMP START] Hydrating Room from cloud...")
                            try {
                                val repo = com.example.data.repository.CloudRepository(uId, at)
                                val db = OptixApplication.instance.database
                                repo.syncCloudToLocal(
                                    com.example.data.repository.CategoryRepository(db.categoryDao()),
                                    com.example.data.repository.BillingItemRepository(db.billingItemDao()),
                                    com.example.data.repository.BillOrderRepository(db.billOrderDao()),
                                    com.example.data.repository.BusinessProfileRepository(db.businessProfileDao()),
                                    com.example.data.repository.PaymentQrRepository(db.paymentQrDao()),
                                    com.example.data.repository.StaffRepository(db.staffDao())
                                )
                                val pCount = db.billingItemDao().getAllItemsSync().size
                                val cCount = db.categoryDao().getAllCategoriesSync().size
                                val oCount = db.billOrderDao().getAllOrdersSync().size
                                android.util.Log.d("OPTIX_FLOW", "[ROOM POPULATED] Products=$pCount, Categories=$cCount, Orders=$oCount")
                            } catch (e: Exception) {
                                android.util.Log.e("OPTIX_FLOW", "[FULL DUMP ERR] ${e.message}")
                            }

                            _userEmail.value = email
                            _userRole.value = "admin"
                            _userId.value = uId
                            _isLoggedIn.value = true
                            mainHandler.post { onComplete(true, null) }
                        }
                    } catch (e: Exception) {
                        mainHandler.post { onComplete(false, "Invalid registration payload") }
                    }
                } else {
                    val errMsg = try {
                        val obj = JSONObject(respStr)
                        obj.optString("message", "Registration failed")
                    } catch (e: Exception) {
                        "Registration failed"
                    }
                    mainHandler.post { onComplete(false, errMsg) }
                }
            }
        })
    }

    var onLogout: (() -> Unit)? = null

    fun logout(scope: CoroutineScope? = null) {
        context.getSharedPreferences("zaddy_sync_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()

        prefs.edit().clear().apply()

        _userRole.value = "admin"
        _staffName.value = null
        _userEmail.value = null
        _userId.value = null
        _userPermissions.value = emptySet()
        _isLoggedIn.value = false

        try {
            // Clear session tokens
        } catch (e: Exception) {
            // Ignore if auth is not initialized
        }

        try {
            val app = OptixApplication.instance
            if (scope != null) {
                scope.launch(Dispatchers.IO) {
                    app.database.clearAllTables()
                }
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    app.database.clearAllTables()
                }
            }
        } catch (e: Exception) {
            // Ignore DB clear error
        }
        
        onLogout?.invoke()
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()

    companion object {
        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AuthManager(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
