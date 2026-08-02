package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val gstNumber: String? = null,
    val currency: String = "Rs.",
    val footerMessage: String = "Thank You! Visit Again 🙏",
    val setupCompleted: Boolean = false,
    val menuSetupCompleted: Boolean = false,
    val openingTime: String = "08:00",
    val closingTime: String = "22:00",
    
    // Receipt Customization Flags
    val showBusinessName: Boolean = true,
    val showAddress: Boolean = true,
    val showPhone: Boolean = true,
    val showGst: Boolean = false,
    val showDateTime: Boolean = true,
    val showOrderNumber: Boolean = true,
    val showCashierName: Boolean = true,
    val showDiscounts: Boolean = true,
    val showTaxes: Boolean = false,
    val taxPercentage: Double = 0.0,
    
    // Receipt Logo Support
    val showLogo: Boolean = false,
    val logoPath: String? = null, // Local internal storage path

    // Receipt QR Branding (Offline-First)
    val qrEnabled: Boolean = false,
    val showVisitAgain: Boolean = true,

    // SaaS Usage Tracking (Local)
    val dailyBillCount: Int = 0,
    val lastResetTimestamp: Long = 0L, // Reset daily at midnight or opening time
    val dailyAiCount: Int = 0,
    val dailyVoiceCount: Int = 0
)

@Entity(tableName = "payment_qrs")
data class PaymentQrEntity(
    @PrimaryKey val id: String = "",
    val businessId: String = "",
    val name: String = "",
    val imagePath: String = "", 
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_subscriptions")
data class UserSubscription(
    @PrimaryKey val uid: String = "",
    val planId: String = "free", // free, monthly, 3_months, 6_months, 9_months, 12_months
    val planName: String = "Free Plan",
    val amount: Double = 0.0,
    val currency: String = "INR",
    val billingCycle: String = "monthly",
    val status: String = "active", 
    val purchaseDate: Long = System.currentTimeMillis(),
    val expiryDate: Long = 0L, 
    val autoRenew: Boolean = false,
    val paymentId: String? = null,
    val orderId: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class SubscriptionPlan(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val durationDays: Int = 30,
    val features: List<String> = emptyList()
)

@Entity(tableName = "billing_items")
data class BillingItem(
    @PrimaryKey val id: String = "", 
    val name: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val price: Double = 0.0, // This is Price per Unit if Weight-Based
    val imageUrl: String? = null,
    val isAvailable: Boolean = true,
    val isOutOfStock: Boolean = false,
    val sortOrder: Int = 0,
    
    // Weight-Based Fields
    val pricingType: String = "FIXED", // FIXED, WEIGHT_BASED
    val unit: String = "Piece" // kg, g, L, ml, Piece, etc.
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: String = "", 
    val name: String = "",
    val businessId: String = "",
    val isCustom: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bill_orders")
data class BillOrder(
    @PrimaryKey val id: String = "", 
    val tokenNumber: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double = 0.0,
    val orderItemsJson: String = "", 
    val paymentMethod: String = "Cash",
    val cashierName: String = "Admin",
    val invoiceNumber: String = "",
    val customerName: String? = null // For search improvements
)

@Entity(tableName = "printer_config")
data class PrinterConfig(
    @PrimaryKey val id: Int = 1,
    val deviceName: String? = null,
    val deviceAddress: String? = null,
    val isConnected: Boolean = false,
    val autoConnect: Boolean = true,
    val paperWidth: Int = 58
)

@Entity(tableName = "staff_accounts")
data class Staff(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val username: String = "", 
    val password: String = "",
    val isDisabled: Boolean = false,
    val role: String = "staff",
    val adminId: String = "",
    
    // Staff Permissions
    val canBillWeightBased: Boolean = true,
    val canEditWeight: Boolean = true,
    val canEnterAmount: Boolean = true,
    val canChangeProductPrice: Boolean = false
)

@Entity(tableName = "daily_reports")
data class DailyReport(
    @PrimaryKey val id: String = "",
    val date: String = "", 
    val filePath: String = "",
    val totalSales: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "support_tickets")
data class SupportTicket(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val subject: String = "",
    val description: String = "",
    val status: String = "open", 
    val createdAt: Long = System.currentTimeMillis()
)

data class OrderItem(
    val itemId: String = "",
    val itemName: String = "",
    val price: Double = 0.0, // Fixed price or Price per Unit
    val quantity: Int = 0, // Used for FIXED type
    val weight: Double? = null, // Used for WEIGHT_BASED type
    val unit: String? = null, // Used for WEIGHT_BASED type
    val pricingType: String = "FIXED" // FIXED, WEIGHT_BASED
)

data class AiMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
