package com.example.presentation.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import com.example.services.PrinterDevice
import com.example.services.PrinterManager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.R
import com.example.data.entity.BillingItem
import com.example.data.entity.BusinessProfile
import com.example.data.entity.Category
import com.example.data.entity.OrderItem
import com.example.presentation.viewmodel.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- MAIN SHELL SCREEN (WITH ADAPTIVE NAVIGATION) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShellScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    profileViewModel: BusinessSetupViewModel,
    billingViewModel: BillingViewModel,
    historyViewModel: OrderHistoryViewModel,
    analyticsViewModel: AnalyticsViewModel,
    itemsViewModel: ItemsViewModel,
    settingsViewModel: SettingsViewModel,
    staffViewModel: StaffViewModel
) {
    val context = LocalContext.current
    val currentProfile = profileViewModel.profile.collectAsState(initial = null).value
    val userRole by authViewModel.userRole.collectAsState()
    val isStaff = userRole == "staff"
    
    // Auto-update the current token count state in billing VM
    LaunchedEffect(Unit) {
        billingViewModel.updateCurrentTokenState(context)
    }

    var currentTab by remember { mutableStateOf("billing") }
    val isPrinterConnected = settingsViewModel.connectedDevice.collectAsState(initial = null).value != null

    // Screen size classes simulation (Adaptive layouts!)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        val navItems = if (isStaff) {
                            listOf(
                                Triple("billing", "Billing", Icons.Default.ReceiptLong),
                                Triple("history", "History", Icons.Default.History),
                                Triple("settings", "Settings", Icons.Default.Settings)
                            )
                        } else {
                            listOf(
                                Triple("billing", "Billing", Icons.Default.ReceiptLong),
                                Triple("history", "History", Icons.Default.History),
                                Triple("items", "Items", Icons.Default.RestaurantMenu),
                                Triple("analytics", "Analytics", Icons.Default.BarChart),
                                Triple("settings", "Settings", Icons.Default.Settings)
                            )
                        }
                        navItems.forEach { (route, label, icon) ->
                            NavigationBarItem(
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 10.sp) },
                                selected = currentTab == route,
                                onClick = { currentTab = route },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_tab_$route")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        bottom = if (isWideScreen) 0.dp else innerPadding.calculateBottomPadding(),
                        top = innerPadding.calculateTopPadding()
                    )
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        header = {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 16.dp)
                                    .size(48.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_zaddy_logo),
                                    contentDescription = "Zaddy Logo",
                                    modifier = Modifier.size(36.dp).clip(CircleShape)
                                )
                            }
                        }
                    ) {
                        val navItems = if (isStaff) {
                            listOf(
                                Triple("billing", "Billing", Icons.Default.ReceiptLong),
                                Triple("history", "History", Icons.Default.History),
                                Triple("settings", "Settings", Icons.Default.Settings)
                            )
                        } else {
                            listOf(
                                Triple("billing", "Billing", Icons.Default.ReceiptLong),
                                Triple("history", "History", Icons.Default.History),
                                Triple("items", "Items", Icons.Default.RestaurantMenu),
                                Triple("analytics", "Analytics", Icons.Default.BarChart),
                                Triple("settings", "Settings", Icons.Default.Settings)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        navItems.forEach { (route, label, icon) ->
                            NavigationRailItem(
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                selected = currentTab == route,
                                onClick = { currentTab = route },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_rail_$route")
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // Main screen routing switcher
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentTab) {
                        "billing" -> BillingScreen(billingViewModel, currentProfile, userRole)
                        "history" -> HistoryScreen(historyViewModel, currentProfile, userRole)
                        "items" -> ItemsScreen(itemsViewModel)
                        "analytics" -> AnalyticsScreen(analyticsViewModel, currentProfile)
                        "settings" -> SettingsScreen(settingsViewModel, profileViewModel, navController, userRole, staffViewModel)
                    }
                }
            }
        }
    }

    // Modal thermal receipt preview overlay
    val showReceipt by billingViewModel.showReceiptPreview
    val receiptText by billingViewModel.lastPrintedReceipt
    if (showReceipt && receiptText != null) {
        ThermalReceiptDialog(
            receiptText = receiptText!!,
            currency = currentProfile?.currency ?: "₹",
            onDismiss = { billingViewModel.showReceiptPreview.value = false }
        )
    }
}

