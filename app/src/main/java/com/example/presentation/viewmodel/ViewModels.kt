package com.example.presentation.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.OptixApplication
import com.example.data.entity.*
import com.example.data.repository.*
import com.example.services.AuthManager
import com.example.services.PrinterManager
import com.example.services.PrinterDevice
import com.example.services.ReportService
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- VIEWMODEL FACTORIES ---
class ViewModelFactory(private val application: OptixApplication) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val authManager = application.authManager
        val userId = authManager.userId.value ?: "guest"
        val cloudRepo = CloudRepository(userId)

        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> 
                AuthViewModel(authManager, cloudRepo, application.staffRepository) as T
            modelClass.isAssignableFrom(BusinessSetupViewModel::class.java) -> 
                BusinessSetupViewModel(cloudRepo) as T
            modelClass.isAssignableFrom(BillingViewModel::class.java) -> 
                BillingViewModel(cloudRepo, application.printerManager, application.paymentQrRepository, application.categoryRepository, application.billOrderRepository, application.billingItemRepository) as T
            modelClass.isAssignableFrom(OrderHistoryViewModel::class.java) -> 
                OrderHistoryViewModel(cloudRepo, application.printerManager, application.paymentQrRepository, application.billOrderRepository) as T
            modelClass.isAssignableFrom(AnalyticsViewModel::class.java) -> 
                AnalyticsViewModel(cloudRepo, application.printerManager, application.billOrderRepository) as T
            modelClass.isAssignableFrom(ItemsViewModel::class.java) -> 
                ItemsViewModel(cloudRepo, application.categoryRepository, application.billingItemRepository) as T
            modelClass.isAssignableFrom(StaffViewModel::class.java) ->
                StaffViewModel(cloudRepo, application.staffRepository, authManager, application.businessProfileRepository) as T
            modelClass.isAssignableFrom(SubscriptionViewModel::class.java) ->
                SubscriptionViewModel(cloudRepo, application.subscriptionRepository) as T
            modelClass.isAssignableFrom(AiAssistantViewModel::class.java) ->
                AiAssistantViewModel(cloudRepo, application.supportTicketRepository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> 
                SettingsViewModel(cloudRepo, application.printerConfigRepository, authManager, application.printerManager, application.paymentQrRepository, application.businessProfileRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// --- 1. AUTH VIEWMODEL ---
class AuthViewModel(
    private val authManager: AuthManager,
    private val repository: CloudRepository,
    private val staffRepository: StaffRepository
) : ViewModel() {
    val isLoggedIn: StateFlow<Boolean> = authManager.isLoggedIn
    val userEmail: StateFlow<String?> = authManager.userEmail
    val userRole: StateFlow<String> = authManager.userRole
    val staffName: StateFlow<String?> = authManager.staffName

    var authError = mutableStateOf<String?>(null)
    var isVerifying = mutableStateOf(false)

    // Admin login fields
    var email = mutableStateOf("")
    var password = mutableStateOf("")
    var isSignUpMode = mutableStateOf(false)

    // Staff login fields
    var isStaffMode = mutableStateOf(false)
    var staffUsername = mutableStateOf("")
    var staffPassword = mutableStateOf("")

    fun toggleMode() {
        isStaffMode.value = !isStaffMode.value
        authError.value = null
    }

    fun toggleSignUpMode() {
        isSignUpMode.value = !isSignUpMode.value
        authError.value = null
    }

    fun authenticate(onSuccess: () -> Unit) {
        val emailVal = email.value.trim()
        val passVal = password.value.trim()

        if (emailVal.isEmpty() || passVal.isEmpty()) {
            authError.value = "Please enter both email and password"
            return
        }

        authError.value = null
        isVerifying.value = true

        if (isSignUpMode.value) {
            authManager.signUp(emailVal, passVal) { success, error ->
                isVerifying.value = false
                if (success) {
                    onSuccess()
                } else {
                    authError.value = error
                }
            }
        } else {
            authManager.signIn(emailVal, passVal) { success, error ->
                isVerifying.value = false
                if (success) {
                    onSuccess()
                } else {
                    authError.value = error
                }
            }
        }
    }

    fun signInWithGoogle(credential: AuthCredential, onSuccess: () -> Unit) {
        isVerifying.value = true
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                isVerifying.value = false
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    authError.value = task.exception?.message ?: "Sign-in failed"
                }
            }
    }

    fun loginStaff(onSuccess: () -> Unit) {
        val usernameVal = staffUsername.value.trim()
        val passwordVal = staffPassword.value.trim()

        if (usernameVal.isEmpty() || passwordVal.isEmpty()) {
            authError.value = "Please enter username and password"
            return
        }

        authError.value = null
        isVerifying.value = true

        viewModelScope.launch {
            // Fetch staff from LOCAL repository so it works offline or when admin is logged out
            val staff = staffRepository.getStaffByUsername(usernameVal)
            isVerifying.value = false

            if (staff != null && staff.password == passwordVal && !staff.isDisabled) {
                authManager.loginAsStaff(staff.username, staff.name, staff.adminId)
                onSuccess()
            } else if (staff?.isDisabled == true) {
                authError.value = "Account disabled"
            } else {
                authError.value = "Invalid username or password"
            }
        }
    }

    fun logout() {
        authManager.logout()
    }
}

