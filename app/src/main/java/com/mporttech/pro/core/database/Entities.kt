package com.mporttech.pro.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String,
    val packageName: String,
    val ipAddress: String = "",
    val macAddress: String = "",
    val accessPoint: String = "",
    val installationDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long?,
    val title: String,
    val description: String,
    val status: String = "OPEN",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "diagnostics")
data class DiagnosticEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val target: String,
    val success: Boolean,
    val latencyMs: Long?,
    val message: String,
    val createdAt: Long = System.currentTimeMillis()
)
