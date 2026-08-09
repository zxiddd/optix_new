package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ── billing_items: add new columns ──
        db.execSQL("ALTER TABLE billing_items ADD COLUMN description TEXT")
        db.execSQL("ALTER TABLE billing_items ADD COLUMN barcode TEXT")
        db.execSQL("ALTER TABLE billing_items ADD COLUMN sku TEXT")
        db.execSQL("ALTER TABLE billing_items ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE billing_items ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE billing_items ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE billing_items ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

        // ── categories: recreate table without isCustom / createdAt ──
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS categories_new (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                businessId TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                version INTEGER NOT NULL DEFAULT 1,
                isSynced INTEGER NOT NULL DEFAULT 0,
                lastModified INTEGER NOT NULL DEFAULT 0,
                isDeleted INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO categories_new (id, name, businessId, sortOrder)
            SELECT id, name, businessId, sortOrder FROM categories
        """.trimIndent())
        db.execSQL("DROP TABLE categories")
        db.execSQL("ALTER TABLE categories_new RENAME TO categories")

        // ── bill_orders: add new columns ──
        db.execSQL("ALTER TABLE bill_orders ADD COLUMN status TEXT NOT NULL DEFAULT 'PAID'")
        db.execSQL("ALTER TABLE bill_orders ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE bill_orders ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
    }
}

// Migration 13 → 14: Enterprise Staff Management
// Adds businessId, lastModified, isDeleted, permissionsJson, failedLoginCount,
// lastActivityAt to staff_accounts. Creates staff_activity_logs and staff_sessions tables.
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ── staff_accounts: add new columns ──
        db.execSQL("ALTER TABLE staff_accounts ADD COLUMN businessId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE staff_accounts ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE staff_accounts ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE staff_accounts ADD COLUMN permissionsJson TEXT NOT NULL DEFAULT '[]'")
        db.execSQL("ALTER TABLE staff_accounts ADD COLUMN failedLoginCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE staff_accounts ADD COLUMN lastActivityAt INTEGER")

        // ── staff_activity_logs: new table ──
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS staff_activity_logs (
                id TEXT NOT NULL PRIMARY KEY,
                staffId TEXT NOT NULL,
                businessId TEXT NOT NULL,
                action TEXT NOT NULL,
                entityType TEXT,
                entityId TEXT,
                metadataJson TEXT,
                deviceId TEXT,
                isSuspicious INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        // ── staff_sessions: new table ──
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS staff_sessions (
                id TEXT NOT NULL PRIMARY KEY,
                staffId TEXT NOT NULL,
                businessId TEXT NOT NULL,
                deviceId TEXT,
                deviceName TEXT,
                loginAt INTEGER NOT NULL DEFAULT 0,
                logoutAt INTEGER,
                isActive INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE staff_accounts ADD COLUMN phone TEXT")
        db.execSQL("ALTER TABLE staff_accounts ADD COLUMN email TEXT")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE staff_activity_logs ADD COLUMN severity TEXT NOT NULL DEFAULT 'NORMAL'")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS notifications (
                id TEXT NOT NULL PRIMARY KEY,
                businessId TEXT NOT NULL DEFAULT '',
                title TEXT NOT NULL DEFAULT '',
                message TEXT NOT NULL DEFAULT '',
                type TEXT NOT NULL DEFAULT 'INFO',
                severity TEXT NOT NULL DEFAULT 'INFO',
                isRead INTEGER NOT NULL DEFAULT 0,
                isArchived INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE business_profile ADD COLUMN timezone TEXT NOT NULL DEFAULT 'Asia/Riyadh'")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE business_profile ADD COLUMN lastResetBusinessDate TEXT")
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- business_profile changes ---
        db.execSQL("ALTER TABLE business_profile ADD COLUMN country TEXT NOT NULL DEFAULT 'India'")

        // --- user_subscriptions changes ---
        db.execSQL("ALTER TABLE user_subscriptions ADD COLUMN country TEXT NOT NULL DEFAULT 'India'")
        db.execSQL("ALTER TABLE user_subscriptions ADD COLUMN billsUsed INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_subscriptions ADD COLUMN productsUsed INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_subscriptions ADD COLUMN activationCode TEXT")
        db.execSQL("ALTER TABLE user_subscriptions ADD COLUMN renewalDate INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [
        BusinessProfile::class,
        BillingItem::class,
        Category::class,
        BillOrder::class,
        PrinterConfig::class,
        Staff::class,
        DailyReport::class,
        UserSubscription::class,
        PaymentQrEntity::class,
        SupportTicket::class,
        StaffActivityLog::class,
        StaffSession::class,
        NotificationEntity::class
    ],
    version = 19,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessProfileDao(): BusinessProfileDao
    abstract fun billingItemDao(): BillingItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun billOrderDao(): BillOrderDao
    abstract fun printerConfigDao(): PrinterConfigDao
    abstract fun staffDao(): StaffDao
    abstract fun dailyReportDao(): DailyReportDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun paymentQrDao(): PaymentQrDao
    abstract fun supportTicketDao(): SupportTicketDao
    abstract fun staffActivityLogDao(): StaffActivityLogDao
    abstract fun staffSessionDao(): StaffSessionDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zaddy_pos_db"
                )
                .addMigrations(MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
                .fallbackToDestructiveMigration(true)
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    // Prepopulate if needed
                }
            }
        }
    }
}
