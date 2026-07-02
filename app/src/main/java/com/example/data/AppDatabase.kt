package com.example.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.entity.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val type = Types.newParameterizedType(List::class.java, OrderItem::class.java)
    private val adapter = moshi.adapter<List<OrderItem>>(type)

    @TypeConverter
    fun fromString(value: String): List<OrderItem>? {
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromList(list: List<OrderItem>): String {
        return try {
            adapter.toJson(list)
        } catch (e: Exception) {
            "[]"
        }
    }
}

@Database(
    entities = [
        BusinessProfile::class,
        BillingItem::class,
        Category::class,
        BillOrder::class,
        PrinterConfig::class,
        Staff::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessProfileDao(): BusinessProfileDao
    abstract fun billingItemDao(): BillingItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun billOrderDao(): BillOrderDao
    abstract fun printerConfigDao(): PrinterConfigDao
    abstract fun staffDao(): StaffDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zaddy_billing_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        suspend fun populateInitialData(db: AppDatabase) {
            // Populate default categories
            val categories = listOf(
                Category(name = "Tea", isCustom = false),
                Category(name = "Coffee", isCustom = false),
                Category(name = "Snacks", isCustom = false),
                Category(name = "Cool Drinks", isCustom = false),
                Category(name = "Desserts", isCustom = false),
                Category(name = "Others", isCustom = false)
            )
            for (category in categories) {
                db.categoryDao().insertCategory(category)
            }

            // Populate initial billing items
            val items = listOf(
                BillingItem(name = "Masala Chai", category = "Tea", price = 15.0),
                BillingItem(name = "Ginger Tea", category = "Tea", price = 15.0),
                BillingItem(name = "Milk Tea", category = "Tea", price = 15.0),
                BillingItem(name = "Filter Coffee", category = "Coffee", price = 20.0),
                BillingItem(name = "Cappuccino", category = "Coffee", price = 35.0),
                BillingItem(name = "Samosa", category = "Snacks", price = 15.0),
                BillingItem(name = "Puff", category = "Snacks", price = 20.0),
                BillingItem(name = "Mango Lassi", category = "Cool Drinks", price = 30.0),
                BillingItem(name = "Fresh Lime Juice", category = "Cool Drinks", price = 25.0),
                BillingItem(name = "Chocolate Brownie", category = "Desserts", price = 45.0)
            )
            for (item in items) {
                db.billingItemDao().insertItem(item)
            }

            // Setup a default printer config
            db.printerConfigDao().insertPrinterConfig(
                PrinterConfig(
                    deviceName = "POS-58 Printer",
                    deviceAddress = "00:11:22:33:44:55",
                    isConnected = false,
                    autoConnect = true,
                    paperWidth = 58
                )
            )
        }
    }
}
