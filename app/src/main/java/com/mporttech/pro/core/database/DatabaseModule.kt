package com.mporttech.pro.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun db(@ApplicationContext c: Context): AppDatabase =
        Room.databaseBuilder(c, AppDatabase::class.java, "mport_tech.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun customerDao(db: AppDatabase): CustomerDao = db.customerDao()

    @Provides
    fun ticketDao(db: AppDatabase): TicketDao = db.ticketDao()

    @Provides
    fun diagnosticDao(db: AppDatabase): DiagnosticDao = db.diagnosticDao()
}