// --- 1. LOGIN / SPLASH SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isOtpSent by viewModel.isOtpSent
    val authError by viewModel.authError
    val isVerifying by viewModel.isVerifying
    val userRole by viewModel.userRole.collectAsState()

    // Auto-login side effect
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            if (userRole == "staff") {
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                }
            } else {
                navController.navigate("business_setup") {
                    popUpTo("login") { inclusive = true }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decorative background glowing brush lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFFFF6B00),
                radius = 180.dp.toPx(),
                center = Offset(size.width, 0f),
                alpha = 0.08f
            )
            drawCircle(
                color = Color(0xFFFF8A33),
                radius = 220.dp.toPx(),
                center = Offset(0f, size.height),
                alpha = 0.05f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant brand header
            Image(
                painter = painterResource(id = R.drawable.img_zaddy_logo),
                contentDescription = "Zaddy Billing Logo",
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(12.dp, RoundedCornerShape(24.dp))
                    .border(2.dp, Color(0xFFFF6B00), RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ZADDY BILLING",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 2.sp
            )

            Text(
                text = "Fast Billing. Smart Business.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Role Selector Tab/Buttons
            val isStaffMode by viewModel.isStaffMode
            Row(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isStaffMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { if (isStaffMode) viewModel.toggleMode() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Admin Login",
                        color = if (!isStaffMode) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isStaffMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { if (!isStaffMode) viewModel.toggleMode() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Staff Login",
                        color = if (isStaffMode) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isStaffMode) {
                            "Staff Account Login"
                        } else if (!isOtpSent) {
                            "Login to Continue"
                        } else {
                            "Verify One-Time PIN"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    if (isStaffMode) {
                        // Staff Username Input
                        OutlinedTextField(
                            value = viewModel.staffUsername.value,
                            onValueChange = { viewModel.staffUsername.value = it },
                            label = { Text("Username or Mobile") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Username") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("staff_username_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Staff Password Input
                        OutlinedTextField(
                            value = viewModel.staffPassword.value,
                            onValueChange = { viewModel.staffPassword.value = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("staff_password_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    } else {
                        if (!isOtpSent) {
                            // Mobile Input
                            OutlinedTextField(
                                value = viewModel.mobileNumber.value,
                                onValueChange = {
                                    if (it.length <= 10) viewModel.mobileNumber.value = it
                                },
                                label = { Text("Mobile Number") },
                                leadingIcon = {
                                    Row(
                                        modifier = Modifier.padding(start = 12.dp, end = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("+91", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(modifier = Modifier.width(1.dp).height(18.dp).background(MaterialTheme.colorScheme.outline))
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("mobile_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        } else {
                            // OTP Input
                            OutlinedTextField(
                                value = viewModel.otpCode.value,
                                onValueChange = {
                                    if (it.length <= 4) viewModel.otpCode.value = it
                                },
                                label = { Text("4-Digit OTP") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("otp_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    }

                    if (authError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = authError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (isStaffMode) {
                                viewModel.loginStaff {
                                    // Managed by LaunchedEffect
                                }
                            } else {
                                if (!isOtpSent) {
                                    viewModel.sendOtp()
                                } else {
                                    viewModel.verifyOtp {
                                        // Managed by LaunchedEffect
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_auth_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = if (isStaffMode) "Login as Staff" else if (!isOtpSent) "Send OTP" else "Verify & Login",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Help indicator badge
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Demo Tip",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Demo Mode: Enter any mobile number and use code '1234' to verify instantly.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

// --- 2. FIRST-LAUNCH BUSINESS SETUP SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSetupScreen(navController: NavController, viewModel: BusinessSetupViewModel) {
    val profile by viewModel.profile.collectAsState()
    val setupError by viewModel.setupError

    // If profile is already set, auto navigate to Main Screen
    LaunchedEffect(profile) {
        if (profile != null && profile?.setupCompleted == true) {
            navController.navigate("main") {
                popUpTo("business_setup") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to Zaddy",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Let's set up your business details. This will appear on your printed bills.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(0.7f)
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Store Configuration",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = viewModel.businessName.value,
                        onValueChange = { viewModel.businessName.value = it },
                        label = { Text("Business / Store Name *") },
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = "Store") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("setup_name"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    OutlinedTextField(
                        value = viewModel.address.value,
                        onValueChange = { viewModel.address.value = it },
                        label = { Text("Store Address *") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "Address") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("setup_address"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    OutlinedTextField(
                        value = viewModel.phone.value,
                        onValueChange = { viewModel.phone.value = it },
                        label = { Text("Store Phone Number *") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("setup_phone"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    OutlinedTextField(
                        value = viewModel.gstNumber.value,
                        onValueChange = { viewModel.gstNumber.value = it },
                        label = { Text("GST Number (Optional)") },
                        leadingIcon = { Icon(Icons.Default.Percent, contentDescription = "GST") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("setup_gst"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    // Currency Selector row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Currency Symbol:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        val currencies = listOf("₹", "$", "€", "£", "AED")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            currencies.forEach { sym ->
                                val selected = viewModel.selectedCurrency.value == sym
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.selectedCurrency.value = sym },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = sym,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = viewModel.footerMessage.value,
                        onValueChange = { viewModel.footerMessage.value = it },
                        label = { Text("Receipt Footer Message") },
                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = "Footer") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    if (setupError != null) {
                        Text(
                            text = setupError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { viewModel.saveBusinessProfile { } },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_profile_setup_btn")
                    ) {
                        Text("Complete Setup & Launch 🚀", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- 3. BILLING SCREEN (CHART / CHECKOUT GRID) ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BillingScreen(viewModel: BillingViewModel, profile: BusinessProfile?, userRole: String = "admin") {
    val items by viewModel.items.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val context = LocalContext.current
    var isSavingOrder by remember { mutableStateOf(false) }

    // Double pane or responsive cart view
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val showSplitPane = maxWidth >= 850.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Left Content Pane (Menu)
            Column(
                modifier = Modifier
                    .weight(if (showSplitPane) 1.6f else 1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                // Main Header Row (Sleek Interface Style)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ZADDY BILLING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = profile?.name ?: "Zaddy Store",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                        }
                    }

                    // Compact Search box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search menu...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(30.dp),
                        modifier = Modifier
                            .width(180.dp)
                            .height(48.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Stats / Token Section (Sleek Interface Style)
                val todaySalesVal by viewModel.todaySales.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Today's Sales Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0x0DFFFFFF)), // border-white/5 thin border
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "TODAY'S SALES",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${profile?.currency ?: "₹"}${todaySalesVal.toInt()}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    // Current Token Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0x0DFFFFFF)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "CURRENT TOKEN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "#${viewModel.currentTokenNum.value}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 12.dp)
                                    .size(32.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            )
                        }
                    }
                }

                // Scrollable categories Row (Sleek Interface Style)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == "All",
                            onClick = { viewModel.setCategory("All") },
                            label = { Text("All Menu", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF1A1A1A),
                                labelColor = Color(0xFFCBD5E1),
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.name,
                            onClick = { viewModel.setCategory(cat.name) },
                            label = { Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF1A1A1A),
                                labelColor = Color(0xFFCBD5E1),
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Filter items by category & search query
                val filteredItems = items.filter { item ->
                    (selectedCategory == "All" || item.category == selectedCategory) &&
                    (searchQuery.isEmpty() || item.name.contains(searchQuery, ignoreCase = true)) &&
                    item.isAvailable
                }

                if (filteredItems.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Inbox,
                                contentDescription = "Empty",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No available items found",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(130.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredItems) { item ->
                            val cartQty = cart[item] ?: 0

                            // Sleek Interface Product Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("item_card_${item.id}")
                                    .clickable { viewModel.addToCart(item) },
                                shape = RoundedCornerShape(28.dp), // curved corners
                                border = BorderStroke(
                                    if (cartQty > 0) 2.dp else 1.dp,
                                    if (cartQty > 0) MaterialTheme.colorScheme.primary else Color(0x0DFFFFFF)
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1A1A1A)
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    // Aspect square inner container
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(Color(0xFF252525)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Circular icon border frame
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    2.dp,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (item.category) {
                                                    "Tea", "Coffee" -> Icons.Default.LocalCafe
                                                    "Snacks" -> Icons.Default.Restaurant
                                                    "Cool Drinks" -> Icons.Default.LocalBar
                                                    "Desserts" -> Icons.Default.Cake
                                                    else -> Icons.Default.Fastfood
                                                },
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Details and optional Badge Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${profile?.currency ?: "₹"}${item.price.toInt()}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        if (cartQty > 0) {
                                            // Dynamic badge pill
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primary)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "x$cartQty",
                                                    color = Color.Black,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // If mobile and cart has items, show a quick float "View Cart" sheet trigger
                if (!showSplitPane && cart.isNotEmpty()) {
                    var showCartSheet by remember { mutableStateOf(false) }

                    Button(
                        onClick = { showCartSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View Bill (${cart.values.sum()} items)", fontWeight = FontWeight.Bold)
                            Text("${profile?.currency ?: "₹"}${viewModel.grandTotal.toInt()}", fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    if (showCartSheet) {
                        Dialog(
                            onDismissRequest = { showCartSheet = false },
                            properties = DialogProperties(usePlatformDefaultWidth = false)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.8f),
                                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Current Bill Detail", fontWeight = FontWeight.Bold, fontSize = 18.dp.value.sp)
                                            IconButton(onClick = { showCartSheet = false }) {
                                                Icon(Icons.Default.Close, contentDescription = "Close")
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Box(modifier = Modifier.weight(1f)) {
                                            CartListContent(viewModel, profile)
                                        }
                                        CheckoutActionPanel(viewModel, profile) {
                                            showCartSheet = false
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Right Pane (Split Cart Checkouts)
            if (showSplitPane) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Current Bill",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            CartListContent(viewModel, profile)
                        }

                        Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                        CheckoutActionPanel(viewModel, profile) { }
                    }
                }
            }
        }
    }
}

// Inner cart helper
@Composable
fun CartListContent(viewModel: BillingViewModel, profile: BusinessProfile?) {
    val cart by viewModel.cart.collectAsState()

    if (cart.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = "Empty Cart",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Bill is empty",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(cart.entries.toList()) { (item, qty) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${profile?.currency ?: "₹"}${item.price.toInt()} each",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Count Controllers
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.removeFromCart(item) },
                            modifier = Modifier
                                .size(30.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
                        }

                        Text(
                            text = qty.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.widthIn(min = 16.dp),
                            textAlign = TextAlign.Center
                        )

                        IconButton(
                            onClick = { viewModel.addToCart(item) },
                            modifier = Modifier
                                .size(30.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Item Total price
                    Text(
                        text = "${profile?.currency ?: "₹"}${(item.price * qty).toInt()}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// Inner Checkout actions panel
@Composable
fun CheckoutActionPanel(
    viewModel: BillingViewModel,
    profile: BusinessProfile?,
    onPrePrintComplete: () -> Unit
) {
    val cart by viewModel.cart.collectAsState()
    val subtotal = viewModel.subtotal
    val discount = viewModel.discount
    val total = viewModel.grandTotal
    val context = LocalContext.current
    var discountText by viewModel.discountValue

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Summary rows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Subtotal", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${profile?.currency ?: "₹"}${subtotal.toInt()}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Discount", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // Enter custom discount in place
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("- ${profile?.currency ?: "₹"}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                BasicTextField(
                    value = discountText,
                    onValueChange = { discountText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.End
                    ),
                    modifier = Modifier
                        .width(50.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Grand Total", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("${profile?.currency ?: "₹"}${total.toInt()}", fontWeight = FontWeight.Black, fontSize = 17.sp, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text("Payment Method", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        var selectedMethod by remember { mutableStateOf("Cash") }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val methods = listOf("Cash", "UPI", "Card", "Other")
            methods.forEach { m ->
                val isSelected = selectedMethod == m
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { selectedMethod = m }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = m,
                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { viewModel.clearCart() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("clear_bill_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear", fontSize = 13.sp)
            }

            Button(
                onClick = {
                    if (cart.isEmpty()) {
                        Toast.makeText(context, "Bill is empty!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (profile == null) {
                        Toast.makeText(context, "Please complete business profile setup!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val currentStaffName = com.example.ZaddyApplication.instance.authManager.staffName.value ?: "Admin"
                    viewModel.saveAndPrintBill(
                        context = context,
                        profile = profile,
                        cashierName = currentStaffName,
                        paymentMethod = selectedMethod
                    ) {
                        onPrePrintComplete()
                        Toast.makeText(context, "Bill Generated successfully!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .weight(2f)
                    .height(48.dp)
                    .testTag("print_bill_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Print, contentDescription = "Print")
                Spacer(modifier = Modifier.width(6.dp))
                Text("PRINT BILL", fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }
    }
}

// --- 4. ORDER HISTORY / REPRINTS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: OrderHistoryViewModel, profile: BusinessProfile?, userRole: String = "admin") {
    val orders by viewModel.filteredOrders.collectAsState()
    val timeFilter by viewModel.timeFilter.collectAsState()
    val searchQuery by viewModel.searchTokenQuery.collectAsState()

    var showReceiptPreviewDialog by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Order History",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Horizontal Filter Chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Today", "Weekly", "Monthly").forEach { filter ->
                    val selected = timeFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(30.dp))
                            .clickable { viewModel.setTimeFilter(filter) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Orders
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search by Token #, Item name...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (orders.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.HistoryToggleOff,
                        contentDescription = "No orders",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No transactions found for $timeFilter",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(orders) { order ->
                    val dateFormatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(order.timestamp))

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Token Number display
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "TOKEN",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = order.tokenNumber,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${profile?.currency ?: "₹"}${order.total.toInt()}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = dateFormatted,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Quick actions: Reprint / View receipt / Delete
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        if (profile != null) {
                                            viewModel.reprintOrder(order, profile)
                                            Toast.makeText(context, "Resent to printer!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = "Reprint", tint = MaterialTheme.colorScheme.primary)
                                }

                                IconButton(
                                    onClick = {
                                        // View on screen
                                        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                                        val type = Types.newParameterizedType(List::class.java, OrderItem::class.java)
                                        val adapter = moshi.adapter<List<OrderItem>>(type)
                                        val items = adapter.fromJson(order.orderItemsJson) ?: emptyList()

                                        val helper = com.example.services.PrinterManager.getInstance()
                                        val receipt = helper.generateReceiptText(
                                            businessName = profile?.name ?: "Zaddy",
                                            address = profile?.address ?: "",
                                            phone = profile?.phone ?: "",
                                            gstNumber = profile?.gstNumber,
                                            tokenNumber = order.tokenNumber,
                                            items = items,
                                            subtotal = order.subtotal,
                                            discount = order.discount,
                                            total = order.total,
                                            footerMessage = profile?.footerMessage ?: "Visit Again",
                                            currency = profile?.currency ?: "₹",
                                            invoiceNumber = order.invoiceNumber,
                                            cashierName = order.cashierName,
                                            paymentMethod = order.paymentMethod
                                        )
                                        showReceiptPreviewDialog = receipt
                                    }
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = "View Receipt", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                if (userRole != "staff") {
                                    IconButton(
                                        onClick = { viewModel.deleteOrder(order) }
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReceiptPreviewDialog != null) {
        ThermalReceiptDialog(
            receiptText = showReceiptPreviewDialog!!,
            currency = profile?.currency ?: "₹",
            onDismiss = { showReceiptPreviewDialog = null }
        )
    }
}

// --- 5. ITEMS / INVENTORY CRUD SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(viewModel: ItemsViewModel) {
    val items by viewModel.items.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<BillingItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Manage Items",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Button(
                onClick = {
                    viewModel.clearForm()
                    itemToEdit = null
                    showAddDialog = true
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("add_item_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Item", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Create Custom Category Row
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.customCategoryName.value,
                    onValueChange = { viewModel.customCategoryName.value = it },
                    placeholder = { Text("New Category name...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.addCustomCategory() },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(50.dp)
                ) {
                    Text("+ Category")
                }
            }
            val catError by viewModel.categoryError
            if (catError != null) {
                Text(catError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Items Grid
        if (items.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No items on the menu. Click Add Item to start!")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(item.category, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Text(
                                    "Price: ${item.price}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Available Toggle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Switch(
                                    checked = item.isAvailable,
                                    onCheckedChange = { viewModel.toggleAvailability(item) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )

                                IconButton(
                                    onClick = {
                                        itemToEdit = item
                                        viewModel.fillForm(item)
                                        showAddDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                }

                                IconButton(onClick = { viewModel.deleteItem(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialog for Add / Edit Item Form
    if (showAddDialog) {
        val formError by viewModel.formError

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (itemToEdit == null) "Add Menu Item" else "Edit Menu Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = viewModel.itemName.value,
                        onValueChange = { viewModel.itemName.value = it },
                        label = { Text("Item Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("item_form_name")
                    )

                    // Category Dropdown simulated
                    var dropExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = viewModel.itemCategory.value,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { IconButton(onClick = { dropExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = dropExpanded,
                            onDismissRequest = { dropExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        viewModel.itemCategory.value = cat.name
                                        dropExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = viewModel.itemPrice.value,
                        onValueChange = { viewModel.itemPrice.value = it },
                        label = { Text("Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("item_form_price")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Available on Menu")
                        Switch(
                            checked = viewModel.itemAvailable.value,
                            onCheckedChange = { viewModel.itemAvailable.value = it }
                        )
                    }

                    if (formError != null) {
                        Text(formError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveItem(itemToEdit) {
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_item_confirm_btn")
                ) {
                    Text("Save Item")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// --- 6. SALES ANALYTICS SCREEN ---
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel, profile: BusinessProfile?) {
    val metrics by viewModel.metrics.collectAsState()
    val timeFrame by viewModel.timeFrame.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sales Analytics",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Timeline select
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Today", "Weekly", "Monthly").forEach { tf ->
                    val selected = timeFrame == tf
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(30.dp))
                            .clickable { viewModel.setTimeFrame(tf) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tf,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Visual Sales Cards (KPIs)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Card 1: Revenue
            Card(
                modifier = Modifier.weight(1.2f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("REVENUE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${profile?.currency ?: "₹"}${String.format(Locale.US, "%.0f", metrics.totalSales)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Card 2: Orders
            Card(
                modifier = Modifier.weight(0.9f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("BILLS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = metrics.numBills.toString(),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Card 3: Average Order Value
            Card(
                modifier = Modifier.weight(1.1f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("AVG BILL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${profile?.currency ?: "₹"}${metrics.averageOrderValue.toInt()}",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Curved Canvas Sales Curve Graph
        Text("Revenue Growth Trend", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                if (metrics.chartPoints.isEmpty() || metrics.chartPoints.all { it.value == 0.0 }) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No sales data available to plot", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    SalesTrendChart(points = metrics.chartPoints)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Top Selling items list
        Text("Top Performing Items", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (metrics.topSellingItems.isEmpty()) {
                    Text("No item sales recorded yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    metrics.topSellingItems.forEachIndexed { idx, topItem ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}.",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                modifier = Modifier.width(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(topItem.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${topItem.quantity} units sold", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                "${profile?.currency ?: "₹"}${topItem.totalRevenue.toInt()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                        if (idx < metrics.topSellingItems.size - 1) {
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

// Custom curving canvas graph
@Composable
fun SalesTrendChart(points: List<ChartPoint>) {
    val maxVal = (points.maxOfOrNull { it.value } ?: 100.0).coerceAtLeast(100.0)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val paddingLeft = 40.dp.toPx()
        val paddingBottom = 20.dp.toPx()
        val chartWidth = width - paddingLeft
        val chartHeight = height - paddingBottom

        val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)
        val path = Path()
        val fillPath = Path()

        val coordinates = points.mapIndexed { idx, pt ->
            val ratio = pt.value / maxVal
            val x = paddingLeft + idx * stepX
            val y = chartHeight - (ratio * chartHeight).toFloat()
            Offset(x, y)
        }

        // Draw Line & Fill curve
        if (coordinates.isNotEmpty()) {
            path.moveTo(coordinates[0].x, coordinates[0].y)
            fillPath.moveTo(coordinates[0].x, chartHeight)
            fillPath.lineTo(coordinates[0].x, coordinates[0].y)

            for (i in 1 until coordinates.size) {
                // Bezier curving for smooth aesthetics
                val prev = coordinates[i - 1]
                val curr = coordinates[i]
                val cp1X = prev.x + (curr.x - prev.x) / 2
                val cp1Y = prev.y
                val cp2X = prev.x + (curr.x - prev.x) / 2
                val cp2Y = curr.y

                path.cubicTo(cp1X, cp1Y, cp2X, cp2Y, curr.x, curr.y)
                fillPath.cubicTo(cp1X, cp1Y, cp2X, cp2Y, curr.x, curr.y)
            }

            fillPath.lineTo(coordinates.last().x, chartHeight)
            fillPath.close()

            // Draw orange transparent gradient under curve
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFF6B00).copy(alpha = 0.35f), Color.Transparent),
                    startY = 0f,
                    endY = chartHeight
                )
            )

            // Draw high contrast line
            drawPath(
                path = path,
                color = Color(0xFFFF6B00),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw glowing node circles on top of lines
            coordinates.forEach { node ->
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = node)
                drawCircle(color = Color(0xFFFF6B00), radius = 3.dp.toPx(), center = node)
            }
        }
    }
}

// --- 7. SETTINGS & PROFILE SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    profileViewModel: BusinessSetupViewModel,
    navController: NavController,
    userRole: String = "admin",
    staffViewModel: StaffViewModel
) {
    val profile = viewModel.profile.collectAsState(initial = null).value
    val printerConfig = viewModel.printerConfig.collectAsState(initial = null).value
    val scannedDevices = viewModel.scannedDevices.collectAsState(initial = emptyList()).value
    val isScanning = viewModel.isScanning.collectAsState(initial = false).value
    val connectedDevice = viewModel.connectedDevice.collectAsState(initial = null).value

    val context = LocalContext.current

    // Initialize the forms if profile is loaded
    LaunchedEffect(profile) {
        profile?.let { viewModel.initProfileForm(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings & Configuration",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // 1. Store Config Update
        if (userRole != "staff") {
            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Edit Store Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = viewModel.profileName.value,
                        onValueChange = { viewModel.profileName.value = it },
                        label = { Text("Store Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("settings_name")
                    )

                    OutlinedTextField(
                        value = viewModel.profileAddress.value,
                        onValueChange = { viewModel.profileAddress.value = it },
                        label = { Text("Address") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("settings_address")
                    )

                    OutlinedTextField(
                        value = viewModel.profilePhone.value,
                        onValueChange = { viewModel.profilePhone.value = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("settings_phone")
                    )

                    OutlinedTextField(
                        value = viewModel.profileGst.value,
                        onValueChange = { viewModel.profileGst.value = it },
                        label = { Text("GST (Optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("settings_gst")
                    )

                    OutlinedTextField(
                        value = viewModel.profileFooter.value,
                        onValueChange = { viewModel.profileFooter.value = it },
                        label = { Text("Receipt Footer Message") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.saveProfileSettings()
                            Toast.makeText(context, "Store Profile Saved!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.align(Alignment.End).testTag("save_settings_btn")
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }

        // 2. Printer settings
        Card(
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Bluetooth Thermal Printer Settings", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (connectedDevice != null) "Connected: ${connectedDevice!!.name}" else "Status: Disconnected",
                            fontWeight = FontWeight.Bold,
                            color = if (connectedDevice != null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                        )
                        if (connectedDevice != null) {
                            Text(connectedDevice!!.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (connectedDevice != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { profile?.let { viewModel.testPrintReceipt(it) } },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Test Print", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.disconnectPrinter() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Disconnect", fontSize = 12.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.scanPrinters() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("scan_printers_btn")
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                            } else {
                                Text("Search Printer", fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (connectedDevice == null && scannedDevices.isNotEmpty()) {
                    Text("Available Printers found nearby:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    scannedDevices.forEach { dev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                .clickable { viewModel.connectPrinter(dev) }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Print, contentDescription = "Printer", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(dev.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(dev.address, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text("Connect ➜", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Quick Maintenance Tools & Actions
        if (userRole != "staff") {
            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("System Operations", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Reset Daily Token Number", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Manually reset token generation back to #001.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                viewModel.resetDailyToken(context)
                                Toast.makeText(context, "Token Number Reset!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Reset Token", fontSize = 12.sp)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Cloud Backup & Sync", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Push transaction logs & menus offline cache to cloud.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { viewModel.runBackup() },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            val active by viewModel.isBackupCompleted
                            if (active) Text("Synced ✓") else Text("Backup Now")
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Cloud Recovery", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Fetch cloud database copy into offline database.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { viewModel.runRestore() },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            val active by viewModel.isRestoreCompleted
                            if (active) Text("Restored ✓") else Text("Restore Now")
                        }
                    }
                }
            }
        }

        // Staff Accounts Management
        if (userRole != "staff") {
            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Staff Accounts Management", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    
                    val staffList by staffViewModel.allStaff.collectAsState()
                    val editingStaff by staffViewModel.editingStaff
                    
                    OutlinedTextField(
                        value = staffViewModel.staffName.value,
                        onValueChange = { staffViewModel.staffName.value = it },
                        label = { Text("Staff Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = staffViewModel.username.value,
                        onValueChange = { staffViewModel.username.value = it },
                        label = { Text("Username or Mobile") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = staffViewModel.password.value,
                        onValueChange = { staffViewModel.password.value = it },
                        label = { Text("Password") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Disable Staff Account", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = staffViewModel.isDisabled.value,
                            onCheckedChange = { staffViewModel.isDisabled.value = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (editingStaff != null) {
                            TextButton(onClick = { staffViewModel.clearFields() }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Button(
                            onClick = {
                                staffViewModel.saveStaff {
                                    Toast.makeText(context, "Staff account saved!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (editingStaff != null) "Update Staff" else "Create Staff")
                        }
                    }

                    if (staffList.isNotEmpty()) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Text("Existing Staff Accounts:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        
                        staffList.forEach { staff ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(staff.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("User: ${staff.username} | Pass: ${staff.password}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (staff.isDisabled) {
                                        Text("DISABLED", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }

                                Row {
                                    IconButton(onClick = { staffViewModel.startEditing(staff) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { staffViewModel.deleteStaff(staff) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Logout / Session end
        Button(
            onClick = {
                viewModel.logout()
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
                Toast.makeText(context, "Logged out of Zaddy!", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("logout_btn")
        ) {
            Icon(Icons.Default.Logout, contentDescription = "Logout")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Logout Business Session", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

// --- VIRTUAL ESC/POS 58MM PAPER RECEIPT PREVIEW DIALOG ---
@Composable
fun ThermalReceiptDialog(
    receiptText: String,
    currency: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // Receipt Content Frame
            Card(
                modifier = Modifier
                    .width(320.dp)
                    .wrapContentHeight()
                    .padding(16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Consuming click to prevent dismissal */ },
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFA)), // Cream off-white paper color
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Serrated paper tearing header
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                    ) {
                        val triangleWidth = 10.dp.toPx()
                        val triangleHeight = 6.dp.toPx()
                        val numTriangles = (size.width / triangleWidth).toInt() + 1
                        val path = Path()
                        path.moveTo(0f, 0f)
                        for (i in 0..numTriangles) {
                            val x1 = i * triangleWidth
                            val y1 = triangleHeight
                            val x2 = (i + 0.5f) * triangleWidth
                            val y2 = 0f
                            val x3 = (i + 1f) * triangleWidth
                            val y3 = triangleHeight
                            path.lineTo(x1, y1)
                            path.lineTo(x2, y2)
                            path.lineTo(x3, y3)
                        }
                        path.lineTo(size.width, size.height)
                        path.lineTo(0f, size.height)
                        path.close()
                        drawPath(path = path, color = Color(0xFFE5E5DF))
                    }

                    // Content details
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Header metadata info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "58mm Thermal Receipt",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )

                            Icon(
                                Icons.Default.Print,
                                contentDescription = "POS Printer Status",
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Parse receipt text formatted with helper codes
                        val cleanReceiptLines = receiptText.split("\n")
                        cleanReceiptLines.forEach { line ->
                            val isBold = line.contains("<b>")
                            val isCenter = line.contains("[C]")
                            val isRight = line.contains("[R]")

                            val stripCodes = line
                                .replace("<b>", "")
                                .replace("</b>", "")
                                .replace("[C]", "")
                                .replace("[L]", "")
                                .replace("[R]", "")

                            if (stripCodes.isNotBlank() || line.isEmpty()) {
                                Text(
                                    text = stripCodes,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = if (isBold) FontWeight.Black else FontWeight.Normal,
                                    color = Color(0xFF111111), // Jet black ink
                                    textAlign = when {
                                        isCenter -> TextAlign.Center
                                        isRight -> TextAlign.End
                                        else -> TextAlign.Start
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF111111),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // Bottom serrated tear paper cut
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                    ) {
                        val triangleWidth = 10.dp.toPx()
                        val triangleHeight = 6.dp.toPx()
                        val numTriangles = (size.width / triangleWidth).toInt() + 1
                        val path = Path()
                        path.moveTo(0f, size.height)
                        for (i in 0..numTriangles) {
                            val x1 = i * triangleWidth
                            val y1 = size.height - triangleHeight
                            val x2 = (i + 0.5f) * triangleWidth
                            val y2 = size.height
                            val x3 = (i + 1f) * triangleWidth
                            val y3 = size.height - triangleHeight
                            path.lineTo(x1, y1)
                            path.lineTo(x2, y2)
                            path.lineTo(x3, y3)
                        }
                        path.lineTo(size.width, 0f)
                        path.lineTo(0f, 0f)
                        path.close()
                        drawPath(path = path, color = Color(0xFFE5E5DF))
                    }
                }
            }
        }
    }
}
