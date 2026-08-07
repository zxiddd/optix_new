package com.example.services

import com.example.data.entity.UserSubscription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object FeatureGate {
    private val _subscription = MutableStateFlow<UserSubscription?>(null)
    val subscription = _subscription.asStateFlow()

    fun updateSubscription(sub: UserSubscription?) {
        _subscription.value = sub
    }

    private val currentPlan: String
        get() = _subscription.value?.planId ?: "TRIAL"

    // Trial Limits
    private val billsUsed: Int get() = _subscription.value?.billsUsed ?: 0
    private val productsUsed: Int get() = _subscription.value?.productsUsed ?: 0

    fun canCreateBill(): Boolean {
        if (currentPlan != "TRIAL") return true
        return billsUsed < 50
    }

    fun canCreateProduct(): Boolean {
        if (currentPlan != "TRIAL") return true
        return productsUsed < 5
    }

    fun canUseStaff(): Boolean = currentPlan == "GROWTH" || currentPlan == "TRIAL"
    fun canUseInventory(): Boolean = currentPlan == "GROWTH" || currentPlan == "TRIAL"
    fun canUseCustomers(): Boolean = currentPlan == "GROWTH" || currentPlan == "TRIAL"
    fun canUseExpenses(): Boolean = currentPlan == "GROWTH" || currentPlan == "TRIAL"
    fun canUseAdvancedReports(): Boolean = currentPlan == "GROWTH" || currentPlan == "TRIAL"
    fun canUseGstTax(): Boolean = currentPlan == "GROWTH" || currentPlan == "TRIAL"
    fun canUseAdvancedReceipt(): Boolean = currentPlan == "GROWTH" || currentPlan == "TRIAL"
    fun canUseMultipleQr(): Boolean = currentPlan == "GROWTH" || currentPlan == "TRIAL"
    fun canUseAI(): Boolean = currentPlan == "GROWTH" || currentPlan == "TRIAL"
    
    // Usage counts for Trial UI
    fun getTrialRemainingBills(): Int = (50 - billsUsed).coerceAtLeast(0)
    fun getTrialRemainingProducts(): Int = (5 - productsUsed).coerceAtLeast(0)
    
    fun isTrial(): Boolean = currentPlan == "TRIAL"
}
