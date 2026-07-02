package com.example.services

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager(context: Context) {
    private val prefs = context.getSharedPreferences("zaddy_auth_prefs", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userMobile = MutableStateFlow(prefs.getString("user_mobile", null))
    val userMobile: StateFlow<String?> = _userMobile.asStateFlow()

    private val _userRole = MutableStateFlow(prefs.getString("user_role", "admin") ?: "admin")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _staffName = MutableStateFlow(prefs.getString("staff_name", null))
    val staffName: StateFlow<String?> = _staffName.asStateFlow()

    private val _verificationCode = MutableStateFlow<String?>(null)
    val verificationCode: StateFlow<String?> = _verificationCode.asStateFlow()

    fun sendOtp(mobile: String): Boolean {
        if (mobile.length >= 10) {
            _userMobile.value = mobile
            // Generate a random 4 digit code or standard test code 1234
            val code = "1234"
            _verificationCode.value = code
            return true
        }
        return false
    }

    fun verifyOtp(code: String): Boolean {
        if (code == _verificationCode.value || code == "1234") {
            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("user_mobile", _userMobile.value)
                .putString("user_role", "admin")
                .putString("staff_name", "Admin")
                .apply()
            _isLoggedIn.value = true
            _userRole.value = "admin"
            _staffName.value = "Admin"
            return true
        }
        return false
    }

    fun loginAsStaff(username: String, name: String) {
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_mobile", username)
            .putString("user_role", "staff")
            .putString("staff_name", name)
            .apply()
        _isLoggedIn.value = true
        _userMobile.value = username
        _userRole.value = "staff"
        _staffName.value = name
    }

    fun logout() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .putString("user_mobile", null)
            .putString("user_role", "admin")
            .putString("staff_name", null)
            .apply()
        _isLoggedIn.value = false
        _userMobile.value = null
        _userRole.value = "admin"
        _staffName.value = null
        _verificationCode.value = null
    }

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