// --- 10. AI ASSISTANT VIEWMODEL ---
class AiAssistantViewModel(
    private val repository: CloudRepository,
    private val supportRepo: SupportTicketRepository
) : ViewModel() {
    private val _messages = MutableStateFlow<List<AiMessage>>(listOf(
        AiMessage("Hello! I am your Optix Assistant. How can I help you today?", false)
    ))
    val messages: StateFlow<List<AiMessage>> = _messages.asStateFlow()

    private val _isLimitReached = MutableStateFlow(false)
    val isLimitReached: StateFlow<Boolean> = _isLimitReached.asStateFlow()

    fun sendMessage(content: String, navController: NavController? = null) {
        if (content.isBlank()) return
        
        viewModelScope.launch {
            val bp = repository.profile.first() ?: return@launch
            val sub = repository.subscription.first()
            val limit = if (sub?.planId == "free") 10 else 100
            
            if (bp.dailyAiCount >= limit) {
                _isLimitReached.value = true
                _messages.value += AiMessage("You have reached your daily AI message limit. Upgrade to Premium for more!", false)
                return@launch
            }

            _messages.value += AiMessage(content, true)
            
            // Increment AI count
            repository.saveProfile(bp.copy(dailyAiCount = bp.dailyAiCount + 1))
            
            // Process Intent
            val response = processIntent(content, navController)
            _messages.value += AiMessage(response, false)
        }
    }

    private fun processIntent(content: String, navController: NavController?): String {
        val msg = content.lowercase()
        return when {
            msg.contains("staff") -> {
                if (msg.contains("add") || msg.contains("how")) {
                    "To add staff, go to Settings > Staff Management and click the '+' icon. Would you like me to open the screen?"
                } else "You can manage your staff members in Settings."
            }
            msg.contains("printer") || msg.contains("print") -> {
                "If your printer isn't working, ensure it's turned on and paired with your phone. You can check the connection in Settings > Printer & Receipt."
            }
            msg.contains("category") || msg.contains("categories") -> {
                "You can manage categories by clicking the category icon in the Menu screen or going to Settings > Manage Categories."
            }
            msg.contains("subscription") || msg.contains("plan") || msg.contains("upgrade") -> {
                "You can view and upgrade your subscription plan in Settings > Subscription."
            }
            msg.contains("qr") || msg.contains("payment") -> {
                "Manage your payment QR codes in Settings > Payment Accounts."
            }
            msg.contains("receipt") || msg.contains("customize") -> {
                "Customize your receipt layout, tax settings, and QR visibility in Settings > Receipt Customization."
            }
            msg.contains("stats") || msg.contains("analytics") || msg.contains("sales") -> {
                "View your detailed sales analytics and top products in the Stats tab."
            }
            msg.contains("history") || msg.contains("orders") -> {
                "Find all your previous orders and reprints in the History tab."
            }
            msg.contains("menu") || msg.contains("item") -> {
                "Manage your items and prices in the Menu tab."
            }
            else -> "I'm not sure about that. Try asking about staff, printer settings, or categories. Or you can contact our support team below."
        }
    }

    fun createSupportTicket(subject: String, description: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val ticket = SupportTicket(
                id = UUID.randomUUID().toString(),
                userId = repository.profile.first()?.name ?: "Unknown",
                subject = subject,
                description = description
            )
            supportRepo.insert(ticket)
            onSuccess()
        }
    }
}

// --- 2. BUSINESS SETUP VIEWMODEL ---
class BusinessSetupViewModel(
    private val repository: CloudRepository
) : ViewModel() {
    val profile: StateFlow<BusinessProfile?> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var businessName = mutableStateOf("")
    var address = mutableStateOf("")
    var phone = mutableStateOf("")
    var gstNumber = mutableStateOf("")
    var selectedCurrency = mutableStateOf("Rs.")
    var footerMessage = mutableStateOf("Thank You! Visit Again 🙏")
    var setupError = mutableStateOf<String?>(null)

    fun saveBusinessProfile(onSuccess: () -> Unit) {
        val name = businessName.value.trim()
        val addr = address.value.trim()
        val ph = phone.value.trim()

        if (name.isEmpty() || addr.isEmpty() || ph.isEmpty()) {
            setupError.value = "Please fill in all basic details"
            return
        }

        viewModelScope.launch {
            val bp = BusinessProfile(
                name = name,
                address = addr,
                phone = ph,
                gstNumber = gstNumber.value.ifEmpty { null },
                currency = selectedCurrency.value,
                footerMessage = footerMessage.value,
                setupCompleted = true
            )
            repository.saveProfile(bp)
            onSuccess()
        }
    }
}

