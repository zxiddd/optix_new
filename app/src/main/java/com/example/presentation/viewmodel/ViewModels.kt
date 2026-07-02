package com.example.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ZaddyApplication
import com.example.data.entity.*
import com.example.data.repository.*
import com.example.services.AuthManager
import com.example.services.PrinterManager
import com.example.services.PrinterDevice
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- VIEWMODEL FACTORIES ---
class ViewModelFactory(private val application: ZaddyApplication) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> 
                AuthViewModel(application.authManager, application.staffRepository) as T
            modelClass.isAssignableFrom(BusinessSetupViewModel::class.java) -> 
                BusinessSetupViewModel(application.businessProfileRepository) as T
            modelClass.isAssignableFrom(BillingViewModel::class.java) -> 
                BillingViewModel(
                    application.billingItemRepository,
                    application.categoryRepository,
                    application.billOrderRepository,
                    application.printerManager
                ) as T
            modelClass.isAssignableFrom(OrderHistoryViewModel::class.java) -> 
                OrderHistoryViewModel(application.billOrderRepository, application.printerManager) as T
            modelClass.isAssignableFrom(AnalyticsViewModel::class.java) -> 
                AnalyticsViewModel(application.billOrderRepository) as T
            modelClass.isAssignableFrom(ItemsViewModel::class.java) -> 
                ItemsViewModel(application.billingItemRepository, application.categoryRepository) as T
            modelClass.isAssignableFrom(StaffViewModel::class.java) ->
                StaffViewModel(application.staffRepository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> 
                SettingsViewModel(
                    application.businessProfileRepository,
                    application.printerConfigRepository,
                    application.authManager,
                    application.printerManager
                ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// --- 1. AUTH VIEWMODEL ---
class AuthViewModel(
    private val authManager: AuthManager,
    private val staffRepository: StaffRepository
) : ViewModel() {
    val isLoggedIn: StateFlow<Boolean> = authManager.isLoggedIn
    val userMobile: StateFlow<String?> = authManager.userMobile
    val userRole: StateFlow<String> = authManager.userRole
    val staffName: StateFlow<String?> = authManager.staffName

    var mobileNumber = mutableStateOf("")
    var otpCode = mutableStateOf("")
    var isOtpSent = mutableStateOf(false)
    var authError = mutableStateOf<String?>(null)
    var isVerifying = mutableStateOf(false)

    // Staff login fields
    var isStaffMode = mutableStateOf(false)
    var staffUsername = mutableStateOf("")
    var staffPassword = mutableStateOf("")

    fun toggleMode() {
        isStaffMode.value = !isStaffMode.value
        authError.value = null
    }

    fun sendOtp() {
        val mobile = mobileNumber.value.trim()
        if (mobile.length < 10) {
            authError.value = "Enter a valid 10-digit mobile number"
            return
        }
        authError.value = null
        val success = authManager.sendOtp(mobile)
        if (success) {
            isOtpSent.value = true
        } else {
            authError.value = "Failed to send OTP. Try again."
        }
    }

    fun verifyOtp(onSuccess: () -> Unit) {
        val code = otpCode.value.trim()
        if (code.length < 4) {
            authError.value = "Enter a 4-digit verification code"
            return
        }
        authError.value = null
        isVerifying.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(600) // Aesthetic delay
            val success = authManager.verifyOtp(code)
            isVerifying.value = false
            if (success) {
                onSuccess()
            } else {
                authError.value = "Invalid OTP. Use '1234' to login."
            }
        }
    }

    fun loginStaff(onSuccess: () -> Unit) {
        val usernameVal = staffUsername.value.trim()
        val passwordVal = staffPassword.value.trim()
        if (usernameVal.isEmpty() || passwordVal.isEmpty()) {
            authError.value = "Please enter both username/mobile and password"
            return
        }
        authError.value = null
        isVerifying.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(600)
            val staff = staffRepository.getStaffByUsername(usernameVal)
            isVerifying.value = false
            if (staff == null) {
                authError.value = "Staff account not found"
            } else if (staff.password != passwordVal) {
                authError.value = "Incorrect password"
            } else if (staff.isDisabled) {
                authError.value = "This account is disabled by admin"
            } else {
                authManager.loginAsStaff(usernameVal, staff.name)
                onSuccess()
            }
        }
    }

    fun logout() {
        authManager.logout()
        // Reset states
        mobileNumber.value = ""
        otpCode.value = ""
        isOtpSent.value = false
        staffUsername.value = ""
        staffPassword.value = ""
        authError.value = null
    }
}

