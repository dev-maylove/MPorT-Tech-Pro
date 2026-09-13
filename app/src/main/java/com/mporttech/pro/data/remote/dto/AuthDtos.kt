package com.mporttech.pro.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Login body accepted by MPorT Laravel AuthController */
data class LoginRequest(
    val login: String? = null,
    val username: String? = null,
    val email: String? = null,
    val password: String,
    @SerializedName("device_name") val deviceName: String = "mport-android"
)

/**
 * Response shape from POST /api/v1/auth/login
 * {
 *   "token": "...",
 *   "access_token": "...",
 *   "refresh_token": "...",
 *   "token_type": "Bearer",
 *   "expires_in": 3600,
 *   "user": { ... }
 * }
 */
data class LoginResponse(
    val token: String? = null,
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("token_type") val tokenType: String? = null,
    @SerializedName("expires_in") val expiresIn: Long? = null,
    @SerializedName("refresh_expires_in") val refreshExpiresIn: Long? = null,
    val user: RemoteUserDto? = null,
    val message: String? = null
) {
    fun resolvedAccessToken(): String? = accessToken?.takeIf { it.isNotBlank() } ?: token
}

data class RemoteUserDto(
    val id: Long? = null,
    val name: String? = null,
    val email: String? = null,
    val username: String? = null,
    val phone: String? = null,
    val role: String? = null,
    @SerializedName("tech_code") val techCode: String? = null,
    @SerializedName("is_active") val isActive: Boolean? = null
)

data class ApiErrorBody(
    val message: String? = null,
    val errors: Map<String, List<String>>? = null
)
