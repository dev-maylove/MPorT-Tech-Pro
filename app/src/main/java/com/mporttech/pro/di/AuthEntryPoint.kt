package com.mporttech.pro.di

import com.mporttech.pro.data.repository.AuthRepository
import com.mporttech.pro.data.repository.TicketRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthEntryPoint {
    fun authRepository(): AuthRepository
    fun ticketRepository(): TicketRepository
}