// --- 2. BUSINESS SETUP VIEWMODEL ---
class BusinessSetupViewModel(private val repository: BusinessProfileRepository) : ViewModel() {
    val profile: StateFlow<BusinessProfile?> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var businessName = mutableStateOf("")
    var address = mutableStateOf("")
    var phone = mutableStateOf("")
    var gstNumber = mutableStateOf("")
    var selectedCurrency = mutableStateOf("₹")
    var footerMessage = mutableStateOf("Thank You! Visit Again 🙏")
    var setupError = mutableStateOf<String?>(null)

    fun saveBusinessProfile(onSuccess: () -> Unit) {
        val name = businessName.value.trim()
        val addr = address.value.trim()
        val ph = phone.value.trim()

        if (name.isEmpty() || addr.isEmpty() || ph.isEmpty()) {
            setupError.value = "Please fill in all mandatory fields"
            return
        }

        setupError.value = null
        viewModelScope.launch {
            val businessProfile = BusinessProfile(
                name = name,
                address = addr,
                phone = ph,
                gstNumber = gstNumber.value.trim().ifEmpty { null },
                currency = selectedCurrency.value,
                footerMessage = footerMessage.value.trim()
            )
            repository.saveProfile(businessProfile)
            onSuccess()
        }
    }
}

// --- 3. BILLING VIEWMODEL ---
class BillingViewModel(
    private val itemRepo: BillingItemRepository,
    private val categoryRepo: CategoryRepository,
    private val orderRepo: BillOrderRepository,
    private val printerManager: PrinterManager
) : ViewModel() {

    val items: StateFlow<List<BillingItem>> = itemRepo.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepo.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val todaySales: StateFlow<Double> = orderRepo.allOrders
        .map { orders ->
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            orders.filter { it.timestamp >= todayStart }.sumOf { it.total }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Map of CartItem to its Quantity
    private val _cart = MutableStateFlow<Map<BillingItem, Int>>(emptyMap())
    val cart: StateFlow<Map<BillingItem, Int>> = _cart.asStateFlow()

    var discountValue = mutableStateOf("0") // discount can be entered in setting panel or during order
    var lastPrintedReceipt = mutableStateOf<String?>(null)
    var showReceiptPreview = mutableStateOf(false)
    var currentTokenNum = mutableStateOf("000")

    val subtotal: Double
        get() = _cart.value.entries.sumOf { it.key.price * it.value }

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
        current[item] = (current[item] ?: 0) + 1
        _cart.value = current
    }

    fun removeFromCart(item: BillingItem) {
        val current = _cart.value.toMutableMap()
        val qty = current[item] ?: 0
        if (qty > 1) {
            current[item] = qty - 1
        } else {
            current.remove(item)
        }
        _cart.value = current
    }

    fun deleteFromCart(item: BillingItem) {
        val current = _cart.value.toMutableMap()
        current.remove(item)
        _cart.value = current
    }

    fun clearCart() {
        _cart.value = emptyMap()
        discountValue.value = "0"
    }

    fun generateNewToken(context: Context): String {
        val prefs = context.getSharedPreferences("zaddy_token_prefs", Context.MODE_PRIVATE)
        val lastDate = prefs.getString("last_date", "")
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val today = sdf.format(Date())
        
        var currentToken = prefs.getInt("current_token", 0)
        if (lastDate != today) {
            currentToken = 1
        } else {
            currentToken += 1
        }
        
        prefs.edit()
            .putString("last_date", today)
            .putInt("current_token", currentToken)
            .apply()
            
        val tokenStr = String.format(Locale.US, "%03d", currentToken)
        currentTokenNum.value = tokenStr
        return tokenStr
    }

    fun updateCurrentTokenState(context: Context) {
        val prefs = context.getSharedPreferences("zaddy_token_prefs", Context.MODE_PRIVATE)
        val lastDate = prefs.getString("last_date", "")
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val today = sdf.format(Date())
        val token = if (lastDate != today) 0 else prefs.getInt("current_token", 0)
        currentTokenNum.value = String.format(Locale.US, "%03d", token)
    }

    fun saveAndPrintBill(
        context: Context,
        profile: BusinessProfile,
        cashierName: String = "Admin",
        paymentMethod: String = "Cash",
        onComplete: () -> Unit
    ) {
        if (_cart.value.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val tokenNum = generateNewToken(context)
            val invoiceNum = "INV-" + SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
            
            val orderItems = _cart.value.map { (item, qty) ->
                OrderItem(
                    itemId = item.id,
                    itemName = item.name,
                    price = item.price,
                    quantity = qty
                )
            }

            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, OrderItem::class.java)
            val adapter = moshi.adapter<List<OrderItem>>(type)
            val jsonItems = adapter.toJson(orderItems)

            // Save order in SQLite database
            val order = BillOrder(
                tokenNumber = tokenNum,
                subtotal = subtotal,
                discount = discount,
                total = grandTotal,
                orderItemsJson = jsonItems,
                paymentMethod = paymentMethod,
                cashierName = cashierName,
                invoiceNumber = invoiceNum
            )
            orderRepo.insert(order)

            // Generate thermal ESC/POS text
            val receiptText = printerManager.generateReceiptText(
                businessName = profile.name,
                address = profile.address,
                phone = profile.phone,
                gstNumber = profile.gstNumber,
                tokenNumber = tokenNum,
                items = orderItems,
                subtotal = subtotal,
                discount = discount,
                total = grandTotal,
                footerMessage = profile.footerMessage,
                currency = profile.currency,
                invoiceNumber = invoiceNum,
                cashierName = cashierName,
                paymentMethod = paymentMethod
            )

            launch(Dispatchers.Main) {
                lastPrintedReceipt.value = receiptText
                showReceiptPreview.value = true
                clearCart()
                onComplete()
            }
        }
    }
}

