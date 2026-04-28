package com.rainyscanner.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class ScanRecord(
    val id: Long = System.currentTimeMillis(),
    val rawContent: String,
    val type: String,         // "URL", "TEXT", "WIFI", "CONTACT", etc.
    val timestamp: String = LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
) {
    val isUrl: Boolean get() = type == "URL" && (rawContent.startsWith("http://") || rawContent.startsWith("https://"))
    val displayTitle: String get() = when {
        isUrl -> "🔗 ${rawContent.take(50)}${if (rawContent.length > 50) "…" else ""}"
        type == "WIFI" -> "📶 Wi-Fi 网络"
        type == "CONTACT" -> "👤 联系人信息"
        type == "EMAIL" -> "📧 邮件地址"
        type == "PHONE" -> "📞 电话号码"
        type == "SMS" -> "💬 短信内容"
        type == "GEO" -> "📍 地理位置"
        else -> rawContent.take(60) + if (rawContent.length > 60) "…" else ""
    }
}

class ScanHistory(private val context: Context) {
    private val prefs = context.getSharedPreferences("scan_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val maxSize = 100

    fun getAll(): List<ScanRecord> {
        val json = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val type = object : TypeToken<List<ScanRecord>>() {}.type
        return try {
            gson.fromJson<List<ScanRecord>>(json, type).reversed()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(record: ScanRecord) {
        val json = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val type = object : TypeToken<MutableList<ScanRecord>>() {}.type
        val all: MutableList<ScanRecord> = try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            mutableListOf()
        }
        // 去重
        all.removeAll { it.rawContent == record.rawContent }
        all.add(record)
        // 限制数量（移除最旧的记录）
        if (all.size > maxSize) {
            val trimmed = all.subList(all.size - maxSize, all.size)
            prefs.edit().putString(KEY_HISTORY, gson.toJson(trimmed)).apply()
        } else {
            prefs.edit().putString(KEY_HISTORY, gson.toJson(all)).apply()
        }
    }

    fun clear() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    fun remove(id: Long) {
        val json = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val type = object : TypeToken<MutableList<ScanRecord>>() {}.type
        val all: MutableList<ScanRecord> = try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            mutableListOf()
        }
        all.removeAll { it.id == id }
        prefs.edit().putString(KEY_HISTORY, gson.toJson(all)).apply()
    }

    companion object {
        private const val KEY_HISTORY = "records"
    }
}