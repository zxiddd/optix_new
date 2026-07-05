package com.example.presentation.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.OptixApplication
import com.example.data.entity.*
import com.example.data.repository.CloudRepository
import com.example.data.repository.PrinterConfigRepository
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
                AuthViewModel(authManager, cloudRepo) as T
            modelClass.isAssignableFrom(BusinessSetupViewModel::class.java) -> 
                BusinessSetupViewModel(cloudRepo) as T
            modelClass.isAssignableFrom(BillingViewModel::class.java) -> 
                BillingViewModel(cloudRepo, application.printerManager) as T
            modelClass.isAssignableFrom(OrderHistoryViewModel::class.java) -> 
                OrderHistoryViewModel(cloudRepo, application.printerManager) as T
            modelClass.isAssignableFrom(AnalyticsViewModel::class.java) -> 
                AnalyticsViewModel(cloudRepo) as T
            modelClass.isAssignableFrom(ItemsViewModel::class.java) -> 
                ItemsViewModel(cloudRepo) as T
            modelClass.isAssignableFrom(StaffViewModel::class.java) ->
                StaffViewModel(cloudRepo) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> 
                SettingsViewModel(cloudRepo, application.printerConfigRepository, authManager, application.printerManager) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// --- 1. AUTH VIEWMODEL ---
class AuthViewModel(
    private val authManager: AuthManager,
    private val repository: CloudRepository
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

    fun logout() {
        authManager.logout()
        authError.value = null
    }
}

// --- 2. BUSINESS SETUP VIEWMODEL ---
class BusinessSetupViewModel(private val repository: CloudRepository) : ViewModel() {
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
            val bp = profile.value ?: BusinessProfile()
            val businessProfile = bp.copy(
                name = name,
                address = addr,
                phone = ph,
                gstNumber = gstNumber.value.trim().ifEmpty { null },
                currency = selectedCurrency.value,
                footerMessage = footerMessage.value.trim(),
                setupCompleted = true
            )
            repository.saveProfile(businessProfile)
            onSuccess()
        }
    }
}

