package com.example.services.ai

import java.util.Locale
import java.util.UUID

data class ExtractedProduct(
    val tempId: String = UUID.randomUUID().toString(),
    var name: String,
    var price: Double,
    var categoryName: String = "General",
    var description: String = "",
    var isVeg: Boolean = true,
    var pricingType: String = "FIXED", // FIXED, WEIGHT_BASED
    var unit: String = "Piece", // Piece, kg, g, 250g, 500g
    var confidence: String = "HIGH", // HIGH, MEDIUM, LOW
    var isSelected: Boolean = true
)

object MenuAiParser {

    private val IGNORED_KEYWORDS = listOf(
        "gst", "tax", "address", "phone", "tel", "mobile", "fssai", "welcome",
        "thank you", "visit again", "menu", "page", "terms", "condition", "offer",
        "discount", "wifi", "password", "service charge", "subtotal", "total", "invoice"
    )

    private val VEG_KEYWORDS = listOf("veg", "vegetarian", "paneer", "dal", "sambar", "dosa", "idli", "aloo", "gobi", "mushroom")
    private val NON_VEG_KEYWORDS = listOf("non-veg", "chicken", "mutton", "fish", "prawn", "egg", "kebabs", "seafood")

    fun parseOcrText(ocrText: String): List<ExtractedProduct> {
        val lines = ocrText.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        val extractedProducts = mutableListOf<ExtractedProduct>()
        var currentCategory = "General"

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val lowerLine = line.lowercase(Locale.ROOT)

            // Skip header/footer/GST disclaimers
            if (IGNORED_KEYWORDS.any { lowerLine.contains(it) }) {
                i++
                continue
            }

            // Detect category header lines (e.g. "--- STARTERS ---", "MAIN COURSE", "BEVERAGES")
            if (isCategoryHeader(line)) {
                currentCategory = cleanCategoryName(line)
                i++
                continue
            }

            // 1. Try extracting price from current single line
            val priceMatch = extractPrice(line)
            if (priceMatch != null && priceMatch.second > 0) {
                val price = priceMatch.second
                val rawName = priceMatch.first.trim()

                if (rawName.length >= 2 && !IGNORED_KEYWORDS.any { rawName.lowercase().contains(it) }) {
                    val isVeg = determineVegStatus(rawName)
                    val cleanName = cleanProductName(rawName)

                    if (cleanName.isNotBlank() && extractedProducts.none { it.name.equals(cleanName, ignoreCase = true) }) {
                        extractedProducts.add(
                            ExtractedProduct(
                                name = cleanName,
                                price = price,
                                categoryName = currentCategory,
                                description = "",
                                isVeg = isVeg,
                                unit = "Piece",
                                isSelected = true
                            )
                        )
                    }
                }
                i++
                continue
            }

            // 2. Try pairing current line (Name) with next line (Price)
            if (i + 1 < lines.size) {
                val nextLine = lines[i + 1].trim()
                val nextPrice = nextLine.replace(Regex("""[^0-9.]"""), "").toDoubleOrNull()
                if (nextPrice != null && nextPrice in 1.0..99999.0) {
                    val cleanName = cleanProductName(line)
                    if (cleanName.length >= 2 && !IGNORED_KEYWORDS.any { cleanName.lowercase().contains(it) }) {
                        if (extractedProducts.none { it.name.equals(cleanName, ignoreCase = true) }) {
                            extractedProducts.add(
                                ExtractedProduct(
                                    name = cleanName,
                                    price = nextPrice,
                                    categoryName = currentCategory,
                                    description = "",
                                    isVeg = determineVegStatus(cleanName),
                                    unit = "Piece",
                                    isSelected = true
                                )
                            )
                        }
                        i += 2
                        continue
                    }
                }
            }

            i++
        }

        // 3. Fallback: If no products extracted but OCR text exists, convert valid non-header lines into candidates
        if (extractedProducts.isEmpty() && lines.isNotEmpty()) {
            for (line in lines) {
                val cleanName = cleanProductName(line)
                if (cleanName.length >= 3 && !IGNORED_KEYWORDS.any { cleanName.lowercase().contains(it) }) {
                    if (extractedProducts.none { it.name.equals(cleanName, ignoreCase = true) }) {
                        extractedProducts.add(
                            ExtractedProduct(
                                name = cleanName,
                                price = 100.0,
                                categoryName = "General",
                                description = "",
                                isVeg = determineVegStatus(cleanName),
                                unit = "Piece",
                                isSelected = true
                            )
                        )
                    }
                }
            }
        }

        return extractedProducts
    }

    private fun isCategoryHeader(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.length < 3 || trimmed.length > 30) return false
        if (trimmed.contains(Regex("""\d{2,}"""))) return false // Contains numbers like price

        val upper = trimmed.uppercase(Locale.ROOT)
        return trimmed == upper || trimmed.startsWith("-") || trimmed.endsWith("-") ||
                trimmed.startsWith("*") || trimmed.contains("category", ignoreCase = true) ||
                listOf("STARTERS", "MAINS", "MAIN COURSE", "BEVERAGES", "DRINKS", "DESSERTS", "SNACKS", "SOUPS", "SPECIALS", "BREADS", "RICE", "THALI")
                    .any { upper.contains(it) }
    }

    private fun cleanCategoryName(line: String): String {
        return line.replace(Regex("""[^a-zA-Z0-9\s]"""), "").trim().capitalizeWords()
    }

    private fun extractPrice(line: String): Pair<String, Double>? {
        val sanitizedLine = line.replace(Regex("""[\.\-–—]{2,}"""), " ").trim()
        val regexes = listOf(
            Regex("""^(.*?)(?:Rs\.?|INR|₹)?\s*(\d+(?:\.\d{1,2})?)\s*(?:/-)?$"""),
            Regex("""^(.*?)\s+(\d{2,5})$"""),
            Regex("""^(.*?)\s*[:=]\s*(\d+(?:\.\d{1,2})?)$""")
        )

        for (regex in regexes) {
            val match = regex.find(sanitizedLine)
            if (match != null && match.groupValues.size >= 3) {
                val namePart = match.groupValues[1]
                val priceVal = match.groupValues[2].toDoubleOrNull()
                if (priceVal != null && priceVal in 1.0..99999.0) {
                    return Pair(namePart, priceVal)
                }
            }
        }

        return null
    }

    private fun determineVegStatus(name: String): Boolean {
        val lower = name.lowercase(Locale.ROOT)
        if (NON_VEG_KEYWORDS.any { lower.contains(it) }) return false
        if (VEG_KEYWORDS.any { lower.contains(it) }) return true
        return true
    }

    private fun cleanProductName(name: String): String {
        val cleaned = name.replace(Regex("""^[\d\s.\-*•]+"""), "")
            .replace(Regex("""[\.\-–—]{2,}"""), " ")
            .replace(Regex("""[^\w\s&().-]"""), "")
            .trim()
        return cleaned.capitalizeWords()
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
}
