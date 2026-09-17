package com.mporttech.pro.data.remote

import com.mporttech.pro.data.remote.dto.CreateTicketRequest
import com.mporttech.pro.data.remote.dto.PaginatedResponse
import com.mporttech.pro.data.remote.dto.RemoteCustomerDto
import com.mporttech.pro.data.remote.dto.RemoteTicketDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MportApi {

    @GET("api/v1/customers")
    suspend fun customers(
        @Query("search") search: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1
    ): Response<PaginatedResponse<RemoteCustomerDto>>

    @GET("api/v1/customers/{id}")
    suspend fun customer(@Path("id") id: Long): Response<RemoteCustomerDto>

    @GET("api/v1/tickets")
    suspend fun tickets(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1
    ): Response<PaginatedResponse<RemoteTicketDto>>

    @GET("api/v1/tickets/{id}")
    suspend fun ticket(@Path("id") id: Long): Response<RemoteTicketDto>

    @POST("api/v1/tickets")
    suspend fun createTicket(@Body body: CreateTicketRequest): Response<RemoteTicketDto>
}