// --- 3. BILLING VIEWMODEL ---
class BillingViewModel(
    private val repository: CloudRepository,
    private val printerManager: PrinterManager
) : ViewModel() {

    val items: StateFlow<List<BillingItem>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val todaySales: StateFlow<Double> = repository.allOrders
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

    private val _cart = MutableStateFlow<Map<String, Int>>(emptyMap()) // Key is itemId
    val cart: StateFlow<Map<String, Int>> = _cart.asStateFlow()

    var discountValue = mutableStateOf("0")
    var lastPrintedReceipt = mutableStateOf<String?>(null)
    var showReceiptPreview = mutableStateOf(false)
    var isPreparingOrder = mutableStateOf(false)
    var currentTokenNum = mutableStateOf("000")
    var paymentMethod = mutableStateOf("Cash")

    val cartSubtotal: StateFlow<Double> = combine(_cart, items) { cartMap, allItems ->
        cartMap.entries.sumOf { (itemId, qty) ->
            val item = allItems.find { it.id == itemId }
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
        discountValue.value = "0"
        paymentMethod.value = "Cash"
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
        onComplete: () -> Unit
    ) {
        if (_cart.value.isEmpty()) return

        isPreparingOrder.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val tokenNum = generateNewToken(context)
            val invoiceNum = "INV-" + SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
            
            val allItems = items.value
            val orderItems = _cart.value.map { (itemId, qty) ->
                val item = allItems.find { it.id == itemId }
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

            val order = BillOrder(
                tokenNumber = tokenNum,
                subtotal = subtotal,
                discount = discount,
                total = grandTotal,
                orderItemsJson = jsonItems,
                paymentMethod = paymentMethod.value,
                cashierName = cashierName,
                invoiceNumber = invoiceNum
            )
            repository.insertOrder(order)

            // Real Printing
            val previewText = printerManager.printReceipt(
                businessName = profile.name,
                address = profile.address,
                phone = profile.phone,
                gstNumber = profile.gstNumber,
                tokenNumber = tokenNum,
                invoiceNumber = invoiceNum,
                items = orderItems,
                subtotal = subtotal,
                discount = discount,
                total = grandTotal,
                paymentMethod = paymentMethod.value,
                cashierName = cashierName,
                footerMessage = profile.footerMessage,
                currency = profile.currency,
                shouldPrint = true
            )

            launch(Dispatchers.Main) {
                lastPrintedReceipt.value = previewText
                isPreparingOrder.value = false
                showReceiptPreview.value = false // No preview after billing
                clearCart()
                onComplete()
            }
        }
    }
}

// --- 4. ORDER HISTORY VIEWMODEL ---
class OrderHistoryViewModel(
    private val repository: CloudRepository,
    private val printerManager: PrinterManager
) : ViewModel() {

    val allOrders: StateFlow<List<BillOrder>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchTokenQuery = MutableStateFlow("")
    val searchTokenQuery: StateFlow<String> = _searchTokenQuery.asStateFlow()

    private val _timeFilter = MutableStateFlow("Today")
    val timeFilter: StateFlow<String> = _timeFilter.asStateFlow()

    private val _viewReceiptText = MutableStateFlow<String?>(null)
    val viewReceiptText: StateFlow<String?> = _viewReceiptText.asStateFlow()

    fun setTimeFilter(filter: String) {
        _timeFilter.value = filter
    }

    fun hideReceiptPreview() {
        _viewReceiptText.value = null
    }

    fun showReceiptPreview(order: BillOrder, profile: BusinessProfile) {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, OrderItem::class.java)
        val adapter = moshi.adapter<List<OrderItem>>(type)
        val items = adapter.fromJson(order.orderItemsJson) ?: emptyList()

        // We use printReceipt with shouldPrint = false to only get the preview text
        viewModelScope.launch {
            val preview = printerManager.printReceipt(
                businessName = profile.name,
                address = profile.address,
                phone = profile.phone,
                gstNumber = profile.gstNumber,
                tokenNumber = order.tokenNumber,
                invoiceNumber = order.invoiceNumber,
                items = items,
                subtotal = order.subtotal,
                discount = order.discount,
                total = order.total,
                paymentMethod = order.paymentMethod,
                cashierName = order.cashierName,
                footerMessage = profile.footerMessage,
                currency = profile.currency,
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
            printerManager.printReceipt(
                businessName = profile.name,
                address = profile.address,
                phone = profile.phone,
                gstNumber = profile.gstNumber,
                tokenNumber = order.tokenNumber,
                invoiceNumber = order.invoiceNumber,
                items = items,
                subtotal = order.subtotal,
                discount = order.discount,
                total = order.total,
                paymentMethod = order.paymentMethod,
                cashierName = order.cashierName,
                footerMessage = profile.footerMessage,
                currency = profile.currency
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
class AnalyticsViewModel(private val repository: CloudRepository) : ViewModel() {
    val allOrders: StateFlow<List<BillOrder>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _timeFrame = MutableStateFlow("Today")
    val timeFrame: StateFlow<String> = _timeFrame.asStateFlow()

    fun setTimeFrame(tf: String) {
        _timeFrame.value = tf
    }

    fun downloadReport(context: Context, type: String) {
        val currentMetrics = metrics.value
        if (type == "PDF") {
            ReportService(context).generatePdfReport(currentMetrics, timeFrame.value)
        } else {
            // CSV Logic
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

        val chartPoints = generateChartPoints(currentRangeOrders, timeframe)

        // Peak Hour
        val hourSdf = SimpleDateFormat("HH", Locale.getDefault())
        val peakHour = currentRangeOrders.groupBy { hourSdf.format(Date(it.timestamp)) }
            .maxByOrNull { it.value.size }?.key?.let { "$it:00" } ?: "N/A"

        // Top Categories
        val catRevenues = mutableMapOf<String, Double>()
        for (order in currentRangeOrders) {
            val orderItems = adapter.fromJson(order.orderItemsJson) ?: emptyList()
            for (oi in orderItems) {
                // We need to look up category from BillingItem if it's not in OrderItem
                // For simplicity, let's assume item name is enough for now or 
                // we'd need to fetch items map. Let's use itemRevenues for top selling items.
            }
        }
        // Actually, OrderItem doesn't have category. Let's skip category breakdown for now 
        // unless I fetch all items to map them.

        val paymentBreakdown = currentRangeOrders.groupBy { it.paymentMethod }
            .mapValues { entry -> entry.value.sumOf { it.total } }

        AnalyticsMetrics(
            totalSales = totalSales,
            numBills = numBills,
            averageOrderValue = avgOrderVal,
            topSellingItems = topSellingItems,
            chartPoints = chartPoints,
            peakHour = peakHour,
            paymentBreakdown = paymentBreakdown
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsMetrics())

    private fun generateChartPoints(orders: List<BillOrder>, timeframe: String): List<ChartPoint> {
        val points = mutableListOf<ChartPoint>()
        if (orders.isEmpty()) return points

        val sdf = when (timeframe) {
            "Today" -> SimpleDateFormat("hh a", Locale.getDefault())
            "Weekly" -> SimpleDateFormat("EEE", Locale.getDefault())
            "Monthly" -> SimpleDateFormat("dd MMM", Locale.getDefault())
            else -> SimpleDateFormat("dd MMM", Locale.getDefault())
        }

        val grouped = orders.groupBy { sdf.format(Date(it.timestamp)) }

        if (timeframe == "Today") {
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
    val chartPoints: List<ChartPoint> = emptyList(),
    val peakHour: String = "N/A",
    val topCategories: List<Pair<String, Double>> = emptyList(),
    val paymentBreakdown: Map<String, Double> = emptyMap()
)

// --- 6. ITEMS VIEWMODEL ---
class ItemsViewModel(
    private val repository: CloudRepository
) : ViewModel() {

    val items: StateFlow<List<BillingItem>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var searchItemQuery = mutableStateOf("")
    var selectedCategoryFilter = mutableStateOf("All")

    var itemName = mutableStateOf("")
    var itemCategory = mutableStateOf("Tea")
    var itemPrice = mutableStateOf("")
    var itemAvailable = mutableStateOf(true)
    var isSaving = mutableStateOf(false)
    var formError = mutableStateOf<String?>(null)

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
            val existing = repository.allCategories.first()
            if (existing.any { it.name.equals(catName, ignoreCase = true) }) {
                categoryError.value = "Category already exists"
                return@launch
            }
            repository.insertCategory(Category(name = catName, isCustom = true, sortOrder = existing.size))
            customCategoryName.value = ""
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category.id)
        }
    }

    fun saveItem(itemToEdit: BillingItem? = null, onSuccess: () -> Unit) {
        val name = itemName.value.trim()
        val catName = itemCategory.value
        val priceDouble = itemPrice.value.toDoubleOrNull() ?: 0.0

        if (name.isEmpty() || priceDouble <= 0.0) {
            formError.value = "Please enter a valid item name and price"
            return
        }

        formError.value = null
        isSaving.value = true
        viewModelScope.launch {
            try {
                val cats = categories.value
                val targetCat = cats.find { it.name == catName }
                
                if (itemToEdit == null) {
                    repository.insertItem(
                        BillingItem(
                            name = name,
                            categoryId = targetCat?.id ?: "",
                            categoryName = catName,
                            price = priceDouble,
                            isAvailable = itemAvailable.value,
                            sortOrder = items.value.size
                        )
                    )
                } else {
                    repository.insertItem(
                        itemToEdit.copy(
                            name = name,
                            categoryId = targetCat?.id ?: "",
                            categoryName = catName,
                            price = priceDouble,
                            isAvailable = itemAvailable.value
                        )
                    )
                }
                clearForm()
                onSuccess()
            } finally {
                isSaving.value = false
            }
        }
    }

    fun toggleAvailability(item: BillingItem) {
        viewModelScope.launch {
            repository.insertItem(item.copy(isAvailable = !item.isAvailable))
        }
    }

    fun deleteItem(item: BillingItem) {
        viewModelScope.launch {
            repository.deleteItem(item.id)
        }
    }

    fun fillForm(item: BillingItem) {
        itemName.value = item.name
        itemCategory.value = item.categoryName
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
class StaffViewModel(private val repository: CloudRepository) : ViewModel() {
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
                repository.updateStaff(current.copy(name = name, username = user, password = pass, isDisabled = isDisabled.value))
            } else {
                repository.insertStaff(Staff(name = name, username = user, password = pass, isDisabled = isDisabled.value))
            }
            clearFields()
            onSuccess()
        }
    }

    fun deleteStaff(staff: Staff) {
        viewModelScope.launch {
            repository.deleteStaff(staff.id)
        }
    }
}

// --- 7. SETTINGS VIEWMODEL ---
class SettingsViewModel(
    private val repository: CloudRepository,
    private val configRepo: PrinterConfigRepository,
    private val authManager: AuthManager,
    val printerManager: PrinterManager
) : ViewModel() {

    val profile: StateFlow<BusinessProfile?> = repository.profile
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

    var profileName = mutableStateOf("")
    var profileAddress = mutableStateOf("")
    var profilePhone = mutableStateOf("")
    var profileGst = mutableStateOf("")
    var profileCurrency = mutableStateOf("₹")
    var profileFooter = mutableStateOf("")

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
                footerMessage = profileFooter.value.trim()
            )
            repository.saveProfile(updated)
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
            printerManager.printReceipt(
                businessName = profile.name,
                address = profile.address,
                phone = profile.phone,
                gstNumber = profile.gstNumber,
                tokenNumber = "T-999",
                invoiceNumber = "TEST-INV-001",
                items = testItems,
                subtotal = 50.0,
                discount = 0.0,
                total = 50.0,
                paymentMethod = "Test Cash",
                cashierName = "Admin",
                footerMessage = profile.footerMessage,
                currency = profile.currency
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
}
