package com.example.services.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class GeminiParsedCategory(
    val name: String,
    val products: List<GeminiParsedProduct> = emptyList()
)

data class GeminiParsedProduct(
    val name: String,
    val price: Double,
    val category: String = "General",
    val description: String? = null,
    val pricingType: String = "FIXED", // FIXED, WEIGHT_BASED
    val unit: String = "Piece", // Piece, kg, g, 250g, 500g
    val isVeg: Boolean = true,
    val confidence: String = "HIGH" // HIGH, MEDIUM, LOW
)

data class GeminiMenuResponse(
    val restaurantName: String? = null,
    val categories: List<GeminiParsedCategory> = emptyList(),
    val products: List<GeminiParsedProduct> = emptyList()
)

object GeminiMenuVisionService {

    private val GEMINI_API_KEY = System.getenv("GEMINI_API_KEY") ?: ""
    private val API_URL: String
        get() = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$GEMINI_API_KEY"


    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    private val moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val SYSTEM_PROMPT = """
        You are an expert restaurant menu parser AI.
        Analyze the uploaded restaurant menu image.
        Understand the menu layout, categories, items, exact prices, veg/non-veg status, and weight-based pricing.
        
        STRICT RULES:
        1. Extract exact prices. Do not hallucinate. If price is unclear, mark confidence "LOW".
        2. Categorize products into logical categories (e.g. Starters, Main Course, Biryani, Rice, Chinese, Beverages, Desserts, Weight-Based Items). If categories exist on the menu, use them; if not, infer them.
        3. Detect weight-based pricing: Any item with units like kg, gm, g, 100g, 250g, 500g, 1kg, Per Kg, Per 100g, Fish, Mutton per kg MUST have pricingType = "WEIGHT_BASED" and proper unit (e.g., "kg", "g", "250g", "500g").
        4. Detect veg/non-veg status: Chicken, Mutton, Fish, Egg, Prawns are non-veg (isVeg: false). Paneer, Dal, Veg, Dosa, Idli are veg (isVeg: true).
        5. Ignore restaurant address, phone numbers, page numbers, GST lines, offers, terms, and watermarks.
        6. Return ONLY valid JSON matching this exact structure:
        {
          "restaurantName": "Optional Restaurant Name",
          "categories": [
            {
              "name": "Category Name",
              "products": [
                {
                  "name": "Chicken Dum Biryani",
                  "price": 280.0,
                  "category": "Biryani",
                  "description": "Flavorful chicken biryani",
                  "pricingType": "FIXED",
                  "unit": "Piece",
                  "isVeg": false,
                  "confidence": "HIGH"
                }
              ]
            }
          ],
          "products": []
        }
    """.trimIndent()

    suspend fun parseMenuImage(context: Context, imageUri: Uri): GeminiMenuResponse = withContext(Dispatchers.IO) {
        try {
            val base64Image = encodeImageToBase64(context, imageUri) ?: return@withContext GeminiMenuResponse()

            val jsonPayload = """
                {
                  "contents": [
                    {
                      "parts": [
                        { "text": ${escapeJson(SYSTEM_PROMPT)} },
                        {
                          "inline_data": {
                            "mime_type": "image/jpeg",
                            "data": "$base64Image"
                          }
                        }
                      ]
                    }
                  ],
                  "generationConfig": {
                    "response_mime_type": "application/json"
                  }
                }
            """.trimIndent()

            val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBodyStr = response.body?.string() ?: ""

            if (response.isSuccessful && responseBodyStr.isNotBlank()) {
                val extractedJson = parseGeminiResponseText(responseBodyStr)
                if (extractedJson.isNotBlank()) {
                    val adapter = moshi.adapter(GeminiMenuResponse::class.java)
                    return@withContext adapter.fromJson(extractedJson) ?: GeminiMenuResponse()
                }
            }

            return@withContext GeminiMenuResponse()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext GeminiMenuResponse()
        }
    }

    private fun encodeImageToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap == null) return null

            // Scale down to max 1600px for optimal speed & quality
            val scaled = if (bitmap.width > 1600 || bitmap.height > 1600) {
                val ratio = 1600f / Math.max(bitmap.width, bitmap.height).toFloat()
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
            } else bitmap

            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseGeminiResponseText(rawResponse: String): String {
        return try {
            val moshiGeneric = Moshi.Builder().build()
            val mapAdapter = moshiGeneric.adapter(Map::class.java)
            val jsonMap = mapAdapter.fromJson(rawResponse) as? Map<*, *> ?: return ""

            val candidates = jsonMap["candidates"] as? List<*> ?: return ""
            if (candidates.isEmpty()) return ""

            val firstCandidate = candidates[0] as? Map<*, *> ?: return ""
            val content = firstCandidate["content"] as? Map<*, *> ?: return ""
            val parts = content["parts"] as? List<*> ?: return ""
            if (parts.isEmpty()) return ""

            val firstPart = parts[0] as? Map<*, *> ?: return ""
            val text = firstPart["text"] as? String ?: ""

            // Clean markdown ```json blocks if present
            text.replace(Regex("""```json\s*"""), "")
                .replace(Regex("""```\s*"""), "")
                .trim()
        } catch (e: Exception) {
            ""
        }
    }

    private fun escapeJson(str: String): String {
        val adapter = moshi.adapter(String::class.java)
        return adapter.toJson(str)
    }
}
