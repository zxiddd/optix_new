package com.example.data.repository

import com.example.data.entity.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudRepository(private val userId: String) {
    private val db = FirebaseFirestore.getInstance()
    private val userDoc = db.collection("users").document(userId)

    // --- Business Profile ---
    val profile: Flow<BusinessProfile?> = userDoc.collection("config")
        .document("profile")
        .snapshots()
        .map { it.toObject<BusinessProfile>() }

    suspend fun saveProfile(profile: BusinessProfile) {
        userDoc.collection("config").document("profile").set(profile).await()
    }

    // --- Items ---
    val allItems: Flow<List<BillingItem>> = userDoc.collection("items")
        .orderBy("sortOrder")
        .snapshots()
        .map { it.toObjects<BillingItem>() }

    suspend fun insertItem(item: BillingItem) {
        val docRef = if (item.id.isEmpty()) userDoc.collection("items").document() else userDoc.collection("items").document(item.id)
        val finalItem = if (item.id.isEmpty()) item.copy(id = docRef.id) else item
        docRef.set(finalItem).await()
    }

    suspend fun deleteItem(itemId: String) {
        userDoc.collection("items").document(itemId).delete().await()
    }

    suspend fun uploadImage(uri: Uri, fileName: String): String {
        val storageRef = FirebaseStorage.getInstance().reference.child("users/$userId/items/$fileName")
        storageRef.putFile(uri).await()
        return storageRef.downloadUrl.await().toString()
    }

    // --- Categories ---
    val allCategories: Flow<List<Category>> = userDoc.collection("categories")
        .orderBy("sortOrder")
        .snapshots()
        .map { it.toObjects<Category>() }

    suspend fun insertCategory(category: Category) {
        val docRef = if (category.id.isEmpty()) userDoc.collection("categories").document() else userDoc.collection("categories").document(category.id)
        val finalCat = if (category.id.isEmpty()) category.copy(id = docRef.id) else category
        docRef.set(finalCat).await()
    }

    suspend fun deleteCategory(categoryId: String) {
        userDoc.collection("categories").document(categoryId).delete().await()
    }

    // --- Orders ---
    val allOrders: Flow<List<BillOrder>> = userDoc.collection("orders")
        .orderBy("timestamp")
        .snapshots()
        .map { it.toObjects<BillOrder>() }

    suspend fun insertOrder(order: BillOrder) {
        val docRef = if (order.id.isEmpty()) userDoc.collection("orders").document() else userDoc.collection("orders").document(order.id)
        val finalOrder = if (order.id.isEmpty()) order.copy(id = docRef.id) else order
        docRef.set(finalOrder).await()
    }

    suspend fun deleteOrder(orderId: String) {
        userDoc.collection("orders").document(orderId).delete().await()
    }

    // --- Staff ---
    val allStaff: Flow<List<Staff>> = userDoc.collection("staff")
        .snapshots()
        .map { it.toObjects<Staff>() }

    suspend fun insertStaff(staff: Staff) {
        val docRef = if (staff.id.isEmpty()) userDoc.collection("staff").document() else userDoc.collection("staff").document(staff.id)
        val finalStaff = if (staff.id.isEmpty()) staff.copy(id = docRef.id) else staff
        docRef.set(finalStaff).await()
    }

    suspend fun updateStaff(staff: Staff) {
        userDoc.collection("staff").document(staff.id).set(staff).await()
    }

    suspend fun deleteStaff(staffId: String) {
        userDoc.collection("staff").document(staffId).delete().await()
    }

    // --- Subscriptions ---
    val subscription: Flow<UserSubscription?> = userDoc.collection("config")
        .document("subscription")
        .snapshots()
        .map { it.toObject<UserSubscription>() }

    suspend fun saveSubscription(sub: UserSubscription) {
        userDoc.collection("config").document("subscription").set(sub).await()
    }

    suspend fun getAvailablePlans(): List<SubscriptionPlan> {
        return db.collection("plans").get().await().toObjects<SubscriptionPlan>()
    }
}
