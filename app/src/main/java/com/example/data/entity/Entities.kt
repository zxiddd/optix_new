package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val country: String = "India",
    val gstNumber: String? = null,
    val currency: String = "₹",
    val footerMessage: String = "Thank You! Visit Again 🙏",
    val setupCompleted: Boolean = false,
    val menuSetupCompleted: Boolean = false,
    val openingTime: String = "09:00",
    val closingTime: String = "22:00",
    val timezone: String = "Asia/Riyadh",
    val lastResetBusinessDate: String? = null,
    
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
    val planId: String = "TRIAL", // TRIAL, STARTER, GROWTH
    val planName: String = "Trial Plan",
    val amount: Double = 0.0,
    val currency: String = "₹",
    val country: String = "India",
    val billingCycle: String = "MONTHLY", // MONTHLY, YEARLY
    val status: String = "active", 
    val billsUsed: Int = 0,
    val productsUsed: Int = 0,
    val activationCode: String? = null,
    val purchaseDate: Long = System.currentTimeMillis(),
    val expiryDate: Long = 0L, 
    val renewalDate: Long = 0L,
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
    val description: String? = null,
    val barcode: String? = null,
    val sku: String? = null,
    val categoryId: String = "",
    val categoryName: String = "",
    val price: Double = 0.0,
    val imageUrl: String? = null,
    val isAvailable: Boolean = true,
    val isOutOfStock: Boolean = false,
    val sortOrder: Int = 0,
    val pricingType: String = "FIXED", // FIXED, WEIGHT, OPEN
    val unit: String = "Piece",
    
    // Sync Metadata
    val version: Int = 1,
    val isSynced: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: String = "", 
    val name: String = "",
    val businessId: String = "",
    val sortOrder: Int = 0,
    
    // Sync Metadata
    val version: Int = 1,
    val isSynced: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "bill_orders")
data class BillOrder(
    @PrimaryKey val id: String = "", 
    val tokenNumber: String = "",
    val invoiceNumber: String = "",
    val status: String = "PAID", // PAID, PENDING, CANCELLED
    val timestamp: Long = System.currentTimeMillis(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double = 0.0,
    val orderItemsJson: String = "", 
    val paymentMethod: String = "CASH",
    val cashierName: String = "Admin",
    val customerName: String? = null,
    
    // Sync Metadata
    val isSynced: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
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
    val businessId: String = "",
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,

    // Serialized permissions from server (e.g., ["WEIGHT_BILLING","CHANGE_PRICE"])
    val permissionsJson: String = "[]",

    // Staff Permissions (local boolean cache of permissionsJson for fast UI access)
    val canBillWeightBased: Boolean = true,
    val canEditWeight: Boolean = true,
    val canEnterAmount: Boolean = true,
    val canChangeProductPrice: Boolean = false,

    // Profile contact info
    val phone: String? = null,
    val email: String? = null,

    // Activity tracking
    val failedLoginCount: Int = 0,
    val lastActivityAt: Long? = null
)

@Entity(tableName = "staff_activity_logs")
data class StaffActivityLog(
    @PrimaryKey val id: String = "",
    val staffId: String = "",
    val businessId: String = "",
    val action: String = "",
    val entityType: String? = null,
    val entityId: String? = null,
    val metadataJson: String? = null,
    val deviceId: String? = null,
    val isSuspicious: Boolean = false,
    val severity: String = "NORMAL",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "staff_sessions")
data class StaffSession(
    @PrimaryKey val id: String = "",
    val staffId: String = "",
    val businessId: String = "",
    val deviceId: String? = null,
    val deviceName: String? = null,
    val loginAt: Long = System.currentTimeMillis(),
    val logoutAt: Long? = null,
    val isActive: Boolean = true
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String = "",
    val businessId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "INFO",
    val severity: String = "INFO",
    val isRead: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
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
    val weight: Double? = null, // Used for WEIGHT type
    val unit: String? = null, // Used for WEIGHT type
    val pricingType: String = "FIXED" // FIXED, WEIGHT, OPEN
)

data class AiMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
