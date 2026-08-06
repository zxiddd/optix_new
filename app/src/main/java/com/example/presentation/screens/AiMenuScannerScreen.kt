package com.example.presentation.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.presentation.viewmodel.AiMenuScannerViewModel
import com.example.presentation.viewmodel.OnboardingStep
import com.example.services.ai.ExtractedProduct
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMenuScannerScreen(
    viewModel: AiMenuScannerViewModel,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val currentStep by viewModel.currentStep.collectAsState()
    val selectedUris by viewModel.selectedImageUris.collectAsState()
    val extractedProducts by viewModel.extractedProducts.collectAsState()
    val detectedCategories by viewModel.detectedCategories.collectAsState()
    val currentWizardIndex by viewModel.currentWizardIndex.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()
    val summary by viewModel.importSummary.collectAsState()

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            val valid = uris.filter { uri ->
                val type = context.contentResolver.getType(uri) ?: ""
                type.contains("jpeg") || type.contains("jpg") || type.contains("png") || type.contains("webp") || type.isEmpty()
            }
            viewModel.setSelectedImages(selectedUris + valid)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            viewModel.setSelectedImages(selectedUris + tempCameraUri!!)
        }
    }

    fun launchCamera() {
        try {
            val photoFile = File(context.cacheDir, "menu_cam_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✨ Build My Menu with AI", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    if (currentStep != OnboardingStep.WELCOME) {
                        TextButton(onClick = { viewModel.resetScanner() }) {
                            Text("Reset", color = OrangePrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (currentStep) {
                OnboardingStep.WELCOME -> {
                    StepWelcome(
                        selectedUris = selectedUris,
                        onCameraClick = { launchCamera() },
                        onGalleryClick = { galleryLauncher.launch("image/*") },
                        onRemoveImage = { viewModel.removeImage(it) },
                        onStartScan = { viewModel.startAiRestaurantSetup(context) }
                    )
                }
                OnboardingStep.AI_PROGRESS -> {
                    StepAiProgress(statusMsg)
                }
                OnboardingStep.CATEGORY_REVIEW -> {
                    StepCategoryReview(
                        categories = detectedCategories,
                        onRename = { oldName, newName -> viewModel.renameCategory(oldName, newName) },
                        onDelete = { viewModel.deleteCategory(it) },
                        onProceed = { viewModel.confirmCategoriesAndProceed() }
                    )
                }
                OnboardingStep.PRODUCT_WIZARD -> {
                    if (extractedProducts.isNotEmpty() && currentWizardIndex in extractedProducts.indices) {
                        val currentProduct = extractedProducts[currentWizardIndex]
                        StepProductWizard(
                            product = currentProduct,
                            currentIndex = currentWizardIndex + 1,
                            totalCount = extractedProducts.size,
                            onAccept = { viewModel.acceptWizardProduct(it) },
                            onReject = { viewModel.rejectWizardProduct(it) },
                            onUpdate = { viewModel.updateProduct(it) },
                            onSkipToSummary = { viewModel.calculateImportSummaryAndShow() }
                        )
                    }
                }
                OnboardingStep.IMPORT_SUMMARY -> {
                    StepImportSummary(
                        summary = summary,
                        isProcessing = isProcessing,
                        onConfirmImport = { viewModel.confirmImport() },
                        onDone = {
                            viewModel.resetScanner()
                            onFinish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StepWelcome(
    selectedUris: List<Uri>,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onRemoveImage: (Uri) -> Unit,
    onStartScan: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("🍽 Welcome to Optix", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Set up your restaurant menu in under 2 minutes.", color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Powered by Gemini Vision AI to extract items, prices & categories.", color = Color.Gray, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onCameraClick,
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("📷 Take Photos", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onGalleryClick,
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = OrangePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("🖼 Choose Images", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedUris.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(52.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Menu Images Selected", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Capture printed menu photos to get started", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(selectedUris) { uri ->
                    Box(
                        modifier = Modifier
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceDark)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { onRemoveImage(uri) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onStartScan,
            enabled = selectedUris.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("✨ BUILD MY MENU WITH AI", fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
    }
}

@Composable
fun StepAiProgress(statusMsg: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            CircularProgressIndicator(color = OrangePrimary, modifier = Modifier.size(64.dp), strokeWidth = 6.dp)
            Spacer(modifier = Modifier.height(28.dp))
            Text("AI RESTAURANT ASSISTANT AT WORK", fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(statusMsg, color = OrangePrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun StepCategoryReview(
    categories: List<String>,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onProceed: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("ORGANIZING CATEGORIES", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
        Text("AI detected ${categories.size} categories. Rename or delete if needed.", color = Color.Gray, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories) { catName ->
                var editingName by remember(catName) { mutableStateOf(catName) }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = OrangePrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = editingName,
                            onValueChange = {
                                editingName = it
                                onRename(catName, it)
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        IconButton(onClick = { onDelete(catName) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onProceed,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("PROCEED TO PRODUCT REVIEW WIZARD →", fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun StepProductWizard(
    product: ExtractedProduct,
    currentIndex: Int,
    totalCount: Int,
    onAccept: (ExtractedProduct) -> Unit,
    onReject: (String) -> Unit,
    onUpdate: (ExtractedProduct) -> Unit,
    onSkipToSummary: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PRODUCT WIZARD ($currentIndex / $totalCount)", fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
            TextButton(onClick = onSkipToSummary) {
                Text("Skip to Summary", color = OrangePrimary, fontWeight = FontWeight.Bold)
            }
        }

        LinearProgressIndicator(
            progress = { currentIndex.toFloat() / totalCount.toFloat() },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = OrangePrimary,
            trackColor = SurfaceDark
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Full Reused Product Form Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (product.isVeg) "🟢 VEG" else "🔴 NON-VEG",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (product.isVeg) Color.Green else Color.Red
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        color = if (product.confidence == "HIGH") Color.Green.copy(alpha = 0.2f) else OrangePrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "AI Confidence: ${product.confidence}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (product.confidence == "HIGH") Color.Green else OrangePrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = product.name,
                    onValueChange = { onUpdate(product.copy(name = it)) },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = if (product.price == 0.0) "" else product.price.toString(),
                        onValueChange = { onUpdate(product.copy(price = it.toDoubleOrNull() ?: 0.0)) },
                        label = { Text("Price (Rs.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = product.categoryName,
                        onValueChange = { onUpdate(product.copy(categoryName = it)) },
                        label = { Text("Category") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pricing Type & Unit Controls (Preserving WEIGHT_BASED without converting to Fixed)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = product.pricingType == "FIXED",
                        onClick = { onUpdate(product.copy(pricingType = "FIXED", unit = "Piece")) },
                        label = { Text("FIXED PRICE") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OrangePrimary, selectedLabelColor = Color.Black)
                    )

                    FilterChip(
                        selected = product.pricingType == "WEIGHT_BASED",
                        onClick = { onUpdate(product.copy(pricingType = "WEIGHT_BASED", unit = "kg")) },
                        label = { Text("⚖️ WEIGHT BASED") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OrangePrimary, selectedLabelColor = Color.Black)
                    )
                }

                if (product.pricingType == "WEIGHT_BASED") {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = product.unit,
                        onValueChange = { onUpdate(product.copy(unit = it)) },
                        label = { Text("Weight Unit (e.g. kg, g, 250g, 500g)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = product.description,
                    onValueChange = { onUpdate(product.copy(description = it)) },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tinder-Style Accept / Reject Action Row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onReject(product.tempId) },
                modifier = Modifier.weight(1f).height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("REJECT (SWIPE LEFT)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = { onAccept(product) },
                modifier = Modifier.weight(1f).height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ACCEPT (SWIPE RIGHT)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StepImportSummary(
    summary: com.example.presentation.viewmodel.EnterpriseImportSummary,
    isProcessing: Boolean,
    onConfirmImport: () -> Unit,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green, modifier = Modifier.size(64.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Text("RESTAURANT MENU READY", fontWeight = FontWeight.Black, color = Color.White, fontSize = 22.sp)
            Text("Review breakdown before importing into Optix POS", color = Color.Gray, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryRow("Categories", summary.categoryCount.toString(), Color.White)
                    SummaryRow("Total Products", summary.totalProducts.toString(), OrangePrimary)
                    SummaryRow("Fixed Price Items", summary.fixedCount.toString(), Color.White)
                    SummaryRow("Weight-Based Items (秤重)", summary.weightBasedCount.toString(), Color.Green)
                    SummaryRow("Low Confidence Items", summary.lowConfidenceCount.toString(), Color.Yellow)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (summary.totalProducts > 0 && !isProcessing) {
                Button(
                    onClick = onConfirmImport,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("IMPORT RESTAURANT MENU", fontWeight = FontWeight.Black)
                }
            } else if (isProcessing) {
                CircularProgressIndicator(color = OrangePrimary)
            } else {
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("DONE & VIEW MENU", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SummaryRow(title: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = Color.Gray, fontSize = 13.sp)
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
