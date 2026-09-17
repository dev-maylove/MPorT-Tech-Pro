package com.mporttech.pro.data.remote

import com.mporttech.pro.data.remote.dto.LoginRequest
import com.mporttech.pro.data.remote.dto.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @GET("api/v1/auth/me")
    suspend fun me(@Header("Authorization") bearer: String): Response<LoginResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Header("Authorization") bearer: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<Unit>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body body: Map<String, String>): Response<LoginResponse>
}