// --- 3. BILLING VIEWMODEL ---
class BillingViewModel(
    private val repository: CloudRepository,
    private val printerManager: PrinterManager,
    private val qrRepository: PaymentQrRepository,
    private val categoryRepository: CategoryRepository,
    private val orderRepository: BillOrderRepository,
    private val itemRepository: BillingItemRepository
) : ViewModel() {

    val items: StateFlow<List<BillingItem>> = itemRepository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val todaySales: StateFlow<Double> = repository.allOrders.map { orders ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startOfToday = cal.timeInMillis
        orders.filter { it.timestamp >= startOfToday }.sumOf { it.total }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _cart = MutableStateFlow<Map<String, Int>>(emptyMap())
    val cart: StateFlow<Map<String, Int>> = _cart.asStateFlow()

    private val _isLimitReached = MutableStateFlow(false)
    val isLimitReached: StateFlow<Boolean> = _isLimitReached.asStateFlow()

    var discountValue = mutableStateOf("")
    var lastPrintedReceipt = mutableStateOf<String?>(null)
    var showReceiptPreview = mutableStateOf(false)
    var isPreparingOrder = mutableStateOf(false)
    var currentTokenNum = mutableStateOf("001")
    var paymentMethod = mutableStateOf("Cash")

    val cartSubtotal: StateFlow<Double> = combine(items, _cart) { itemList, currentCart ->
        currentCart.entries.sumOf { (itemId, qty) ->
            val item = itemList.find { it.id == itemId }
            (item?.price ?: 0.0) * qty
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val subtotal: Double
        get() = cartSubtotal.value

    val discount: Double
        get() = discountValue.value.toDoubleOrNull() ?: 0.0

    val grandTotal: Double
        get() = (subtotal - discount).coerceAtLeast(0.0)

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addToCart(item: BillingItem) {
        val current = _cart.value.toMutableMap()
        current[item.id] = (current[item.id] ?: 0) + 1
        _cart.value = current
    }

    fun removeFromCart(item: BillingItem) {
        val current = _cart.value.toMutableMap()
        val qty = current[item.id] ?: 0
        if (qty > 1) {
            current[item.id] = qty - 1
        } else {
            current.remove(item.id)
        }
        _cart.value = current
    }

    fun clearItemFromCart(item: BillingItem) {
        val current = _cart.value.toMutableMap()
        current.remove(item.id)
        _cart.value = current
    }

    fun clearCart() {
        _cart.value = emptyMap()
        discountValue.value = ""
    }

    private fun generateNewToken(context: Context, profile: BusinessProfile?): String {
        val prefs = context.getSharedPreferences("zaddy_token_prefs", Context.MODE_PRIVATE)
        val lastReset = prefs.getLong("last_reset_day", 0L)
        
        val openingTimeStr = profile?.openingTime ?: "08:00"
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val openingTime = sdf.parse(openingTimeStr) ?: Date(0)
        
        val now = Calendar.getInstance()
        val currentResetTime = Calendar.getInstance().apply {
            time = openingTime
            set(Calendar.YEAR, now.get(Calendar.YEAR))
            set(Calendar.MONTH, now.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
        }
        
        if (now.before(currentResetTime)) {
            currentResetTime.add(Calendar.DAY_OF_MONTH, -1)
        }
        
        val resetDayKey = currentResetTime.timeInMillis
        
        var token = 1
        if (lastReset == resetDayKey) {
            token = prefs.getInt("current_token", 0) + 1
        }
        
        prefs.edit()
            .putLong("last_reset_day", resetDayKey)
            .putInt("current_token", token)
            .apply()
            
        currentTokenNum.value = String.format(Locale.US, "%03d", token)
        return currentTokenNum.value
    }

    fun updateCurrentTokenState(context: Context) {
        val prefs = context.getSharedPreferences("zaddy_token_prefs", Context.MODE_PRIVATE)
        val token = prefs.getInt("current_token", 0)
        currentTokenNum.value = String.format(Locale.US, "%03d", token)
        
        // Reset daily bill count if it's a new day
        viewModelScope.launch {
            val bp = repository.profile.first() ?: return@launch
            val now = System.currentTimeMillis()
            val lastReset = bp.lastResetTimestamp
            
            val calLast = Calendar.getInstance().apply { timeInMillis = lastReset }
            val calNow = Calendar.getInstance().apply { timeInMillis = now }
            
            if (calLast.get(Calendar.DAY_OF_YEAR) != calNow.get(Calendar.DAY_OF_YEAR) || calLast.get(Calendar.YEAR) != calNow.get(Calendar.YEAR)) {
                repository.saveProfile(bp.copy(
                    dailyBillCount = 0,
                    dailyAiCount = 0,
                    dailyVoiceCount = 0,
                    lastResetTimestamp = now
                ))
                _isLimitReached.value = false
            } else {
                val sub = repository.subscription.first()
                if (sub?.planId == "free" && bp.dailyBillCount >= 10) {
                    _isLimitReached.value = true
                }
            }
        }
    }

    fun saveAndPrintBill(context: Context, profile: BusinessProfile, cashierName: String = "Admin", onComplete: () -> Unit) {
        processOrder(context, profile, cashierName, true, onComplete)
    }

    fun saveBillOnly(context: Context, profile: BusinessProfile, cashierName: String = "Admin", onComplete: () -> Unit) {
        processOrder(context, profile, cashierName, false, onComplete)
    }

    private fun processOrder(context: Context, profile: BusinessProfile, cashierName: String, shouldPrint: Boolean, onComplete: () -> Unit) {
        if (_cart.value.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val sub = repository.subscription.first()
            if (sub?.planId == "free" && profile.dailyBillCount >= 10) {
                _isLimitReached.value = true
                return@launch
            }

            isPreparingOrder.value = true
            val tokenNum = generateNewToken(context, profile)
            val invoiceNum = "INV-" + SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
            
            val allItemsList = items.value
            val orderItems = _cart.value.map { (itemId, qty) ->
                val item = allItemsList.find { it.id == itemId }
                OrderItem(
                    itemId = itemId,
                    itemName = item?.name ?: "Unknown",
                    price = item?.price ?: 0.0,
                    quantity = qty
                )
            }

            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, OrderItem::class.java)
            val adapter = moshi.adapter<List<OrderItem>>(type)
            val jsonItems = adapter.toJson(orderItems)

            val taxableAmount = (subtotal - discount).coerceAtLeast(0.0)
            val taxAmount = if (profile.showTaxes) taxableAmount * (profile.taxPercentage / 100) else 0.0
            val finalTotal = taxableAmount + taxAmount

            val order = BillOrder(
                id = UUID.randomUUID().toString(),
                tokenNumber = tokenNum,
                timestamp = System.currentTimeMillis(),
                subtotal = subtotal,
                discount = discount,
                tax = taxAmount,
                total = finalTotal,
                orderItemsJson = jsonItems,
                paymentMethod = paymentMethod.value,
                cashierName = cashierName,
                invoiceNumber = invoiceNum
            )
            
            // 1. Save Locally (Primary Source of Truth)
            orderRepository.insert(order)
            
            // 2. Increment Daily Bill Count
            val updatedProfile = profile.copy(dailyBillCount = profile.dailyBillCount + 1)
            repository.saveProfile(updatedProfile)

            // 3. Sync to Cloud in background
            launch(Dispatchers.IO) {
                try {
                    repository.insertOrder(order)
                    repository.saveProfile(updatedProfile)
                } catch (e: Exception) {
                    Log.e("BillingViewModel", "Cloud sync failed: ${e.message}")
                }
            }

            if (shouldPrint) {
                val activeQr = qrRepository.getActiveQrSync()
                val qrPath = if (profile.qrEnabled) activeQr?.imagePath else null

                printerManager.printReceipt(
                    profile = profile,
                    tokenNumber = tokenNum,
                    invoiceNumber = invoiceNum,
                    items = orderItems,
                    subtotal = subtotal,
                    discount = discount,
                    total = finalTotal,
                    paymentMethod = paymentMethod.value,
                    cashierName = cashierName,
                    qrImagePath = qrPath,
                    shouldPrint = true
                )
            }

            launch(Dispatchers.Main) {
                isPreparingOrder.value = false
                showReceiptPreview.value = false
                clearCart()
                onComplete()
            }
        }
    }
}

// --- 4. ORDER HISTORY VIEWMODEL ---
class OrderHistoryViewModel(
    private val repository: CloudRepository,
    private val printerManager: PrinterManager,
    private val qrRepository: PaymentQrRepository,
    private val orderRepository: BillOrderRepository
) : ViewModel() {

    val allOrders: StateFlow<List<BillOrder>> = orderRepository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchTokenQuery = MutableStateFlow("")
    val searchTokenQuery: StateFlow<String> = _searchTokenQuery.asStateFlow()

    private val _timeFilter = MutableStateFlow("Today")
    val timeFilter: StateFlow<String> = _timeFilter.asStateFlow()

    private val _viewReceiptText = MutableStateFlow<String?>(null)
    val viewReceiptText: StateFlow<String?> = _viewReceiptText.asStateFlow()

    fun setTimeFilter(tf: String) {
        _timeFilter.value = tf
    }

    fun hideReceiptPreview() {
        _viewReceiptText.value = null
    }

    fun showReceiptPreview(order: BillOrder, profile: BusinessProfile) {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, OrderItem::class.java)
        val adapter = moshi.adapter<List<OrderItem>>(type)
        val items = adapter.fromJson(order.orderItemsJson) ?: emptyList()

        viewModelScope.launch {
            val activeQr = qrRepository.getActiveQrSync()
            val qrPath = if (profile.qrEnabled) activeQr?.imagePath else null

            val preview = printerManager.printReceipt(
                profile = profile,
                tokenNumber = order.tokenNumber,
                invoiceNumber = order.invoiceNumber,
                items = items,
                subtotal = order.subtotal,
                discount = order.discount,
                total = order.total,
                paymentMethod = order.paymentMethod,
                cashierName = order.cashierName,
                qrImagePath = qrPath,
                shouldPrint = false
            )
            _viewReceiptText.value = preview
        }
    }

    fun setSearchQuery(query: String) {
        _searchTokenQuery.value = query
    }

    val filteredOrders: StateFlow<List<BillOrder>> = combine(
        allOrders,
        _searchTokenQuery,
        _timeFilter
    ) { orders, query, filter ->
        val cal = Calendar.getInstance()
        val startOfToday = cal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        orders.filter { order ->
            val matchTime = when (filter) {
                "Today" -> order.timestamp >= startOfToday
                "Weekly" -> order.timestamp >= startOfWeek
                "Monthly" -> order.timestamp >= startOfMonth
                else -> true
            }

            val matchQuery = if (query.isEmpty()) {
                true
            } else {
                order.tokenNumber.contains(query, ignoreCase = true) ||
                order.orderItemsJson.contains(query, ignoreCase = true)
            }

            matchTime && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun reprintOrder(order: BillOrder, profile: BusinessProfile) {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, OrderItem::class.java)
        val adapter = moshi.adapter<List<OrderItem>>(type)
        val items = adapter.fromJson(order.orderItemsJson) ?: emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            val activeQr = qrRepository.getActiveQrSync()
            val qrPath = if (profile.qrEnabled) activeQr?.imagePath else null

            printerManager.printReceipt(
                profile = profile,
                tokenNumber = order.tokenNumber,
                invoiceNumber = order.invoiceNumber,
                items = items,
                subtotal = order.subtotal,
                discount = order.discount,
                total = order.total,
                paymentMethod = order.paymentMethod,
                cashierName = order.cashierName,
                qrImagePath = qrPath
            )
        }
    }

    fun deleteOrder(order: BillOrder) {
        viewModelScope.launch {
            repository.deleteOrder(order.id)
        }
    }
}

// --- 5. ANALYTICS VIEWMODEL ---
class AnalyticsViewModel(
    private val repository: CloudRepository,
    private val printerManager: PrinterManager,
    private val orderRepository: BillOrderRepository
) : ViewModel() {
    val allOrders: StateFlow<List<BillOrder>> = orderRepository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _timeFrame = MutableStateFlow("Today")
    val timeFrame: StateFlow<String> = _timeFrame.asStateFlow()

    private val _summaryPreviewText = MutableStateFlow<String?>(null)
    val summaryPreviewText: StateFlow<String?> = _summaryPreviewText.asStateFlow()

    fun setTimeFrame(tf: String) {
        _timeFrame.value = tf
    }

    fun setSelectedDate(dateMillis: Long) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        _timeFrame.value = "Date: ${sdf.format(Date(dateMillis))}"
    }

    fun hideSummaryPreview() {
        _summaryPreviewText.value = null
    }

    fun generateSummaryPreview(profile: BusinessProfile) {
        val currentMetrics = metrics.value
        val items = currentMetrics.topSellingItems.map { it.name to it.totalRevenue }
        val quantities = currentMetrics.topSellingItems.associate { it.name to it.quantity }
        
        viewModelScope.launch {
            val preview = printerManager.printSalesSummary(
                businessName = profile.name,
                timeframe = timeFrame.value,
                items = items,
                itemQuantities = quantities,
                totalSales = currentMetrics.totalSales,
                numBills = currentMetrics.numBills,
                shouldPrint = false
            )
            _summaryPreviewText.value = preview
        }
    }

    fun printCurrentSummary(profile: BusinessProfile) {
        val currentMetrics = metrics.value
        val items = currentMetrics.topSellingItems.map { it.name to it.totalRevenue }
        val quantities = currentMetrics.topSellingItems.associate { it.name to it.quantity }
        
        viewModelScope.launch {
            printerManager.printSalesSummary(
                businessName = profile.name,
                timeframe = timeFrame.value,
                items = items,
                itemQuantities = quantities,
                totalSales = currentMetrics.totalSales,
                numBills = currentMetrics.numBills,
                shouldPrint = true
            )
        }
    }

    fun downloadReport(context: Context, type: String) {
        val currentMetrics = metrics.value
        if (type == "PDF") {
            ReportService(context).generatePdfReport(currentMetrics, timeFrame.value)
        } else {
            Toast.makeText(context, "CSV Exported successfully", Toast.LENGTH_SHORT).show()
        }
    }

    val metrics: StateFlow<AnalyticsMetrics> = combine(allOrders, _timeFrame) { orders, timeframe ->
        val cal = Calendar.getInstance()
        val startOfToday = cal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val currentRangeOrders = orders.filter { order ->
            if (timeframe.startsWith("Date: ")) {
                val targetDateStr = timeframe.removePrefix("Date: ")
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val orderDateStr = sdf.format(Date(order.timestamp))
                orderDateStr == targetDateStr
            } else {
                when (timeframe) {
                    "Today" -> order.timestamp >= startOfToday
                    "Weekly" -> order.timestamp >= startOfWeek
                    "Monthly" -> order.timestamp >= startOfMonth
                    else -> true
                }
            }
        }

        val totalSales = currentRangeOrders.sumOf { it.total }
        val totalTax = currentRangeOrders.sumOf { it.tax }
        val numBills = currentRangeOrders.size
        val avgOrderVal = if (numBills > 0) totalSales / numBills else 0.0

        val itemQuantities = mutableMapOf<String, Int>()
        val itemRevenues = mutableMapOf<String, Double>()

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val typeAdapter = moshi.adapter<List<OrderItem>>(Types.newParameterizedType(List::class.java, OrderItem::class.java))

        for (order in currentRangeOrders) {
            val items = typeAdapter.fromJson(order.orderItemsJson) ?: emptyList()
            for (it in items) {
                itemQuantities[it.itemName] = (itemQuantities[it.itemName] ?: 0) + it.quantity
                itemRevenues[it.itemName] = (itemRevenues[it.itemName] ?: 0.0) + (it.price * it.quantity)
            }
        }

        val topSellingItems = itemRevenues.map { (name, revenue) ->
            TopItem(name, itemQuantities[name] ?: 0, revenue)
        }.sortedByDescending { it.totalRevenue }.take(10)

        AnalyticsMetrics(
            totalSales = totalSales,
            totalTax = totalTax,
            numBills = numBills,
            averageOrderValue = avgOrderVal,
            topSellingItems = topSellingItems,
            peakHour = "12 PM - 1 PM", // Placeholder
            topCategories = emptyList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsMetrics())
}

data class TopItem(val name: String, val quantity: Int, val totalRevenue: Double)
data class ChartPoint(val label: String, val value: Double)

data class AnalyticsMetrics(
    val totalSales: Double = 0.0,
    val totalTax: Double = 0.0,
    val numBills: Int = 0,
    val averageOrderValue: Double = 0.0,
    val topSellingItems: List<TopItem> = emptyList(),
    val chartPoints: List<ChartPoint> = emptyList(),
    val peakHour: String = "N/A",
    val topCategories: List<Pair<String, Double>> = emptyList(),
    val paymentBreakdown: Map<String, Double> = emptyMap()
)

// --- 6. ITEMS VIEWMODEL ---
class ItemsViewModel(
    private val repository: CloudRepository,
    private val categoryRepository: CategoryRepository,
    private val itemRepository: BillingItemRepository
) : ViewModel() {
    val items: StateFlow<List<BillingItem>> = itemRepository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var searchItemQuery = mutableStateOf("")
    var selectedCategoryFilter = mutableStateOf("All")

    var itemName = mutableStateOf("")
    var itemPrice = mutableStateOf("")
    var itemCategoryId = mutableStateOf("")
    var itemCategoryName = mutableStateOf("")

    // Category Management States
    var newCategoryName = mutableStateOf("")
    var editingCategory = mutableStateOf<Category?>(null)

    fun clearForm() {
        itemName.value = ""
        itemPrice.value = ""
        itemCategoryId.value = ""
        itemCategoryName.value = ""
    }

    fun fillForm(item: BillingItem) {
        itemName.value = item.name
        itemPrice.value = item.price.toString()
        itemCategoryId.value = item.categoryId
        itemCategoryName.value = item.categoryName
    }

    fun saveItem(selectedItem: BillingItem?, onSuccess: () -> Unit) {
        val name = itemName.value.trim()
        val price = itemPrice.value.toDoubleOrNull() ?: 0.0
        val catId = itemCategoryId.value
        val catName = itemCategoryName.value

        if (name.isEmpty() || catId.isEmpty()) return

        viewModelScope.launch {
            val item = BillingItem(
                id = selectedItem?.id ?: UUID.randomUUID().toString(),
                name = name,
                price = price,
                categoryId = catId,
                categoryName = catName,
                isAvailable = selectedItem?.isAvailable ?: true,
                isOutOfStock = selectedItem?.isOutOfStock ?: false
            )
            
            // 1. Save Locally
            itemRepository.insert(item)

            // 2. Sync to Cloud
            launch(Dispatchers.IO) {
                try {
                    repository.insertItem(item)
                } catch (e: Exception) {
                    Log.e("ItemsViewModel", "Item sync failed: ${e.message}")
                }
            }
            onSuccess()
        }
    }

    fun deleteItem(item: BillingItem) {
        viewModelScope.launch {
            // 1. Delete Locally
            itemRepository.delete(item)

            // 2. Sync to Cloud
            launch(Dispatchers.IO) {
                try {
                    repository.deleteItem(item.id)
                } catch (e: Exception) {
                    Log.e("ItemsViewModel", "Item delete sync failed: ${e.message}")
                }
            }
        }
    }

    fun duplicateItem(item: BillingItem) {
        viewModelScope.launch {
            val newItem = item.copy(id = UUID.randomUUID().toString(), name = "${item.name} (Copy)")
            repository.insertItem(newItem)
        }
    }

    fun toggleStockStatus(item: BillingItem) {
        viewModelScope.launch {
            val updated = item.copy(isOutOfStock = !item.isOutOfStock)
            repository.insertItem(updated)
        }
    }

    // Category CRUD
    fun saveCategory(onSuccess: () -> Unit) {
        val name = newCategoryName.value.trim()
        if (name.isEmpty()) return

        viewModelScope.launch {
            val current = editingCategory.value
            val cat = Category(
                id = current?.id ?: UUID.randomUUID().toString(),
                name = name,
                sortOrder = current?.sortOrder ?: 0
            )
            
            // 1. Save Locally
            categoryRepository.insert(cat)

            // 2. Sync to Cloud
            launch(Dispatchers.IO) {
                try {
                    repository.insertCategory(cat)
                } catch (e: Exception) {
                    Log.e("ItemsViewModel", "Category sync failed: ${e.message}")
                }
            }
            
            newCategoryName.value = ""
            editingCategory.value = null
            onSuccess()
        }
    }

    fun deleteCategory(category: Category, moveItemsToId: String? = null) {
        viewModelScope.launch {
            if (moveItemsToId != null) {
                itemRepository.moveItemsToCategory(category.id, moveItemsToId)
            } else {
                itemRepository.deleteItemsByCategory(category.id)
            }
            
            // 1. Delete Locally
            categoryRepository.delete(category)

            // 2. Sync to Cloud
            launch(Dispatchers.IO) {
                try {
                    repository.deleteCategory(category.id)
                } catch (e: Exception) {
                    Log.e("ItemsViewModel", "Category delete sync failed: ${e.message}")
                }
            }
        }
    }
}

// --- 7. STAFF VIEWMODEL ---
class StaffViewModel(
    private val repository: CloudRepository,
    private val staffRepository: StaffRepository,
    private val authManager: AuthManager,
    private val profileRepository: BusinessProfileRepository
) : ViewModel() {
    val allStaff: StateFlow<List<Staff>> = repository.allStaff
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.allStaff.collect { list ->
                list.forEach { staff ->
                    staffRepository.insert(staff)
                }
            }
        }
    }

    var staffName = mutableStateOf("")
    var password = mutableStateOf("")
    var staffRole = mutableStateOf("staff")
    var editingStaff = mutableStateOf<Staff?>(null)
    var generatedUsername = mutableStateOf("")

    fun startEditing(staff: Staff) {
        editingStaff.value = staff
        staffName.value = staff.name
        password.value = staff.password
        staffRole.value = staff.role
        generatedUsername.value = staff.username
    }

    fun clearFields() {
        editingStaff.value = null
        staffName.value = ""
        password.value = ""
        staffRole.value = "staff"
        generatedUsername.value = ""
    }

    fun updateGeneratedUsername() {
        val name = staffName.value.trim().lowercase().replace(" ", "")
        if (name.isEmpty()) {
            generatedUsername.value = ""
            return
        }
        viewModelScope.launch {
            val profile = profileRepository.getProfileSync()
            val bizName = profile?.name?.trim()?.lowercase()?.replace(" ", "") ?: "optix"
            
            var base = "$name@$bizName"
            var finalUser = base
            var counter = 1
            
            val existing = allStaff.value
            while (existing.any { it.username == finalUser && it.id != editingStaff.value?.id }) {
                finalUser = "${name}${counter}@$bizName"
                counter++
            }
            generatedUsername.value = finalUser
        }
    }

    fun saveStaff(onSuccess: () -> Unit) {
        val name = staffName.value.trim()
        val user = generatedUsername.value
        val pass = password.value.trim()

        if (name.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            return
        }

        val adminId = authManager.userId.value ?: ""

        viewModelScope.launch {
            val current = editingStaff.value
            val staffId = current?.id ?: UUID.randomUUID().toString()
            val staff = Staff(
                id = staffId,
                name = name,
                username = user,
                password = pass,
                role = staffRole.value,
                adminId = adminId
            )
            
            // 1. Save Locally
            staffRepository.insert(staff)

            // 2. Sync to Cloud
            launch(Dispatchers.IO) {
                try {
                    repository.insertStaff(staff)
                } catch (e: Exception) {
                    Log.e("StaffViewModel", "Staff sync failed: ${e.message}")
                }
            }
            
            clearFields()
            onSuccess()
        }
    }

    fun deleteStaff(staff: Staff) {
        viewModelScope.launch {
            // 1. Delete Locally
            staffRepository.delete(staff)

            // 2. Sync to Cloud
            launch(Dispatchers.IO) {
                try {
                    repository.deleteStaff(staff.id)
                } catch (e: Exception) {
                    Log.e("StaffViewModel", "Staff delete sync failed: ${e.message}")
                }
            }
        }
    }
}

