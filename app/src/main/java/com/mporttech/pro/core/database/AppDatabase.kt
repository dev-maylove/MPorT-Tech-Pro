package com.mporttech.pro.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CustomerEntity::class, TicketEntity::class, DiagnosticEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun ticketDao(): TicketDao
    abstract fun diagnosticDao(): DiagnosticDao
}
