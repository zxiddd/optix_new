package com.example.data.repository

import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

class BusinessProfileRepository(private val dao: BusinessProfileDao) {
    val profile: Flow<BusinessProfile?> = dao.getProfile()

    fun getProfileSync(): BusinessProfile? = dao.getProfileSync()

    suspend fun saveProfile(profile: BusinessProfile) = dao.insertProfile(profile)
}

class BillingItemRepository(private val dao: BillingItemDao) {
    val allItems: Flow<List<BillingItem>> = dao.getAllItems()

    fun getItemsByCategory(catId: String): Flow<List<BillingItem>> = dao.getItemsByCategory(catId)

    suspend fun insert(item: BillingItem) = dao.insertItem(item)

    suspend fun update(item: BillingItem) = dao.updateItem(item)

    suspend fun delete(item: BillingItem) = dao.deleteItem(item)

    suspend fun deleteById(itemId: String) = dao.deleteItemById(itemId)

    suspend fun moveItemsToCategory(oldCatId: String, newCatId: String) = dao.moveItemsToCategory(oldCatId, newCatId)

    suspend fun deleteItemsByCategory(catId: String) = dao.deleteItemsByCategory(catId)
}

class CategoryRepository(private val dao: CategoryDao) {
    val allCategories: Flow<List<Category>> = dao.getAllCategories()

    suspend fun getAllCategoriesSync(): List<Category> = dao.getAllCategoriesSync()

    suspend fun insert(category: Category) = dao.insertCategory(category)

    suspend fun update(category: Category) = dao.updateCategory(category)

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

    fun getPrinterConfigSync(): PrinterConfig? = dao.getPrinterConfigSync()

    suspend fun savePrinterConfig(config: PrinterConfig) = dao.insertPrinterConfig(config)
}

class StaffRepository(private val dao: StaffDao) {
    val allStaff: Flow<List<Staff>> = dao.getAllStaff()

    suspend fun getStaffByUsername(user: String): Staff? = dao.getStaffByUsername(user)

    suspend fun insert(staff: Staff) = dao.insertStaff(staff)

    suspend fun update(staff: Staff) = dao.updateStaff(staff)

    suspend fun delete(staff: Staff) = dao.deleteStaff(staff)
}

class DailyReportRepository(private val dao: DailyReportDao) {
    val allReports: Flow<List<DailyReport>> = dao.getAllReports()

    suspend fun insert(report: DailyReport) = dao.insertReport(report)

    suspend fun delete(report: DailyReport) = dao.deleteReport(report)
}

class SubscriptionRepository(private val dao: SubscriptionDao) {
    val subscription: Flow<UserSubscription?> = dao.getSubscription()

    suspend fun saveSubscription(sub: UserSubscription) = dao.insertSubscription(sub)
}

class PaymentQrRepository(private val dao: PaymentQrDao) {
    val allQrs: Flow<List<PaymentQrEntity>> = dao.getAllQrs()
    val activeQr: Flow<PaymentQrEntity?> = dao.getActiveQrFlow()

    suspend fun getActiveQrSync(): PaymentQrEntity? = dao.getActiveQr()

    suspend fun insert(qr: PaymentQrEntity) = dao.insertQr(qr)

    suspend fun delete(qr: PaymentQrEntity) = dao.deleteQr(qr)

    suspend fun setActive(qrId: String) = dao.setActiveQr(qrId)
}

class SupportTicketRepository(private val dao: SupportTicketDao) {
    val allTickets: Flow<List<SupportTicket>> = dao.getAllTickets()

    suspend fun insert(ticket: SupportTicket) = dao.insertTicket(ticket)
}
