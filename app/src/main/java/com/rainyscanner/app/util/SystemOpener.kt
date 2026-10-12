package com.rainyscanner.app.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

/**
 * 把扫码结果原样交给系统去处理喵~
 *
 * - 普通 URL → 交给浏览器或对应 App
 * - `FIDO:/…` 通行密钥链接 → 交给系统里的 passkey 提供方
 *   （Google Play 服务的 `com.google.android.gms.fido.authenticator.ui.QRBounceActivity`，
 *   随后进入 WebAuthn 跨设备 / 混合登录流程）
 *
 * 本 App 只负责“递过去”，不解析、不代理、不代替用户确认，凭据始终留在系统里。
 */
object SystemOpener {

    private const val TAG = "SystemOpener"

    /**
     * 用 [Intent.ACTION_VIEW] 把 [raw] 交给系统打开。
     *
     * 只会去掉首尾空白（二维码偶尔带多余空白；展示与复制的仍是原始内容），其余原样递给系统。
     *
     * @return true 表示已成功发起（系统接管），false 表示没有应用能处理。
     */
    fun open(context: Context, raw: String, failureHint: String): Boolean {
        val uri = runCatching { Uri.parse(raw.trim()) }.getOrNull()
        if (uri == null) {
            // 解析都失败了（正常 Uri.parse 不抛异常，兜底避免冒泡到点击回调）
            Toast.makeText(context, failureHint, Toast.LENGTH_SHORT).show()
            return false
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                // 调用方不是 Activity 时（Service / Receiver 等）需要 NEW_TASK
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            // 只记 scheme，避免把 FIDO 载荷 / URL 参数写进日志
            Log.w(TAG, "no activity handles scheme=${uri.scheme}")
            Toast.makeText(context, failureHint, Toast.LENGTH_SHORT).show()
            false
        } catch (e: Exception) {
            Log.w(TAG, "startActivity failed for scheme=${uri.scheme}: ${e.javaClass.simpleName}")
            Toast.makeText(context, failureHint, Toast.LENGTH_SHORT).show()
            false
        }
    }

    /** FIDO / passkey 链接打开失败时的提示文案喵~ */
    const val FIDO_FAILURE_HINT = "喵~ 系统里没有能处理 FIDO 的应用（需要 Google Play 服务）"

    /** 其它内容打不开时的通用提示喵~ */
    const val GENERIC_FAILURE_HINT = "喵~ 没有应用能打开它"
}
