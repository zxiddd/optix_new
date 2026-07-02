package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val logoPath: String? = null,
    val address: String,
    val phone: String,
    val gstNumber: String? = null,
    val currency: String = "₹",
    val footerMessage: String = "Powered by Zaddy Billing",
    val setupCompleted: Boolean = true
)

@Entity(tableName = "billing_items")
data class BillingItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val price: Double,
    val imageUrl: String? = null,
    val isAvailable: Boolean = true
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isCustom: Boolean = false
)

@Entity(tableName = "bill_orders")
data class BillOrder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tokenNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val subtotal: Double,
    val discount: Double,
    val total: Double,
    val orderItemsJson: String, // Serialized List<OrderItem>
    val paymentMethod: String = "Cash",
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
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val username: String, // Username or mobile number
    val password: String,
    val isDisabled: Boolean = false
)

// Simple data class representing items in an order (used for JSON serialization)
data class OrderItem(
    val itemId: Int,
    val itemName: String,
    val price: Double,
    val quantity: Int
)
