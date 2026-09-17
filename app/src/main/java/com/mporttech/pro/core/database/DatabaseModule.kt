package com.mporttech.pro.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** v1 → v2: add remote fields without silently hiding unrelated SQL failures. */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            fun hasColumn(table: String, column: String): Boolean {
                db.query("PRAGMA table_info($table)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        if (cursor.getString(nameIndex) == column) return true
                    }
                }
                return false
            }
            fun add(table: String, column: String, definition: String) {
                if (!hasColumn(table, column)) {
                    db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
                }
            }

            add("customers", "remoteId", "INTEGER")
            add("customers", "customerCode", "TEXT NOT NULL DEFAULT ''")
            add("customers", "email", "TEXT NOT NULL DEFAULT ''")
            add("customers", "status", "TEXT NOT NULL DEFAULT 'active'")
            add("customers", "ipAddress", "TEXT NOT NULL DEFAULT ''")
            add("customers", "macAddress", "TEXT NOT NULL DEFAULT ''")
            add("customers", "accessPoint", "TEXT NOT NULL DEFAULT ''")
            add("tickets", "customerName", "TEXT NOT NULL DEFAULT ''")
            add("tickets", "technicianName", "TEXT NOT NULL DEFAULT ''")
            add("tickets", "remoteId", "INTEGER")
            add("tickets", "ticketNumber", "TEXT NOT NULL DEFAULT ''")
            add("tickets", "priority", "TEXT NOT NULL DEFAULT 'normal'")
            add("tickets", "category", "TEXT NOT NULL DEFAULT ''")
        }
    }

    @Provides
    @Singleton
    fun db(@ApplicationContext c: Context): AppDatabase =
        Room.databaseBuilder(c, AppDatabase::class.java, "mport_tech.db")
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    fun customerDao(db: AppDatabase): CustomerDao = db.customerDao()

    @Provides
    fun ticketDao(db: AppDatabase): TicketDao = db.ticketDao()

    @Provides
    fun diagnosticDao(db: AppDatabase): DiagnosticDao = db.diagnosticDao()
}
