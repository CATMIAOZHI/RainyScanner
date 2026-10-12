package com.rainyscanner.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** FIDO / passkey 混合登录链接的前缀（二维码里一般是大写的 `FIDO:/`） */
const val FIDO_SCHEME_PREFIX = "FIDO:/"

/** 判断扫码内容是不是 FIDO / passkey 链接（大小写都不挑，容忍前导空白喵~） */
fun isFidoLink(raw: String): Boolean =
    raw.trimStart().startsWith(FIDO_SCHEME_PREFIX, ignoreCase = true)

data class ScanRecord(
    val id: Long = System.currentTimeMillis(),
    val rawContent: String,
    val type: String,         // "URL", "TEXT", "WIFI", "CONTACT", "FIDO", etc.
    val timestamp: String = LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
) {
    /** 普通网页链接（前导空白不影响判定，交给系统时会去掉首尾空白） */
    val isUrl: Boolean get() {
        if (type != "URL") return false
        val value = rawContent.trimStart()
        return value.startsWith("http://") || value.startsWith("https://")
    }

    /** FIDO / 通行密钥（passkey）登录请求，例如 `FIDO:/xxxx` */
    val isFido: Boolean get() = isFidoLink(rawContent)

    /** 能不能交给系统打开（普通 URL 或 FIDO 登录链接） */
    val isOpenable: Boolean get() = isUrl || isFido

    val displayTitle: String get() = when {
        isUrl -> "🔗 ${rawContent.take(50)}${if (rawContent.length > 50) "…" else ""}"
        isFido -> "🔐 通行密钥（Passkey）登录"
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