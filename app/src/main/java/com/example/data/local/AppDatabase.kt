package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.Site
import com.example.data.model.Worker
import com.example.util.BackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Worker::class,
        Site::class,
        AttendanceRecord::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workerDao(): WorkerDao
    abstract fun siteDao(): SiteDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workers ADD COLUMN address TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE workers ADD COLUMN nrcNo TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE workers ADD COLUMN emergencyContactName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE workers ADD COLUMN emergencyPhone TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE workers ADD COLUMN bloodType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE workers ADD COLUMN skillLevel TEXT NOT NULL DEFAULT 'Skilled'")
                db.execSQL("ALTER TABLE workers ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'Cash'")
                db.execSQL("ALTER TABLE workers ADD COLUMN paymentAccount TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE workers ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "labour_master_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(context.applicationContext, scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val context: Context,
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                scope.launch(Dispatchers.IO) {
                    try {
                        val database = getDatabase(context, scope)
                        populateDatabase(context, database)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                scope.launch(Dispatchers.IO) {
                    try {
                        val database = getDatabase(context, scope)
                        if (database.workerDao().getWorkerCount() == 0) {
                            populateDatabase(context, database)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            suspend fun populateDatabase(context: Context, database: AppDatabase) {
                val savedWorkers = com.example.util.PermanentWorkerStore.loadWorkers(context) ?: emptyList()
                if (savedWorkers.isNotEmpty()) {
                    database.workerDao().insertWorkers(savedWorkers)
                }
            }
        }
    }
}
