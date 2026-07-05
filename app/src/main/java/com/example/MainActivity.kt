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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.presentation.screens.*
import com.example.presentation.viewmodel.AuthViewModel
import com.example.presentation.viewmodel.BusinessSetupViewModel
import com.example.presentation.viewmodel.ViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
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
    
    // Check initial destination: if logged in AND business setup is done, go straight to main shell (Auto Login!)
    val startDestination = when {
        authViewModel.isLoggedIn.value && (authViewModel.userRole.value == "staff" || profileViewModel.profile.value != null) -> "main"
        authViewModel.isLoggedIn.value -> "business_setup"
        else -> "login"
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
    }
}