// --- 4. ORDER HISTORY VIEWMODEL ---
class OrderHistoryViewModel(
    private val repository: BillOrderRepository,
    private val printerManager: PrinterManager
) : ViewModel() {

    val allOrders: StateFlow<List<BillOrder>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchTokenQuery = MutableStateFlow("")
    val searchTokenQuery: StateFlow<String> = _searchTokenQuery.asStateFlow()

    private val _timeFilter = MutableStateFlow("Today") // "Today", "Weekly", "Monthly"
    val timeFilter: StateFlow<String> = _timeFilter.asStateFlow()

    fun setTimeFilter(filter: String) {
        _timeFilter.value = filter
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
            // Filter by time
            val matchTime = when (filter) {
                "Today" -> order.timestamp >= startOfToday
                "Weekly" -> order.timestamp >= startOfWeek
                "Monthly" -> order.timestamp >= startOfMonth
                else -> true
            }

            // Filter by search (token or items inside order)
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

        printerManager.generateReceiptText(
            businessName = profile.name,
            address = profile.address,
            phone = profile.phone,
            gstNumber = profile.gstNumber,
            tokenNumber = order.tokenNumber,
            items = items,
            subtotal = order.subtotal,
            discount = order.discount,
            total = order.total,
            footerMessage = profile.footerMessage,
            currency = profile.currency
        )
    }

    fun deleteOrder(order: BillOrder) {
        viewModelScope.launch {
            repository.delete(order)
        }
    }
}

