package com.mporttech.pro.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY id DESC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY id DESC")
    suspend fun getAll(): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CustomerEntity>)

    @Query("DELETE FROM customers")
    suspend fun clear()

    @Delete
    suspend fun delete(item: CustomerEntity)
}

@Dao
interface TicketDao {
    @Query("SELECT * FROM tickets ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets ORDER BY createdAt DESC")
    suspend fun getAll(): List<TicketEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TicketEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TicketEntity>)

    @Query("DELETE FROM tickets")
    suspend fun clear()

    @Update
    suspend fun update(item: TicketEntity)
}

@Dao
interface DiagnosticDao {
    @Query("SELECT * FROM diagnostics ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DiagnosticEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DiagnosticEntity)

    @Query("DELETE FROM diagnostics")
    suspend fun clear()
}
