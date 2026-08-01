package com.example.services

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager(context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val prefs = context.getSharedPreferences("zaddy_auth_prefs", Context.MODE_PRIVATE)

    private val _userRole = MutableStateFlow<String>(prefs.getString("user_role", "admin") ?: "admin")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _staffName = MutableStateFlow<String?>(prefs.getString("staff_name", null))
    val staffName: StateFlow<String?> = _staffName.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null || _userRole.value == "staff")
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(auth.currentUser?.email)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _userId = MutableStateFlow<String?>(auth.currentUser?.uid ?: prefs.getString("admin_id", null))
    val userId: StateFlow<String?> = _userId.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _userEmail.value = user?.email
            
            if (user != null) {
                // Firebase Admin is logged in
                _userId.value = user.uid
                _isLoggedIn.value = true
                _userRole.value = "admin"
                _staffName.value = user.displayName ?: "Admin"
                prefs.edit()
                    .putString("user_role", "admin")
                    .putString("admin_id", user.uid)
                    .apply()
            } else {
                // No Firebase user. Check if we are in Staff mode
                val savedRole = prefs.getString("user_role", "admin")
                if (savedRole == "staff") {
                    _userRole.value = "staff"
                    _isLoggedIn.value = true
                    _userId.value = prefs.getString("admin_id", null)
                } else {
                    _userRole.value = "admin"
                    _isLoggedIn.value = false
                    _userId.value = null
                    _staffName.value = null
                    prefs.edit().putString("user_role", "admin").apply()
                }
            }
        }
    }

    fun loginAsStaff(username: String, name: String, adminId: String?) {
        // Sign out from Firebase if an Admin is logged in, 
        // but keep the adminId to access their data.
        auth.signOut() 

        prefs.edit()
            .putString("user_role", "staff")
            .putString("staff_name", name)
            .putString("admin_id", adminId)
            .apply()
        _userRole.value = "staff"
        _staffName.value = name
        _userId.value = adminId
        _isLoggedIn.value = true
    }

    fun signIn(email: String, pass: String, onComplete: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    prefs.edit()
                        .putString("user_role", "admin")
                        .putString("admin_id", user?.uid)
                        .apply()
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message ?: "Login failed")
                }
            }
    }

    fun signUp(email: String, pass: String, onComplete: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    prefs.edit()
                        .putString("user_role", "admin")
                        .putString("admin_id", user?.uid)
                        .apply()
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message ?: "Registration failed")
                }
            }
    }

    fun logout() {
        auth.signOut()
        prefs.edit()
            .putString("user_role", "admin")
            .putString("staff_name", null)
            .putString("admin_id", null)
            .apply()
        _userRole.value = "admin"
        _staffName.value = null
        _userId.value = null
        _isLoggedIn.value = false
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
