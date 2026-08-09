package com.example.data.repository

import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

class BusinessProfileRepository(private val dao: BusinessProfileDao) {
    val profile: Flow<BusinessProfile?> = dao.getProfile()

    suspend fun getProfileSync(): BusinessProfile? = dao.getProfileSync()

    suspend fun saveProfile(profile: BusinessProfile) = dao.insertProfile(profile)
    
    suspend fun getBillCountSync(): Int = 0 // Placeholder or use orderRepo
    suspend fun getProductCountSync(): Int = 0
}

class CategoryRepository(private val dao: CategoryDao) {
    val allCategories: Flow<List<Category>> = dao.getAllCategories()

    suspend fun getAllCategoriesSync(): List<Category> = dao.getAllCategoriesSync()

    suspend fun insert(category: Category) = dao.insertCategory(category)

    suspend fun update(category: Category) = dao.updateCategory(category)

    suspend fun delete(category: Category) = dao.deleteCategory(category)
}

class BillingItemRepository(private val dao: BillingItemDao) {
    val allItems: Flow<List<BillingItem>> = dao.getAllItems()

    suspend fun getAllItemsSync(): List<BillingItem> = dao.getAllItemsSync()

    fun getItemsByCategory(categoryId: String): Flow<List<BillingItem>> {
        return dao.getItemsByCategory(categoryId)
    }

    suspend fun insert(item: BillingItem) {
        val prods = getAllItemsSync().size
        com.example.services.FeatureGate.setUsageOverrides(
            bills = com.example.services.FeatureGate.subscription.value?.billsUsed ?: 0,
            products = prods
        )
        
        // Only check limit for NEW products
        val isNew = getAllItemsSync().none { it.id == item.id }
        if (isNew && !com.example.services.FeatureGate.canCreateProduct()) {
            throw com.example.services.TrialLimitException("Product limit reached (5 products max in Trial)")
        }
        dao.insertItem(item)
    }

    suspend fun update(item: BillingItem) = dao.updateItem(item)

    suspend fun delete(item: BillingItem) = dao.deleteItem(item)

    suspend fun deleteById(id: String) = dao.deleteItemById(id)

    suspend fun moveItemsToCategory(oldCatId: String, newCatId: String) = dao.moveItemsToCategory(oldCatId, newCatId)

    suspend fun deleteItemsByCategory(catId: String) = dao.deleteItemsByCategory(catId)
}

class BillOrderRepository(private val dao: BillOrderDao) {
    val allOrders: Flow<List<BillOrder>> = dao.getAllOrders()

    suspend fun getOrdersSync(): List<BillOrder> = dao.getAllOrdersSync()

    fun getOrdersInTimeRange(start: Long, end: Long): Flow<List<BillOrder>> {
        return dao.getOrdersInTimeRange(start, end)
    }

    suspend fun insert(order: BillOrder) {
        val bills = getOrdersSync().size
        com.example.services.FeatureGate.setUsageOverrides(
            bills = bills,
            products = com.example.services.FeatureGate.subscription.value?.productsUsed ?: 0
        )

        if (!com.example.services.FeatureGate.canCreateBill()) {
            throw com.example.services.TrialLimitException("Billing limit reached (50 bills max in Trial)")
        }
        dao.insertOrder(order)
    }

    suspend fun delete(order: BillOrder) = dao.deleteOrder(order)
}

class PrinterConfigRepository(private val dao: PrinterConfigDao) {
    val printerConfig: Flow<PrinterConfig?> = dao.getPrinterConfig()

    suspend fun getPrinterConfigSync(): PrinterConfig? = dao.getPrinterConfigSync()

    suspend fun savePrinterConfig(config: PrinterConfig) = dao.insertPrinterConfig(config)
}

class StaffRepository(private val dao: StaffDao) {
    val allStaff: Flow<List<Staff>> = dao.getAllStaff()

    suspend fun getStaffByUsername(user: String): Staff? = dao.getStaffByUsername(user)

    suspend fun getStaffById(id: String): Staff? = dao.getStaffById(id)

    suspend fun getAllSync(): List<Staff> = dao.getAllStaffSync()

    suspend fun insert(staff: Staff) = dao.insertStaff(staff)

    suspend fun update(staff: Staff) = dao.updateStaff(staff)

    suspend fun delete(staff: Staff) = dao.deleteStaff(staff)

    suspend fun deleteById(id: String) = dao.deleteById(id)

    suspend fun setDisabled(id: String, isDisabled: Boolean) = dao.setDisabled(id, isDisabled)

    suspend fun updatePermissionsJson(id: String, permissionsJson: String) = dao.updatePermissionsJson(id, permissionsJson)

    suspend fun updateLastActivityAt(id: String, timestamp: Long) = dao.updateLastActivityAt(id, timestamp)
}

class DailyReportRepository(private val dao: DailyReportDao) {
    val allReports: Flow<List<DailyReport>> = dao.getAllReports()

    suspend fun insert(report: DailyReport) = dao.insertReport(report)

    suspend fun delete(report: DailyReport) = dao.deleteReport(report)
}

class SubscriptionRepository(private val dao: SubscriptionDao) {
    val subscription: Flow<UserSubscription?> = dao.getSubscription()

    suspend fun getSubscriptionSync(): UserSubscription? = dao.getSubscriptionSync()

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

class StaffActivityLogRepository(private val dao: StaffActivityLogDao) {
    val allLogs: Flow<List<StaffActivityLog>> = dao.getAllLogs()

    fun getLogsByStaff(staffId: String): Flow<List<StaffActivityLog>> = dao.getLogsByStaff(staffId)

    suspend fun getRecentLogs(limit: Int = 50): List<StaffActivityLog> = dao.getRecentLogs(limit)

    suspend fun insert(log: StaffActivityLog) = dao.insertLog(log)

    suspend fun pruneOlderThan(cutoff: Long) = dao.pruneOlderThan(cutoff)
}

class StaffSessionRepository(private val dao: StaffSessionDao) {
    val allSessions: Flow<List<StaffSession>> = dao.getAllSessions()

    fun getSessionsByStaff(staffId: String): Flow<List<StaffSession>> = dao.getSessionsByStaff(staffId)

    suspend fun getActiveSessions(): List<StaffSession> = dao.getActiveSessions()

    suspend fun insertSession(session: StaffSession) = dao.insertSession(session)

    suspend fun closeSession(id: String) = dao.closeSession(id)
}

class NotificationRepository(private val dao: NotificationDao) {
    val unreadNotifications: Flow<List<NotificationEntity>> = dao.getAllNotifications()
    val allNotifications: Flow<List<NotificationEntity>> = dao.getAllNotifications()

    suspend fun insert(notif: NotificationEntity) = dao.insertNotification(notif)

    suspend fun markAllRead() = dao.markAllRead()
}
