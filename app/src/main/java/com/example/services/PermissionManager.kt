package com.example.services

import com.example.OptixApplication

object PermissionManager {
    // ── BILLING PERMISSIONS ──
    const val CREATE_BILLS = "CREATE_BILLS"
    const val CANCEL_BILLS = "CANCEL_BILLS"
    const val REFUND_BILLS = "REFUND_BILLS"
    const val APPLY_DISCOUNTS = "APPLY_DISCOUNTS"
    const val EDIT_BILLS = "EDIT_BILLS"
    const val WEIGHT_BILLING = "WEIGHT_BILLING"
    const val EDIT_WEIGHT = "EDIT_WEIGHT"
    const val ENTER_AMOUNT = "ENTER_AMOUNT"
    const val CHANGE_PRICE = "CHANGE_PRICE"

    // ── PRODUCTS PERMISSIONS ──
    const val VIEW_PRODUCTS = "VIEW_PRODUCTS"
    const val ADD_PRODUCTS = "ADD_PRODUCTS"
    const val EDIT_PRODUCTS = "EDIT_PRODUCTS"
    const val DELETE_PRODUCTS = "DELETE_PRODUCTS"

    // ── CATEGORIES PERMISSIONS ──
    const val VIEW_CATEGORIES = "VIEW_CATEGORIES"
    const val ADD_CATEGORIES = "ADD_CATEGORIES"
    const val EDIT_CATEGORIES = "EDIT_CATEGORIES"
    const val DELETE_CATEGORIES = "DELETE_CATEGORIES"

    // ── CUSTOMERS PERMISSIONS ──
    const val VIEW_CUSTOMERS = "VIEW_CUSTOMERS"
    const val ADD_CUSTOMERS = "ADD_CUSTOMERS"
    const val EDIT_CUSTOMERS = "EDIT_CUSTOMERS"
    const val DELETE_CUSTOMERS = "DELETE_CUSTOMERS"

    // ── INVENTORY PERMISSIONS ──
    const val VIEW_INVENTORY = "VIEW_INVENTORY"
    const val UPDATE_INVENTORY = "UPDATE_INVENTORY"
    const val STOCK_ADJUSTMENT = "STOCK_ADJUSTMENT"

    // ── REPORTS PERMISSIONS ──
    const val VIEW_REPORTS = "VIEW_REPORTS"
    const val EXPORT_REPORTS = "EXPORT_REPORTS"

    // ── SETTINGS PERMISSIONS ──
    const val MANAGE_RECEIPT = "MANAGE_RECEIPT"
    const val MANAGE_PRINTER = "MANAGE_PRINTER"
    const val MANAGE_QR = "MANAGE_QR"
    const val MANAGE_TAXES = "MANAGE_TAXES"

    // ── STAFF PERMISSIONS ──
    const val VIEW_STAFF = "VIEW_STAFF"
    const val ADD_STAFF = "ADD_STAFF"
    const val EDIT_STAFF = "EDIT_STAFF"
    const val DELETE_STAFF = "DELETE_STAFF"
    const val MANAGE_PERMISSIONS = "MANAGE_PERMISSIONS"

    fun can(action: String): Boolean {
        val authManager = OptixApplication.instance.authManager
        val role = authManager.userRole.value.lowercase()
        if (role == "admin" || role == "owner") return true
        return authManager.hasPermission(action)
    }

    fun canAny(vararg actions: String): Boolean {
        return actions.any { can(it) }
    }
}
