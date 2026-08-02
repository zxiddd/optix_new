package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.presentation.screens.*
import com.example.presentation.viewmodel.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val application = OptixApplication.instance
            val factory = ViewModelFactory(application)
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            val profile by settingsViewModel.profile.collectAsState(initial = null)
            
            val isDark = true // Defaulting to true as requested to remove dark mode toggle

            MyApplicationTheme(darkTheme = isDark) {
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { }

                LaunchedEffect(Unit) {
                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                        )
                    } else {
                        arrayOf(
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    }
                    
                    val needed = permissions.filter {
                        ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (needed.isNotEmpty()) {
                        launcher.launch(needed.toTypedArray())
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OptixBillingApp()
                }
            }
        }
    }
}

@Composable
fun OptixBillingApp() {
    val navController = rememberNavController()
    
    // Retrieve the Application instance to pass to the ViewModel factory
    val application = OptixApplication.instance
    val factory = ViewModelFactory(application)

    // Instantiate shared ViewModels
    val authViewModel: AuthViewModel = viewModel(factory = factory)
    val profileViewModel: BusinessSetupViewModel = viewModel(factory = factory)
    
    // Check initial destination ONCE
    val startDestination = remember {
        val isLoggedIn = authViewModel.isLoggedIn.value
        val userRole = authViewModel.userRole.value
        val profile = profileViewModel.profile.value
        when {
            isLoggedIn && (userRole == "staff" || profile?.setupCompleted == true) -> "main"
            isLoggedIn -> "business_setup"
            else -> "login"
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(navController, authViewModel)
        }
        
        composable("business_setup") {
            val setupViewModel: BusinessSetupViewModel = viewModel(factory = factory)
            BusinessSetupScreen(navController, setupViewModel)
        }
        
        composable("main") {
            MainShellScreen(
                navController = navController,
                authViewModel = authViewModel,
                profileViewModel = profileViewModel,
                billingViewModel = viewModel(factory = factory),
                historyViewModel = viewModel(factory = factory),
                analyticsViewModel = viewModel(factory = factory),
                itemsViewModel = viewModel(factory = factory),
                settingsViewModel = viewModel(factory = factory),
                staffViewModel = viewModel(factory = factory)
            )
        }

        composable("receipt_customization") {
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            ReceiptCustomizationScreen(navController, settingsViewModel)
        }

        composable("payment_accounts") {
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            PaymentAccountsScreen(navController, settingsViewModel)
        }

        composable("manage_categories") {
            val itemsViewModel: ItemsViewModel = viewModel(factory = factory)
            ManageCategoriesScreen(navController, itemsViewModel)
        }

        composable("manage_staff") {
            val staffViewModel: StaffViewModel = viewModel(factory = factory)
            ManageStaffScreen(navController, staffViewModel)
        }

        composable("add_edit_item") {
            val itemsViewModel: ItemsViewModel = viewModel(factory = factory)
            AddEditItemScreen(navController, itemsViewModel)
        }

        composable("subscription") {
            val subViewModel: SubscriptionViewModel = viewModel(factory = factory)
            SubscriptionScreen(navController, subViewModel)
        }

        composable("support") {
            val aiViewModel: AiAssistantViewModel = viewModel(factory = factory)
            SupportScreen(navController, aiViewModel)
        }
    }
}
