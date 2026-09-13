package com.mporttech.pro.core.auth

import com.mporttech.pro.core.security.SecureStorage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists Sanctum access / refresh tokens in encrypted storage.
 */
@Singleton
class TokenStore @Inject constructor(
    private val secure: SecureStorage
) {
    companion object {
        private const val KEY_ACCESS = "auth_access_token"
        private const val KEY_REFRESH = "auth_refresh_token"
        private const val KEY_TOKEN_TYPE = "auth_token_type"
        private const val KEY_EXPIRES_AT = "auth_expires_at_ms"
    }

    fun saveTokens(
        accessToken: String,
        refreshToken: String?,
        tokenType: String = "Bearer",
        expiresInSec: Long? = null
    ) {
        secure.putString(KEY_ACCESS, accessToken)
        secure.putString(KEY_REFRESH, refreshToken)
        secure.putString(KEY_TOKEN_TYPE, tokenType)
        val expiresAt = expiresInSec?.let { System.currentTimeMillis() + it * 1000L }
        secure.putString(KEY_EXPIRES_AT, expiresAt?.toString())
    }

    fun accessToken(): String? = secure.getString(KEY_ACCESS)

    fun refreshToken(): String? = secure.getString(KEY_REFRESH)

    fun bearerHeader(): String? {
        val t = accessToken() ?: return null
        val type = secure.getString(KEY_TOKEN_TYPE) ?: "Bearer"
        return "$type $t"
    }

    fun isLoggedIn(): Boolean = !accessToken().isNullOrBlank()

    fun clear() {
        secure.remove(KEY_ACCESS)
        secure.remove(KEY_REFRESH)
        secure.remove(KEY_TOKEN_TYPE)
        secure.remove(KEY_EXPIRES_AT)
    }
}
