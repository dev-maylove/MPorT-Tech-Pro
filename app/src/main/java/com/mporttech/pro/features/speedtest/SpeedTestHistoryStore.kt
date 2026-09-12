package com.mporttech.pro.features.speedtest

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SpeedTestRecord(
    val timestamp: Long,
    val serverName: String,
    val location: String,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Double,
    val jitterMs: Double,
    val lossPct: Double
)

object SpeedTestHistoryStore {
    private const val PREFS = "mport_speed_history"
    private const val KEY = "records"
    private const val MAX = 30

    fun add(context: Context, record: SpeedTestRecord) {
        val list = load(context).toMutableList()
        list.add(0, record)
        save(context, list.take(MAX))
    }

    fun load(context: Context): List<SpeedTestRecord> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                SpeedTestRecord(
                    timestamp = o.optLong("ts"),
                    serverName = o.optString("server"),
                    location = o.optString("loc"),
                    downloadMbps = o.optDouble("dl"),
                    uploadMbps = o.optDouble("ul"),
                    pingMs = o.optDouble("ping"),
                    jitterMs = o.optDouble("jitter"),
                    lossPct = o.optDouble("loss")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }

    private fun save(context: Context, list: List<SpeedTestRecord>) {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(JSONObject().apply {
                put("ts", r.timestamp)
                put("server", r.serverName)
                put("loc", r.location)
                put("dl", r.downloadMbps)
                put("ul", r.uploadMbps)
                put("ping", r.pingMs)
                put("jitter", r.jitterMs)
                put("loss", r.lossPct)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}
