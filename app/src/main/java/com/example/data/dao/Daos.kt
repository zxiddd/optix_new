package com.example.data.dao

import androidx.room.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessProfileDao {
    @Query("SELECT * FROM business_profile WHERE id = 1")
    fun getProfile(): Flow<BusinessProfile?>

    @Query("SELECT * FROM business_profile WHERE id = 1")
    fun getProfileSync(): BusinessProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: BusinessProfile)
}

@Dao
interface BillingItemDao {
    @Query("SELECT * FROM billing_items")
    fun getAllItems(): Flow<List<BillingItem>>

    @Query("SELECT * FROM billing_items")
    suspend fun getAllItemsSync(): List<BillingItem>

    @Query("SELECT * FROM billing_items WHERE categoryId = :catId")
    fun getItemsByCategory(catId: String): Flow<List<BillingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: BillingItem)

    @Update
    suspend fun updateItem(item: BillingItem)

    @Delete
    suspend fun deleteItem(item: BillingItem)

    @Query("DELETE FROM billing_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: String)

    @Query("UPDATE billing_items SET categoryId = :newCatId WHERE categoryId = :oldCatId")
    suspend fun moveItemsToCategory(oldCatId: String, newCatId: String)

    @Query("DELETE FROM billing_items WHERE categoryId = :catId")
    suspend fun deleteItemsByCategory(catId: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY sortOrder")
    suspend fun getAllCategoriesSync(): List<Category>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)
}

@Dao
interface BillOrderDao {
    @Query("SELECT * FROM bill_orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<BillOrder>>

    @Query("SELECT * FROM bill_orders")
    suspend fun getAllOrdersSync(): List<BillOrder>

    @Query("SELECT * FROM bill_orders WHERE timestamp BETWEEN :start AND :end")
    fun getOrdersInTimeRange(start: Long, end: Long): Flow<List<BillOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: BillOrder)

    @Delete
    suspend fun deleteOrder(order: BillOrder)
}

@Dao
interface PrinterConfigDao {
    @Query("SELECT * FROM printer_config WHERE id = 1")
    fun getPrinterConfig(): Flow<PrinterConfig?>

    @Query("SELECT * FROM printer_config WHERE id = 1")
    fun getPrinterConfigSync(): PrinterConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrinterConfig(config: PrinterConfig)
}

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff_accounts WHERE isDeleted = 0")
    fun getAllStaff(): Flow<List<Staff>>

    @Query("SELECT * FROM staff_accounts WHERE isDeleted = 0")
    suspend fun getAllStaffSync(): List<Staff>

    @Query("SELECT * FROM staff_accounts WHERE id = :id LIMIT 1")
    suspend fun getStaffById(id: String): Staff?

    @Query("SELECT * FROM staff_accounts WHERE username = :user LIMIT 1")
    suspend fun getStaffByUsername(user: String): Staff?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: Staff)

    @Update
    suspend fun updateStaff(staff: Staff)

    @Delete
    suspend fun deleteStaff(staff: Staff)

    @Query("DELETE FROM staff_accounts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE staff_accounts SET isDisabled = :isDisabled WHERE id = :id")
    suspend fun setDisabled(id: String, isDisabled: Boolean)

    @Query("UPDATE staff_accounts SET permissionsJson = :permissionsJson WHERE id = :id")
    suspend fun updatePermissionsJson(id: String, permissionsJson: String)

    @Query("UPDATE staff_accounts SET lastActivityAt = :timestamp WHERE id = :id")
    suspend fun updateLastActivityAt(id: String, timestamp: Long)
}

@Dao
interface StaffActivityLogDao {
    @Query("SELECT * FROM staff_activity_logs ORDER BY createdAt DESC")
    fun getAllLogs(): Flow<List<StaffActivityLog>>

    @Query("SELECT * FROM staff_activity_logs WHERE staffId = :staffId ORDER BY createdAt DESC")
    fun getLogsByStaff(staffId: String): Flow<List<StaffActivityLog>>

    @Query("SELECT * FROM staff_activity_logs ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 50): List<StaffActivityLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: StaffActivityLog)

    @Query("DELETE FROM staff_activity_logs WHERE createdAt < :cutoff")
    suspend fun pruneOlderThan(cutoff: Long)
}

@Dao
interface StaffSessionDao {
    @Query("SELECT * FROM staff_sessions ORDER BY loginAt DESC")
    fun getAllSessions(): Flow<List<StaffSession>>

    @Query("SELECT * FROM staff_sessions WHERE staffId = :staffId ORDER BY loginAt DESC")
    fun getSessionsByStaff(staffId: String): Flow<List<StaffSession>>

    @Query("SELECT * FROM staff_sessions WHERE isActive = 1")
    suspend fun getActiveSessions(): List<StaffSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StaffSession)

    @Query("UPDATE staff_sessions SET isActive = 0, logoutAt = :logoutAt WHERE id = :id")
    suspend fun closeSession(id: String, logoutAt: Long = System.currentTimeMillis())
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notif: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllRead()
}

@Dao
interface DailyReportDao {
    @Query("SELECT * FROM daily_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<DailyReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: DailyReport)

    @Delete
    suspend fun deleteReport(report: DailyReport)
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM user_subscriptions LIMIT 1")
    fun getSubscription(): Flow<UserSubscription?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: UserSubscription)
}

@Dao
interface PaymentQrDao {
    @Query("SELECT * FROM payment_qrs ORDER BY createdAt DESC")
    fun getAllQrs(): Flow<List<PaymentQrEntity>>

    @Query("SELECT * FROM payment_qrs WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveQr(): PaymentQrEntity?

    @Query("SELECT * FROM payment_qrs WHERE isActive = 1 LIMIT 1")
    fun getActiveQrFlow(): Flow<PaymentQrEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQr(qr: PaymentQrEntity)

    @Delete
    suspend fun deleteQr(qr: PaymentQrEntity)

    @Query("DELETE FROM payment_qrs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Transaction
    suspend fun setActiveQr(qrId: String) {
        deactivateAll()
        activateQr(qrId)
    }

    @Query("UPDATE payment_qrs SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE payment_qrs SET isActive = 1 WHERE id = :qrId")
    suspend fun activateQr(qrId: String)
}

@Dao
interface SupportTicketDao {
    @Query("SELECT * FROM support_tickets ORDER BY createdAt DESC")
    fun getAllTickets(): Flow<List<SupportTicket>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: SupportTicket)

    @Delete
    suspend fun deleteTicket(ticket: SupportTicket)
}