// --- 5. ANALYTICS VIEWMODEL ---
class AnalyticsViewModel(private val repository: BillOrderRepository) : ViewModel() {
    val allOrders: StateFlow<List<BillOrder>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _timeFrame = MutableStateFlow("Today") // "Today", "Weekly", "Monthly"
    val timeFrame: StateFlow<String> = _timeFrame.asStateFlow()

    fun setTimeFrame(tf: String) {
        _timeFrame.value = tf
    }

    // Calculated metrics derived from orders
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
            when (timeframe) {
                "Today" -> order.timestamp >= startOfToday
                "Weekly" -> order.timestamp >= startOfWeek
                "Monthly" -> order.timestamp >= startOfMonth
                else -> true
            }
        }

        val totalSales = currentRangeOrders.sumOf { it.total }
        val numBills = currentRangeOrders.size
        val avgOrderVal = if (numBills > 0) totalSales / numBills else 0.0

        // Parse items to find top selling
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, OrderItem::class.java)
        val adapter = moshi.adapter<List<OrderItem>>(type)

        val itemQuantities = mutableMapOf<String, Int>()
        val itemRevenues = mutableMapOf<String, Double>()

        for (order in currentRangeOrders) {
            val orderItems = adapter.fromJson(order.orderItemsJson) ?: emptyList()
            for (oi in orderItems) {
                itemQuantities[oi.itemName] = (itemQuantities[oi.itemName] ?: 0) + oi.quantity
                itemRevenues[oi.itemName] = (itemRevenues[oi.itemName] ?: 0.0) + (oi.price * oi.quantity)
            }
        }

        val topSellingItems = itemQuantities.entries
            .map { entry -> TopItem(entry.key, entry.value, itemRevenues[entry.key] ?: 0.0) }
            .sortedByDescending { it.quantity }
            .take(5)

        // Generate sales chart data points based on timeframe
        val chartPoints = generateChartPoints(currentRangeOrders, timeframe)

        AnalyticsMetrics(
            totalSales = totalSales,
            numBills = numBills,
            averageOrderValue = avgOrderVal,
            topSellingItems = topSellingItems,
            chartPoints = chartPoints
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsMetrics())

    private fun generateChartPoints(orders: List<BillOrder>, timeframe: String): List<ChartPoint> {
        val points = mutableListOf<ChartPoint>()
        if (orders.isEmpty()) return points

        val sdf = when (timeframe) {
            "Today" -> SimpleDateFormat("hh a", Locale.getDefault()) // 12 AM, 1 AM
            "Weekly" -> SimpleDateFormat("EEE", Locale.getDefault()) // Mon, Tue
            "Monthly" -> SimpleDateFormat("dd MMM", Locale.getDefault()) // 01 Jul, 02 Jul
            else -> SimpleDateFormat("dd MMM", Locale.getDefault())
        }

        // Group by parsed date string
        val grouped = orders.groupBy { sdf.format(Date(it.timestamp)) }

        if (timeframe == "Today") {
            // Fill 24 hours or show intervals
            val intervals = listOf("08 AM", "10 AM", "12 PM", "02 PM", "04 PM", "06 PM", "08 PM", "10 PM")
            for (interval in intervals) {
                val sum = grouped[interval]?.sumOf { it.total } ?: 0.0
                points.add(ChartPoint(interval, sum))
            }
        } else if (timeframe == "Weekly") {
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            for (day in days) {
                val sum = grouped[day]?.sumOf { it.total } ?: 0.0
                points.add(ChartPoint(day, sum))
            }
        } else {
            // Monthly, take last 7 active transaction dates to keep chart gorgeous
            val sortedGrouped = grouped.entries.sortedBy { it.value.first().timestamp }.takeLast(7)
            for (entry in sortedGrouped) {
                points.add(ChartPoint(entry.key, entry.value.sumOf { it.total }))
            }
        }
        return points
    }
}

