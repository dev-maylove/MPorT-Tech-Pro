package com.mporttech.pro.core.security

import javax.inject.Inject
import javax.inject.Singleton

data class RouterCredentials(
    val host: String,
    val username: String,
    val password: String,
    val port: Int = 8728
)

/**
 * Stores MikroTik / router credentials in encrypted storage only.
 */
@Singleton
class CredentialManager @Inject constructor(
    private val secure: SecureStorage
) {
    fun saveRouter(credentials: RouterCredentials) {
        secure.putString(KEY_HOST, credentials.host)
        secure.putString(KEY_USER, credentials.username)
        secure.putString(KEY_PASS, credentials.password)
        secure.putString(KEY_PORT, credentials.port.toString())
    }

    fun loadRouter(): RouterCredentials? {
        val host = secure.getString(KEY_HOST) ?: return null
        val user = secure.getString(KEY_USER) ?: return null
        val pass = secure.getString(KEY_PASS) ?: return null
        val port = secure.getString(KEY_PORT)?.toIntOrNull() ?: 8728
        return RouterCredentials(host, user, pass, port)
    }

    fun clearRouter() {
        secure.remove(KEY_HOST)
        secure.remove(KEY_USER)
        secure.remove(KEY_PASS)
        secure.remove(KEY_PORT)
    }

    companion object {
        private const val KEY_HOST = "mt_host"
        private const val KEY_USER = "mt_user"
        private const val KEY_PASS = "mt_pass"
        private const val KEY_PORT = "mt_port"
    }
}
