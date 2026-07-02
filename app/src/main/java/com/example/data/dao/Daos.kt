package com.example.data.dao

import androidx.room.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessProfileDao {
    @Query("SELECT * FROM business_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<BusinessProfile?>

    @Query("SELECT * FROM business_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileSync(): BusinessProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: BusinessProfile)
}

@Dao
interface BillingItemDao {
    @Query("SELECT * FROM billing_items ORDER BY category, name")
    fun getAllItems(): Flow<List<BillingItem>>

    @Query("SELECT * FROM billing_items WHERE category = :category ORDER BY name")
    fun getItemsByCategory(category: String): Flow<List<BillingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: BillingItem)

    @Update
    suspend fun updateItem(item: BillingItem)

    @Delete
    suspend fun deleteItem(item: BillingItem)

    @Query("DELETE FROM billing_items WHERE id = :id")
    suspend fun deleteItemById(id: Int)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isCustom, name")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY isCustom, name")
    suspend fun getAllCategoriesSync(): List<Category>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)
}

@Dao
interface BillOrderDao {
    @Query("SELECT * FROM bill_orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<BillOrder>>

    @Query("SELECT * FROM bill_orders WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    fun getOrdersInTimeRange(start: Long, end: Long): Flow<List<BillOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: BillOrder)

    @Delete
    suspend fun deleteOrder(order: BillOrder)
}

@Dao
interface PrinterConfigDao {
    @Query("SELECT * FROM printer_config WHERE id = 1 LIMIT 1")
    fun getPrinterConfig(): Flow<PrinterConfig?>

    @Query("SELECT * FROM printer_config WHERE id = 1 LIMIT 1")
    suspend fun getPrinterConfigSync(): PrinterConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrinterConfig(config: PrinterConfig)
}

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff_accounts ORDER BY name ASC")
    fun getAllStaff(): Flow<List<Staff>>

    @Query("SELECT * FROM staff_accounts WHERE username = :username LIMIT 1")
    suspend fun getStaffByUsername(username: String): Staff?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: Staff)

    @Update
    suspend fun updateStaff(staff: Staff)

    @Delete
    suspend fun deleteStaff(staff: Staff)
}
