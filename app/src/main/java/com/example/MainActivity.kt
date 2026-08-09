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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.presentation.screens.*
import com.example.presentation.viewmodel.*
import com.example.data.entity.UserSubscription
import com.example.ui.theme.MyApplicationTheme
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    companion object {
        var paymentResultListener: ((Boolean, String?, PaymentData?) -> Unit)? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Checkout.preload(applicationContext)
        
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

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        android.util.Log.d("OPTIX_PAYMENT", "[SUCCESS] ID: $razorpayPaymentId")
        paymentResultListener?.invoke(true, razorpayPaymentId, paymentData)
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        android.util.Log.e("OPTIX_PAYMENT", "[ERROR] Code: $code, Resp: $response")
        paymentResultListener?.invoke(false, response, paymentData)
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
    val itemsViewModel: ItemsViewModel = viewModel(factory = factory)
    
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val userRole by authViewModel.userRole.collectAsState()

    // Enforce FeatureGate globally
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            withContext(Dispatchers.IO) {
                // Initial sync of FeatureGate from DB
                val sub = application.subscriptionRepository.getSubscriptionSync()
                val bills = application.billOrderRepository.getOrdersSync().size
                val prods = application.billingItemRepository.getAllItemsSync().size
                
                val initialSub = (sub ?: UserSubscription()).copy(
                    billsUsed = bills,
                    productsUsed = prods
                )
                com.example.services.FeatureGate.updateSubscription(initialSub)
            }

            // Keep observing for plan changes
            application.subscriptionRepository.subscription.collect { s ->
                if (s != null) {
                    withContext(Dispatchers.IO) {
                        val currentBills = application.billOrderRepository.getOrdersSync().size
                        val currentProds = application.billingItemRepository.getAllItemsSync().size
                        val updatedSub = if (s.planId == "TRIAL") {
                            s.copy(billsUsed = currentBills, productsUsed = currentProds)
                        } else s
                        com.example.services.FeatureGate.updateSubscription(updatedSub)
                    }
                }
            }
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            com.example.services.RealtimeSyncManager.getInstance(application).connect()
            com.example.services.SyncManager.getInstance(application).startSyncLoop()
        } else {
            com.example.services.RealtimeSyncManager.getInstance(application).disconnect()
        }
    }

    val startDestination = remember(isLoggedIn, userRole) {
        when {
            !isLoggedIn -> "login"
            userRole == "staff" || application.authManager.isSetupCompleted() -> "main"
            else -> "business_setup"
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
                itemsViewModel = itemsViewModel,
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
            ManageCategoriesScreen(navController, itemsViewModel)
        }

        composable("manage_staff") {
            val staffViewModel: StaffViewModel = viewModel(factory = factory)
            ManageStaffScreen(navController, staffViewModel)
        }

        composable("staff_detail/{staffId}") { backStackEntry ->
            val staffId = backStackEntry.arguments?.getString("staffId") ?: ""
            val staffViewModel: StaffViewModel = viewModel(factory = factory)
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            StaffDetailScreen(navController, staffId, staffViewModel, settingsViewModel)
        }

        composable("add_edit_item") {
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
