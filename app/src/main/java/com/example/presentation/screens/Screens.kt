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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
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
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider

// --- THEME COLORS ---
val OrangePrimary = Color(0xFFFF6B00)
val DarkBackground = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)

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
    val currentProfile by profileViewModel.profile.collectAsState(initial = null)
    val userRole by authViewModel.userRole.collectAsState()
    val isStaff = userRole == "staff"
    
    LaunchedEffect(Unit) {
        billingViewModel.updateCurrentTokenState(context)
    }

    var currentTab by remember { mutableStateOf("billing") }
    val isPrinterConnected by settingsViewModel.connectedDevice.collectAsState(initial = null)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = maxWidth
        val isWideScreen = width >= 600.dp

        Scaffold(
            containerColor = DarkBackground,
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = SurfaceDark,
                        tonalElevation = 8.dp
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
                                Triple("items", "Menu", Icons.Default.RestaurantMenu),
                                Triple("analytics", "Stats", Icons.Default.BarChart),
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
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = SurfaceDark,
                        header = {
                            Image(
                                painter = painterResource(id = R.drawable.img_zaddy_logo),
                                contentDescription = "Logo",
                                modifier = Modifier.size(48.dp).padding(8.dp).clip(CircleShape)
                            )
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
                                Triple("items", "Menu", Icons.Default.RestaurantMenu),
                                Triple("analytics", "Stats", Icons.Default.BarChart),
                                Triple("settings", "Settings", Icons.Default.Settings)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        navItems.forEach { (route, label, icon) ->
                            NavigationRailItem(
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 11.sp) },
                                selected = currentTab == route,
                                onClick = { currentTab = route },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = OrangePrimary,
                                    unselectedIconColor = Color.Gray,
                                    indicatorColor = OrangePrimary.copy(alpha = 0.1f)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

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
    val isPreparing by billingViewModel.isPreparingOrder

    if (isPreparing) {
        Dialog(onDismissRequest = {}) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.size(200.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = OrangePrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("PRINTING...", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

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
    val authError by viewModel.authError
    val isVerifying by viewModel.isVerifying
    val userRole by viewModel.userRole.collectAsState()
    val context = LocalContext.current

    // Google Sign-In setup
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
            viewModel.signInWithGoogle(credential) { }
        } catch (e: Exception) {
            viewModel.authError.value = "Google Sign-In failed: ${e.message}"
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            if (userRole == "staff") {
                navController.navigate("main") { popUpTo("login") { inclusive = true } }
            } else {
                navController.navigate("business_setup") { popUpTo("login") { inclusive = true } }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = OrangePrimary, radius = 200.dp.toPx(), center = Offset(size.width, 0f), alpha = 0.1f)
            drawCircle(color = OrangePrimary, radius = 300.dp.toPx(), center = Offset(0f, size.height), alpha = 0.05f)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_zaddy_logo),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp).clip(RoundedCornerShape(28.dp)).border(2.dp, OrangePrimary, RoundedCornerShape(28.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("OPTIX BILLING", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 4.sp)
            Text("Smart. Cloud. Fast.", fontSize = 16.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(48.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LoginCard(
                    title = if (viewModel.isSignUpMode.value) "Register Admin" else "Admin Login",
                    fields = listOf(
                        LoginField("Email", viewModel.email, Icons.Default.Email),
                        LoginField("Password", viewModel.password, Icons.Default.Lock, isPassword = true)
                    ),
                    error = authError,
                    isLoading = isVerifying,
                    onAction = { viewModel.authenticate { } },
                    actionLabel = if (viewModel.isSignUpMode.value) "Create Account" else "Login"
                )
                
                TextButton(onClick = { viewModel.toggleSignUpMode() }) {
                    Text(if (viewModel.isSignUpMode.value) "Have an account? Login" else "New Admin? Register", color = OrangePrimary)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("OR", color = Color.DarkGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { launcher.launch(googleSignInClient.signInIntent) },
                    modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Continue with Google", color = Color.DarkGray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

data class LoginField(val label: String, val state: MutableState<String>, val icon: androidx.compose.ui.graphics.vector.ImageVector, val isPassword: Boolean = false)

@Composable
fun LoginCard(title: String, fields: List<LoginField>, error: String?, isLoading: Boolean, onAction: () -> Unit, actionLabel: String) {
    Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = OrangePrimary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                else Text(actionLabel, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            }
        }
    }
}

// --- 2. BUSINESS SETUP SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSetupScreen(navController: NavController, viewModel: BusinessSetupViewModel) {
    val profile by viewModel.profile.collectAsState()
    val setupError by viewModel.setupError

    LaunchedEffect(profile) {
        if (profile != null && profile?.setupCompleted == true) {
            navController.navigate("main") { popUpTo("business_setup") { inclusive = true } }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Welcome to Optix", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("Complete your business profile to start billing.", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))
            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = viewModel.businessName.value, 
                    onValueChange = { viewModel.businessName.value = it }, 
                    label = { Text("Business Name") }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                    OutlinedTextField(value = viewModel.address.value, onValueChange = { viewModel.address.value = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = viewModel.phone.value, onValueChange = { viewModel.phone.value = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = viewModel.gstNumber.value, onValueChange = { viewModel.gstNumber.value = it }, label = { Text("GST (Optional)") }, modifier = Modifier.fillMaxWidth())
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Currency:", color = Color.White, modifier = Modifier.weight(1f))
                        listOf("₹", "$", "€").forEach { curr ->
                            FilterChip(
                                selected = viewModel.selectedCurrency.value == curr,
                                onClick = { viewModel.selectedCurrency.value = curr },
                                label = { Text(curr) },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    if (setupError != null) Text(setupError!!, color = Color.Red, fontSize = 12.sp)
                    Button(onClick = { viewModel.saveBusinessProfile { } }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)) {
                        Text("Launch POS 🚀", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}

// --- 3. BILLING SCREEN (FRICTIONLESS) ---
@Composable
fun BillingScreen(viewModel: BillingViewModel, profile: BusinessProfile?, userRole: String = "admin") {
    val items by viewModel.items.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
            // LEFT: MENU
            Column(modifier = Modifier.weight(0.6f).fillMaxHeight().padding(8.dp)) {
                BillingMenuSection(viewModel, profile, items, categories, selectedCategory, searchQuery)
            }
            // RIGHT: CART
            Column(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
                BillingCartSection(viewModel, profile)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
            // TOP: MENU
            Column(modifier = Modifier.weight(0.55f).padding(8.dp)) {
                BillingMenuSection(viewModel, profile, items, categories, selectedCategory, searchQuery)
            }
            // BOTTOM: CART (Fixed size but content scrollable)
            Box(modifier = Modifier.weight(0.45f).fillMaxWidth()) {
                BillingCartSection(viewModel, profile)
            }
        }
    }
}

@Composable
fun BillingMenuSection(
    viewModel: BillingViewModel,
    profile: BusinessProfile?,
    items: List<BillingItem>,
    categories: List<Category>,
    selectedCategory: String,
    searchQuery: String
) {
    // Header & Search
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(profile?.name?.uppercase() ?: "STORE", fontSize = 16.sp, fontWeight = FontWeight.Black, color = OrangePrimary)
            Text("TOKEN #${viewModel.currentTokenNum.value}", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = OrangePrimary, modifier = Modifier.size(18.dp)) },
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier.width(160.dp).height(54.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangePrimary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
            singleLine = true
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Categories Row
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selectedCategory == "All",
                onClick = { viewModel.setCategory("All") },
                label = { Text("All", fontSize = 11.sp) },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OrangePrimary,
                    selectedLabelColor = Color.Black,
                    containerColor = SurfaceDark,
                    labelColor = Color.Gray
                ),
                border = null,
                modifier = Modifier.height(32.dp)
            )
        }
        items(categories) { cat ->
            FilterChip(
                selected = selectedCategory == cat.name,
                onClick = { viewModel.setCategory(cat.name) },
                label = { Text(cat.name, fontSize = 11.sp) },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OrangePrimary,
                    selectedLabelColor = Color.Black,
                    containerColor = SurfaceDark,
                    labelColor = Color.Gray
                ),
                border = null,
                modifier = Modifier.height(32.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Items Grid
    val filteredItems = items.filter { it.isAvailable && (selectedCategory == "All" || it.categoryName == selectedCategory) && it.name.contains(searchQuery, ignoreCase = true) }
    
    val configuration = LocalConfiguration.current
    val columns = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 3

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(filteredItems) { item ->
            ItemCard(item, viewModel, profile)
        }
    }
}

@Composable
fun ItemCard(item: BillingItem, viewModel: BillingViewModel, profile: BusinessProfile?) {
    val cart by viewModel.cart.collectAsState()
    val qty = cart[item.id] ?: 0
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
            .clickable {
                scope.launch {
                    scale.animateTo(0.95f, animationSpec = tween(100))
                    scale.animateTo(1f, animationSpec = tween(100))
                }
                viewModel.addToCart(item)
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = if (qty > 0) BorderStroke(1.5.dp, OrangePrimary) else null
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                if (false && !item.imageUrl.isNullOrEmpty()) { // Disabled images
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Fastfood, null, tint = OrangePrimary.copy(alpha = 0.2f), modifier = Modifier.size(32.dp))
                }
                
                if (qty > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(OrangePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$qty", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
            Text("${profile?.currency ?: "₹"}${item.price.toInt()}", fontSize = 12.sp, color = OrangePrimary, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun BillingCartSection(viewModel: BillingViewModel, profile: BusinessProfile?) {
    val context = LocalContext.current
    val cart by viewModel.cart.collectAsState()
    val subtotal by viewModel.cartSubtotal.collectAsState()
    val discount = viewModel.discount
    val grandTotal = (subtotal - discount).coerceAtLeast(0.0)
    val allItems by viewModel.items.collectAsState()
    
    Card(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 0.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 12.dp)) {
            // Cart Header (Fixed)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShoppingCart, null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("CURRENT BILL", fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp, fontSize = 14.sp)
                Spacer(modifier = Modifier.weight(1f))
                if (cart.isNotEmpty()) {
                    Surface(color = OrangePrimary.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text("${cart.values.sum()} ITEMS", color = OrangePrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Everything else except Buttons is scrollable
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cart Items
                    if (cart.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxHeight(0.5f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Empty Bill", color = Color.DarkGray, fontSize = 12.sp)
                            }
                        }
                    } else {
                        items(cart.entries.toList(), key = { it.key }) { (itemId, qty) ->
                            val item = allItems.find { it.id == itemId } ?: return@items
                            CartItemRow(item, qty, viewModel, profile)
                        }
                    }

                    // Totals Section (Now inside Scrollable)
                    if (cart.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Subtotal", color = Color.Gray, fontSize = 12.sp)
                                    Text("${profile?.currency ?: "₹"}${subtotal.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Discount", color = Color.Gray, fontSize = 12.sp)
                                    Text("- ${profile?.currency ?: "₹"}${discount.toInt()}", color = OrangePrimary, fontSize = 12.sp)
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                    drawLine(color = Color.Gray.copy(alpha = 0.2f), start = Offset(0f, 0.5f), end = Offset(size.width, 0.5f), pathEffect = pathEffect, strokeWidth = 1.dp.toPx())
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("TOTAL", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                    Text("${profile?.currency ?: "₹"}${grandTotal.toInt()}", color = OrangePrimary, fontWeight = FontWeight.Black, fontSize = 24.sp)
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                PaymentMethodSelector(viewModel)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }

            // Fixed Buttons at the very bottom
            Spacer(modifier = Modifier.height(12.dp))
            FixedActionButtons(viewModel, profile, cart.isNotEmpty(), context)
        }
    }
}

@Composable
fun CartItemRow(item: BillingItem, qty: Int, viewModel: BillingViewModel, profile: BusinessProfile?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Item Icon (Fixed Size)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Coffee, null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        
        // Name and Unit Price (Flexible)
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${profile?.currency ?: "₹"}${item.price.toInt()}", fontSize = 11.sp, color = OrangePrimary)
        }
        
        // Quantity Controls (No Overlap)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Surface(
                onClick = { viewModel.removeFromCart(item) },
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            
            Text(
                text = "$qty",
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            
            Surface(
                onClick = { viewModel.addToCart(item) },
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = OrangePrimary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Total Price for this item
        Text(
            text = "${profile?.currency ?: "₹"}${(item.price * qty).toInt()}",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.widthIn(min = 40.dp),
            textAlign = TextAlign.End
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        // Delete Button
        IconButton(
            onClick = { viewModel.clearItemFromCart(item) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Default.Delete, null, tint = Color.DarkGray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun PaymentMethodSelector(viewModel: BillingViewModel) {
    val paymentMethod by viewModel.paymentMethod
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Cash", "UPI", "Card").forEach { method ->
            val selected = paymentMethod == method
            Surface(
                modifier = Modifier.weight(1f).height(40.dp).clickable { viewModel.paymentMethod.value = method },
                shape = RoundedCornerShape(10.dp),
                color = if (selected) OrangePrimary.copy(alpha = 0.1f) else Color.Transparent,
                border = BorderStroke(1.dp, if (selected) OrangePrimary else Color.White.copy(alpha = 0.1f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = OrangePrimary, unselectedColor = Color.Gray), modifier = Modifier.scale(0.8f))
                    Text(method, color = if (selected) OrangePrimary else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FixedActionButtons(viewModel: BillingViewModel, profile: BusinessProfile?, hasItems: Boolean, context: Context) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { viewModel.clearCart() },
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
        ) {
            Text("CLEAR", fontWeight = FontWeight.Black, color = Color.White, fontSize = 12.sp)
        }
        Button(
            onClick = {
                val staffName = OptixApplication.instance.authManager.staffName.value ?: "Admin"
                viewModel.saveAndPrintBill(context, profile ?: return@Button, staffName) { 
                    Toast.makeText(context, "ORDER PLACED", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.weight(2f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
            enabled = hasItems
        ) {
            Icon(Icons.Default.Print, null, tint = Color.Black, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("PRINT", fontWeight = FontWeight.ExtraBold, color = Color.Black, fontSize = 14.sp)
        }
    }
}

// --- 4. HISTORY SCREEN ---
@Composable
fun HistoryScreen(viewModel: OrderHistoryViewModel, profile: BusinessProfile?, userRole: String = "admin") {
    val orders by viewModel.filteredOrders.collectAsState()
    val searchTokenQuery by viewModel.searchTokenQuery.collectAsState()
    val timeFilter by viewModel.timeFilter.collectAsState()
    val previewText by viewModel.viewReceiptText.collectAsState()

    if (previewText != null) {
        ThermalReceiptDialog(
            receiptText = previewText!!,
            currency = profile?.currency ?: "₹",
            onDismiss = { viewModel.hideReceiptPreview() }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ORDER HISTORY", fontWeight = FontWeight.Black, color = Color.White, fontSize = 24.sp, modifier = Modifier.weight(1f))
            
            Row(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceDark).padding(4.dp)) {
                listOf("Today", "Weekly", "All").forEach { tf ->
                    val selected = timeFilter == tf
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) OrangePrimary else Color.Transparent)
                            .clickable { viewModel.setTimeFilter(tf) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(tf, color = if (selected) Color.Black else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchTokenQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search by token or item...", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = OrangePrimary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangePrimary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                unfocusedContainerColor = SurfaceDark,
                focusedContainerColor = SurfaceDark
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(orders) { order ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(OrangePrimary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                    Text("#${order.tokenNumber.takeLast(2)}", color = OrangePrimary, fontWeight = FontWeight.Black)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Token #${order.tokenNumber}", fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
                                    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                    Text(sdf.format(Date(order.timestamp)), color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                            Text("${profile?.currency ?: "₹"}${order.total.toInt()}", fontWeight = FontWeight.Black, color = OrangePrimary, fontSize = 20.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color.White.copy(alpha = 0.05f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Payment, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(order.paymentMethod, color = Color.Gray, fontSize = 13.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(order.cashierName, color = Color.Gray, fontSize = 13.sp)
                            }
                            
                            Row {
                                IconButton(onClick = { viewModel.showReceiptPreview(order, profile ?: return@IconButton) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Visibility, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                IconButton(onClick = { viewModel.reprintOrder(order, profile ?: return@IconButton) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Print, null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                                }
                                if (userRole == "admin") {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    IconButton(onClick = { viewModel.deleteOrder(order) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 5. ITEMS SCREEN ---
@Composable
fun ItemsScreen(viewModel: ItemsViewModel) {
    val items by viewModel.items.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItemForEdit by remember { mutableStateOf<BillingItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var filterCategory by remember { mutableStateOf("All") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("MANAGE MENU", fontWeight = FontWeight.Black, color = Color.White, fontSize = 24.sp, modifier = Modifier.weight(1f))
            Button(
                onClick = { 
                    viewModel.clearForm()
                    selectedItemForEdit = null
                    showAddDialog = true 
                }, 
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text("ADD ITEM", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))

        // Search & Category Management
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search items...", color = Color.Gray) },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = OrangePrimary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    unfocusedContainerColor = SurfaceDark,
                    focusedContainerColor = SurfaceDark
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            var showAddCat by remember { mutableStateOf(false) }
            IconButton(
                onClick = { showAddCat = true },
                modifier = Modifier.background(SurfaceDark, RoundedCornerShape(12.dp)).size(50.dp)
            ) {
                Icon(Icons.Default.Category, null, tint = OrangePrimary)
            }

            if (showAddCat) {
                AlertDialog(
                    onDismissRequest = { showAddCat = false },
                    containerColor = SurfaceDark,
                    title = { Text("Add Category", color = Color.White) },
                    text = {
                        OutlinedTextField(
                            value = viewModel.customCategoryName.value,
                            onValueChange = { viewModel.customCategoryName.value = it },
                            label = { Text("Category Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.addCustomCategory(); showAddCat = false }, colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)) {
                            Text("ADD", color = Color.Black)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = filterCategory == "All",
                    onClick = { filterCategory = "All" },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OrangePrimary, selectedLabelColor = Color.Black)
                )
            }
            items(categories) { cat ->
                FilterChip(
                    selected = filterCategory == cat.name,
                    onClick = { filterCategory = cat.name },
                    label = { Text(cat.name) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OrangePrimary, selectedLabelColor = Color.Black)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val filteredList = items.filter { 
            (filterCategory == "All" || it.categoryName == filterCategory) &&
            it.name.contains(searchQuery, ignoreCase = true)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filteredList) { item ->
                Card(
                    shape = RoundedCornerShape(20.dp), 
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.name.take(1).uppercase(), color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            Text(item.categoryName, fontSize = 12.sp, color = Color.Gray)
                            Text("Rs.${item.price.toInt()}", fontSize = 14.sp, color = OrangePrimary, fontWeight = FontWeight.Black)
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Switch(
                                checked = item.isAvailable, 
                                onCheckedChange = { viewModel.toggleAvailability(item) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OrangePrimary,
                                    checkedTrackColor = OrangePrimary.copy(alpha = 0.3f),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                                )
                            )
                            Row {
                                IconButton(onClick = { 
                                    viewModel.fillForm(item)
                                    selectedItemForEdit = item
                                    showAddDialog = true
                                }) { 
                                    Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(20.dp)) 
                                }
                                IconButton(onClick = { viewModel.deleteItem(item) }) { 
                                    Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.4f), modifier = Modifier.size(20.dp)) 
                                }
                            }
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
            title = { Text(if (selectedItemForEdit == null) "Add Item" else "Edit Item", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = viewModel.itemName.value, 
                        onValueChange = { viewModel.itemName.value = it }, 
                        label = { Text("Item Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = viewModel.itemPrice.value, 
                        onValueChange = { viewModel.itemPrice.value = it }, 
                        label = { Text("Price") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = viewModel.itemCategory.value, 
                            onValueChange = {}, 
                            label = { Text("Category") }, 
                            readOnly = true, 
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { IconButton(onClick = { expanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat.name) }, onClick = { viewModel.itemCategory.value = cat.name; expanded = false })
                            }
                        }
                    }
                }
            },
            confirmButton = { 
                Button(
                    onClick = { viewModel.saveItem(selectedItemForEdit) { showAddDialog = false } },
                    enabled = !viewModel.isSaving.value,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) { 
                    if (viewModel.isSaving.value) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                    } else {
                        Text("SAVE ITEM", color = Color.Black, fontWeight = FontWeight.Bold) 
                    }
                } 
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("CANCEL", color = Color.Gray)
                }
            }
        )
    }
}

// --- 6. ANALYTICS SCREEN ---
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel, profile: BusinessProfile?) {
    val metrics by viewModel.metrics.collectAsState()
    val timeFrame by viewModel.timeFrame.collectAsState()
    val context = LocalContext.current
    var showDownloadOptions by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("ANALYTICS", fontWeight = FontWeight.Black, color = Color.White, fontSize = 24.sp)
                Text("Insights for your business", color = Color.Gray, fontSize = 12.sp)
            }
            
            Box {
                IconButton(
                    onClick = { showDownloadOptions = true },
                    modifier = Modifier.background(SurfaceDark, CircleShape)
                ) {
                    Icon(Icons.Default.Download, null, tint = OrangePrimary)
                }
                DropdownMenu(expanded = showDownloadOptions, onDismissRequest = { showDownloadOptions = false }) {
                    DropdownMenuItem(text = { Text("Download PDF") }, onClick = { viewModel.downloadReport(context, "PDF"); showDownloadOptions = false })
                    DropdownMenuItem(text = { Text("Export CSV") }, onClick = { viewModel.downloadReport(context, "CSV"); showDownloadOptions = false })
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceDark).padding(4.dp)) {
            listOf("Today", "Weekly", "Monthly").forEach { tf ->
                val selected = timeFrame == tf
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) OrangePrimary else Color.Transparent)
                        .clickable { viewModel.setTimeFrame(tf) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tf, color = if (selected) Color.Black else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Key Performance Indicators
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(label = "TOTAL REVENUE", value = "Rs.${metrics.totalSales.toInt()}", modifier = Modifier.weight(1.2f), isPrimary = true)
            StatCard(label = "ORDERS", value = "${metrics.numBills}", modifier = Modifier.weight(0.8f))
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(label = "AVG BILL", value = "Rs.${metrics.averageOrderValue.toInt()}", modifier = Modifier.weight(1f))
            StatCard(label = "PEAK HOUR", value = metrics.peakHour, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        // Sales Chart Card
        Card(
            shape = RoundedCornerShape(24.dp), 
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SALES TREND", fontWeight = FontWeight.Black, color = Color.White, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(24.dp))
                SalesTrendChart(metrics.chartPoints)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        // Top Products List
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("TOP PRODUCTS", fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
            TextButton(onClick = { }) {
                Text("VIEW ALL", color = OrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (metrics.topSellingItems.isEmpty()) {
                    Text("No sales data yet", color = Color.Gray, modifier = Modifier.padding(20.dp).align(Alignment.CenterHorizontally))
                }
                metrics.topSellingItems.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f)), 
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.name.take(1).uppercase(), color = OrangePrimary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Rs.${item.totalRevenue.toInt()}", color = Color.Gray, fontSize = 12.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(OrangePrimary.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("${item.quantity} sold", color = OrangePrimary, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        // Payment Breakdown
        Text("PAYMENT DISTRIBUTION", fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (metrics.paymentBreakdown.isEmpty()) {
                    Text("Waiting for transactions...", color = Color.Gray, modifier = Modifier.padding(10.dp).align(Alignment.CenterHorizontally))
                }
                metrics.paymentBreakdown.forEach { (method, amount) ->
                    val percentage = if (metrics.totalSales > 0) (amount / metrics.totalSales).toFloat() else 0f
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(method, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Rs.${amount.toInt()} (${(percentage * 100).toInt()}%)", color = Color.Gray, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = percentage,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = OrangePrimary,
                            trackColor = Color.White.copy(alpha = 0.05f)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier, isPrimary: Boolean = false) {
    Card(modifier = modifier, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = if (isPrimary) OrangePrimary else SurfaceDark)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isPrimary) Color.Black.copy(alpha = 0.6f) else Color.Gray)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = if (isPrimary) Color.Black else Color.White)
        }
    }
}

@Composable
fun SalesTrendChart(points: List<ChartPoint>) {
    if (points.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Text("No data available", color = Color.Gray)
        }
        return
    }
    
    val maxValue = (points.maxOfOrNull { it.value } ?: 0.0).coerceAtLeast(1.0)
    
    Row(
        modifier = Modifier.fillMaxWidth().height(150.dp).padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        points.forEach { point ->
            val ratio = (point.value / maxValue).toFloat()
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .fillMaxHeight(ratio.coerceAtLeast(0.05f))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(OrangePrimary, OrangePrimary.copy(alpha = 0.3f))
                            )
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(point.label, fontSize = 8.sp, color = Color.Gray, maxLines = 1)
            }
        }
    }
}

// --- 7. SETTINGS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, profileViewModel: BusinessSetupViewModel, navController: NavController, userRole: String = "admin", staffViewModel: StaffViewModel) {
    val profile by viewModel.profile.collectAsState(initial = null)
    val context = LocalContext.current
    LaunchedEffect(profile) { profile?.let { viewModel.initProfileForm(it) } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("SETTINGS", fontWeight = FontWeight.Black, color = Color.White, fontSize = 24.sp)
        
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("BUSINESS PROFILE", fontWeight = FontWeight.Black, color = OrangePrimary, fontSize = 16.sp)
                
                OutlinedTextField(
                    value = viewModel.profileName.value,
                    onValueChange = { viewModel.profileName.value = it },
                    label = { Text("Business Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = viewModel.profileAddress.value,
                    onValueChange = { viewModel.profileAddress.value = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = viewModel.profilePhone.value,
                        onValueChange = { viewModel.profilePhone.value = it },
                        label = { Text("Phone") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    OutlinedTextField(
                        value = viewModel.profileGst.value,
                        onValueChange = { viewModel.profileGst.value = it },
                        label = { Text("GST NO") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = viewModel.profileFooter.value,
                    onValueChange = { viewModel.profileFooter.value = it },
                    label = { Text("Receipt Footer Message") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = { viewModel.saveProfileSettings() },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("UPDATE BUSINESS INFO", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Printer Settings
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
            val connectedDevice by viewModel.connectedDevice.collectAsState()
            val scannedDevices by viewModel.scannedDevices.collectAsState()
            val isScanning by viewModel.isScanning.collectAsState()
            val printerError by viewModel.printerError.collectAsState()

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("PRINTER CONFIGURATION", fontWeight = FontWeight.Black, color = OrangePrimary, fontSize = 16.sp)
                
                if (connectedDevice != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Print, null, tint = Color.Green, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(connectedDevice!!.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(connectedDevice!!.address, color = Color.Gray, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.logout() /* Add disconnect if needed */ }) {
                            Text("DISCONNECT", color = Color.Red)
                        }
                    }
                    Button(
                        onClick = { viewModel.testPrint() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("TEST PRINT")
                    }
                } else {
                    Text("No printer connected", color = Color.Gray, fontSize = 14.sp)
                    if (printerError != null) {
                        Text(printerError!!, color = Color.Red, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { viewModel.scanPrinters() },
                        enabled = !isScanning,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isScanning) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                        else Text("SCAN FOR PRINTERS", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                if (scannedDevices.isNotEmpty() && connectedDevice == null) {
                    Text("AVAILABLE PRINTERS", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    scannedDevices.forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.connectPrinter(device) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Bluetooth, null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(device.name, color = Color.White, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.logout(); navController.navigate("login") { popUpTo(0) } }, 
            modifier = Modifier.fillMaxWidth().height(56.dp), 
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
        ) {
            Icon(Icons.Default.Logout, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("LOGOUT ACCOUNT", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ThermalReceiptDialog(receiptText: String, currency: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Receipt Header
                Text(
                    "RECEIPT PREVIEW",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Receipt Content (Fixed Width 58mm feel)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = receiptText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("DONE", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { /* Share Logic */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SHARE", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
