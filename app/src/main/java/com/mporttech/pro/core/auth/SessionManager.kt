package com.mporttech.pro.core.auth

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.mporttech.pro.BuildConfig
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

enum class UserRole { GUEST, TECHNICIAN, ADMIN }

data class AppUser(
    val id: String,
    val name: String,
    val username: String,
    val role: UserRole,
    val password: String = ""
)

/**
 * Session manager.
 * - GUEST: no login required — limited tools (network diagnostics only)
 * - TECHNICIAN / ADMIN: require login (server or offline demo)
 */
object SessionManager {
    private const val PREFS = "mport_session"
    private const val KEY_USER = "user_json"
    private const val KEY_TECHS = "technicians_json"
    private const val KEY_SOURCE = "auth_source" // remote | local | guest

    fun isLoggedIn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(KEY_USER)

    /** App may be used (guest session or staff login). */
    fun hasSession(context: Context): Boolean = isLoggedIn(context)

    fun currentUser(context: Context): AppUser? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_USER, null)
            ?: return null
        return parseUser(raw)
    }

    fun isAdmin(context: Context): Boolean =
        currentUser(context)?.role == UserRole.ADMIN

    fun isTechnician(context: Context): Boolean =
        currentUser(context)?.role == UserRole.TECHNICIAN

    fun isGuest(context: Context): Boolean =
        currentUser(context)?.role == UserRole.GUEST

    /** Staff = technician or admin (not guest). */
    fun isStaff(context: Context): Boolean {
        val r = currentUser(context)?.role ?: return false
        return r == UserRole.ADMIN || r == UserRole.TECHNICIAN
    }

    fun isRemoteSession(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SOURCE, "local") == "remote"

    /** Enter app as public guest — no credentials. */
    fun enterAsGuest(context: Context) {
        val guest = AppUser(
            id = "guest",
            name = "Pengguna Umum",
            username = "guest",
            role = UserRole.GUEST
        )
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_USER, toJson(guest).toString())
            .putString(KEY_SOURCE, "guest")
            .apply()
    }

    fun saveRemoteSession(context: Context, user: AppUser) {
        // Never accept guest from remote
        val role = when (user.role) {
            UserRole.ADMIN -> UserRole.ADMIN
            UserRole.TECHNICIAN -> UserRole.TECHNICIAN
            UserRole.GUEST -> UserRole.TECHNICIAN
        }
        val safe = user.copy(role = role)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_USER, toJson(safe).toString())
            .putString(KEY_SOURCE, "remote")
            .apply()
    }

    fun loginOffline(context: Context, username: String, password: String): AppUser? {
        val u = username.trim()
        val p = password
        // Demo credentials are available only in debug/demo builds. They are never
        // accepted by production builds.
        if (BuildConfig.ALLOW_OFFLINE_DEMO_LOGIN && u.equals("admin", true) && p == "admin123") {
            val admin = AppUser("admin", "Administrator", "admin", UserRole.ADMIN)
            saveLocalSession(context, admin)
            return admin
        }
        val tech = listTechnicians(context).firstOrNull {
            it.username.equals(u, true) && verifyPassword(p, it.password)
        } ?: return null
        saveLocalSession(context, tech)
        return tech
    }

    fun login(context: Context, username: String, password: String): AppUser? =
        loginOffline(context, username, password)

    fun logout(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_USER)
            .remove(KEY_SOURCE)
            .apply()
    }

    fun listTechnicians(context: Context): List<AppUser> {
        ensureSeed(context)
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TECHS, "[]")
            ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { parseUserObj(it) }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addTechnician(context: Context, name: String, username: String, password: String): Boolean {
        if (!isAdmin(context)) return false
        val list = listTechnicians(context).toMutableList()
        if (list.any { it.username.equals(username, true) } || username.equals("admin", true)) {
            return false
        }
        list.add(
            AppUser(
                id = "tech_${System.currentTimeMillis()}",
                name = name.trim(),
                username = username.trim(),
                role = UserRole.TECHNICIAN,
                password = hashPassword(password)
            )
        )
        saveTechs(context, list)
        return true
    }

    fun removeTechnician(context: Context, id: String): Boolean {
        if (!isAdmin(context)) return false
        val list = listTechnicians(context).filterNot { it.id == id }
        saveTechs(context, list)
        return true
    }

    private fun saveLocalSession(context: Context, user: AppUser) {
        val sessionUser = user.copy(password = "")
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_USER, toJson(sessionUser, includePassword = false).toString())
            .putString(KEY_SOURCE, "local")
            .apply()
    }

    private fun ensureSeed(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_TECHS)) {
            val seed = listOf(
                AppUser("tech_seed", "Budi Santoso", "budi", UserRole.TECHNICIAN, hashPassword("budi123"))
            )
            saveTechs(context, seed)
        }
    }

    private fun saveTechs(context: Context, list: List<AppUser>) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TECHS, arr.toString()).apply()
    }

    private fun toJson(u: AppUser, includePassword: Boolean = true) = JSONObject().apply {
        put("id", u.id)
        put("name", u.name)
        put("username", u.username)
        put("role", u.role.name)
        if (includePassword && u.password.isNotBlank()) put("password", u.password)
    }

    private fun hashPassword(password: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(salt + password.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(bytes)
    }

    private fun verifyPassword(password: String, stored: String): Boolean {
        // Backward-compatible with existing installs; successful legacy login is
        // migrated to a hash by add/update flows on future saves.
        if (!stored.contains(":")) return constantTimeEquals(stored, password)
        val parts = stored.split(":", limit = 2)
        if (parts.size != 2) return false
        return try {
            val salt = Base64.getDecoder().decode(parts[0])
            val expected = Base64.getDecoder().decode(parts[1])
            val actual = MessageDigest.getInstance("SHA-256")
                .digest(salt + password.toByteArray(Charsets.UTF_8))
            MessageDigest.isEqual(expected, actual)
        } catch (_: IllegalArgumentException) { false }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))

    private fun parseUser(raw: String): AppUser? = try {
        parseUserObj(JSONObject(raw))
    } catch (_: Exception) {
        null
    }

    private fun parseUserObj(o: JSONObject): AppUser? = try {
        val roleStr = o.optString("role", "GUEST")
        val role = try {
            UserRole.valueOf(roleStr)
        } catch (_: Exception) {
            UserRole.GUEST
        }
        AppUser(
            id = o.getString("id"),
            name = o.getString("name"),
            username = o.getString("username"),
            role = role,
            password = o.optString("password", "")
        )
    } catch (_: Exception) {
        null
    }
}