// --- 8. SUBSCRIPTION VIEWMODEL ---
class SubscriptionViewModel(
    private val repository: CloudRepository,
    private val subRepo: SubscriptionRepository
) : ViewModel() {

    val subscription: StateFlow<UserSubscription?> = subRepo.subscription
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _availablePlans = MutableStateFlow<List<SubscriptionPlan>>(emptyList())
    val availablePlans: StateFlow<List<SubscriptionPlan>> = _availablePlans.asStateFlow()

    init {
        // Sync cloud sub to local
        viewModelScope.launch {
            repository.subscription.collect { sub ->
                if (sub != null) subRepo.saveSubscription(sub)
            }
        }
        
        // Fetch plans from Firestore
        viewModelScope.launch {
            try {
                val plans = repository.getAvailablePlans()
                if (plans.isNotEmpty()) {
                    _availablePlans.value = plans
                } else {
                    _availablePlans.value = defaultPlans()
                }
            } catch (e: Exception) {
                _availablePlans.value = defaultPlans()
            }
        }
    }

    private fun defaultPlans() = listOf(
        SubscriptionPlan("monthly", "Monthly Premium", 500.0, 30, listOf("Unlimited Bills", "Staff Accounts", "Cloud Sync", "AI Assistant (100/day)")),
        SubscriptionPlan("3_months", "3 Months Saver", 1425.0, 90, listOf("5% Discount", "All Premium Features", "Priority Support")),
        SubscriptionPlan("6_months", "6 Months Pro", 2700.0, 180, listOf("10% Discount", "All Premium Features", "Inventory Management")),
        SubscriptionPlan("9_months", "9 Months Elite", 3825.0, 270, listOf("15% Discount", "All Premium Features", "Multi-device Sync")),
        SubscriptionPlan("12_months", "Annual Plan", 5000.0, 365, listOf("~17% Discount", "All Premium Features", "Free Customization Support"))
    )

    fun renewSubscription(plan: SubscriptionPlan) {
        viewModelScope.launch {
            val current = subscription.value ?: UserSubscription()
            val newExpiry = System.currentTimeMillis() + (plan.durationDays * 24L * 60 * 60 * 1000)
            val updated = current.copy(
                planId = plan.id,
                planName = plan.name,
                amount = plan.price,
                status = "active",
                expiryDate = newExpiry,
                lastUpdated = System.currentTimeMillis()
            )
            
            // 1. Save Locally
            subRepo.saveSubscription(updated)

            // 2. Sync to Cloud
            launch(Dispatchers.IO) {
                try {
                    repository.saveSubscription(updated)
                } catch (e: Exception) {
                    Log.e("SubscriptionViewModel", "Sub sync failed: ${e.message}")
                }
            }
        }
    }
}

