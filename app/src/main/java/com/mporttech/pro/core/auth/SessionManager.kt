package com.mporttech.pro.core.auth

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class UserRole { ADMIN, TECHNICIAN }

data class AppUser(
    val id: String,
    val name: String,
    val username: String,
    val role: UserRole,
    val password: String = "" // empty for remote sessions
)

/**
 * Session + optional local technician roster.
 * Remote login is preferred (AuthRepository); offline demo remains as fallback.
 */
object SessionManager {
    private const val PREFS = "mport_session"
    private const val KEY_USER = "user_json"
    private const val KEY_TECHS = "technicians_json"
    private const val KEY_SOURCE = "auth_source" // "remote" | "local"

    fun isLoggedIn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(KEY_USER)

    fun currentUser(context: Context): AppUser? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_USER, null)
            ?: return null
        return parseUser(raw)
    }

    fun isAdmin(context: Context): Boolean =
        currentUser(context)?.role == UserRole.ADMIN

    fun isRemoteSession(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SOURCE, "local") == "remote"

    /** Persist user coming from API login. */
    fun saveRemoteSession(context: Context, user: AppUser) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_USER, toJson(user).toString())
            .putString(KEY_SOURCE, "remote")
            .apply()
    }

    /**
     * Offline demo login (admin/admin123, seeded techs).
     * Kept for field use when server is unreachable.
     */
    fun loginOffline(context: Context, username: String, password: String): AppUser? {
        val u = username.trim()
        val p = password
        if (u.equals("admin", true) && p == "admin123") {
            val admin = AppUser("admin", "Administrator", "admin", UserRole.ADMIN, "admin123")
            saveLocalSession(context, admin)
            return admin
        }
        val tech = listTechnicians(context).firstOrNull {
            it.username.equals(u, true) && it.password == p
        } ?: return null
        saveLocalSession(context, tech)
        return tech
    }

    /** @deprecated Prefer AuthRepository.login — kept for compatibility */
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
                password = password
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
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_USER, toJson(user).toString())
            .putString(KEY_SOURCE, "local")
            .apply()
    }

    private fun ensureSeed(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_TECHS)) {
            val seed = listOf(
                AppUser("tech_seed", "Budi Santoso", "budi", UserRole.TECHNICIAN, "budi123")
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

    private fun toJson(u: AppUser) = JSONObject().apply {
        put("id", u.id)
        put("name", u.name)
        put("username", u.username)
        put("role", u.role.name)
        put("password", u.password)
    }

    private fun parseUser(raw: String): AppUser? = try {
        parseUserObj(JSONObject(raw))
    } catch (_: Exception) {
        null
    }

    private fun parseUserObj(o: JSONObject): AppUser? = try {
        AppUser(
            id = o.getString("id"),
            name = o.getString("name"),
            username = o.getString("username"),
            role = UserRole.valueOf(o.optString("role", "TECHNICIAN")),
            password = o.optString("password", "")
        )
    } catch (_: Exception) {
        null
    }
}
