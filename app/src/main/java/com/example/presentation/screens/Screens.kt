package com.example.presentation.screens

import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import coil.compose.AsyncImage
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.R
import com.example.OptixApplication
import com.example.data.entity.*
import com.example.presentation.viewmodel.*
import com.example.services.PrinterDevice
import com.example.services.PrinterManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*

// --- THEME COLORS ---
val OrangePrimary = Color(0xFFFF6B00)
val DarkBackground = Color(0xFF0A0A0A)
val SurfaceDark = Color(0xFF1C1C1E)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick ?: {},
                        onLongClick = onLongClick
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(OrangePrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            if (subtitle != null) {
                Text(subtitle, color = Color.Gray, fontSize = 13.sp)
            }
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}

// --- MAIN SHELL SCREEN ---
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
    val currentProfile by profileViewModel.profile.collectAsState(initial = null)
    val userRole by authViewModel.userRole.collectAsState()
    val isStaff = userRole == "staff"
    
    LaunchedEffect(Unit) {
        billingViewModel.updateCurrentTokenState(context)
    }

    val currentTab by settingsViewModel.currentTab

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = maxWidth
        val isWideScreen = width >= 600.dp

        Scaffold(
            containerColor = DarkBackground,
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(containerColor = SurfaceDark) {
                        val userPermissions by OptixApplication.instance.authManager.userPermissions.collectAsState()
                        val navItems = remember(userPermissions, isStaff) {
                            val list = mutableListOf<Triple<String, String, ImageVector>>()
                            list.add(Triple("projexa", "Projexa", Icons.Default.Dashboard))
                            if (com.example.services.PermissionManager.can(com.example.services.PermissionManager.CREATE_BILLS)) {
                                list.add(Triple("billing", "Billing", Icons.Default.ReceiptLong))
                            }
                            if (com.example.services.PermissionManager.canAny(com.example.services.PermissionManager.VIEW_REPORTS, com.example.services.PermissionManager.CANCEL_BILLS)) {
                                list.add(Triple("history", "History", Icons.Default.History))
                            }
                            if (com.example.services.PermissionManager.can(com.example.services.PermissionManager.VIEW_PRODUCTS)) {
                                list.add(Triple("items", "Menu", Icons.Default.RestaurantMenu))
                            }
                            if (com.example.services.PermissionManager.can(com.example.services.PermissionManager.VIEW_REPORTS) && 
                                com.example.services.FeatureGate.canUseAdvancedReports()) {
                                list.add(Triple("analytics", "Stats", Icons.Default.BarChart))
                            }
                            list.add(Triple("settings", "Settings", Icons.Default.Settings))
                            list
                        }
                        navItems.forEach { (route, label, icon) ->
                            NavigationBarItem(
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 10.sp) },
                                selected = currentTab == route,
                                onClick = { settingsViewModel.currentTab.value = route },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = OrangePrimary,
                                    unselectedIconColor = Color.Gray,
                                    indicatorColor = OrangePrimary.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (isWideScreen) {
                    NavigationRail(containerColor = SurfaceDark) {
                        val userPermissions by OptixApplication.instance.authManager.userPermissions.collectAsState()
                        val navItems = remember(userPermissions, isStaff) {
                            val list = mutableListOf<Triple<String, String, ImageVector>>()
                            list.add(Triple("projexa", "Projexa", Icons.Default.Dashboard))
                            if (com.example.services.PermissionManager.can(com.example.services.PermissionManager.CREATE_BILLS)) {
                                list.add(Triple("billing", "Billing", Icons.Default.ReceiptLong))
                            }
                            if (com.example.services.PermissionManager.canAny(com.example.services.PermissionManager.VIEW_REPORTS, com.example.services.PermissionManager.CANCEL_BILLS)) {
                                list.add(Triple("history", "History", Icons.Default.History))
                            }
                            if (com.example.services.PermissionManager.can(com.example.services.PermissionManager.VIEW_PRODUCTS)) {
                                list.add(Triple("items", "Menu", Icons.Default.RestaurantMenu))
                            }
                            if (com.example.services.PermissionManager.can(com.example.services.PermissionManager.VIEW_REPORTS) && 
                                com.example.services.FeatureGate.canUseAdvancedReports()) {
                                list.add(Triple("analytics", "Stats", Icons.Default.BarChart))
                            }
                            list.add(Triple("settings", "Settings", Icons.Default.Settings))
                            list
                        }
                        navItems.forEach { (route, label, icon) ->
                            NavigationRailItem(
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 11.sp) },
                                selected = currentTab == route,
                                onClick = { settingsViewModel.currentTab.value = route },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = OrangePrimary,
                                    unselectedIconColor = Color.Gray,
                                    indicatorColor = OrangePrimary.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentTab) {
                        "projexa" -> ProjexaCommandCenterScreen()
                        "billing" -> BillingScreen(billingViewModel, currentProfile, userRole, onUpgrade = { navController.navigate("subscription") })
                        "history" -> HistoryScreen(historyViewModel, currentProfile, userRole)
                        "items" -> ItemsScreen(itemsViewModel, navController)
                        "analytics" -> AnalyticsScreen(analyticsViewModel, currentProfile)
                        "settings" -> SettingsScreen(settingsViewModel, profileViewModel, navController, userRole, staffViewModel)
                    }
                }
            }
        }
    }

    val showReceipt by billingViewModel.showReceiptPreview
    val receiptText by billingViewModel.lastPrintedReceipt
    val isPreparing by billingViewModel.isPreparingOrder

    if (isPreparing) {
        Dialog(onDismissRequest = {}) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
                Column(modifier = Modifier.size(200.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = OrangePrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("PRINTING...", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    val currency = currentProfile?.currency ?: "₹"
    if (showReceipt && receiptText != null) {
        ThermalReceiptDialog(
            receiptText = receiptText!!,
            currency = currency,
            onDismiss = { billingViewModel.showReceiptPreview.value = false }
        )
    }
}

// --- LOGIN SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val authError by viewModel.authError
    val isVerifying by viewModel.isVerifying
    val userRole by viewModel.userRole.collectAsState()
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            val email = account?.email ?: "admin@optixapp.in"
            val name = account?.displayName ?: "Google Admin"
            val googleId = account?.id ?: ("google_" + System.currentTimeMillis())

            Toast.makeText(context, "Signing in as $name ($email)...", Toast.LENGTH_SHORT).show()

            OptixApplication.instance.authManager.signInWithGoogle(
                email = email,
                name = name,
                googleId = googleId
            ) { success, err ->
                if (success) {
                    val isSetup = OptixApplication.instance.authManager.isSetupCompleted()
                    val dest = if (isSetup) "main" else "business_setup"
                    navController.navigate(dest) { popUpTo("login") { inclusive = true } }
                } else {
                    Toast.makeText(context, err ?: "Google sign-in failed", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            val fallbackEmail = viewModel.email.value.ifEmpty { "admin@optixapp.in" }
            OptixApplication.instance.authManager.signInWithGoogle(
                email = fallbackEmail,
                name = "Google Admin",
                googleId = "google_" + System.currentTimeMillis()
            ) { success, err ->
                if (success) {
                    val isSetup = OptixApplication.instance.authManager.isSetupCompleted()
                    val dest = if (isSetup) "main" else "business_setup"
                    navController.navigate(dest) { popUpTo("login") { inclusive = true } }
                } else {
                    Toast.makeText(context, err ?: "Google sign-in failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            navController.navigate("main") { popUpTo("login") { inclusive = true } }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_zaddy_logo),
                contentDescription = "Logo",
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(24.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("OPTIX BILLING", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(modifier = Modifier.height(32.dp))

            val isStaffMode by viewModel.isStaffMode
            Row(modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceDark).padding(4.dp)) {
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (!isStaffMode) OrangePrimary else Color.Transparent).clickable { if (isStaffMode) viewModel.toggleMode() }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text("ADMIN", color = if (!isStaffMode) Color.Black else Color.Gray, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (isStaffMode) OrangePrimary else Color.Transparent).clickable { if (!isStaffMode) viewModel.toggleMode() }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text("STAFF", color = if (isStaffMode) Color.Black else Color.Gray, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isStaffMode) {
                LoginCard("Staff Login", listOf(
                    LoginField("Username", viewModel.staffUsername, Icons.Default.Person),
                    LoginField("Password", viewModel.staffPassword, Icons.Default.Lock, true)
                ), authError, isVerifying, { viewModel.loginStaff { } }, "Login as Staff")
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val fields = if (viewModel.isSignUpMode.value) {
                        listOf(
                            LoginField("Business Name", viewModel.businessName, Icons.Default.Storefront),
                            LoginField("Phone Number", viewModel.phone, Icons.Default.Phone),
                            LoginField("Address", viewModel.address, Icons.Default.LocationOn),
                            LoginField("Email", viewModel.email, Icons.Default.Email),
                            LoginField("Password", viewModel.password, Icons.Default.Lock, true),
                            LoginField("Confirm Password", viewModel.confirmPassword, Icons.Default.Lock, true)
                        )
                    } else {
                        listOf(
                            LoginField("Email", viewModel.email, Icons.Default.Email),
                            LoginField("Password", viewModel.password, Icons.Default.Lock, true)
                        )
                    }

                    LoginCard(if (viewModel.isSignUpMode.value) "Register Business" else "Admin Login", fields, authError, isVerifying, { 
                        viewModel.authenticate { 
                            navController.navigate("main") { popUpTo("login") { inclusive = true } }
                        } 
                    }, if (viewModel.isSignUpMode.value) "Create Account & Start Billing" else "Login")
                    
                    Button(
                        onClick = {
                            try {
                                val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                    com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                                )
                                    .requestEmail()
                                    .requestProfile()
                                    .build()
                                val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                                client.signOut().addOnCompleteListener {
                                    googleSignInLauncher.launch(client.signInIntent)
                                }
                            } catch (e: Exception) {
                                val userEmail = viewModel.email.value.ifEmpty { "admin@optixapp.in" }
                                OptixApplication.instance.authManager.signInWithGoogle(
                                    email = userEmail,
                                    name = "Google Admin",
                                    googleId = "google_" + System.currentTimeMillis()
                                ) { success, err ->
                                    if (success) {
                                        navController.navigate("main") { popUpTo("login") { inclusive = true } }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("Continue with Google 🌐", fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = { viewModel.toggleSignUpMode() }) {
                        Text(if (viewModel.isSignUpMode.value) "Have an account? Login" else "New Admin? Register Business", color = OrangePrimary)
                    }
                }
            }
        }
    }
}

data class LoginField(val label: String, val state: MutableState<String>, val icon: androidx.compose.ui.graphics.vector.ImageVector, val isPassword: Boolean = false)

@Composable
fun LoginCard(title: String, fields: List<LoginField>, error: String?, isLoading: Boolean, onAction: () -> Unit, actionLabel: String) {
    Card(modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(20.dp))
            fields.forEach { field ->
                OutlinedTextField(
                    value = field.state.value,
                    onValueChange = { field.state.value = it },
                    label = { Text(field.label) },
                    leadingIcon = { Icon(field.icon, null, tint = OrangePrimary) },
                    visualTransformation = if (field.isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (error != null) Text(error, color = Color.Red, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onAction, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)) {
                if (isLoading) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                else Text(actionLabel, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

// --- BUSINESS SETUP SCREEN ---
@Composable
fun BusinessSetupScreen(navController: NavController, viewModel: BusinessSetupViewModel) {
    val profile by viewModel.profile.collectAsState()
    val setupError by viewModel.setupError

    LaunchedEffect(profile) {
        if (profile?.setupCompleted == true) {
            navController.navigate("main") { popUpTo("business_setup") { inclusive = true } }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Welcome to Optix", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(modifier = Modifier.height(32.dp))
            Card(modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(value = viewModel.businessName.value, onValueChange = { viewModel.businessName.value = it }, label = { Text("Business Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = viewModel.address.value, onValueChange = { viewModel.address.value = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = viewModel.phone.value, onValueChange = { viewModel.phone.value = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                    
                    var countryExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = viewModel.selectedCountry.value,
                            onValueChange = { },
                            label = { Text("Country") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { IconButton(onClick = { countryExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                        )
                        DropdownMenu(expanded = countryExpanded, onDismissRequest = { countryExpanded = false }) {
                            com.example.services.PricingEngine.getCountryList().forEach { country ->
                                DropdownMenuItem(text = { Text(country) }, onClick = {
                                    viewModel.selectedCountry.value = country
                                    countryExpanded = false
                                })
                            }
                        }
                    }

                    OutlinedTextField(
                        value = "Currency: ${viewModel.selectedCurrency}",
                        onValueChange = { },
                        label = { Text("Automatic Currency") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    )

                    if (setupError != null) Text(setupError!!, color = Color.Red)
                    Button(
                        onClick = {
                            viewModel.saveBusinessProfile {
                                navController.navigate("main") { popUpTo("business_setup") { inclusive = true } }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Text("Launch POS 🚀", fontWeight = FontWeight.Bold, color = Color.Black)
                    }

                    TextButton(
                        onClick = {
                            viewModel.saveBusinessProfile {
                                navController.navigate("main") { popUpTo("business_setup") { inclusive = true } }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Explore First (14 Days Trial)", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// --- BILLING SCREEN ---
@Composable
fun BillingScreen(viewModel: BillingViewModel, profile: BusinessProfile?, userRole: String, onUpgrade: () -> Unit) {
    val items by viewModel.items.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val cart by viewModel.cartItems.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLimitReached by viewModel.isLimitReached.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
                Column(modifier = Modifier.weight(0.6f).fillMaxHeight().padding(8.dp)) {
                    BillingMenuSection(viewModel, profile, items, categories, selectedCategory, searchQuery)
                }
                Column(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
                    BillingCartSection(viewModel, profile)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
                Column(modifier = Modifier.weight(0.55f).padding(8.dp)) {
                    BillingMenuSection(viewModel, profile, items, categories, selectedCategory, searchQuery)
                }
                Box(modifier = Modifier.weight(0.45f).fillMaxWidth()) {
                    BillingCartSection(viewModel, profile)
                }
            }
        }

        if (isLimitReached) {
            TrialLimitDialog(onUpgrade = onUpgrade)
        }

        // Weight Based Dialog
        val weightItem = viewModel.weightItemToEdit.value
        if (weightItem != null) {
            AlertDialog(
                onDismissRequest = { viewModel.weightItemToEdit.value = null },
                containerColor = SurfaceDark,
                title = { Text(weightItem.name, color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Price: ${profile?.currency}${weightItem.price}/${weightItem.unit}", color = OrangePrimary, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = viewModel.currentWeight.value,
                            onValueChange = { viewModel.onWeightChanged(it) },
                            label = { Text("Weight (${weightItem.unit})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = viewModel.currentAmount.value,
                            onValueChange = { viewModel.onAmountChanged(it) },
                            label = { Text("Amount (${profile?.currency})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.confirmWeightItem() }, colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black)) {
                        Text("ADD TO BILL")
                    }
                }
            )
        }
    }
}

@Composable
fun BillingMenuSection(viewModel: BillingViewModel, profile: BusinessProfile?, items: List<BillingItem>, categories: List<Category>, selectedCategory: String, searchQuery: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(profile?.name ?: "STORE", fontWeight = FontWeight.Bold, color = OrangePrimary)
            Text("TOKEN #" + viewModel.currentTokenNum.value, fontSize = 12.sp, color = Color.Gray)
        }
        OutlinedTextField(value = searchQuery, onValueChange = { viewModel.setSearchQuery(it) }, placeholder = { Text("Search...") }, modifier = Modifier.width(150.dp).height(50.dp), shape = RoundedCornerShape(25.dp))
    }
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(selected = selectedCategory == "All", onClick = { viewModel.setCategory("All") }, label = { Text("All") })
        }
        items(categories) { cat ->
            FilterChip(selected = selectedCategory == cat.name, onClick = { viewModel.setCategory(cat.name) }, label = { Text(cat.name) })
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    val filtered = items.filter { !it.isOutOfStock && it.isAvailable && (selectedCategory == "All" || it.categoryId == categories.find { it.name == selectedCategory }?.id) && it.name.contains(searchQuery, true) }
    LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filtered) { item ->
            ItemCard(item, viewModel, profile)
        }
    }
}

@Composable
fun ItemCard(item: BillingItem, viewModel: BillingViewModel, profile: BusinessProfile?) {
    val cart by viewModel.cartItems.collectAsState()
    val qty = if (item.pricingType == "FIXED") cart.find { it.itemId == item.id }?.quantity ?: 0 else 0
    val weightInCart = if (item.pricingType == "WEIGHT") cart.filter { it.itemId == item.id }.sumOf { it.weight ?: 0.0 } else 0.0

    Card(modifier = Modifier.fillMaxWidth().height(100.dp).clickable { viewModel.addToCart(item) }, colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(item.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
            Text((profile?.currency ?: "₹") + item.price.toInt().toString() + (if (item.pricingType == "WEIGHT") "/${item.unit}" else ""), color = OrangePrimary)
            if (qty > 0) Text(qty.toString() + " in cart", fontSize = 10.sp, color = Color.Green)
            if (weightInCart > 0) Text(String.format("%.3f %s", weightInCart, item.unit), fontSize = 10.sp, color = Color.Green)
        }
    }
}

@Composable
fun BillingCartSection(viewModel: BillingViewModel, profile: BusinessProfile?) {
    val context = LocalContext.current
    val cart by viewModel.cartItems.collectAsState()
    val total = viewModel.grandTotal

    Card(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val userPermissions by com.example.OptixApplication.instance.authManager.userPermissions.collectAsState()
            val canCreateBills = remember(userPermissions) { com.example.services.PermissionManager.can(com.example.services.PermissionManager.CREATE_BILLS) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShoppingCart, null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("CURRENT BILL", fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
                Spacer(modifier = Modifier.weight(1f))
                if (cart.isNotEmpty()) {
                    Text("${cart.size} ITEMS", color = OrangePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (cart.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxHeight(0.5f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Empty Bill", color = Color.DarkGray, fontSize = 12.sp)
                            }
                        }
                    } else {
                        items(cart) { item ->
                            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.05f)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.itemName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, maxLines = 1)
                                    val currency = profile?.currency ?: "₹"
                                    if (item.pricingType == "WEIGHT") {
                                        Text("${String.format("%.3f", item.weight)} ${item.unit} x $currency${item.price.toInt()}", color = OrangePrimary, fontSize = 11.sp)
                                    } else {
                                        Text(currency + item.price.toInt().toString(), color = OrangePrimary, fontSize = 11.sp)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { viewModel.removeFromCart(item) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                                    if (item.pricingType == "FIXED") {
                                        Text(item.quantity.toString(), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                        IconButton(
                                            onClick = { 
                                                viewModel.addToCart(BillingItem(id = item.itemId, name = item.itemName, price = item.price))
                                            }, 
                                            modifier = Modifier.size(30.dp)
                                        ) { 
                                            Icon(Icons.Default.Add, null, tint = OrangePrimary, modifier = Modifier.size(16.dp)) 
                                        }
                                    }
                                }
                                val lineTotal = if (item.pricingType == "WEIGHT") item.price * (item.weight ?: 0.0) else item.price * item.quantity
                                Text((profile?.currency ?: "₹") + String.format("%.2f", lineTotal), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 45.dp), textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }

            Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("GRAND TOTAL", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text((profile?.currency ?: "₹") + String.format("%.2f", total), color = OrangePrimary, fontWeight = FontWeight.Black, fontSize = 24.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.clearCart() },
                    modifier = Modifier.weight(0.8f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                ) {
                    Text("CLEAR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = { 
                        val cashier = OptixApplication.instance.authManager.staffName.value ?: "Admin"
                        val p = profile ?: BusinessProfile(name = "Optix Store", address = "", phone = "")
                        viewModel.saveBillOnly(context, p, cashier) { 
                            Toast.makeText(context, "BILL SAVED", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(0.8f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White),
                    enabled = cart.isNotEmpty() && canCreateBills
                ) {
                    Text(if (canCreateBills) "SAVE" else "LOCKED", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                
                Button(
                    onClick = { 
                        val cashier = OptixApplication.instance.authManager.staffName.value ?: "Admin"
                        val p = profile ?: BusinessProfile(name = "Optix Store", address = "", phone = "")
                        viewModel.saveAndPrintBill(context, p, cashier) { 
                            Toast.makeText(context, "BILL PRINTED", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1.4f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (canCreateBills) OrangePrimary else Color.Gray, contentColor = Color.Black),
                    enabled = cart.isNotEmpty() && canCreateBills
                ) {
                    Icon(Icons.Default.Print, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (canCreateBills) "PRINT & SAVE" else "LOCKED", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun TrialLimitDialog(onUpgrade: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        containerColor = SurfaceDark,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Lock, null, tint = OrangePrimary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Trial Limit Reached", color = Color.White, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            }
        },
        text = {
            val sub by com.example.services.FeatureGate.subscription.collectAsState()
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "You have reached the maximum allowed usage for the Trial plan.",
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                UsageProgress("Bills Created", sub?.billsUsed ?: 0, 50)
                Spacer(modifier = Modifier.height(12.dp))
                UsageProgress("Products Added", sub?.productsUsed ?: 0, 5)
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("Upgrade to Starter or Growth to continue using Optix.", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            Button(
                onClick = onUpgrade,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("VIEW PLANS", fontWeight = FontWeight.Black)
            }
        }
    )
}

// --- HISTORY SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: OrderHistoryViewModel, profile: BusinessProfile?, userRole: String) {
    val orders by viewModel.filteredOrders.collectAsState()
    val filter by viewModel.timeFilter.collectAsState()
    val sort by viewModel.sortBy.collectAsState()
    val searchQuery by viewModel.searchTokenQuery.collectAsState()
    val previewText by viewModel.viewReceiptText.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val context = LocalContext.current

    val currency = profile?.currency ?: "₹"
    if (previewText != null) {
        ThermalReceiptDialog(
            receiptText = previewText!!,
            currency = currency,
            onDismiss = { viewModel.hideReceiptPreview() }
        )
    }
    
    val app = OptixApplication.instance

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            viewModel.refresh(context)
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("ORDER HISTORY", fontWeight = FontWeight.Black, color = Color.White, fontSize = 24.sp)
            
            Spacer(modifier = Modifier.height(16.dp))

            // Search and Sort Row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search ID, Name, Items...") },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) }
                )
                
                var showSortMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showSortMenu = true }, modifier = Modifier.background(SurfaceDark, RoundedCornerShape(12.dp))) {
                        Icon(Icons.Default.Sort, null, tint = OrangePrimary)
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }, containerColor = SurfaceDark) {
                        listOf("Newest First", "Oldest First", "Highest Amount", "Lowest Amount", "Bill Number").forEach { s ->
                            DropdownMenuItem(text = { Text(s, color = if (sort == s) OrangePrimary else Color.White) }, onClick = { viewModel.setSortBy(s); showSortMenu = false })
                        }
                    }
                }
            }

            Row(modifier = Modifier.padding(vertical = 12.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Today", "Yesterday", "Weekly", "Monthly", "All")) { tf ->
                        FilterChip(
                            selected = filter == tf,
                            onClick = { viewModel.setTimeFilter(tf) },
                            label = { Text(tf) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OrangePrimary, selectedLabelColor = Color.Black)
                        )
                    }
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                items(orders) { order ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Token #" + order.tokenNumber, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                Text("${SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(order.timestamp))} • ${order.paymentMethod}", fontSize = 12.sp, color = Color.Gray)
                                if (order.customerName != null) {
                                    Text("Customer: ${order.customerName}", fontSize = 12.sp, color = OrangePrimary)
                                }
                            }
                            Text((profile?.currency ?: "₹") + order.total.toInt().toString(), color = OrangePrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(onClick = { viewModel.showReceiptPreview(order, profile ?: return@IconButton) }) {
                                Icon(Icons.Default.Visibility, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { viewModel.reprintOrder(order, profile ?: return@IconButton) }) {
                                Icon(Icons.Default.Print, null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- ITEMS SCREEN ---
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(viewModel: ItemsViewModel, navController: NavController) {
    val items by viewModel.items.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchItemQuery
    val selectedFilter by viewModel.selectedCategoryFilter
    val selectedIds by viewModel.selectedItemsForBulk
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLimitReached by viewModel.isLimitReached.collectAsState()
    val context = LocalContext.current
    
    var showLongPressMenu by remember { mutableStateOf<BillingItem?>(null) }
    var fabExpanded by remember { mutableStateOf(false) }
    var isBulkMode by remember { mutableStateOf(false) }

    val app = OptixApplication.instance
    val profile by app.businessProfileRepository.profile.collectAsState(initial = null)
    val userPermissions by app.authManager.userPermissions.collectAsState()
    val canAddProduct = remember(userPermissions) { com.example.services.PermissionManager.can(com.example.services.PermissionManager.ADD_PRODUCTS) }
    val canEditProduct = remember(userPermissions) { com.example.services.PermissionManager.can(com.example.services.PermissionManager.EDIT_PRODUCTS) }
    val canManageCategory = remember(userPermissions) { com.example.services.PermissionManager.canAny(com.example.services.PermissionManager.ADD_CATEGORIES, com.example.services.PermissionManager.VIEW_CATEGORIES) }

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "MENU",
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontSize = 34.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                softWrap = false
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isBulkMode) {
                    IconButton(onClick = { isBulkMode = false; viewModel.selectedItemsForBulk.value = emptySet() }) {
                        Icon(Icons.Default.Close, null, tint = Color.Red)
                    }
                } else if (canEditProduct) {
                    IconButton(
                        onClick = { isBulkMode = true },
                        modifier = Modifier.background(SurfaceDark, CircleShape)
                    ) {
                        Icon(Icons.Default.EditNote, null, tint = OrangePrimary)
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchItemQuery.value = it },
            placeholder = { Text("Search menu items...", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = OrangePrimary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == "All",
                    onClick = { viewModel.selectedCategoryFilter.value = "All" },
                    label = { Text("All", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OrangePrimary,
                        selectedLabelColor = Color.Black,
                        containerColor = SurfaceDark,
                        labelColor = Color.White
                    ),
                    border = null,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            items(categories) { cat ->
                FilterChip(
                    selected = selectedFilter == cat.name,
                    onClick = { viewModel.selectedCategoryFilter.value = cat.name },
                    label = { Text(cat.name, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OrangePrimary,
                        selectedLabelColor = Color.Black,
                        containerColor = SurfaceDark,
                        labelColor = Color.White
                    ),
                    border = null,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Content Area with PullToRefresh
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh(context) },
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val filteredItems = items.filter {
                    (selectedFilter == "All" || it.categoryId == categories.find { c -> c.name == selectedFilter }?.id) &&
                            it.name.contains(searchQuery, ignoreCase = true)
                }

                if (filteredItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No items found", color = Color.Gray)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredItems) { item ->
                            val isSelected = selectedIds.contains(item.id)
                            PremiumCard(
                                onClick = {
                                    if (isBulkMode) {
                                        val current = selectedIds.toMutableSet()
                                        if (isSelected) current.remove(item.id) else current.add(item.id)
                                        viewModel.selectedItemsForBulk.value = current
                                    } else {
                                        viewModel.fillForm(item)
                                        navController.navigate("add_edit_item")
                                    }
                                },
                                onLongClick = { if (!isBulkMode) showLongPressMenu = item },
                                modifier = Modifier
                                    .alpha(if (item.isOutOfStock) 0.5f else 1.0f)
                                    .border(
                                        2.dp,
                                        if (isSelected) OrangePrimary else Color.Transparent,
                                        RoundedCornerShape(22.dp)
                                    )
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth().padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(75.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color.White.copy(alpha = 0.07f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            item.name.take(1).uppercase(),
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Black,
                                            color = OrangePrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        item.name,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "${profile?.currency ?: "₹"}${item.price.toInt()}${if (item.pricingType == "WEIGHT") "/${item.unit}" else ""}",
                                        color = OrangePrimary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp
                                    )

                                    if (item.isOutOfStock) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "OUT OF STOCK",
                                            color = Color.Red,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // EXPANDABLE FLOATING ACTION BUTTON
                if (!isBulkMode) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 20.dp, end = 20.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnimatedVisibility(
                            visible = fabExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (canAddProduct) {
                                    ExtendedFloatingActionButton(
                                        onClick = {
                                            fabExpanded = false
                                            viewModel.clearForm()
                                            navController.navigate("add_edit_item")
                                        },
                                        containerColor = OrangePrimary,
                                        contentColor = Color.Black,
                                        icon = { Icon(Icons.Default.Add, null) },
                                        text = { Text("Add Product", fontWeight = FontWeight.Bold) },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }

                                if (canManageCategory) {
                                    ExtendedFloatingActionButton(
                                        onClick = {
                                            fabExpanded = false
                                            navController.navigate("manage_categories")
                                        },
                                        containerColor = SurfaceDark,
                                        contentColor = Color.White,
                                        icon = { Icon(Icons.Default.Category, null) },
                                        text = { Text("Add Category", fontWeight = FontWeight.Bold) },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                            }
                        }

                        FloatingActionButton(
                            onClick = { fabExpanded = !fabExpanded },
                            containerColor = OrangePrimary,
                            contentColor = Color.Black,
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(8.dp)
                        ) {
                            Icon(
                                imageVector = if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "Actions",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLongPressMenu != null) {
        val item = showLongPressMenu!!
        AlertDialog(
            onDismissRequest = { showLongPressMenu = null },
            containerColor = SurfaceDark,
            title = { Text(item.name, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { 
                        viewModel.fillForm(item)
                        navController.navigate("add_edit_item")
                        showLongPressMenu = null
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Edit Item", color = Color.White)
                    }
                    TextButton(onClick = { 
                        viewModel.duplicateItem(item)
                        showLongPressMenu = null
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Duplicate Item", color = Color.White)
                    }
                    if (com.example.services.FeatureGate.canUseInventory()) {
                        TextButton(onClick = { 
                            viewModel.toggleStockStatus(item)
                            showLongPressMenu = null
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (item.isOutOfStock) "Mark In Stock" else "Mark Out Of Stock", color = if (item.isOutOfStock) Color.Green else Color.Yellow)
                        }
                    }
                    TextButton(onClick = { 
                        viewModel.deleteItem(item)
                        showLongPressMenu = null
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Delete Item", color = Color.Red)
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemScreen(navController: NavController, viewModel: ItemsViewModel) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val isEditing = viewModel.editingItem.value != null
    val app = OptixApplication.instance
    val profile by app.businessProfileRepository.profile.collectAsState(initial = null)
    val isLimitReached by viewModel.isLimitReached.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Item" else "Add Item", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section: Basic Info
            PremiumCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("BASIC INFORMATION", color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    OutlinedTextField(
                        value = viewModel.itemName.value,
                        onValueChange = { viewModel.itemName.value = it },
                        label = { Text("Product Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary)
                    )

                    var catExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = viewModel.itemCategoryName.value,
                            onValueChange = { },
                            label = { Text("Select Category") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            trailingIcon = { IconButton(onClick = { catExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary)
                        )
                        DropdownMenu(
                            expanded = catExpanded,
                            onDismissRequest = { catExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f).background(SurfaceDark)
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name, color = Color.White) },
                                    onClick = {
                                        viewModel.itemCategoryId.value = cat.id
                                        viewModel.itemCategoryName.value = cat.name
                                        catExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Section: Pricing
            PremiumCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("PRICING & UNITS", color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    var typeExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = viewModel.pricingType.value,
                            onValueChange = { },
                            label = { Text("Pricing Method") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            trailingIcon = { IconButton(onClick = { typeExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary)
                        )
                        DropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f).background(SurfaceDark)
                        ) {
                            listOf("FIXED", "WEIGHT").forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.replace("_", " "), color = Color.White) },
                                    onClick = { viewModel.pricingType.value = t; typeExpanded = false }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = viewModel.itemPrice.value,
                        onValueChange = { viewModel.itemPrice.value = it },
                        label = { Text(if (viewModel.pricingType.value == "WEIGHT") "Price per Unit (${viewModel.itemUnit.value})" else "Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        prefix = { Text("${profile?.currency ?: "₹"} ", color = OrangePrimary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary)
                    )

                    if (viewModel.pricingType.value == "WEIGHT") {
                        var unitExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedTextField(
                                value = viewModel.itemUnit.value,
                                onValueChange = { },
                                label = { Text("Selling Unit") },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                trailingIcon = { IconButton(onClick = { unitExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary)
                            )
                            DropdownMenu(
                                expanded = unitExpanded,
                                onDismissRequest = { unitExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f).background(SurfaceDark)
                            ) {
                                listOf("kg", "g", "L", "ml", "Piece").forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(u, color = Color.White) },
                                        onClick = { viewModel.itemUnit.value = u; unitExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { 
                    viewModel.saveItem(null) { 
                        Toast.makeText(context, "Product Saved", Toast.LENGTH_SHORT).show()
                        navController.popBackStack() 
                    } 
                },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black)
            ) {
                Text("SAVE PRODUCT", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (isLimitReached) {
            TrialLimitDialog(onUpgrade = { navController.navigate("subscription") })
        }
    }
}

// --- MANAGE STAFF SCREEN ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManageStaffScreen(navController: NavController, viewModel: StaffViewModel) {
    if (!com.example.services.FeatureGate.canUseStaff()) {
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }
    val staffList by viewModel.filteredStaff.collectAsState()
    val rawStaffList by viewModel.allStaff.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedRoleFilter by viewModel.selectedRoleFilter.collectAsState()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsState()

    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var staffToDelete by remember { mutableStateOf<Staff?>(null) }

    val authManager = OptixApplication.instance.authManager
    val userRole by authManager.userRole.collectAsState()
    val userPermissions by authManager.userPermissions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff Management", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        containerColor = DarkBackground,
        floatingActionButton = {
            if (userRole == "admin" || userRole == "owner" || authManager.hasPermission("ADD_STAFF")) {
                FloatingActionButton(onClick = { viewModel.clearFields(); showAddDialog = true }, containerColor = OrangePrimary) {
                    Icon(Icons.Default.Add, null, tint = Color.Black)
                }
            }
        }
    ) { padding ->
        if (userRole != "admin" && userRole != "owner" && !authManager.hasPermission("VIEW_STAFF")) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Access Denied: You do not have permission to access Staff Management", color = Color.Red, fontSize = 14.sp)
            }
            return@Scaffold
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh(context) },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ── 1. Search Bar ─────────────────────────────────────────────
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search by name or username...", color = Color.Gray, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = OrangePrimary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = OrangePrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // ── 2. Filter Chips (Role & Status) ─────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Role Filters
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Role:", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        listOf("ALL", "ADMIN", "STAFF").forEach { role ->
                            FilterChip(
                                selected = selectedRoleFilter == role,
                                onClick = { viewModel.selectedRoleFilter.value = role },
                                label = { Text(role, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OrangePrimary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = SurfaceDark,
                                    labelColor = Color.LightGray
                                ),
                                border = null
                            )
                        }
                    }

                    // Status Filters
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Status:", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        listOf("ALL", "ACTIVE", "DISABLED").forEach { status ->
                            FilterChip(
                                selected = selectedStatusFilter == status,
                                onClick = { viewModel.selectedStatusFilter.value = status },
                                label = { Text(status, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OrangePrimary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = SurfaceDark,
                                    labelColor = Color.LightGray
                                ),
                                border = null
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                // ── 3. Staff List or Empty State ────────────────────────────
                if (staffList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.PersonOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                            Text(
                                text = if (rawStaffList.isEmpty()) "No Staff Members Yet" else "No Staff Members Found",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (rawStaffList.isEmpty()) "Tap + button below to add your first staff member" else "Try adjusting your search query or filter filters",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(staffList, key = { it.id }) { s ->
                            val isStaffDisabled = s.isDisabled

                            // Format Last Active timestamp
                            val lastActiveText = remember(s.lastActivityAt) {
                                val ts = s.lastActivityAt
                                if (ts == null || ts == 0L) {
                                    "Never active"
                                } else {
                                    val diff = System.currentTimeMillis() - ts
                                    when {
                                        diff < 60_000L -> "Active just now"
                                        diff < 3600_000L -> "${diff / 60_000L}m ago"
                                        diff < 86400_000L -> "${diff / 3600_000L}h ago"
                                        else -> "${diff / 86400_000L}d ago"
                                    }
                                }
                            }

                            // Online / Offline Indicator heuristic (active in last 5 minutes & not disabled)
                            val isOnline = remember(s.lastActivityAt, s.isDisabled) {
                                !s.isDisabled && s.lastActivityAt != null && (System.currentTimeMillis() - s.lastActivityAt) < 300_000L
                            }

                            PremiumCard(
                                modifier = Modifier.fillMaxWidth().alpha(if (isStaffDisabled) 0.6f else 1.0f).clickable {
                                    navController.navigate("staff_detail/${s.id}")
                                }
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {

                                        // Avatar with Online/Offline indicator dot
                                        Box(contentAlignment = Alignment.BottomEnd) {
                                            Box(
                                                modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isStaffDisabled) Color.Gray else OrangePrimary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(s.name.take(1).uppercase(), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            }
                                            // Status Dot
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(DarkBackground)
                                                    .padding(2.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isOnline) Color.Green else Color.Gray)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(s.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                if (isStaffDisabled) {
                                                    Surface(color = Color.Red.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                                        Text("DISABLED", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                    }
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(s.role.uppercase(), color = OrangePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text("•", color = Color.Gray, fontSize = 11.sp)
                                                Text(lastActiveText, color = Color.Gray, fontSize = 11.sp)
                                            }
                                        }

                                        // Disable / Enable Switch
                                        Switch(
                                            checked = !isStaffDisabled,
                                            onCheckedChange = { active ->
                                                if (active) viewModel.enableStaff(s) else viewModel.disableStaff(s)
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary, checkedTrackColor = SurfaceDark)
                                        )

                                        IconButton(onClick = { viewModel.startEditing(s); showAddDialog = true }) {
                                            Icon(Icons.Default.Edit, null, tint = Color.Gray)
                                        }

                                        IconButton(onClick = { staffToDelete = s }) {
                                            Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f))
                                        }
                                    }

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Username:", color = Color.Gray, fontSize = 12.sp)
                                        Text(s.username, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 4. Add / Edit Staff Dialog ───────────────────────────────────────
        if (showAddDialog) {
            val isEditing = viewModel.editingStaff.value != null
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = SurfaceDark,
                properties = DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp),
                title = { Text(if (isEditing) "Edit Staff Member" else "Add New Staff Member", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                        OutlinedTextField(
                            value = viewModel.staffName.value,
                            onValueChange = { viewModel.staffName.value = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = viewModel.staffUsername.value,
                            onValueChange = { viewModel.staffUsername.value = it },
                            label = { Text("Username") },
                            placeholder = { Text("e.g. staff1") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = viewModel.password.value,
                            onValueChange = { viewModel.password.value = it },
                            label = { Text(if (isEditing) "New Password (leave blank to keep)" else "Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        var roleExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedTextField(
                                value = viewModel.staffRole.value.uppercase(),
                                onValueChange = { },
                                label = { Text("Role") },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { IconButton(onClick = { roleExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                            )
                            DropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                                listOf("staff", "admin").forEach { r ->
                                    DropdownMenuItem(text = { Text(r.uppercase()) }, onClick = { viewModel.staffRole.value = r; roleExpanded = false })
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.saveStaff { showAddDialog = false } },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black)
                    ) {
                        Text(if (isEditing) "SAVE CHANGES" else "ADD STAFF", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = Color.Gray) }
                }
            )
        }

        // ── 5. Delete Confirmation Dialog ────────────────────────────────────
        staffToDelete?.let { staff ->
            AlertDialog(
                onDismissRequest = { staffToDelete = null },
                containerColor = SurfaceDark,
                title = { Text("Delete Staff Member?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete ${staff.name} (${staff.username})? This action cannot be undone.", color = Color.LightGray, fontSize = 14.sp) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteStaff(staff)
                            staffToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
                    ) {
                        Text("DELETE", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { staffToDelete = null }) { Text("Cancel", color = Color.Gray) }
                }
            )
        }
    }
}

// ─── STAGE 2: ENTERPRISE STAFF DETAIL & PERMISSION MATRIX ────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StaffDetailScreen(
    navController: NavController,
    staffId: String,
    viewModel: StaffViewModel,
    settingsViewModel: SettingsViewModel
) {
    if (!com.example.services.FeatureGate.canUseStaff()) {
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }
    val staffList by viewModel.allStaff.collectAsState()
    val activePermissions by viewModel.activePermissions.collectAsState()
    val profile by settingsViewModel.profile.collectAsState()
    val context = LocalContext.current

    val staff = staffList.find { it.id == staffId }

    if (staff == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Staff Detail", fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = Color.White, navigationIconContentColor = Color.White)
                )
            },
            containerColor = DarkBackground
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Staff member not found or deleted", color = Color.Gray, fontSize = 14.sp)
            }
        }
        return
    }

    // Load active permissions into ViewModel state on first compose / staff change
    LaunchedEffect(staff.id, staff.permissionsJson) {
        viewModel.loadPermissionsForStaff(staff)
    }

    // Profile form state
    var editName by remember(staff.id) { mutableStateOf(staff.name) }
    var editRole by remember(staff.id) { mutableStateOf(staff.role) }
    var editPhone by remember(staff.id) { mutableStateOf(staff.phone ?: "") }
    var editEmail by remember(staff.id) { mutableStateOf(staff.email ?: "") }
    var editPassword by remember(staff.id) { mutableStateOf("") }

    var isSavingPermissions by remember { mutableStateOf(false) }
    var isSavingProfile by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(staff.name, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── 1. Staff Header Profile Card ─────────────────────────────────
            item {
                val isStaffDisabled = staff.isDisabled
                val isOnline = remember(staff.lastActivityAt, staff.isDisabled) {
                    !staff.isDisabled && staff.lastActivityAt != null && (System.currentTimeMillis() - staff.lastActivityAt) < 300_000L
                }
                val lastActiveText = remember(staff.lastActivityAt) {
                    val ts = staff.lastActivityAt
                    if (ts == null || ts == 0L) {
                        "Never active"
                    } else {
                        val diff = System.currentTimeMillis() - ts
                        when {
                            diff < 60_000L -> "Active just now"
                            diff < 3600_000L -> "${diff / 60_000L}m ago"
                            diff < 86400_000L -> "${diff / 3600_000L}h ago"
                            else -> "${diff / 86400_000L}d ago"
                        }
                    }
                }

                val createdDateText = remember(staff.lastModified) {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(staff.lastModified))
                }

                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier.size(56.dp).clip(CircleShape).background(if (isStaffDisabled) Color.Gray else OrangePrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(staff.name.take(1).uppercase(), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(DarkBackground)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) Color.Green else Color.Gray)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(staff.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    if (isStaffDisabled) {
                                        Surface(color = Color.Red.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                            Text("DISABLED", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }

                                Text(staff.username, color = OrangePrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }

                            Switch(
                                checked = !isStaffDisabled,
                                onCheckedChange = { active ->
                                    if (active) viewModel.enableStaff(staff) else viewModel.disableStaff(staff)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary, checkedTrackColor = SurfaceDark)
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        // Metadata Grid
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Role:", color = Color.Gray, fontSize = 13.sp)
                                Text(staff.role.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Business Name:", color = Color.Gray, fontSize = 13.sp)
                                Text(profile?.name ?: "Optix POS", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Last Active:", color = Color.Gray, fontSize = 13.sp)
                                Text(lastActiveText, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Created Date:", color = Color.Gray, fontSize = 13.sp)
                                Text(createdDateText, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // ── 2. Profile Editing & Password Reset Card ────────────────────
            item {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("STAFF PROFILE & CREDENTIALS", color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        var roleExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedTextField(
                                value = editRole.uppercase(),
                                onValueChange = { },
                                label = { Text("Role") },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { IconButton(onClick = { roleExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                            )
                            DropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                                listOf("staff", "admin").forEach { r ->
                                    DropdownMenuItem(text = { Text(r.uppercase()) }, onClick = { editRole = r; roleExpanded = false })
                                }
                            }
                        }

                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = it },
                            label = { Text("Phone Number (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = editEmail,
                            onValueChange = { editEmail = it },
                            label = { Text("Email (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = editPassword,
                            onValueChange = { editPassword = it },
                            label = { Text("Reset Password (leave blank to keep current)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                isSavingProfile = true
                                viewModel.updateStaffProfile(
                                    staff = staff,
                                    newName = editName,
                                    newRole = editRole,
                                    newPhone = editPhone,
                                    newEmail = editEmail,
                                    newPassword = editPassword
                                ) {
                                    isSavingProfile = false
                                    Toast.makeText(context, "Staff profile updated successfully", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black),
                            enabled = !isSavingProfile
                        ) {
                            if (isSavingProfile) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                            } else {
                                Text("SAVE PROFILE CHANGES", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── 3. Enterprise Permission Matrix ─────────────────────────────
            item {
                Text("ENTERPRISE PERMISSION MATRIX", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Collapsible categories state
            item {
                var expandedCategories by remember { mutableStateOf(setOf("Billing")) }

                val permissionCategories = listOf(
                    "Billing" to listOf(
                        "CREATE_BILLS" to "Create Bills",
                        "CANCEL_BILLS" to "Cancel Bills",
                        "REFUND_BILLS" to "Refund Bills",
                        "APPLY_DISCOUNTS" to "Apply Discounts",
                        "EDIT_BILLS" to "Edit Bills",
                        "WEIGHT_BILLING" to "Weight Based Billing",
                        "EDIT_WEIGHT" to "Edit Weight/Amount",
                        "ENTER_AMOUNT" to "Enter Open Amount",
                        "CHANGE_PRICE" to "Change Product Price"
                    ),
                    "Products" to listOf(
                        "VIEW_PRODUCTS" to "View Products",
                        "ADD_PRODUCTS" to "Add Products",
                        "EDIT_PRODUCTS" to "Edit Products",
                        "DELETE_PRODUCTS" to "Delete Products"
                    ),
                    "Categories" to listOf(
                        "VIEW_CATEGORIES" to "View Categories",
                        "ADD_CATEGORIES" to "Add Categories",
                        "EDIT_CATEGORIES" to "Edit Categories",
                        "DELETE_CATEGORIES" to "Delete Categories"
                    ),
                    "Customers" to listOf(
                        "VIEW_CUSTOMERS" to "View Customers",
                        "ADD_CUSTOMERS" to "Add Customers",
                        "EDIT_CUSTOMERS" to "Edit Customers",
                        "DELETE_CUSTOMERS" to "Delete Customers"
                    ),
                    "Inventory" to listOf(
                        "VIEW_INVENTORY" to "View Inventory",
                        "UPDATE_INVENTORY" to "Update Inventory Stock",
                        "STOCK_ADJUSTMENT" to "Stock Adjustment"
                    ),
                    "Reports" to listOf(
                        "VIEW_REPORTS" to "View Reports",
                        "EXPORT_REPORTS" to "Export Reports"
                    ),
                    "Settings" to listOf(
                        "MANAGE_RECEIPT" to "Manage Receipt Settings",
                        "MANAGE_PRINTER" to "Manage Printer Config",
                        "MANAGE_QR" to "Manage Payment QRs",
                        "MANAGE_TAXES" to "Manage Tax Settings"
                    ),
                    "Staff" to listOf(
                        "VIEW_STAFF" to "View Staff",
                        "ADD_STAFF" to "Add Staff",
                        "EDIT_STAFF" to "Edit Staff",
                        "DELETE_STAFF" to "Delete Staff",
                        "MANAGE_PERMISSIONS" to "Manage Permissions"
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    permissionCategories.forEach { (catName, perms) ->
                        val isExpanded = expandedCategories.contains(catName)
                        val activeCount = perms.count { activePermissions.contains(it.first) }
                        val allActive = activeCount == perms.size

                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedCategories = if (isExpanded) expandedCategories - catName else expandedCategories + catName
                                        }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = OrangePrimary
                                        )
                                        Text(catName.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Surface(color = OrangePrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)) {
                                            Text("$activeCount/${perms.size}", color = OrangePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                        }
                                    }

                                    // Category Master Toggle
                                    Switch(
                                        checked = allActive,
                                        onCheckedChange = { enableAll ->
                                            perms.forEach { (act, _) ->
                                                if (enableAll && !activePermissions.contains(act)) viewModel.togglePermissionAction(act)
                                                else if (!enableAll && activePermissions.contains(act)) viewModel.togglePermissionAction(act)
                                            }
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary, checkedTrackColor = SurfaceDark)
                                    )
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                        perms.forEach { (action, label) ->
                                            val isChecked = activePermissions.contains(action)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.togglePermissionAction(action) }
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(label, color = Color.White, fontSize = 13.sp)
                                                Switch(
                                                    checked = isChecked,
                                                    onCheckedChange = { viewModel.togglePermissionAction(action) },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary, checkedTrackColor = SurfaceDark)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 4. Save Permissions Button ──────────────────────────────────
            item {
                Button(
                    onClick = {
                        isSavingPermissions = true
                        viewModel.saveFullPermissionMatrix(staff) {
                            isSavingPermissions = false
                            Toast.makeText(context, "Permissions updated & synced across devices", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black),
                    enabled = !isSavingPermissions
                ) {
                    if (isSavingPermissions) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                    } else {
                        Text("SAVE & SYNC PERMISSIONS", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            // ── 5. Active Sessions & Device Tracking ────────────────────────
            item {
                val sessions by viewModel.sessions.collectAsState()
                val staffSessions = remember(sessions, staff.id) { sessions.filter { it.staffId == staff.id } }

                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("ACTIVE SESSIONS & DEVICES", color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Surface(color = Color.Green.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)) {
                                Text("${staffSessions.count { it.isActive }} Active", color = Color.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        if (staffSessions.isEmpty()) {
                            Text("No session records found", color = Color.Gray, fontSize = 13.sp)
                        } else {
                            staffSessions.take(10).forEach { session ->
                                val sdf = remember { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()) }
                                val loginTimeStr = sdf.format(java.util.Date(session.loginAt))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(session.deviceName ?: "Unknown Device", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Logged in: $loginTimeStr", color = Color.Gray, fontSize = 11.sp)
                                    }

                                    if (session.isActive) {
                                        Button(
                                            onClick = { viewModel.terminateRemoteSession(session.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f), contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("TERMINATE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Text("Ended", color = Color.Gray, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 6. Staff Activity Log Timeline ──────────────────────────────
            item {
                val activityLogs by viewModel.activityLogs.collectAsState()
                val staffLogs = remember(activityLogs, staff.id) { activityLogs.filter { it.staffId == staff.id } }

                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("ACTIVITY TIMELINE", color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        if (staffLogs.isEmpty()) {
                            Text("No activity logs recorded yet", color = Color.Gray, fontSize = 13.sp)
                        } else {
                            staffLogs.take(25).forEach { log ->
                                val sdf = remember { java.text.SimpleDateFormat("HH:mm:ss dd/MM", java.util.Locale.getDefault()) }
                                val timeStr = sdf.format(java.util.Date(log.createdAt))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(log.action.replace("_", " "), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            if (log.isSuspicious || log.severity == "CRITICAL" || log.severity == "WARNING") {
                                                Surface(
                                                    color = if (log.severity == "CRITICAL") Color.Red.copy(alpha = 0.3f) else Color.Yellow.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(log.severity, color = if (log.severity == "CRITICAL") Color.Red else Color.Yellow, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                        if (!log.entityType.isNullOrEmpty()) {
                                            Text("Target: ${log.entityType} ${log.entityId ?: ""}", color = Color.Gray, fontSize = 11.sp)
                                        }
                                    }

                                    Text(timeStr, color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StaffPermissionToggle(label: String, state: MutableState<Boolean>) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { state.value = !state.value }) {
        Checkbox(checked = state.value, onCheckedChange = { state.value = it }, colors = CheckboxDefaults.colors(checkedColor = OrangePrimary))
        Text(label, color = Color.White, fontSize = 13.sp)
    }
}

// --- MANAGE CATEGORIES SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(navController: NavController, viewModel: ItemsViewModel) {
    val categories by viewModel.categories.collectAsState()
    val items by viewModel.items.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.editingCategory.value = null; viewModel.newCategoryName.value = ""; showAddDialog = true }, containerColor = OrangePrimary) {
                Icon(Icons.Default.Add, null, tint = Color.Black)
            }
        }
    ) { padding ->
        val isRefreshing by viewModel.isRefreshing.collectAsState()
        val context = LocalContext.current
        val app = OptixApplication.instance

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh(context) },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(categories) { cat ->
                PremiumCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(cat.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.editingCategory.value = cat; viewModel.newCategoryName.value = cat.name; showAddDialog = true }) {
                            Icon(Icons.Default.Edit, null, tint = Color.Gray)
                        }
                        IconButton(onClick = { categoryToDelete = cat }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = SurfaceDark,
                title = { Text(if (viewModel.editingCategory.value == null) "Add Category" else "Rename Category", color = Color.White) },
                text = {
                    OutlinedTextField(value = viewModel.newCategoryName.value, onValueChange = { viewModel.newCategoryName.value = it }, label = { Text("Category Name") })
                },
                confirmButton = {
                    Button(onClick = { viewModel.saveCategory { showAddDialog = false } }, colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black)) {
                        Text("SAVE", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        if (categoryToDelete != null) {
            val cat = categoryToDelete!!
            val itemsInCat = items.filter { it.categoryId == cat.id }
            
            AlertDialog(
                onDismissRequest = { categoryToDelete = null },
                containerColor = SurfaceDark,
                title = { Text("Delete Category?", color = Color.White) },
                text = {
                    if (itemsInCat.isNotEmpty()) {
                        Text("There are ${itemsInCat.size} items in this category. What would you like to do?", color = Color.Gray)
                    } else {
                        Text("Are you sure you want to delete '${cat.name}'?", color = Color.Gray)
                    }
                },
                confirmButton = {
                    Column {
                        if (itemsInCat.isNotEmpty()) {
                            Button(
                                onClick = { viewModel.deleteCategory(cat); categoryToDelete = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Delete Category and All Items")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            var showMoveDialog by remember { mutableStateOf(false) }
                            Button(
                                onClick = { showMoveDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Move Items and Delete Category")
                            }

                            if (showMoveDialog) {
                                Dialog(onDismissRequest = { showMoveDialog = false }) {
                                    PremiumCard {
                                        Text("Move items to:", color = Color.White, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        categories.filter { it.id != cat.id }.forEach { target ->
                                            TextButton(onClick = { 
                                                viewModel.deleteCategory(cat, target.id)
                                                categoryToDelete = null
                                                showMoveDialog = false
                                            }, modifier = Modifier.fillMaxWidth()) {
                                                Text(target.name, color = OrangePrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Button(onClick = { viewModel.deleteCategory(cat); categoryToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                                Text("Delete")
                            }
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { categoryToDelete = null }) { Text("Cancel", color = Color.Gray) }
                }
            )
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier, primary: Boolean = false) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = if (primary) OrangePrimary else SurfaceDark)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontSize = 10.sp, color = if (primary) Color.Black else Color.Gray)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = if (primary) Color.Black else Color.White)
        }
    }
}

// --- ANALYTICS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel, profile: BusinessProfile?) {
    if (!com.example.services.FeatureGate.canUseAdvancedReports()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Default.Lock, null, tint = OrangePrimary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Advanced Analytics is a Growth Feature", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Upgrade your plan to unlock detailed business insights.", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
        return
    }
    val metrics by viewModel.metrics.collectAsState()
    val timeFrame by viewModel.timeFrame.collectAsState()
    val context = LocalContext.current
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    val summaryPreview by viewModel.summaryPreviewText.collectAsState()
    val app = OptixApplication.instance
    val currency = profile?.currency ?: "₹"

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.setSelectedDate(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("CANCEL") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (summaryPreview != null) {
        ThermalReceiptDialog(
            receiptText = summaryPreview!!,
            currency = profile?.currency ?: "₹",
            onDismiss = { viewModel.hideSummaryPreview() },
            onPrint = { profile?.let { viewModel.printCurrentSummary(it) } }
        )
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            viewModel.refresh(context)
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("BUSINESS STATS", fontWeight = FontWeight.Black, color = Color.White, fontSize = 24.sp)
                    Text(timeFrame.uppercase(), color = OrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                
                var showReportsMenu by remember { mutableStateOf(false) }

                Box {
                    Button(
                        onClick = { showReportsMenu = true },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reports", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    DropdownMenu(
                        expanded = showReportsMenu,
                        onDismissRequest = { showReportsMenu = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Preview PDF Report", color = Color.White, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Default.PictureInPicture, null, tint = OrangePrimary) },
                            onClick = {
                                showReportsMenu = false
                                viewModel.downloadReport(context, "PDF")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Print PDF Report", color = Color.White, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Default.Print, null, tint = OrangePrimary) },
                            onClick = {
                                showReportsMenu = false
                                viewModel.printPdfReport(context)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share PDF Report", color = Color.White, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Default.Share, null, tint = OrangePrimary) },
                            onClick = {
                                showReportsMenu = false
                                viewModel.sharePdfReport(context)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Save PDF to Downloads", color = Color.White, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Default.Download, null, tint = OrangePrimary) },
                            onClick = {
                                showReportsMenu = false
                                viewModel.savePdfToDownloads(context)
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        DropdownMenuItem(
                            text = { Text("Thermal Print Summary", color = Color.White, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Default.Receipt, null, tint = OrangePrimary) },
                            onClick = {
                                showReportsMenu = false
                                profile?.let { viewModel.generateSummaryPreview(it) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Select Custom Date", color = Color.White, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, null, tint = OrangePrimary) },
                            onClick = {
                                showReportsMenu = false
                                showDatePicker = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceDark).padding(4.dp)) {
                listOf("Today", "Weekly", "Monthly").forEach { tf ->
                    val selected = timeFrame == tf
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (selected) OrangePrimary else Color.Transparent).clickable { viewModel.setTimeFrame(tf) }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(tf, color = if (selected) Color.Black else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("TOTAL REVENUE", currency + metrics.totalSales.toInt(), Modifier.weight(1.2f), true)
                StatCard("ORDERS", metrics.numBills.toString(), Modifier.weight(0.8f))
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("AVG ORDER", currency + metrics.averageOrderValue.toInt(), Modifier.weight(1f))
                StatCard("TOTAL TAX", currency + metrics.totalTax.toInt(), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("TOP SELLING PRODUCTS", fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (metrics.topSellingItems.isEmpty()) {
                        Text("No sales data for this period", color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                    metrics.topSellingItems.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                                Text(item.name.take(1).uppercase(), color = OrangePrimary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(currency + item.totalRevenue.toInt(), color = Color.Gray, fontSize = 11.sp)
                            }
                            Surface(color = OrangePrimary.copy(alpha = 0.1f), shape = CircleShape) {
                                Text(item.quantity.toString() + " sold", color = OrangePrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // REPORT HISTORY SECTION
            val reportsList by app.dailyReportRepository.allReports.collectAsState(initial = emptyList())
            Text("REPORT HISTORY", fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (reportsList.isEmpty()) {
                        Text("No saved reports in history", color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 13.sp)
                    } else {
                        reportsList.take(10).forEach { r ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.03f)).clickable { viewModel.openReportPdf(context, r) }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PictureInPicture, null, tint = OrangePrimary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Daily Report - ${r.date}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Sales: ${currency}${r.totalSales.toInt()}", color = Color.Gray, fontSize = 11.sp)
                                }
                                Icon(Icons.Default.Download, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                color = OrangePrimary,
                trackColor = Color.Transparent
            )
        }
    }
}

// --- SETTINGS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, profileViewModel: BusinessSetupViewModel, navController: NavController, userRole: String, staffViewModel: StaffViewModel) {
    val profile by viewModel.profile.collectAsState(null)
    val context = LocalContext.current
    val isAdmin = userRole == "admin"
    var showEditDialog by viewModel.showEditBusinessDialog

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 32.sp)

        val userPermissions by OptixApplication.instance.authManager.userPermissions.collectAsState()
        val canManageQr = remember(userPermissions) { com.example.services.PermissionManager.can(com.example.services.PermissionManager.MANAGE_QR) }
        val canManageReceipt = remember(userPermissions) { com.example.services.PermissionManager.can(com.example.services.PermissionManager.MANAGE_RECEIPT) }
        val canViewStaff = remember(userPermissions) { com.example.services.PermissionManager.can(com.example.services.PermissionManager.VIEW_STAFF) }
        val canManageTaxes = remember(userPermissions) { com.example.services.PermissionManager.can(com.example.services.PermissionManager.MANAGE_TAXES) }

        if (isAdmin || canManageTaxes) {
            // Business Card
            PremiumCard(onClick = { viewModel.initProfileForm(profile ?: BusinessProfile()); showEditDialog = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(OrangePrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Store, null, tint = OrangePrimary, modifier = Modifier.size(30.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile?.name ?: "Business Name", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(profile?.phone ?: "Phone Number", color = Color.Gray, fontSize = 14.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                }
            }

            if (showEditDialog) {
                AlertDialog(
                    onDismissRequest = { showEditDialog = false },
                    containerColor = SurfaceDark,
                    title = { Text("Edit Business Info", color = Color.White) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = viewModel.profileName.value, onValueChange = { viewModel.profileName.value = it }, label = { Text("Business Name") })
                            OutlinedTextField(value = viewModel.profileAddress.value, onValueChange = { viewModel.profileAddress.value = it }, label = { Text("Address") })
                            OutlinedTextField(value = viewModel.profilePhone.value, onValueChange = { viewModel.profilePhone.value = it }, label = { Text("Phone") })
                            
                            var countryExpanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedTextField(
                                    value = viewModel.profileCountry.value,
                                    onValueChange = { },
                                    label = { Text("Country") },
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = { IconButton(onClick = { countryExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                                )
                                DropdownMenu(expanded = countryExpanded, onDismissRequest = { countryExpanded = false }) {
                                    com.example.services.PricingEngine.getCountryList().forEach { country ->
                                        DropdownMenuItem(text = { Text(country) }, onClick = {
                                            viewModel.profileCountry.value = country
                                            viewModel.profileCurrency.value = com.example.services.PricingEngine.getCurrencyForCountry(country)
                                            countryExpanded = false
                                        })
                                    }
                                }
                            }

                            OutlinedTextField(value = viewModel.profileGst.value, onValueChange = { viewModel.profileGst.value = it }, label = { Text("GST Number") })
                            
                            OutlinedTextField(
                                value = "Currency: ${viewModel.profileCurrency.value}",
                                onValueChange = { },
                                label = { Text("Automatic Currency") },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false
                            )

                            Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))
                            Text("Business Timings", color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            OutlinedTextField(value = viewModel.openingTime.value, onValueChange = { viewModel.openingTime.value = it }, label = { Text("Opening Time (e.g. 09:00)") })
                            OutlinedTextField(value = viewModel.closingTime.value, onValueChange = { viewModel.closingTime.value = it }, label = { Text("Closing Time (e.g. 22:00)") })
                            OutlinedTextField(value = viewModel.timezone.value, onValueChange = { viewModel.timezone.value = it }, label = { Text("Timezone (e.g. Asia/Riyadh)") })
                        }
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.saveProfileSettings(); showEditDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black)) {
                            Text("SAVE", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }

        if (isAdmin || canManageQr) {
            val hasAccess = com.example.services.FeatureGate.canUseMultipleQr()
            // Payment Accounts Card
            PremiumCard(onClick = { 
                if (hasAccess) navController.navigate("payment_accounts") 
                else navController.navigate("subscription")
            }) {
                SettingsItem(
                    icon = Icons.Default.QrCode,
                    title = "Payment Accounts",
                    subtitle = if (hasAccess) "Manage payment QR codes" else "Upgrade to GROWTH for multiple QRs",
                    trailing = {
                        if (!hasAccess) Icon(Icons.Default.Lock, null, tint = OrangePrimary, modifier = Modifier.size(16.dp))
                    },
                    onClick = { 
                        if (hasAccess) navController.navigate("payment_accounts") 
                        else navController.navigate("subscription")
                    }
                )
            }
        }

        if (isAdmin) {
            // Subscription Card
            PremiumCard(onClick = { navController.navigate("subscription") }) {
                SettingsItem(
                    icon = Icons.Default.CardMembership,
                    title = "Subscription",
                    subtitle = "Manage your plan",
                    onClick = { navController.navigate("subscription") }
                )
            }
        }

        if (isAdmin || canManageReceipt) {
            val hasAccess = com.example.services.FeatureGate.canUseAdvancedReceipt()
            // Receipt Customization
            PremiumCard(onClick = { 
                if (hasAccess) navController.navigate("receipt_customization") 
                else navController.navigate("subscription")
            }) {
                SettingsItem(
                    icon = Icons.Default.Receipt,
                    title = "Receipt Customization",
                    subtitle = if (hasAccess) "Edit headers & QR visibility" else "Upgrade to GROWTH to customize receipts",
                    trailing = {
                        if (!hasAccess) Icon(Icons.Default.Lock, null, tint = OrangePrimary, modifier = Modifier.size(16.dp))
                    },
                    onClick = { 
                        if (hasAccess) navController.navigate("receipt_customization") 
                        else navController.navigate("subscription")
                    }
                )
            }
        }

        if (isAdmin || canViewStaff) {
            if (com.example.services.FeatureGate.canUseStaff()) {
                // Staff Management
                PremiumCard(onClick = { navController.navigate("manage_staff") }) {
                    SettingsItem(
                        icon = Icons.Default.People,
                        title = "Staff Management",
                        subtitle = "Manage credentials & permissions",
                        onClick = { navController.navigate("manage_staff") }
                    )
                }
            } else {
                PremiumCard(onClick = { navController.navigate("subscription") }) {
                    SettingsItem(
                        icon = Icons.Default.People,
                        title = "Staff Management",
                        subtitle = com.example.services.FeatureGate.getFeatureSubtitle("STAFF_MANAGEMENT", "Upgrade to GROWTH to manage staff"),

                        trailing = { Icon(Icons.Default.Lock, null, tint = OrangePrimary, modifier = Modifier.size(16.dp)) },
                        onClick = { navController.navigate("subscription") }
                    )
                }
            }
        }

        // Printer Card
        PremiumCard {
            val connected by viewModel.connectedDevice.collectAsState()
            val scanning by viewModel.isScanning.collectAsState()
            val scanned by viewModel.scannedDevices.collectAsState()
            val printerError by viewModel.printerError.collectAsState()
            
            Text("Printer & Receipt", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingsItem(
                icon = Icons.Default.Print,
                title = connected?.name ?: "No Printer Connected",
                subtitle = if (connected != null) "Ready to print" else "Scan to connect a device",
                trailing = {
                    if (connected != null) Icon(Icons.Default.CheckCircle, null, tint = Color.Green)
                    else if (scanning) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OrangePrimary)
                }
            )

            if (printerError != null) {
                Text(printerError!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
            }

            if (connected == null && scanned.isNotEmpty()) {
                Text("PAIRED DEVICES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                scanned.forEach { d ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.connectPrinter(d) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Bluetooth, null, tint = OrangePrimary.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(d.name, color = Color.White, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { if (connected != null) viewModel.disconnectPrinter() else viewModel.scanPrinters() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (connected != null) Color.Red.copy(alpha = 0.1f) else OrangePrimary, contentColor = if (connected != null) Color.Red else Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (connected != null) "Disconnect" else "Scan Printers")
                }
                if (connected != null) {
                    Button(
                        onClick = { viewModel.testPrint() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Test Print")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Paper Size", color = Color.Gray, fontSize = 12.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("58mm", "80mm", "Auto").forEach { size ->
                    val selected = viewModel.paperWidth.value == size
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (selected) OrangePrimary else Color.White.copy(alpha = 0.05f)).clickable { viewModel.paperWidth.value = size }.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(size, color = if (selected) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Security / Backup
        PremiumCard {
            Text("Data & Security", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            SettingsItem(icon = Icons.Default.Lock, title = "Reset Password", onClick = { Toast.makeText(context, "Reset email sent!", Toast.LENGTH_SHORT).show() })
            SettingsItem(icon = Icons.Default.Backup, title = "Cloud Sync", onClick = { viewModel.runBackup(); Toast.makeText(context, "Syncing...", Toast.LENGTH_SHORT).show() })
        }

        // Support Card
        PremiumCard(onClick = { navController.navigate("support") }) {
            SettingsItem(
                icon = Icons.Default.SupportAgent,
                title = "Support & AI Assistant",
                subtitle = "AI Chat, Voice commands & FAQs",
                onClick = { navController.navigate("support") }
            )
        }

        // Logout
        OutlinedButton(
            onClick = { viewModel.logout(context) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
        ) {
            Icon(Icons.Default.Logout, null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("LOGOUT ACCOUNT", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

// --- RECEIPT CUSTOMIZATION SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptCustomizationScreen(navController: NavController, viewModel: SettingsViewModel) {
    if (!com.example.services.FeatureGate.canUseAdvancedReceipt()) {
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState(null)
    val activeQr by viewModel.activeQr.collectAsState(null)
    
    val logoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadLogo(uri, context) }
    }

    val previewText = buildString {
        // Business Name logic for one line
        val bizName = profile?.name ?: "STORE NAME"
        val displayBizName = if (bizName.length > 24) bizName.take(21) + "..." else bizName
        
        append("$displayBizName\n")
        if (viewModel.showAddress.value) append("${profile?.address ?: "Address"}\n")
        if (viewModel.showPhone.value) append("Ph: ${profile?.phone ?: "Phone"}\n")
        if (viewModel.showGst.value) append("GST: 27AAAAA0000A1Z5\n")
        append("--------------------------------\n")
        if (viewModel.showOrderNumber.value) append("TOKEN NO: T-123\n")
        append("--------------------------------\n")
        append("Invoice: #INV-001\n")
        if (viewModel.showDateTime.value) append("Date: 15/08/2026  Time: 12:00 PM\n")
        if (viewModel.showCashierName.value) append("Cashier: Admin\n")
        append("--------------------------------\n")
        append("Masala Chai      2  30.00\n")
        append("Samosa           1  20.00\n")
        append("--------------------------------\n")
        val currencySymbol = profile?.currency ?: "₹"
        append("TOTAL: $currencySymbol 50.00\n")
        append("--------------------------------\n")
        if (viewModel.qrEnabled.value && activeQr != null) append("[QR: ${activeQr?.name}]\n")
        append("${profile?.footerMessage}\n")
        if (viewModel.showVisitAgain.value) append("Visit Again!\n")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receipt Customization") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("LIVE PREVIEW (MONOCHROME)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if ((viewModel.showLogo.value || profile?.showLogo == true) && !profile?.logoPath.isNullOrEmpty()) {
                        AsyncImage(
                            model = profile!!.logoPath,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp).padding(bottom = 12.dp),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) // Preview as monochrome
                        )
                    }
                    Text(
                        text = previewText, 
                        fontFamily = FontFamily.Monospace, 
                        fontSize = 13.sp, 
                        color = Color.Black, 
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    if (viewModel.qrEnabled.value && activeQr != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AsyncImage(
                            model = activeQr!!.imagePath,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            ReceiptSectionTitle("Header")
            if (com.example.services.FeatureGate.canUseAdvancedReceipt()) {
                ReceiptToggleItem("Show Logo", viewModel.showLogo) { viewModel.saveReceiptToggle("showLogo", it) }
                if (viewModel.showLogo.value) {
                    Button(onClick = { logoLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text("Update Logo")
                    }
                    TextButton(onClick = { viewModel.removeLogo(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Remove Logo", color = Color.Red)
                    }
                }
            }
            ReceiptToggleItem("Show Business Name", viewModel.showBusinessName) { viewModel.saveReceiptToggle("showBusinessName", it) }
            ReceiptToggleItem("Show Address", viewModel.showAddress) { viewModel.saveReceiptToggle("showAddress", it) }
            ReceiptToggleItem("Show Phone Number", viewModel.showPhone) { viewModel.saveReceiptToggle("showPhone", it) }
            if (com.example.services.FeatureGate.canUseAdvancedReceipt()) {
                ReceiptToggleItem("Show GST Number", viewModel.showGst) { viewModel.saveReceiptToggle("showGst", it) }
            }
            ReceiptToggleItem("Show Date & Time", viewModel.showDateTime) { viewModel.saveReceiptToggle("showDateTime", it) }

            Spacer(modifier = Modifier.height(16.dp))

            ReceiptSectionTitle("Body")
            ReceiptToggleItem("Show Order Number", viewModel.showOrderNumber) { viewModel.saveReceiptToggle("showOrderNumber", it) }
            ReceiptToggleItem("Show Cashier Name", viewModel.showCashierName) { viewModel.saveReceiptToggle("showCashierName", it) }
            ReceiptToggleItem("Show Discounts", viewModel.showDiscounts) { viewModel.saveReceiptToggle("showDiscounts", it) }
            if (com.example.services.FeatureGate.canUseGstTax()) {
                ReceiptToggleItem("Show Taxes", viewModel.showTaxes) { viewModel.saveReceiptToggle("showTaxes", it) }
                if (viewModel.showTaxes.value) {
                    OutlinedTextField(
                        value = viewModel.taxPercentage.value,
                        onValueChange = { viewModel.taxPercentage.value = it },
                        label = { Text("Tax Percentage (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ReceiptSectionTitle("Footer")
            ReceiptToggleItem("Show Payment QR", viewModel.qrEnabled) { viewModel.saveReceiptToggle("qrEnabled", it) }
            if (viewModel.qrEnabled.value) {
                if (activeQr != null) {
                    Text("Active Account: ${activeQr?.name}", color = Color.Green, fontSize = 12.sp)
                } else {
                    Text("No active payment account. Go to Settings > Payment Accounts", color = Color.Red, fontSize = 12.sp)
                }
            }
            ReceiptToggleItem("Show \"Visit Again\"", viewModel.showVisitAgain) { viewModel.saveReceiptToggle("showVisitAgain", it) }

            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.resetReceiptSettings() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))) {
                    Text("Reset")
                }
                Button(
                    onClick = { 
                        viewModel.saveReceiptSettings { 
                            Toast.makeText(context, "Receipt settings saved successfully", Toast.LENGTH_SHORT).show()
                            navController.popBackStack() 
                        } 
                    }, 
                    modifier = Modifier.weight(1f), 
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black)
                ) {
                    Text("Save Receipt")
                }
            }
            
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

@Composable
fun ReceiptSectionTitle(title: String) {
    Text(title.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun ReceiptToggleItem(label: String, state: MutableState<Boolean>, onToggleChange: ((Boolean) -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Switch(
            checked = state.value, 
            onCheckedChange = { 
                state.value = it
                onToggleChange?.invoke(it)
            }, 
            colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary)
        )
    }
}

// --- PAYMENT ACCOUNTS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentAccountsScreen(navController: NavController, viewModel: SettingsViewModel) {
    val qrs by viewModel.allQrs.collectAsState()
    if (!com.example.services.FeatureGate.canUseMultipleQr() && qrs.size >= 1) {
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    val qrLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.savePaymentQr(uri, context) { showAddDialog = false } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Accounts") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        containerColor = DarkBackground,
        floatingActionButton = {
            if (com.example.services.FeatureGate.canUseMultipleQr() || qrs.isEmpty()) {
                FloatingActionButton(onClick = { viewModel.qrAccountName.value = ""; showAddDialog = true }, containerColor = OrangePrimary) {
                    Icon(Icons.Default.Add, null, tint = Color.Black)
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(qrs) { qr ->
                PremiumCard(onClick = { if (!qr.isActive) viewModel.setActiveQr(qr.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = qr.imagePath,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(qr.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            if (qr.isActive) {
                                Text("ACTIVE", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Tap to activate", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        IconButton(onClick = { viewModel.deleteQr(qr) }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f))
                        }
                    }
                }
            }
            if (qrs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No payment accounts found.\nTap + to add one.", color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = SurfaceDark,
                title = { Text("Add Payment Account", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = viewModel.qrAccountName.value,
                            onValueChange = { viewModel.qrAccountName.value = it },
                            label = { Text("Account Name (e.g. PhonePe)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("You will upload the QR image after clicking Save.", color = Color.Gray, fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    Button(onClick = { qrLauncher.launch("image/*") }, colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black)) {
                        Text("Select QR & Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

// --- SUBSCRIPTION SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(navController: NavController, viewModel: SubscriptionViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sub by viewModel.subscription.collectAsState()
    val profile by viewModel.businessProfile.collectAsState()
    val country = profile?.country ?: "India"
    val plans = com.example.services.PricingEngine.getPlansForCountry(country)
    
    var billingCycle by viewModel.billingCycle
    var showActivationDialog by remember { mutableStateOf(false) }

    val isProcessing by viewModel.isProcessingPayment
    val paymentErr by viewModel.paymentError

    if (isProcessing) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            CircularProgressIndicator(color = OrangePrimary)
        }
    }

    if (paymentErr != null) {
        LaunchedEffect(paymentErr) {
            Toast.makeText(context, paymentErr, Toast.LENGTH_LONG).show()
            viewModel.paymentError.value = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Subscription", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Current Plan Header ---
            Box(modifier = Modifier.fillMaxWidth().background(SurfaceDark).padding(24.dp)) {
                Column {
                    Text("CURRENT PLAN", fontSize = 12.sp, fontWeight = FontWeight.Black, color = OrangePrimary, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(sub?.planName ?: "Trial Mode", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
                        if (sub?.planId == "TRIAL") {
                            Spacer(modifier = Modifier.width(12.dp))
                            Surface(color = OrangePrimary.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                Text("TRIAL", color = OrangePrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(
                        if (sub?.expiryDate == 0L) "Valid Forever" 
                        else "Expires on ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(sub?.expiryDate ?: 0L))}",
                        color = Color.Gray, fontSize = 14.sp
                    )
                }
            }

            // --- Usage Stats (Trial Only) ---
            if (sub?.planId == "TRIAL") {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("TRIAL USAGE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    UsageProgress("Bills Created", sub?.billsUsed ?: 0, 50)
                    Spacer(modifier = Modifier.height(12.dp))
                    UsageProgress("Products Added", sub?.productsUsed ?: 0, 5)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Billing Cycle Toggle ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(SurfaceDark, RoundedCornerShape(32.dp)).padding(4.dp)
            ) {
                listOf("MONTHLY", "YEARLY").forEach { cycle ->
                    val isSelected = billingCycle == cycle
                    Surface(
                        onClick = { billingCycle = cycle },
                        color = if (isSelected) OrangePrimary else Color.Transparent,
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier.width(120.dp)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(
                                cycle, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 12.sp,
                                color = if (isSelected) Color.Black else Color.Gray
                            )
                        }
                    }
                }
            }
            
            if (billingCycle == "YEARLY") {
                Text("SAVE 10% ON ANNUAL PLANS 🎉", color = Color.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Plan Cards ---
            val context = androidx.compose.ui.platform.LocalContext.current
            val activity = remember(context) { 
                var ctx = context
                while (ctx is android.content.ContextWrapper) {
                    if (ctx is android.app.Activity) break
                    ctx = ctx.baseContext
                }
                ctx as? android.app.Activity
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                plans.forEach { plan ->
                    val currentPlanId = sub?.planId
                    val currentCycle = sub?.billingCycle ?: "MONTHLY"
                    // isCurrent = true only for TRIAL on its own plan (no payment button needed)
                    // For paid plans, always show a button (renew or switch cycle)
                    val isTrial = currentPlanId == "TRIAL"
                    val isPaidAndSamePlan = !isTrial && currentPlanId == plan.planId
                    val isCurrent = isTrial && plan.planId == currentPlanId
                    
                    // A higher plan includes features of lower plans
                    val isIncluded = currentPlanId == "GROWTH" && plan.planId == "STARTER"

                    PremiumPlanCard(
                        plan = plan,
                        cycle = billingCycle,
                        isCurrent = isCurrent,
                        isPaidCurrentPlan = isPaidAndSamePlan,
                        currentCycle = currentCycle,
                        isIncluded = isIncluded,
                        onUpgrade = {
                            if (activity != null) {
                                viewModel.initiateRazorpayPayment(activity, plan.planId, billingCycle)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            TextButton(onClick = { showActivationDialog = true }) {
                Text("Have an Activation Code?", color = OrangePrimary, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showActivationDialog) {
        AlertDialog(
            onDismissRequest = { showActivationDialog = false },
            containerColor = SurfaceDark,
            title = { Text("Activate Subscription", color = Color.White) },
            text = {
                Column {
                    Text("Enter the code provided by your administrator.", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = viewModel.activationCode.value,
                        onValueChange = { viewModel.activationCode.value = it },
                        placeholder = { Text("XXXX-XXXX-XXXX") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (viewModel.activationError.value != null) {
                        Text(viewModel.activationError.value!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.activateCode { showActivationDialog = false } },
                    enabled = !viewModel.isVerifyingCode.value
                ) {
                    if (viewModel.isVerifyingCode.value) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("ACTIVATE")
                }
            }
        )
    }
}

@Composable
fun UsageProgress(label: String, used: Int, limit: Int) {
    val progress = (used.toFloat() / limit).coerceIn(0f, 1f)
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Text("$used / $limit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = if (progress > 0.9f) Color.Red else OrangePrimary,
            trackColor = Color.White.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun PremiumPlanCard(
    plan: com.example.services.PlanPricing,
    cycle: String,
    isCurrent: Boolean,
    isPaidCurrentPlan: Boolean = false,
    currentCycle: String = "MONTHLY",
    isIncluded: Boolean = false,
    onUpgrade: () -> Unit
) {
    val price = if (cycle == "MONTHLY") plan.monthlyPrice else plan.yearlyPrice

    // Determine button label
    val buttonLabel = when {
        isIncluded -> "INCLUDED IN YOUR PLAN"
        isPaidCurrentPlan && cycle != currentCycle -> "SWITCH TO ${cycle}"
        isPaidCurrentPlan -> "RENEW ${plan.planName.uppercase()}"
        else -> "UPGRADE TO ${plan.planName.uppercase()}"
    }

    PremiumCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(plan.planName, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(modifier = Modifier.weight(1f))
                if (isCurrent || isPaidCurrentPlan) {
                    Surface(color = Color.Green.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                        Text("ACTIVE", color = Color.Green, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (isIncluded) {
                    Surface(color = OrangePrimary.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                        Text("INCLUDED", color = OrangePrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${plan.currency} ${price.toInt()}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = OrangePrimary)
                Text(if (cycle == "MONTHLY") " / month" else " / year", color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
            }

            Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                plan.features.forEach { feat ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color.Green, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(feat, color = Color.LightGray, fontSize = 13.sp)
                    }
                }
                plan.nonFeatures.forEach { feat ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cancel, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(feat, color = Color.DarkGray, fontSize = 13.sp)
                    }
                }
            }

            // Show button for all plans EXCEPT TRIAL plan already active (isCurrent = true and no payment needed)
            if (!isCurrent) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onUpgrade,
                    enabled = !isIncluded,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isIncluded) Color.DarkGray else OrangePrimary,
                        contentColor = if (isIncluded) Color.Gray else Color.Black
                    )
                ) {
                    Text(buttonLabel, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun SubscriptionStat(label: String, value: String, valueColor: Color) {
    Column {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

// --- SUPPORT & AI ASSISTANT SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(navController: NavController, viewModel: AiAssistantViewModel) {
    if (!com.example.services.FeatureGate.canUseAI()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Default.AutoAwesome, null, tint = OrangePrimary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("AI Assistant is a Growth Feature", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Upgrade your plan to get personalized business help from our AI.", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { navController.navigate("subscription") }, colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black)) {
                    Text("VIEW PLANS")
                }
            }
        }
        return
    }
    val messages by viewModel.messages.collectAsState()
    val isLimitReached by viewModel.isLimitReached.collectAsState()
    var userMessage by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support & AI") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.weight(1f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionChip(Icons.Default.Phone, "Call") { /* Call Support */ }
                QuickActionChip(Icons.Default.Email, "Email") { /* Email Support */ }
                QuickActionChip(Icons.Default.QuestionAnswer, "FAQ") { /* FAQ */ }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Voice */ }) { Icon(Icons.Default.Mic, null, tint = OrangePrimary) }
                    OutlinedTextField(
                        value = userMessage,
                        onValueChange = { userMessage = it },
                        placeholder = { Text("Ask anything...") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent),
                        singleLine = true
                    )
                    IconButton(
                        onClick = { 
                            viewModel.sendMessage(userMessage, navController)
                            userMessage = ""
                        },
                        enabled = userMessage.isNotBlank() && !isLimitReached
                    ) {
                        Icon(Icons.Default.Send, null, tint = if (userMessage.isNotBlank()) OrangePrimary else Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: AiMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val color = if (message.isUser) OrangePrimary else Color.White.copy(alpha = 0.1f)
    val textColor = if (message.isUser) Color.Black else Color.White

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 0.dp,
                bottomEnd = if (message.isUser) 0.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = color)
        ) {
            Text(text = message.content, modifier = Modifier.padding(12.dp), color = textColor, fontSize = 14.sp)
        }
    }
}

@Composable
fun QuickActionChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, color = Color.White, fontSize = 12.sp) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp), tint = OrangePrimary) },
        colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceDark)
    )
}

@Composable
fun ThermalReceiptDialog(receiptText: String, currency: String, onDismiss: () -> Unit, onPrint: (() -> Unit)? = null) {
    val application = OptixApplication.instance
    val factory = ViewModelFactory(application)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
    val profile by settingsViewModel.profile.collectAsState(null)
    val activeQr by settingsViewModel.activeQr.collectAsState(null)

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("RECEIPT PREVIEW", fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).verticalScroll(rememberScrollState()).padding(12.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (profile?.showLogo == true && profile?.logoPath != null) {
                            AsyncImage(
                                model = profile!!.logoPath,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp).clip(CircleShape),
                                contentScale = ContentScale.Fit,
                                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Text(receiptText, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.Black)
                        
                        if (profile?.qrEnabled == true && activeQr != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            AsyncImage(
                                model = activeQr!!.imagePath,
                                contentDescription = null,
                                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black)) {
                        Text("CLOSE")
                    }
                    if (onPrint != null) {
                        Button(onClick = { onPrint(); onDismiss() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black)) {
                            Icon(Icons.Default.Print, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PRINT")
                        }
                    }
                }
            }
        }
    }
}


// --- PROJEXA COMMAND CENTER PROTOTYPE ---
private data class ProjexaMetric(
    val label: String,
    val value: String,
    val detail: String,
    val color: Color,
    val icon: ImageVector
)

private data class ProjexaModule(
    val title: String,
    val subtitle: String,
    val status: String,
    val icon: ImageVector,
    val accent: Color
)

private data class ProjexaDocument(
    val title: String,
    val number: String,
    val revision: String,
    val status: String,
    val reviewer: String,
    val warning: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjexaCommandCenterScreen() {
    val modules = remember {
        listOf(
            ProjexaModule("Company onboarding", "Registration, company profile, project setup, user invites and AI Excel/CSV import.", "Setup 72%", Icons.Default.Business, Color(0xFF2DD4BF)),
            ProjexaModule("Multi-project operations", "Remote sites, areas, manpower, daily progress, approvals and management overview.", "3 active", Icons.Default.LocationOn, Color(0xFF60A5FA)),
            ProjexaModule("Document control", "Folder access, drawing uploads, revision history, transmittals and latest approved files.", "18 pending", Icons.Default.Folder, Color(0xFFFBBF24)),
            ProjexaModule("Attendance", "Worker, foreman and supervisor attendance with GPS, trade counts and overtime.", "426 present", Icons.Default.People, Color(0xFFA78BFA)),
            ProjexaModule("Store inventory", "Stock in/out, store issue vouchers, low stock alerts and site-wise balances.", "7 low stock", Icons.Default.Inventory, Color(0xFFFB7185)),
            ProjexaModule("Material requests", "Supervisor and PE request flow from approval to procurement, receipt and site issue.", "12 open", Icons.Default.Assignment, Color(0xFFF97316)),
            ProjexaModule("QC department", "Material submittals, MIR, WIR, RFIs, NCRs, checklists and consultant comments.", "9 reviews", Icons.Default.Verified, Color(0xFF22C55E)),
            ProjexaModule("Safety permits", "Area-wise hot work, height work, electrical, excavation and lifting permits.", "5 active", Icons.Default.Security, Color(0xFFEF4444)),
            ProjexaModule("Finance", "Cash flow, expenses, contractor payments, incoming payments and budget forecasts.", "SAR 18.4M", Icons.Default.AccountBalance, Color(0xFF38BDF8))
        )
    }
    val metrics = remember {
        listOf(
            ProjexaMetric("Projects", "3", "Hyundai EV, Yamama Palace, KFIA", Color(0xFF60A5FA), Icons.Default.Domain),
            ProjexaMetric("Manpower", "426", "34 absent, 61 overtime", Color(0xFFA78BFA), Icons.Default.Groups),
            ProjexaMetric("Cash flow", "SAR 8.2M", "Incoming this month", Color(0xFF22C55E), Icons.Default.Payments),
            ProjexaMetric("Approvals", "44", "Docs, RFIs, permits, payments", Color(0xFFFBBF24), Icons.Default.PendingActions)
        )
    }
    val documents = remember {
        listOf(
            ProjexaDocument("HVAC duct layout - Zone 3", "HYD-HVAC-SD-204", "Rev C", "Approved for Construction", "Consultant QC"),
            ProjexaDocument("Chilled water riser detail", "HYD-MEP-CW-118", "Rev B", "Under Review", "MEP Lead"),
            ProjexaDocument("Basement civil opening plan", "YAM-CIV-OP-044", "Rev A", "Superseded", "Document Controller", warning = true)
        )
    }
    val permits = remember {
        listOf(
            "Hot work - Factory roof AHU platform - expires 16:30",
            "Height work - Yamama Palace lobby ducting - safety review due",
            "Electrical isolation - Dammam airport pump room - active"
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Projexa", color = Color.White, fontWeight = FontWeight.Black)
                        Text("Construction project command center", color = Color.Gray, fontSize = 12.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { }) { Icon(Icons.Default.Search, null, tint = Color.White) }
                    IconButton(onClick = { }) { Icon(Icons.Default.AutoAwesome, null, tint = OrangePrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ProjexaHeroCard() }
            item { ProjexaMetricsGrid(metrics) }
            item { ProjexaOnboardingCard() }
            item { ProjexaSectionTitle("Operations Modules", "Website dashboard, mobile app, and mobile web split") }
            items(modules) { module -> ProjexaModuleCard(module) }
            item { ProjexaSectionTitle("Document Versioning", "GitHub-style revision history for drawings and files") }
            items(documents) { document -> ProjexaDocumentRow(document) }
            item { ProjexaSectionTitle("Safety Today", "Area-wise permits visible to the safety department") }
            items(permits) { permit -> ProjexaPermitRow(permit) }
            item { ProjexaAiImportCard() }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ProjexaHeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Factory, null, tint = OrangePrimary, modifier = Modifier.size(34.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hyundai EV Factory - Jeddah", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text("HVAC, MEP, civil and maintenance execution", color = Color.Gray, fontSize = 13.sp)
                }
            }
            Text(
                "Today: 426 workers present, 18 document approvals pending, 12 material requests open, 5 active safety permits and 3 RFIs overdue.",
                color = Color(0xFFE5E7EB),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            LinearProgressIndicator(
                progress = 0.68f,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = OrangePrimary,
                trackColor = Color.White.copy(alpha = 0.12f)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Planned 74%", color = Color.Gray, fontSize = 12.sp)
                Text("Actual 68%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ProjexaMetricsGrid(metrics: List<ProjexaMetric>) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.height(210.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false
    ) {
        items(metrics) { metric ->
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Icon(metric.icon, null, tint = metric.color, modifier = Modifier.size(24.dp))
                    Column {
                        Text(metric.value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 21.sp)
                        Text(metric.label, color = Color.Gray, fontSize = 12.sp)
                        Text(metric.detail, color = Color(0xFFCBD5E1), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjexaOnboardingCard() {
    PremiumCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PersonAdd, null, tint = OrangePrimary)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Company Registration Onboarding", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        listOf(
            "Company signup with industry, currency, timezone and logo",
            "Create first project with remote locations and areas",
            "Invite PM, PE, QC, Safety, Store, Accounts, Contractor and Consultant users",
            "AI imports Excel/CSV registers for attendance, inventory, RFIs, drawings and payments"
        ).forEach { item ->
            Row(modifier = Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF22C55E), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(item, color = Color.LightGray, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ProjexaSectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(subtitle, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
private fun ProjexaModuleCard(module: ProjexaModule) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(module.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(module.icon, null, tint = module.accent, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(module.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(module.subtitle, color = Color.Gray, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Surface(color = module.accent.copy(alpha = 0.16f), shape = RoundedCornerShape(10.dp)) {
                Text(module.status, color = module.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
            }
        }
    }
}

@Composable
private fun ProjexaDocumentRow(document: ProjexaDocument) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, null, tint = if (document.warning) Color(0xFFEF4444) else OrangePrimary)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(document.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${document.number} - ${document.revision} - ${document.reviewer}", color = Color.Gray, fontSize = 12.sp)
                }
            }
            if (document.warning) {
                Surface(color = Color(0xFF7F1D1D), shape = RoundedCornerShape(10.dp)) {
                    Text("SUPERSEDED VERSION. DO NOT USE FOR CONSTRUCTION.", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(10.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { }, label = { Text(document.status) }, leadingIcon = { Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp)) })
                AssistChip(onClick = { }, label = { Text("Version history") }, leadingIcon = { Icon(Icons.Default.Folder, null, modifier = Modifier.size(16.dp)) })
            }
        }
    }
}

@Composable
private fun ProjexaPermitRow(permit: String) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1415))) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, null, tint = Color(0xFFEF4444))
            Spacer(modifier = Modifier.width(10.dp))
            Text(permit, color = Color(0xFFFEE2E2), fontSize = 13.sp, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ProjexaAiImportCard() {
    PremiumCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, tint = OrangePrimary)
            Spacer(modifier = Modifier.width(10.dp))
            Text("AI Copilot and Data Import", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Upload old Excel logs and Projexa maps columns into attendance, material requests, drawing registers, RFIs, expenses and contractor payments. Ask natural questions like: show low stock items, generate weekly report, or find latest approved Zone 3 HVAC drawings.",
            color = Color.LightGray,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}