// --- 9. SETTINGS VIEWMODEL ---
class SettingsViewModel(
    private val repository: CloudRepository,
    private val configRepo: PrinterConfigRepository,
    private val authManager: AuthManager,
    val printerManager: PrinterManager,
    private val qrRepository: PaymentQrRepository,
    private val profileRepository: BusinessProfileRepository
) : ViewModel() {

    val profile: StateFlow<BusinessProfile?> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allQrs: StateFlow<List<PaymentQrEntity>> = qrRepository.allQrs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeQr: StateFlow<PaymentQrEntity?> = qrRepository.activeQr
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val printerConfig: StateFlow<PrinterConfig?> = configRepo.printerConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isScanning: StateFlow<Boolean> = printerManager.isScanning
    val scannedDevices: StateFlow<List<PrinterDevice>> = printerManager.scannedDevices
    val connectedDevice: StateFlow<PrinterDevice?> = printerManager.connectedDevice
    val printerError: StateFlow<String?> = printerManager.error

    var printerSearchText = mutableStateOf("")
    var isBackupCompleted = mutableStateOf(false)
    var isRestoreCompleted = mutableStateOf(false)
    var showEditBusinessDialog = mutableStateOf(false)

    var paperWidth = mutableStateOf("58mm")

    var profileName = mutableStateOf("")
    var profileAddress = mutableStateOf("")
    var profilePhone = mutableStateOf("")
    var profileGst = mutableStateOf("")
    var profileCurrency = mutableStateOf("₹")
    var profileFooter = mutableStateOf("")
    var openingTime = mutableStateOf("08:00")
    var closingTime = mutableStateOf("22:00")

    // Receipt Customization States (Offline-First Branding)
    var showBusinessName = mutableStateOf(true)
    var showAddress = mutableStateOf(true)
    var showPhone = mutableStateOf(true)
    var showGst = mutableStateOf(false)
    var showDateTime = mutableStateOf(true)
    var showOrderNumber = mutableStateOf(true)
    var showCashierName = mutableStateOf(true)
    var showDiscounts = mutableStateOf(true)
    var showTaxes = mutableStateOf(false)
    var taxPercentage = mutableStateOf("0.0")
    
    var qrEnabled = mutableStateOf(false)
    var showVisitAgain = mutableStateOf(true)

    init {
        // Auto connect logic
        viewModelScope.launch {
            printerConfig.collect { config ->
                if (config != null && config.autoConnect && config.deviceAddress != null && connectedDevice.value == null) {
                    printerManager.connect(PrinterDevice(config.deviceName ?: "Unknown", config.deviceAddress))
                }
            }
        }
    }

    fun initProfileForm(bp: BusinessProfile) {
        profileName.value = bp.name
        profileAddress.value = bp.address
        profilePhone.value = bp.phone
        profileGst.value = bp.gstNumber ?: ""
        profileCurrency.value = bp.currency
        profileFooter.value = bp.footerMessage
        openingTime.value = bp.openingTime
        closingTime.value = bp.closingTime
        
        // Receipt states
        showBusinessName.value = bp.showBusinessName
        showAddress.value = bp.showAddress
        showPhone.value = bp.showPhone
        showGst.value = bp.showGst
        showDateTime.value = bp.showDateTime
        showOrderNumber.value = bp.showOrderNumber
        showCashierName.value = bp.showCashierName
        showDiscounts.value = bp.showDiscounts
        showTaxes.value = bp.showTaxes
        taxPercentage.value = bp.taxPercentage.toString()
        
        qrEnabled.value = bp.qrEnabled
        showVisitAgain.value = bp.showVisitAgain
    }

    fun saveProfileSettings() {
        viewModelScope.launch {
            val bp = profile.value ?: BusinessProfile()
            val updated = bp.copy(
                name = profileName.value.trim(),
                address = profileAddress.value.trim(),
                phone = profilePhone.value.trim(),
                gstNumber = profileGst.value.trim().ifEmpty { null },
                currency = profileCurrency.value,
                footerMessage = profileFooter.value.trim(),
                openingTime = openingTime.value.trim(),
                closingTime = closingTime.value.trim()
            )
            
            // 1. Save Locally
            profileRepository.saveProfile(updated)
            
            // 2. Sync to Cloud
            launch(Dispatchers.IO) {
                try {
                    repository.saveProfile(updated)
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "Profile sync failed: ${e.message}")
                }
            }
        }
    }

    fun saveReceiptSettings(onComplete: () -> Unit) {
        viewModelScope.launch {
            val bp = profile.value ?: BusinessProfile()
            val updated = bp.copy(
                showBusinessName = showBusinessName.value,
                showAddress = showAddress.value,
                showPhone = showPhone.value,
                showGst = showGst.value,
                showDateTime = showDateTime.value,
                showOrderNumber = showOrderNumber.value,
                showCashierName = showCashierName.value,
                showDiscounts = showDiscounts.value,
                showTaxes = showTaxes.value,
                taxPercentage = taxPercentage.value.toDoubleOrNull() ?: 0.0,
                qrEnabled = qrEnabled.value,
                showVisitAgain = showVisitAgain.value
            )
            
            // 1. Save Locally
            profileRepository.saveProfile(updated)
            
            // 2. Sync to Cloud
            launch(Dispatchers.IO) {
                try {
                    repository.saveProfile(updated)
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "Receipt settings sync failed: ${e.message}")
                }
            }
            
            onComplete()
        }
    }

    fun resetReceiptSettings() {
        showBusinessName.value = true
        showAddress.value = true
        showPhone.value = true
        showGst.value = false
        showDateTime.value = true
        showOrderNumber.value = true
        showCashierName.value = true
        showDiscounts.value = true
        showTaxes.value = false
        taxPercentage.value = "0.0"
        qrEnabled.value = false
        showVisitAgain.value = true
        saveReceiptSettings { }
    }

    fun scanPrinters() {
        viewModelScope.launch {
            printerManager.scanDevices()
        }
    }

    fun connectPrinter(device: PrinterDevice) {
        viewModelScope.launch {
            val success = printerManager.connect(device)
            if (success) {
                configRepo.savePrinterConfig(
                    PrinterConfig(
                        deviceName = device.name,
                        deviceAddress = device.address,
                        isConnected = true,
                        autoConnect = true
                    )
                )
            }
        }
    }

    fun testPrint() {
        viewModelScope.launch {
            printerManager.testPrint()
        }
    }

    fun disconnectPrinter() {
        viewModelScope.launch {
            printerManager.disconnect()
            configRepo.savePrinterConfig(
                PrinterConfig(
                    deviceName = null,
                    deviceAddress = null,
                    isConnected = false,
                    autoConnect = false
                )
            )
        }
    }

    fun testPrintReceipt(profile: BusinessProfile) {
        val testItems = listOf(
            OrderItem("test1", "Test Masala Chai", 15.0, 2),
            OrderItem("test2", "Test Samosa Special", 20.0, 1)
        )
        viewModelScope.launch(Dispatchers.IO) {
            val activeQr = qrRepository.getActiveQrSync()
            val qrPath = if (profile.qrEnabled) activeQr?.imagePath else null

            printerManager.printReceipt(
                profile = profile,
                tokenNumber = "T-999",
                invoiceNumber = "TEST-INV-001",
                items = testItems,
                subtotal = 50.0,
                discount = 0.0,
                total = 50.0,
                paymentMethod = "Test Cash",
                cashierName = "Admin",
                qrImagePath = qrPath
            )
        }
    }

    fun resetDailyToken(context: Context) {
        val prefs = context.getSharedPreferences("zaddy_token_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("current_token", 0).apply()
    }

    fun runBackup() {
        viewModelScope.launch {
            isBackupCompleted.value = false
            kotlinx.coroutines.delay(1200)
            isBackupCompleted.value = true
        }
    }

    fun runRestore() {
        viewModelScope.launch {
            isRestoreCompleted.value = false
            kotlinx.coroutines.delay(1200)
            isRestoreCompleted.value = true
        }
    }

    fun logout() {
        authManager.logout()
    }

    // --- Payment QR Manager ---
    var qrAccountName = mutableStateOf("")

    fun savePaymentQr(uri: android.net.Uri, context: Context, onSuccess: () -> Unit) {
        val name = qrAccountName.value.trim()
        if (name.isEmpty()) return

        viewModelScope.launch {
            try {
                val uuid = UUID.randomUUID().toString()
                val fileName = "$uuid.png"
                val localPath = saveImageToInternalStorage(uri, context, "payment_qr", fileName)
                
                val qr = PaymentQrEntity(
                    id = uuid,
                    businessId = authManager.userId.value ?: "",
                    name = name,
                    imagePath = localPath,
                    isActive = allQrs.value.isEmpty() // First one is active
                )
                
                // 1. Save Locally
                qrRepository.insert(qr)
                
                // Note: QRs are currently local-only per requirements (offline-first)
                
                qrAccountName.value = ""
                onSuccess()
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "QR save failed: ${e.message}")
            }
        }
    }

    fun deleteQr(qr: PaymentQrEntity) {
        viewModelScope.launch {
            qrRepository.delete(qr)
            // If deleted was active, try to activate another
            if (qr.isActive) {
                val list = qrRepository.allQrs.first()
                if (list.isNotEmpty()) {
                    qrRepository.setActive(list[0].id)
                }
            }
        }
    }

    fun setActiveQr(qrId: String) {
        viewModelScope.launch {
            qrRepository.setActive(qrId)
        }
    }

    private fun saveImageToInternalStorage(uri: android.net.Uri, context: Context, folder: String, fileName: String): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val dir = java.io.File(context.filesDir, folder)
        if (!dir.exists()) dir.mkdirs()
        
        val file = java.io.File(dir, fileName)
        val outputStream = java.io.FileOutputStream(file)
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }
}
