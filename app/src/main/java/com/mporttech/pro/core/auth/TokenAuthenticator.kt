package com.mporttech.pro.core.auth

import com.google.gson.Gson
import com.mporttech.pro.core.common.Constants
import com.mporttech.pro.data.remote.dto.LoginResponse
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On HTTP 401, try POST /api/v1/auth/refresh once, then retry the original request.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val gson: Gson
) : Authenticator {

    private val refreshClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Avoid infinite loop
        if (responseCount(response) >= 2) return null
        if (response.request.header("Authorization") == null) return null

        val refresh = tokenStore.refreshToken() ?: run {
            tokenStore.clear()
            return null
        }

        synchronized(this) {
            // Another thread may have refreshed already
            val currentAccess = tokenStore.accessToken()
            val requestToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")
                ?.trim()
            if (!currentAccess.isNullOrBlank() && currentAccess != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccess")
                    .build()
            }

            val bodyJson = JSONObject()
                .put("refresh_token", refresh)
                .put("device_name", "mport-android")
                .toString()

            val refreshReq = Request.Builder()
                .url(Constants.API_BASE_URL + "api/v1/auth/refresh")
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .header("Accept", "application/json")
                .build()

            return try {
                refreshClient.newCall(refreshReq).execute().use { refreshResp ->
                    if (!refreshResp.isSuccessful) {
                        tokenStore.clear()
                        return null
                    }
                    val raw = refreshResp.body?.string().orEmpty()
                    val parsed = gson.fromJson(raw, LoginResponse::class.java)
                    val newAccess = parsed.resolvedAccessToken()
                    if (newAccess.isNullOrBlank()) {
                        tokenStore.clear()
                        return null
                    }
                    tokenStore.saveTokens(
                        accessToken = newAccess,
                        refreshToken = parsed.refreshToken ?: refresh,
                        tokenType = parsed.tokenType ?: "Bearer",
                        expiresInSec = parsed.expiresIn
                    )
                    response.request.newBuilder()
                        .header("Authorization", "Bearer $newAccess")
                        .build()
                }
            } catch (_: Exception) {
                tokenStore.clear()
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