data class TopItem(val name: String, val quantity: Int, val totalRevenue: Double)
data class ChartPoint(val label: String, val value: Double)
data class AnalyticsMetrics(
    val totalSales: Double = 0.0,
    val numBills: Int = 0,
    val averageOrderValue: Double = 0.0,
    val topSellingItems: List<TopItem> = emptyList(),
    val chartPoints: List<ChartPoint> = emptyList()
)

// --- 6. ITEMS VIEWMODEL ---
class ItemsViewModel(
    private val itemRepo: BillingItemRepository,
    private val categoryRepo: CategoryRepository
) : ViewModel() {

    val items: StateFlow<List<BillingItem>> = itemRepo.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepo.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var searchItemQuery = mutableStateOf("")
    var selectedCategoryFilter = mutableStateOf("All")

    // Form variables
    var itemName = mutableStateOf("")
    var itemCategory = mutableStateOf("Tea")
    var itemPrice = mutableStateOf("")
    var itemAvailable = mutableStateOf(true)
    var formError = mutableStateOf<String?>(null)

    // Custom category variables
    var customCategoryName = mutableStateOf("")
    var categoryError = mutableStateOf<String?>(null)

    fun addCustomCategory() {
        val catName = customCategoryName.value.trim()
        if (catName.isEmpty()) {
            categoryError.value = "Category name cannot be empty"
            return
        }
        categoryError.value = null
        viewModelScope.launch {
            val existing = categoryRepo.getAllCategoriesSync()
            if (existing.any { it.name.equals(catName, ignoreCase = true) }) {
                categoryError.value = "Category already exists"
                return@launch
            }
            categoryRepo.insert(Category(name = catName, isCustom = true))
            customCategoryName.value = ""
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepo.delete(category)
        }
    }

    fun saveItem(itemToEdit: BillingItem? = null, onSuccess: () -> Unit) {
        val name = itemName.value.trim()
        val cat = itemCategory.value
        val priceDouble = itemPrice.value.toDoubleOrNull() ?: 0.0

        if (name.isEmpty() || priceDouble <= 0.0) {
            formError.value = "Please enter a valid item name and price"
            return
        }

        formError.value = null
        viewModelScope.launch {
            if (itemToEdit == null) {
                // Add Item
                itemRepo.insert(
                    BillingItem(
                        name = name,
                        category = cat,
                        price = priceDouble,
                        isAvailable = itemAvailable.value
                    )
                )
            } else {
                // Edit Item
                itemRepo.update(
                    itemToEdit.copy(
                        name = name,
                        category = cat,
                        price = priceDouble,
                        isAvailable = itemAvailable.value
                    )
                )
            }
            clearForm()
            onSuccess()
        }
    }

    fun toggleAvailability(item: BillingItem) {
        viewModelScope.launch {
            itemRepo.update(item.copy(isAvailable = !item.isAvailable))
        }
    }

    fun deleteItem(item: BillingItem) {
        viewModelScope.launch {
            itemRepo.delete(item)
        }
    }

    fun fillForm(item: BillingItem) {
        itemName.value = item.name
        itemCategory.value = item.category
        itemPrice.value = item.price.toString()
        itemAvailable.value = item.isAvailable
    }

    fun clearForm() {
        itemName.value = ""
        itemCategory.value = "Tea"
        itemPrice.value = ""
        itemAvailable.value = true
        formError.value = null
    }
}

// --- STAFF VIEWMODEL ---
class StaffViewModel(private val repository: StaffRepository) : ViewModel() {
    val allStaff: StateFlow<List<Staff>> = repository.allStaff
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var staffName = mutableStateOf("")
    var username = mutableStateOf("")
    var password = mutableStateOf("")
    var isDisabled = mutableStateOf(false)
    var editingStaff = mutableStateOf<Staff?>(null)

    fun startEditing(staff: Staff) {
        editingStaff.value = staff
        staffName.value = staff.name
        username.value = staff.username
        password.value = staff.password
        isDisabled.value = staff.isDisabled
    }

