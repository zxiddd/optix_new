package com.example.services

data class PlanPricing(
    val planId: String,
    val planName: String,
    val monthlyPrice: Double,
    val yearlyPrice: Double,
    val currency: String,
    val features: List<String>,
    val nonFeatures: List<String> = emptyList()
)

object PricingEngine {
    private val pricingMap = mapOf(
        "India" to listOf(
            PlanPricing(
                planId = "STARTER",
                planName = "Starter",
                monthlyPrice = 499.0,
                yearlyPrice = 4849.0, // (499 * 12) * 0.9 (approx)
                currency = "INR",
                features = listOf(
                    "Unlimited Billing", "Unlimited Products", "Unlimited Categories",
                    "Weight Based Products", "Bluetooth Printing", "Offline Billing",
                    "Realtime Sync", "Basic Reports", "Business Logo"
                ),
                nonFeatures = listOf(
                    "Staff Management", "Inventory", "Customers", "Expenses",
                    "GST/Tax Settings", "Advanced Receipt", "Multiple QRs"
                )
            ),
            PlanPricing(
                planId = "GROWTH",
                planName = "Growth",
                monthlyPrice = 999.0,
                yearlyPrice = 9709.0,
                currency = "INR",
                features = listOf(
                    "Everything in Starter", "Unlimited Staff", "Permissions & Sessions",
                    "Inventory Management", "Expense Tracking", "Customer CRM",
                    "Advanced Reports", "GST & Tax Configuration", "Advanced Receipt",
                    "Unlimited Payment QRs"
                )
            )
        ),
        "Saudi Arabia" to listOf(
            PlanPricing(
                planId = "STARTER",
                planName = "Starter",
                monthlyPrice = 120.0,
                yearlyPrice = 1296.0,
                currency = "SAR",
                features = listOf(
                    "Unlimited Billing", "Unlimited Products", "Unlimited Categories",
                    "Weight Based Products", "Bluetooth Printing", "Offline Billing",
                    "Realtime Sync", "Basic Reports", "Business Logo"
                ),
                nonFeatures = listOf(
                    "Staff Management", "Inventory", "Customers", "Expenses",
                    "GST/Tax Settings", "Advanced Receipt", "Multiple QRs"
                )
            ),
            PlanPricing(
                planId = "GROWTH",
                planName = "Growth",
                monthlyPrice = 199.0,
                yearlyPrice = 2149.0,
                currency = "SAR",
                features = listOf(
                    "Everything in Starter", "Unlimited Staff", "Permissions & Sessions",
                    "Inventory Management", "Expense Tracking", "Customer CRM",
                    "Advanced Reports", "GST & Tax Configuration", "Advanced Receipt",
                    "Unlimited Payment QRs"
                )
            )
        )
    )

    fun getPlansForCountry(country: String): List<PlanPricing> {
        return pricingMap[country] ?: pricingMap["India"]!!
    }

    fun getCurrencyForCountry(country: String): String {
        return when (country) {
            "India" -> "INR"
            "Saudi Arabia" -> "SAR"
            else -> "INR"
        }
    }

    fun getCountryList(): List<String> = listOf("India", "Saudi Arabia")
}
