package com.example.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.BillingItem
import com.example.data.entity.Category
import com.example.data.repository.BillingItemRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.CloudRepository
import com.example.services.ai.ExtractedProduct
import com.example.services.ai.GeminiMenuVisionService
import com.example.services.ai.GeminiParsedProduct
import com.example.services.ai.MenuAiParser
import com.example.services.ai.MenuOcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

enum class OnboardingStep {
    WELCOME,
    AI_PROGRESS,
    CATEGORY_REVIEW,
    PRODUCT_WIZARD,
    IMPORT_SUMMARY
}

data class EnterpriseImportSummary(
    val categoryCount: Int = 0,
    val totalProducts: Int = 0,
    val weightBasedCount: Int = 0,
    val fixedCount: Int = 0,
    val duplicateCount: Int = 0,
    val skippedCount: Int = 0,
    val lowConfidenceCount: Int = 0
)

class AiMenuScannerViewModel(
    private val cloudRepo: CloudRepository,
    private val itemRepository: BillingItemRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _currentStep = MutableStateFlow(OnboardingStep.WELCOME)
    val currentStep: StateFlow<OnboardingStep> = _currentStep.asStateFlow()

    private val _selectedImageUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImageUris: StateFlow<List<Uri>> = _selectedImageUris.asStateFlow()

    private val _extractedProducts = MutableStateFlow<List<ExtractedProduct>>(emptyList())
    val extractedProducts: StateFlow<List<ExtractedProduct>> = _extractedProducts.asStateFlow()

    private val _detectedCategories = MutableStateFlow<List<String>>(emptyList())
    val detectedCategories: StateFlow<List<String>> = _detectedCategories.asStateFlow()

    private val _currentWizardIndex = MutableStateFlow(0)
    val currentWizardIndex: StateFlow<Int> = _currentWizardIndex.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _importSummary = MutableStateFlow(EnterpriseImportSummary())
    val importSummary: StateFlow<EnterpriseImportSummary> = _importSummary.asStateFlow()

    fun setSelectedImages(uris: List<Uri>) {
        _selectedImageUris.value = uris
    }

    fun removeImage(uri: Uri) {
        _selectedImageUris.value = _selectedImageUris.value.filter { it != uri }
    }

    fun resetScanner() {
        _currentStep.value = OnboardingStep.WELCOME
        _selectedImageUris.value = emptyList()
        _extractedProducts.value = emptyList()
        _detectedCategories.value = emptyList()
        _currentWizardIndex.value = 0
        _importSummary.value = EnterpriseImportSummary()
        _isProcessing.value = false
        _statusMessage.value = ""
    }

    fun startAiRestaurantSetup(context: Context) {
        val uris = _selectedImageUris.value
        if (uris.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            _currentStep.value = OnboardingStep.AI_PROGRESS

            val steps = listOf(
                "📷 Uploading Images...",
                "🧠 Gemini Understanding Your Menu...",
                "🍛 Organizing Categories...",
                "💰 Detecting Prices...",
                "⚖️ Detecting Weight-Based Items...",
                "📦 Preparing Restaurant..."
            )

            for (stepMsg in steps) {
                _statusMessage.value = stepMsg
                delay(400)
            }

            val allProducts = mutableListOf<ExtractedProduct>()
            val categorySet = mutableSetOf<String>()

            for (uri in uris) {
                val response = GeminiMenuVisionService.parseMenuImage(context, uri)

                // 1. Process structured response from Gemini Vision
                if (response.categories.isNotEmpty() || response.products.isNotEmpty()) {
                    for (cat in response.categories) {
                        val catName = cat.name.ifBlank { "General" }
                        categorySet.add(catName)
                        for (p in cat.products) {
                            allProducts.add(mapGeminiProduct(p, catName))
                        }
                    }
                    for (p in response.products) {
                        val catName = p.category.ifBlank { "General" }
                        categorySet.add(catName)
                        allProducts.add(mapGeminiProduct(p, catName))
                    }
                }

                // 2. Fallback to OCR + MenuAiParser if Gemini Vision response was empty
                if (allProducts.isEmpty()) {
                    _statusMessage.value = "Running fallback OCR analysis..."
                    val ocrRes = MenuOcrEngine.processUri(context, uri)
                    if (ocrRes.rawText.isNotBlank()) {
                        val fallbackParsed = MenuAiParser.parseOcrText(ocrRes.rawText)
                        allProducts.addAll(fallbackParsed)
                        fallbackParsed.forEach { categorySet.add(it.categoryName) }
                    }
                }
            }

            _extractedProducts.value = allProducts
            _detectedCategories.value = categorySet.toList()
            _isProcessing.value = false

            if (allProducts.isNotEmpty()) {
                _currentStep.value = OnboardingStep.CATEGORY_REVIEW
            } else {
                _currentStep.value = OnboardingStep.WELCOME
                _statusMessage.value = "No menu items recognized. Please ensure photo is clear and retry."
            }
        }
    }

    private fun mapGeminiProduct(p: GeminiParsedProduct, catName: String): ExtractedProduct {
        val isWeight = p.pricingType == "WEIGHT_BASED" || isWeightUnit(p.unit) || isWeightName(p.name)
        val cleanUnit = if (isWeight && p.unit == "Piece") "kg" else p.unit

        return ExtractedProduct(
            name = p.name,
            price = p.price,
            categoryName = catName,
            description = p.description ?: "",
            isVeg = p.isVeg,
            pricingType = if (isWeight) "WEIGHT_BASED" else "FIXED",
            unit = cleanUnit,
            confidence = p.confidence.ifBlank { "HIGH" },
            isSelected = true
        )
    }

    private fun isWeightUnit(unit: String): Boolean {
        val u = unit.lowercase(Locale.ROOT)
        return listOf("kg", "g", "gm", "250g", "500g", "1kg", "gram", "kilo").any { u.contains(it) }
    }

    private fun isWeightName(name: String): Boolean {
        val n = name.lowercase(Locale.ROOT)
        return listOf("per kg", "per 100g", "per 250g", "per 500g", "/kg", "/g", "per gram").any { n.contains(it) }
    }

    // Category Management
    fun renameCategory(oldName: String, newName: String) {
        if (newName.isBlank()) return
        val currentCats = _detectedCategories.value.toMutableList()
        val index = currentCats.indexOf(oldName)
        if (index != -1) {
            currentCats[index] = newName
            _detectedCategories.value = currentCats
        }
        // Update product category references
        _extractedProducts.value = _extractedProducts.value.map {
            if (it.categoryName.equals(oldName, ignoreCase = true)) it.copy(categoryName = newName) else it
        }
    }

    fun deleteCategory(catName: String) {
        _detectedCategories.value = _detectedCategories.value.filter { it != catName }
        _extractedProducts.value = _extractedProducts.value.map {
            if (it.categoryName.equals(catName, ignoreCase = true)) it.copy(categoryName = "General") else it
        }
    }

    fun confirmCategoriesAndProceed() {
        _currentStep.value = OnboardingStep.PRODUCT_WIZARD
        _currentWizardIndex.value = 0
    }

    // Wizard Product Actions
    fun acceptWizardProduct(product: ExtractedProduct) {
        updateProduct(product.copy(isSelected = true))
        advanceWizard()
    }

    fun rejectWizardProduct(tempId: String) {
        deleteProduct(tempId)
        advanceWizard()
    }

    private fun advanceWizard() {
        if (_currentWizardIndex.value < _extractedProducts.value.size - 1) {
            _currentWizardIndex.value = _currentWizardIndex.value + 1
        } else {
            calculateImportSummaryAndShow()
        }
    }

    fun updateProduct(product: ExtractedProduct) {
        _extractedProducts.value = _extractedProducts.value.map {
            if (it.tempId == product.tempId) product else it
        }
    }

    fun deleteProduct(tempId: String) {
        _extractedProducts.value = _extractedProducts.value.filter { it.tempId != tempId }
    }

    fun calculateImportSummaryAndShow() {
        val selectedProds = _extractedProducts.value.filter { it.isSelected }
        val catCount = selectedProds.map { it.categoryName }.distinct().size
        val weightCount = selectedProds.count { it.pricingType == "WEIGHT_BASED" }
        val fixedCount = selectedProds.count { it.pricingType == "FIXED" }
        val lowConfCount = selectedProds.count { it.confidence == "LOW" }

        _importSummary.value = EnterpriseImportSummary(
            categoryCount = catCount,
            totalProducts = selectedProds.size,
            weightBasedCount = weightCount,
            fixedCount = fixedCount,
            duplicateCount = 0,
            skippedCount = 0,
            lowConfidenceCount = lowConfCount
        )

        _currentStep.value = OnboardingStep.IMPORT_SUMMARY
    }

    fun confirmImport() {
        val selectedProds = _extractedProducts.value.filter { it.isSelected && it.name.isNotBlank() }
        if (selectedProds.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            _statusMessage.value = "Building your restaurant menu..."

            val existingItems = itemRepository.getAllItemsSync()
            val existingCategories = categoryRepository.getAllCategoriesSync()

            val categoryMap = mutableMapOf<String, String>()
            existingCategories.forEach { cat ->
                categoryMap[cat.name.lowercase()] = cat.id
            }

            var imported = 0
            var duplicates = 0
            var errors = 0

            for (p in selectedProds) {
                try {
                    val isDup = existingItems.any { item -> item.name.equals(p.name, ignoreCase = true) }
                    if (isDup) duplicates++

                    val catNameClean = p.categoryName.ifBlank { "General" }
                    val catKey = catNameClean.lowercase()
                    var catId = categoryMap[catKey]

                    if (catId == null) {
                        catId = UUID.randomUUID().toString()
                        val newCategory = Category(
                            id = catId,
                            name = catNameClean,
                            sortOrder = existingCategories.size + 1
                        )
                        categoryRepository.insert(newCategory)
                        categoryMap[catKey] = catId
                        try { cloudRepo.insertCategory(newCategory) } catch (e: Exception) {}
                    }

                    // PRESERVE WEIGHT_BASED PRICING TYPE AND UNIT ACCURATELY (Fix Bug B)
                    val isWeight = p.pricingType == "WEIGHT_BASED" || isWeightUnit(p.unit) || isWeightName(p.name)
                    val cleanPricingType = if (isWeight) "WEIGHT_BASED" else "FIXED"
                    val cleanUnit = if (isWeight && p.unit == "Piece") "kg" else p.unit

                    val newItem = BillingItem(
                        id = UUID.randomUUID().toString(),
                        name = p.name,
                        price = p.price,
                        categoryId = catId,
                        categoryName = catNameClean,
                        description = p.description,
                        isAvailable = true,
                        isOutOfStock = false,
                        pricingType = cleanPricingType,
                        unit = cleanUnit
                    )

                    itemRepository.insert(newItem)
                    try { cloudRepo.insertItem(newItem) } catch (e: Exception) {}

                    imported++
                } catch (e: Exception) {
                    errors++
                }
            }

            _isProcessing.value = false
            _currentStep.value = OnboardingStep.IMPORT_SUMMARY
        }
    }
}
