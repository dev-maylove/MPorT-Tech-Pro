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

    /** v1 → v2: add remoteId / customerCode / email / status / ipAddress on customers; remote fields on tickets */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Customer table — add columns if missing (SQLite ignores duplicate via try)
            try { db.execSQL("ALTER TABLE customers ADD COLUMN remoteId INTEGER") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE customers ADD COLUMN customerCode TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE customers ADD COLUMN email TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE customers ADD COLUMN status TEXT NOT NULL DEFAULT 'active'") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE customers ADD COLUMN ipAddress TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE customers ADD COLUMN macAddress TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE customers ADD COLUMN accessPoint TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE tickets ADD COLUMN customerName TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE tickets ADD COLUMN technicianName TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
            // Tickets
            try { db.execSQL("ALTER TABLE tickets ADD COLUMN remoteId INTEGER") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE tickets ADD COLUMN ticketNumber TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE tickets ADD COLUMN priority TEXT NOT NULL DEFAULT 'normal'") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE tickets ADD COLUMN category TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
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