    fun clearFields() {
        editingStaff.value = null
        staffName.value = ""
        username.value = ""
        password.value = ""
        isDisabled.value = false
    }

    fun saveStaff(onSuccess: () -> Unit) {
        val name = staffName.value.trim()
        val user = username.value.trim()
        val pass = password.value.trim()

        if (name.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            return
        }

        viewModelScope.launch {
            val current = editingStaff.value
            if (current != null) {
                repository.update(current.copy(name = name, username = user, password = pass, isDisabled = isDisabled.value))
            } else {
                repository.insert(Staff(name = name, username = user, password = pass, isDisabled = isDisabled.value))
            }
            clearFields()
            onSuccess()
        }
    }

    fun deleteStaff(staff: Staff) {
        viewModelScope.launch {
            repository.delete(staff)
        }
    }
}

// --- 7. SETTINGS VIEWMODEL ---
class SettingsViewModel(
    private val profileRepo: BusinessProfileRepository,
    private val configRepo: PrinterConfigRepository,
    private val authManager: AuthManager,
    val printerManager: PrinterManager
) : ViewModel() {

    val profile: StateFlow<BusinessProfile?> = profileRepo.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val printerConfig: StateFlow<PrinterConfig?> = configRepo.printerConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isScanning: StateFlow<Boolean> = printerManager.isScanning
    val scannedDevices: StateFlow<List<PrinterDevice>> = printerManager.scannedDevices
    val connectedDevice: StateFlow<PrinterDevice?> = printerManager.connectedDevice

    var printerSearchText = mutableStateOf("")
    var isBackupCompleted = mutableStateOf(false)
    var isRestoreCompleted = mutableStateOf(false)

    // Form states for settings profile edit
    var profileName = mutableStateOf("")
    var profileAddress = mutableStateOf("")
    var profilePhone = mutableStateOf("")
    var profileGst = mutableStateOf("")
    var profileCurrency = mutableStateOf("₹")
    var profileFooter = mutableStateOf("")

    fun initProfileForm(bp: BusinessProfile) {
        profileName.value = bp.name
        profileAddress.value = bp.address
        profilePhone.value = bp.phone
        profileGst.value = bp.gstNumber ?: ""
        profileCurrency.value = bp.currency
        profileFooter.value = bp.footerMessage
    }

    fun saveProfileSettings() {
        viewModelScope.launch {
            val bp = BusinessProfile(
                name = profileName.value.trim(),
                address = profileAddress.value.trim(),
                phone = profilePhone.value.trim(),
                gstNumber = profileGst.value.trim().ifEmpty { null },
                currency = profileCurrency.value,
                footerMessage = profileFooter.value.trim()
            )
            profileRepo.saveProfile(bp)
        }
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
            OrderItem(1, "Test Masala Chai", 15.0, 2),
            OrderItem(2, "Test Samosa Special", 20.0, 1)
        )
        printerManager.generateReceiptText(
            businessName = profile.name,
            address = profile.address,
            phone = profile.phone,
            gstNumber = profile.gstNumber,
            tokenNumber = "T-999",
            items = testItems,
            subtotal = 50.0,
            discount = 5.0,
            total = 45.0,
            footerMessage = "TEST PRINT - OK",
            currency = profile.currency
        )
    }

    fun resetDailyToken(context: Context) {
        val prefs = context.getSharedPreferences("zaddy_token_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("current_token", 0).apply()
    }

    fun runBackup() {
        viewModelScope.launch {
            isBackupCompleted.value = false
            kotlinx.coroutines.delay(1200) // Simulate cloud backup
            isBackupCompleted.value = true
        }
    }

    fun runRestore() {
        viewModelScope.launch {
            isRestoreCompleted.value = false
            kotlinx.coroutines.delay(1200) // Simulate cloud restore
            isRestoreCompleted.value = true
        }
    }

    fun logout() {
        authManager.logout()
    }
}
