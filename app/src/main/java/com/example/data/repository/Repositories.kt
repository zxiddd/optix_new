package com.example.data.repository

import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

class BusinessProfileRepository(private val dao: BusinessProfileDao) {
    val profile: Flow<BusinessProfile?> = dao.getProfile()
    
    suspend fun getProfileSync(): BusinessProfile? = dao.getProfileSync()
    
    suspend fun saveProfile(profile: BusinessProfile) = dao.insertProfile(profile)
}

class BillingItemRepository(private val dao: BillingItemDao) {
    val allItems: Flow<List<BillingItem>> = dao.getAllItems()
    
    fun getItemsByCategory(category: String): Flow<List<BillingItem>> = dao.getItemsByCategory(category)
    
    suspend fun insert(item: BillingItem) = dao.insertItem(item)
    
    suspend fun update(item: BillingItem) = dao.updateItem(item)
    
    suspend fun delete(item: BillingItem) = dao.deleteItem(item)
    
    suspend fun deleteById(id: Int) = dao.deleteItemById(id)
}

class CategoryRepository(private val dao: CategoryDao) {
    val allCategories: Flow<List<Category>> = dao.getAllCategories()
    
    suspend fun getAllCategoriesSync(): List<Category> = dao.getAllCategoriesSync()
    
    suspend fun insert(category: Category) = dao.insertCategory(category)
    
    suspend fun delete(category: Category) = dao.deleteCategory(category)
}

class BillOrderRepository(private val dao: BillOrderDao) {
    val allOrders: Flow<List<BillOrder>> = dao.getAllOrders()
    
    fun getOrdersInTimeRange(start: Long, end: Long): Flow<List<BillOrder>> {
        return dao.getOrdersInTimeRange(start, end)
    }
    
    suspend fun insert(order: BillOrder) = dao.insertOrder(order)
    
    suspend fun delete(order: BillOrder) = dao.deleteOrder(order)
}

class PrinterConfigRepository(private val dao: PrinterConfigDao) {
    val printerConfig: Flow<PrinterConfig?> = dao.getPrinterConfig()
    
    suspend fun getPrinterConfigSync(): PrinterConfig? = dao.getPrinterConfigSync()
    
    suspend fun savePrinterConfig(config: PrinterConfig) = dao.insertPrinterConfig(config)
}

class StaffRepository(private val dao: StaffDao) {
    val allStaff: Flow<List<Staff>> = dao.getAllStaff()

    suspend fun getStaffByUsername(username: String): Staff? = dao.getStaffByUsername(username)

    suspend fun insert(staff: Staff) = dao.insertStaff(staff)

    suspend fun update(staff: Staff) = dao.updateStaff(staff)

    suspend fun delete(staff: Staff) = dao.deleteStaff(staff)
}
