package com.example.services

import com.example.data.entity.UserSubscription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log

object FeatureGate {
    private val _subscription = MutableStateFlow<UserSubscription?>(null)
    val subscription = _subscription.asStateFlow()

    // Remote Feature Flags map: FeatureKey -> Status ("ON", "OFF", "BETA", "MAINTENANCE")
    private val _featureFlags = MutableStateFlow<Map<String, String>>(emptyMap())
    val featureFlags = _featureFlags.asStateFlow()

    // Volatile overrides for real-time usage tracking without DB writes
    @Volatile private var billsOverride: Int? = null
    @Volatile private var productsOverride: Int? = null

    fun updateSubscription(sub: UserSubscription?) {
        _subscription.value = sub
        billsOverride = null
        productsOverride = null
    }

    fun updateFeatureFlags(flags: Map<String, String>) {
        val current = _featureFlags.value.toMutableMap()
        current.putAll(flags)
        _featureFlags.value = current
        Log.d("FEATURE_GATE", "[FLAGS UPDATED] New flags state: $current")
    }

    fun setUsageOverrides(bills: Int, products: Int) {
        billsOverride = bills
        productsOverride = products
    }

    private val currentPlan: String
        get() = _subscription.value?.planId ?: "TRIAL"

    private val billsUsed: Int 
        get() = billsOverride ?: _subscription.value?.billsUsed ?: 0
        
    private val productsUsed: Int 
        get() = productsOverride ?: _subscription.value?.productsUsed ?: 0

    /**
     * Checks if a feature key is enabled.
     * Hierarchy:
     * 1. If remote flag is "OFF" or "MAINTENANCE" -> FALSE
     * 2. If remote flag is "ON" or "BETA" -> TRUE (or check plan)
     * 3. Fallback to plan check
     */
    fun isFeatureEnabled(key: String): Boolean {
        val remoteStatus = _featureFlags.value[key]
        if (remoteStatus == "OFF" || remoteStatus == "MAINTENANCE") {
            Log.d("FEATURE_GATE", "[FLAG BLOCKED] $key is remotely set to $remoteStatus")
            return false
        }
        if (remoteStatus == "ON" || remoteStatus == "BETA") {
            return true
        }
        return true
    }

    // --- FEATURE ACCESS CONTROL ---

    fun canCreateBill(): Boolean {
        if (!isFeatureEnabled("BILLING")) return false
        if (currentPlan != "TRIAL") return true
        return billsUsed < 50
    }

    fun canCreateProduct(): Boolean {
        if (!isFeatureEnabled("PRODUCTS")) return false
        if (currentPlan != "TRIAL") return true
        return productsUsed < 5
    }

    fun canUseStaff(): Boolean = isFeatureEnabled("STAFF_MANAGEMENT") && (currentPlan == "GROWTH" || currentPlan == "TRIAL")
    fun canUseInventory(): Boolean = isFeatureEnabled("INVENTORY") && (currentPlan == "GROWTH" || currentPlan == "TRIAL")
    fun canUseCustomers(): Boolean = isFeatureEnabled("CUSTOMERS") && (currentPlan == "GROWTH" || currentPlan == "TRIAL")
    fun canUseExpenses(): Boolean = isFeatureEnabled("EXPENSES") && (currentPlan == "GROWTH" || currentPlan == "TRIAL")
    fun canUseAdvancedReports(): Boolean = isFeatureEnabled("ADVANCED_REPORTS") && (currentPlan == "GROWTH" || currentPlan == "TRIAL")
    fun canUseGstTax(): Boolean = isFeatureEnabled("GST") && isFeatureEnabled("TAXES") && (currentPlan == "GROWTH" || currentPlan == "TRIAL")
    fun canUseAdvancedReceipt(): Boolean = isFeatureEnabled("RECEIPT_CUSTOMIZATION") && (currentPlan == "GROWTH" || currentPlan == "TRIAL")
    fun canUsePaymentQr(): Boolean = isFeatureEnabled("PAYMENT_QR")
    fun canUseMultipleQr(): Boolean = isFeatureEnabled("MULTIPLE_QR") && (currentPlan == "GROWTH" || currentPlan == "TRIAL")
    fun canUseAI(): Boolean = isFeatureEnabled("AI_MENU_IMPORT") && (currentPlan == "GROWTH" || currentPlan == "TRIAL")
    fun canUseOwnerDashboard(): Boolean = isFeatureEnabled("ANALYTICS") && (currentPlan == "GROWTH" || currentPlan == "TRIAL")
    fun canUseMultiBusiness(): Boolean = currentPlan == "GROWTH"
    fun canUseBluetoothPrinting(): Boolean = isFeatureEnabled("BLUETOOTH_PRINTING")
    fun canUseKitchenOrders(): Boolean = isFeatureEnabled("KITCHEN_ORDERS")
    fun canUseLoyalty(): Boolean = isFeatureEnabled("LOYALTY")
    fun canUseCoupons(): Boolean = isFeatureEnabled("COUPONS")

    // Usage counts for Trial UI
    fun getTrialRemainingBills(): Int = (50 - billsUsed).coerceAtLeast(0)
    fun getTrialRemainingProducts(): Int = (5 - productsUsed).coerceAtLeast(0)
    
    fun isTrial(): Boolean = currentPlan == "TRIAL"
}
