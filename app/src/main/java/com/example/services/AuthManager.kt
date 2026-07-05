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

    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(auth.currentUser?.email)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _userRole = MutableStateFlow<String>(prefs.getString("user_role", "admin") ?: "admin")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _staffName = MutableStateFlow<String?>(prefs.getString("staff_name", auth.currentUser?.displayName))
    val staffName: StateFlow<String?> = _staffName.asStateFlow()

    private val _userId = MutableStateFlow<String?>(auth.currentUser?.uid)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _isLoggedIn.value = user != null
            _userEmail.value = user?.email
            _userId.value = user?.uid
            if (user == null) {
                _userRole.value = "admin"
                _staffName.value = null
            } else {
                // If it's a new login, we might need to fetch role from Firestore
                // For now, default to admin if not staff
                if (_userRole.value != "staff") {
                    _userRole.value = "admin"
                    _staffName.value = user.displayName ?: "Admin"
                }
            }
        }
    }

    fun loginAsStaff(username: String, name: String) {
        prefs.edit()
            .putString("user_role", "staff")
            .putString("staff_name", name)
            .apply()
        _userRole.value = "staff"
        _staffName.value = name
    }

    fun signIn(email: String, pass: String, onComplete: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
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
            .apply()
        _userRole.value = "admin"
        _staffName.value = null
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
