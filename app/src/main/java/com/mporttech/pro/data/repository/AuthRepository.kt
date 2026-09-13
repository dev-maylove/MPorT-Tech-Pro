package com.mporttech.pro.data.repository

import android.content.Context
import com.google.gson.Gson
import com.mporttech.pro.core.auth.AppUser
import com.mporttech.pro.core.auth.SessionManager
import com.mporttech.pro.core.auth.TokenStore
import com.mporttech.pro.core.auth.UserRole
import com.mporttech.pro.core.common.Constants
import com.mporttech.pro.core.common.Result
import com.mporttech.pro.data.remote.AuthApi
import com.mporttech.pro.data.remote.dto.ApiErrorBody
import com.mporttech.pro.data.remote.dto.LoginRequest
import com.mporttech.pro.data.remote.dto.RemoteUserDto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: AuthApi,
    private val tokenStore: TokenStore,
    private val gson: Gson
) {

    /**
     * Login against Laravel backend. Falls back to offline demo only if
     * [Constants.ALLOW_OFFLINE_DEMO_LOGIN] and network fails.
     */
    suspend fun login(identity: String, password: String): Result<AppUser> {
        val trimmed = identity.trim()
        if (trimmed.isEmpty() || password.isEmpty()) {
            return Result.Error("Username / email dan password wajib diisi")
        }

        return try {
            val body = LoginRequest(
                login = trimmed,
                username = trimmed,
                password = password,
                deviceName = "mport-android"
            )
            val response = api.login(body)
            if (response.isSuccessful) {
                val payload = response.body()
                    ?: return Result.Error("Respons server kosong")
                val access = payload.resolvedAccessToken()
                    ?: return Result.Error("Token tidak ditemukan di respons")
                val userDto = payload.user
                    ?: return Result.Error("Data user tidak ditemukan")

                tokenStore.saveTokens(
                    accessToken = access,
                    refreshToken = payload.refreshToken,
                    tokenType = payload.tokenType ?: "Bearer",
                    expiresInSec = payload.expiresIn
                )

                val appUser = userDto.toAppUser()
                SessionManager.saveRemoteSession(context, appUser)
                Result.Success(appUser)
            } else {
                val errMsg = parseError(response.errorBody()?.string())
                    ?: "Login gagal (${response.code()})"
                // Optional offline fallback when server rejects and demo allowed
                if (Constants.ALLOW_OFFLINE_DEMO_LOGIN && response.code() in 500..599) {
                    offlineFallback(trimmed, password) ?: Result.Error(errMsg)
                } else {
                    Result.Error(errMsg)
                }
            }
        } catch (e: Exception) {
            // Network / timeout → try offline demo if enabled
            if (Constants.ALLOW_OFFLINE_DEMO_LOGIN) {
                offlineFallback(trimmed, password)
                    ?: Result.Error(e.message ?: "Tidak dapat terhubung ke server")
            } else {
                Result.Error(e.message ?: "Tidak dapat terhubung ke server")
            }
        }
    }

    suspend fun logout() {
        try {
            val bearer = tokenStore.bearerHeader()
            if (bearer != null) {
                val refresh = tokenStore.refreshToken()
                val body = if (refresh != null) mapOf("refresh_token" to refresh) else emptyMap()
                api.logout(bearer, body)
            }
        } catch (_: Exception) {
            // ignore network errors on logout
        } finally {
            tokenStore.clear()
            SessionManager.logout(context)
        }
    }

    fun isLoggedIn(): Boolean =
        tokenStore.isLoggedIn() || SessionManager.isLoggedIn(context)

    private fun offlineFallback(identity: String, password: String): Result<AppUser>? {
        val user = SessionManager.loginOffline(context, identity, password) ?: return null
        return Result.Success(user)
    }

    private fun parseError(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return try {
            val body = gson.fromJson(raw, ApiErrorBody::class.java)
            body.errors?.values?.flatten()?.firstOrNull()
                ?: body.message
        } catch (_: Exception) {
            raw.take(200)
        }
    }

    private fun RemoteUserDto.toAppUser(): AppUser {
        val roleStr = (role ?: "").lowercase()
        val mapped = when {
            roleStr == "admin" -> UserRole.ADMIN
            roleStr == "technician" || roleStr == "tech" -> UserRole.TECHNICIAN
            else -> UserRole.TECHNICIAN
        }
        return AppUser(
            id = id?.toString() ?: "0",
            name = name ?: email ?: "User",
            username = username ?: email ?: techCode ?: id?.toString() ?: "",
            role = mapped,
            password = "" // never store server password locally
        )
    }
}
