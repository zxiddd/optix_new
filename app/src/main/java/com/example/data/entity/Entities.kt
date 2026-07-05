package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val logoUrl: String? = null,
    val address: String = "",
    val phone: String = "",
    val gstNumber: String? = null,
    val currency: String = "₹",
    val footerMessage: String = "Thank You! Visit Again 🙏",
    val setupCompleted: Boolean = false,
    val menuSetupCompleted: Boolean = false
)

@Entity(tableName = "billing_items")
data class BillingItem(
    @PrimaryKey val id: String = "", // Changed to String for Firestore UID compatibility
    val name: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val price: Double = 0.0,
    val imageUrl: String? = null,
    val isAvailable: Boolean = true,
    val sortOrder: Int = 0
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: String = "", // Changed to String
    val name: String = "",
    val isCustom: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(tableName = "bill_orders")
data class BillOrder(
    @PrimaryKey val id: String = "", // Changed to String
    val tokenNumber: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
    val orderItemsJson: String = "", 
    val paymentMethod: String = "Cash", // Cash, UPI, Card, Other
    val cashierName: String = "Admin",
    val invoiceNumber: String = ""
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
    @PrimaryKey val id: String = "", // Changed to String
    val name: String = "",
    val username: String = "", 
    val password: String = "",
    val isDisabled: Boolean = false,
    val role: String = "staff" // Changed to role-based
)

data class OrderItem(
    val itemId: String = "",
    val itemName: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0
)
