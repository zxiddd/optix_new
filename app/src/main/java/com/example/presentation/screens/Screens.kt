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
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider

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
                        val navItems = if (isStaff) {
                            listOf(
                                Triple("billing", "Billing", Icons.Default.ReceiptLong),
                                Triple("items", "Menu", Icons.Default.RestaurantMenu),
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
                        val navItems = if (isStaff) {
                            listOf(
                                Triple("billing", "Billing", Icons.Default.ReceiptLong),
                                Triple("items", "Menu", Icons.Default.RestaurantMenu),
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
                        "billing" -> BillingScreen(billingViewModel, currentProfile, userRole)
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

    if (showReceipt && receiptText != null) {
        ThermalReceiptDialog(
            receiptText = receiptText!!,
            currency = currentProfile?.currency ?: "Rs.",
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
            viewModel.authError.value = "Google Sign-In failed: " + e.message
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LoginCard(if (viewModel.isSignUpMode.value) "Register Admin" else "Admin Login", listOf(
                        LoginField("Email", viewModel.email, Icons.Default.Email),
                        LoginField("Password", viewModel.password, Icons.Default.Lock, true)
                    ), authError, isVerifying, { viewModel.authenticate { } }, if (viewModel.isSignUpMode.value) "Create Account" else "Login")
                    
                    TextButton(onClick = { viewModel.toggleSignUpMode() }) {
                        Text(if (viewModel.isSignUpMode.value) "Have an account? Login" else "New Admin? Register", color = OrangePrimary)
                    }
                    Button(onClick = { launcher.launch(googleSignInClient.signInIntent) }, modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
                        Text("Continue with Google", color = Color.Black)
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
                    if (setupError != null) Text(setupError!!, color = Color.Red)
                    Button(onClick = { viewModel.saveBusinessProfile { } }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)) {
                        Text("Launch POS 🚀", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}

// --- BILLING SCREEN ---
@Composable
fun BillingScreen(viewModel: BillingViewModel, profile: BusinessProfile?, userRole: String) {
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
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Limit Reached") },
                text = { Text("You have reached today's free billing limit. Upgrade to Premium for unlimited billing.") },
                confirmButton = {
                    Button(onClick = { /* Navigate to subscription if possible */ }) {
                        Text("Upgrade Now")
                    }
                }
            )
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
    val weightInCart = if (item.pricingType == "WEIGHT_BASED") cart.filter { it.itemId == item.id }.sumOf { it.weight ?: 0.0 } else 0.0

    Card(modifier = Modifier.fillMaxWidth().height(100.dp).clickable { viewModel.addToCart(item) }, colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(item.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
            Text((profile?.currency ?: "Rs.") + item.price.toInt().toString() + (if (item.pricingType == "WEIGHT_BASED") "/${item.unit}" else ""), color = OrangePrimary)
            if (qty > 0) Text(qty.toString() + " in cart", fontSize = 10.sp, color = Color.Green)
            if (weightInCart > 0) Text(String.format("%.3f %s", weightInCart, item.unit), fontSize = 10.sp, color = Color.Green)
        }
    }
}

@Composable
fun BillingCartSection(viewModel: BillingViewModel, profile: BusinessProfile?) {
    val context = LocalContext.current
    val cart by viewModel.cartItems.collectAsState()
    val subtotal = viewModel.subtotal
    val discount = viewModel.discount
    val total = viewModel.grandTotal

    Card(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                                    if (item.pricingType == "WEIGHT_BASED") {
                                        Text("${String.format("%.3f", item.weight)} ${item.unit} x ${profile?.currency}${item.price.toInt()}", color = OrangePrimary, fontSize = 11.sp)
                                    } else {
                                        Text((profile?.currency ?: "Rs.") + item.price.toInt().toString(), color = OrangePrimary, fontSize = 11.sp)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { viewModel.removeFromCart(item) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                                    if (item.pricingType == "FIXED") {
                                        Text(item.quantity.toString(), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                        IconButton(onClick = { 
                                            // Handle add for fixed type
                                        }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Add, null, tint = OrangePrimary, modifier = Modifier.size(16.dp)) }
                                    }
                                }
                                val lineTotal = if (item.pricingType == "WEIGHT_BASED") item.price * (item.weight ?: 0.0) else item.price * item.quantity
                                Text((profile?.currency ?: "Rs.") + lineTotal.toInt().toString(), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 45.dp), textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }

            Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("GRAND TOTAL", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text((profile?.currency ?: "Rs.") + total.toInt().toString(), color = OrangePrimary, fontWeight = FontWeight.Black, fontSize = 26.sp)
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
                        viewModel.saveBillOnly(context, profile ?: return@Button, cashier) { 
                            Toast.makeText(context, "SAVED", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(0.8f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White),
                    enabled = cart.isNotEmpty()
                ) {
                    Text("SAVE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                
                Button(
                    onClick = { 
                        val cashier = OptixApplication.instance.authManager.staffName.value ?: "Admin"
                        viewModel.saveAndPrintBill(context, profile ?: return@Button, cashier) { 
                            Toast.makeText(context, "PRINTED", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1.4f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black),
                    enabled = cart.isNotEmpty()
                ) {
                    Icon(Icons.Default.Print, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PRINT BILL", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }
        }
    }
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

    if (previewText != null) {
        ThermalReceiptDialog(
            receiptText = previewText!!,
            currency = profile?.currency ?: "Rs.",
            onDismiss = { viewModel.hideReceiptPreview() }
        )
    }
    
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

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        Text((profile?.currency ?: "Rs.") + order.total.toInt().toString(), color = OrangePrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
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

// --- ITEMS SCREEN ---
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(viewModel: ItemsViewModel, navController: NavController) {
    val items by viewModel.items.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchItemQuery
    val selectedFilter by viewModel.selectedCategoryFilter
    val selectedIds by viewModel.selectedItemsForBulk
    
    var showLongPressMenu by remember { mutableStateOf<BillingItem?>(null) }
    var isBulkMode by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "MENU",
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontSize = 32.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                softWrap = false
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isBulkMode) {
                    IconButton(onClick = { isBulkMode = false; viewModel.selectedItemsForBulk.value = emptySet() }) {
                        Icon(Icons.Default.Close, null, tint = Color.Red)
                    }
                } else {
                    IconButton(onClick = { navController.navigate("manage_categories") }, modifier = Modifier.background(SurfaceDark, CircleShape)) {
                        Icon(Icons.Default.Category, null, tint = OrangePrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { isBulkMode = true }, modifier = Modifier.background(SurfaceDark, CircleShape)) {
                        Icon(Icons.Default.EditNote, null, tint = OrangePrimary)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { viewModel.clearForm(); navController.navigate("add_edit_item") },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ADD ITEM", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchItemQuery.value = it },
            placeholder = { Text("Search menu items...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Category Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedFilter == "All",
                    onClick = { viewModel.selectedCategoryFilter.value = "All" },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OrangePrimary, selectedLabelColor = Color.Black)
                )
            }
            items(categories) { cat ->
                FilterChip(
                    selected = selectedFilter == cat.name,
                    onClick = { viewModel.selectedCategoryFilter.value = cat.name },
                    label = { Text(cat.name) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OrangePrimary, selectedLabelColor = Color.Black)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val filteredItems = items.filter { 
            (selectedFilter == "All" || it.categoryId == categories.find { c -> c.name == selectedFilter }?.id) &&
            it.name.contains(searchQuery, ignoreCase = true)
        }

        if (filteredItems.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No items found", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
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
                            .border(2.dp, if (isSelected) OrangePrimary else Color.Transparent, RoundedCornerShape(22.dp))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(item.name.take(1).uppercase(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(item.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.categoryName, color = Color.Gray, fontSize = 12.sp)
                            Text("Rs.${item.price.toInt()}${if (item.pricingType == "WEIGHT_BASED") "/${item.unit}" else ""}", color = OrangePrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            
                            if (item.isOutOfStock) {
                                Text("OUT OF STOCK", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (isBulkMode && selectedIds.isNotEmpty()) {
            var showBulkActionDialog by remember { mutableStateOf(false) }
            Button(
                onClick = { showBulkActionDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black)
            ) {
                Text("BULK UPDATE (${selectedIds.size} ITEMS)", fontWeight = FontWeight.Bold)
            }

            if (showBulkActionDialog) {
                AlertDialog(
                    onDismissRequest = { showBulkActionDialog = false },
                    containerColor = SurfaceDark,
                    title = { Text("Bulk Price Update", color = Color.White) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedTextField(
                                    value = viewModel.bulkPriceAction.value,
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Operation") },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = { IconButton(onClick = { expanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                                )
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    listOf("Set Same Price", "Increase by Fixed Amount", "Decrease by Fixed Amount", "Increase by Percentage", "Decrease by Percentage").forEach { op ->
                                        DropdownMenuItem(text = { Text(op) }, onClick = { viewModel.bulkPriceAction.value = op; expanded = false })
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = viewModel.bulkValue.value,
                                onValueChange = { viewModel.bulkValue.value = it },
                                label = { Text("Value") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.applyBulkUpdate(); showBulkActionDialog = false; isBulkMode = false }) {
                            Text("APPLY")
                        }
                    }
                )
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
                    TextButton(onClick = { 
                        viewModel.toggleStockStatus(item)
                        showLongPressMenu = null
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (item.isOutOfStock) "Mark In Stock" else "Mark Out Of Stock", color = if (item.isOutOfStock) Color.Green else Color.Yellow)
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
    val isEditing = viewModel.itemName.value.isNotEmpty()

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
                            listOf("FIXED", "WEIGHT_BASED").forEach { t ->
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
                        label = { Text(if (viewModel.pricingType.value == "WEIGHT_BASED") "Price per Unit" else "Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        prefix = { Text("Rs. ", color = OrangePrimary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary)
                    )

                    if (viewModel.pricingType.value == "WEIGHT_BASED") {
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
    }
}

// --- MANAGE STAFF SCREEN ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManageStaffScreen(navController: NavController, viewModel: StaffViewModel) {
    val staffList by viewModel.allStaff.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff Management") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.clearFields(); showAddDialog = true }, containerColor = OrangePrimary) {
                Icon(Icons.Default.Add, null, tint = Color.Black)
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(staffList) { s ->
                PremiumCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(OrangePrimary), contentAlignment = Alignment.Center) {
                                Text(s.name.take(1).uppercase(), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(s.role.uppercase(), color = OrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { viewModel.startEditing(s); showAddDialog = true }) { Icon(Icons.Default.Edit, null, tint = Color.Gray) }
                            IconButton(onClick = { viewModel.deleteStaff(s) }) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f)) }
                        }
                        
                        Divider(color = Color.White.copy(alpha = 0.05f))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("LOGIN CREDENTIALS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Username:", color = Color.Gray, fontSize = 13.sp)
                                Text(s.username, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Password:", color = Color.Gray, fontSize = 13.sp)
                                Text(s.password, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("PERMISSIONS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            val permissions = mutableListOf<String>()
                            if (s.canBillWeightBased) permissions.add("Weight Billing")
                            if (s.canEditWeight) permissions.add("Edit Weight/Amount")
                            if (s.canChangeProductPrice) permissions.add("Change Price")
                            
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                permissions.forEach { p ->
                                    Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(8.dp)) {
                                        Text(p, color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            val isEditing = viewModel.editingStaff.value != null
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = SurfaceDark,
                properties = DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp),
                title = { Text(if (isEditing) "Edit Staff" else "Add New Staff", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                        OutlinedTextField(
                            value = viewModel.staffName.value,
                            onValueChange = { viewModel.staffName.value = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = viewModel.staffUsername.value,
                            onValueChange = { viewModel.staffUsername.value = it },
                            label = { Text("Username") },
                            placeholder = { Text("e.g. staff1") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = viewModel.password.value,
                            onValueChange = { viewModel.password.value = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth()
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

                        Text("Permissions", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        StaffPermissionToggle("Can Bill Weight Based Items", viewModel.canBillWeightBased)
                        StaffPermissionToggle("Can Edit Weight/Amount", viewModel.canEditWeight)
                        StaffPermissionToggle("Can Change Product Price", viewModel.canChangeProductPrice)
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.saveStaff { showAddDialog = false } }, colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black)) {
                        Text("SAVE STAFF", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = Color.Gray) }
                }
            )
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
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
    val metrics by viewModel.metrics.collectAsState()
    val timeFrame by viewModel.timeFrame.collectAsState()
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    val summaryPreview by viewModel.summaryPreviewText.collectAsState()

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
            currency = profile?.currency ?: "Rs.",
            onDismiss = { viewModel.hideSummaryPreview() },
            onPrint = { profile?.let { viewModel.printCurrentSummary(it) } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("BUSINESS STATS", fontWeight = FontWeight.Black, color = Color.White, fontSize = 24.sp)
                Text(timeFrame.uppercase(), color = OrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
            Row {
                IconButton(onClick = { showDatePicker = true }, modifier = Modifier.background(SurfaceDark, CircleShape)) {
                    Icon(Icons.Default.CalendarToday, null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { profile?.let { viewModel.generateSummaryPreview(it) } }, modifier = Modifier.background(SurfaceDark, CircleShape)) {
                    Icon(Icons.Default.Print, null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { viewModel.downloadReport(context, "PDF") }, modifier = Modifier.background(SurfaceDark, CircleShape)) {
                    Icon(Icons.Default.Download, null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
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
            StatCard("TOTAL REVENUE", "Rs." + metrics.totalSales.toInt(), Modifier.weight(1.2f), true)
            StatCard("ORDERS", metrics.numBills.toString(), Modifier.weight(0.8f))
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("AVG ORDER", "Rs." + metrics.averageOrderValue.toInt(), Modifier.weight(1f))
            StatCard("TOTAL TAX", "Rs." + metrics.totalTax.toInt(), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))

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
                            Text("Rs." + item.totalRevenue.toInt(), color = Color.Gray, fontSize = 11.sp)
                        }
                        Surface(color = OrangePrimary.copy(alpha = 0.1f), shape = CircleShape) {
                            Text(item.quantity.toString() + " sold", color = OrangePrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
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

        if (isAdmin) {
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
                            OutlinedTextField(value = viewModel.profileGst.value, onValueChange = { viewModel.profileGst.value = it }, label = { Text("GST Number") })
                        }
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.saveProfileSettings(); showEditDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black)) {
                            Text("SAVE", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Payment Accounts Card
            PremiumCard(onClick = { navController.navigate("payment_accounts") }) {
                SettingsItem(
                    icon = Icons.Default.QrCode,
                    title = "Payment Accounts",
                    subtitle = "Manage payment QR codes",
                    onClick = { navController.navigate("payment_accounts") }
                )
            }

            // Subscription Card
            PremiumCard(onClick = { navController.navigate("subscription") }) {
                SettingsItem(
                    icon = Icons.Default.CardMembership,
                    title = "Subscription",
                    subtitle = "Manage your plan",
                    onClick = { navController.navigate("subscription") }
                )
            }

            // Receipt Customization
            PremiumCard(onClick = { navController.navigate("receipt_customization") }) {
                SettingsItem(
                    icon = Icons.Default.Receipt,
                    title = "Receipt Customization",
                    subtitle = "Edit headers & QR visibility",
                    onClick = { navController.navigate("receipt_customization") }
                )
            }

            // Staff Management
            PremiumCard(onClick = { navController.navigate("manage_staff") }) {
                SettingsItem(
                    icon = Icons.Default.People,
                    title = "Staff Management",
                    subtitle = "Manage credentials & permissions",
                    onClick = { navController.navigate("manage_staff") }
                )
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
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState(null)
    val activeQr by viewModel.activeQr.collectAsState(null)
    
    val logoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadLogo(it, context) }
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
        append("TOTAL: Rs. 50.00\n")
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
                    if (viewModel.showLogo.value && profile?.logoPath != null) {
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
            ReceiptToggleItem("Show Logo", viewModel.showLogo)
            if (viewModel.showLogo.value) {
                Button(onClick = { logoLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Update Logo")
                }
                TextButton(onClick = { viewModel.removeLogo() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Remove Logo", color = Color.Red)
                }
            }
            ReceiptToggleItem("Show Business Name", viewModel.showBusinessName)
            ReceiptToggleItem("Show Address", viewModel.showAddress)
            ReceiptToggleItem("Show Phone", viewModel.showPhone)
            ReceiptToggleItem("Show GST", viewModel.showGst)

            Spacer(modifier = Modifier.height(16.dp))

            ReceiptSectionTitle("Body")
            ReceiptToggleItem("Show Date & Time", viewModel.showDateTime)
            ReceiptToggleItem("Show Order Number", viewModel.showOrderNumber)
            ReceiptToggleItem("Show Cashier Name", viewModel.showCashierName)
            ReceiptToggleItem("Show Discounts", viewModel.showDiscounts)
            ReceiptToggleItem("Show Taxes", viewModel.showTaxes)
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

            Spacer(modifier = Modifier.height(16.dp))

            ReceiptSectionTitle("Footer")
            ReceiptToggleItem("Show Payment QR", viewModel.qrEnabled)
            if (viewModel.qrEnabled.value) {
                if (activeQr != null) {
                    Text("Active Account: ${activeQr?.name}", color = Color.Green, fontSize = 12.sp)
                } else {
                    Text("No active payment account. Go to Settings > Payment Accounts", color = Color.Red, fontSize = 12.sp)
                }
            }
            ReceiptToggleItem("Show \"Visit Again\"", viewModel.showVisitAgain)

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
fun ReceiptToggleItem(label: String, state: MutableState<Boolean>) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Switch(checked = state.value, onCheckedChange = { state.value = it }, colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary))
    }
}

// --- PAYMENT ACCOUNTS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentAccountsScreen(navController: NavController, viewModel: SettingsViewModel) {
    val qrs by viewModel.allQrs.collectAsState()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    val qrLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.savePaymentQr(it, context) { showAddDialog = false } }
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
            FloatingActionButton(onClick = { viewModel.qrAccountName.value = ""; showAddDialog = true }, containerColor = OrangePrimary) {
                Icon(Icons.Default.Add, null, tint = Color.Black)
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
    val sub by viewModel.subscription.collectAsState()
    val plans by viewModel.availablePlans.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscription") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("CURRENT PLAN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
            Spacer(modifier = Modifier.height(8.dp))
            PremiumCard {
                Text(sub?.planName ?: "Free Plan", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("Rs.${sub?.amount?.toInt() ?: 0} / Month", color = OrangePrimary, fontWeight = FontWeight.Bold)
                
                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SubscriptionStat("Status", sub?.status?.uppercase() ?: "ACTIVE", if (sub?.status == "active") Color.Green else Color.Red)
                    SubscriptionStat("Expires On", if (sub?.expiryDate == 0L) "LIFETIME" else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(sub?.expiryDate ?: 0L)), Color.White)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (sub?.expiryDate != 0L) {
                    val remainingDays = ((sub?.expiryDate ?: System.currentTimeMillis()) - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
                    Text("$remainingDays Days Remaining", color = if (remainingDays < 5) Color.Red else Color.Gray, fontSize = 14.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black)) {
                    Text("Renew Now")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("UPGRADE PLANS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
            Spacer(modifier = Modifier.height(12.dp))
            plans.forEach { plan ->
                PremiumCard(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(plan.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Rs.${plan.price.toInt()}", color = OrangePrimary, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.renewSubscription(plan) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (sub?.planId == plan.id) Color.White.copy(alpha = 0.1f) else OrangePrimary),
                            enabled = sub?.planId != plan.id
                        ) {
                            Text(if (sub?.planId == plan.id) "Current" else "Upgrade")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    plan.features.forEach { feature ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            Icon(Icons.Default.Check, null, tint = Color.Green, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(feature, color = Color.Gray, fontSize = 13.sp)
                        }
                    }
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
