package com.rainyscanner.app

import com.rainyscanner.app.data.ScanRecord
import com.rainyscanner.app.data.isFidoLink
import com.rainyscanner.app.ui.screen.detectBarcodeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 扫码类型识别 + FIDO / passkey 链接判定喵~ */
class ScanTypeDetectionTest {

    // 真实 FIDO 混合登录载荷（结构：FIDO:/ + base64url）
    private val fidoPayload =
        "FIDO:/261848785648354946983408780549606368650795796128877116497180149959843755576086587249342996699899200915539608548221605020069163271108560552338730947183362107096654083076"

    @Test
    fun fidoLinkIsDetected() {
        assertEquals("FIDO", detectBarcodeType(fidoPayload))
        assertTrue(isFidoLink(fidoPayload))
        // scheme 大小写不敏感
        assertTrue(isFidoLink("fido:/abcdef"))
    }

    @Test
    fun fidoRecordIsOpenableButNotUrl() {
        val record = ScanRecord(rawContent = fidoPayload, type = detectBarcodeType(fidoPayload))
        assertTrue(record.isFido)
        assertTrue(record.isOpenable)
        assertFalse(record.isUrl)
    }

    @Test
    fun fidoLinkToleratesSurroundingWhitespace() {
        // 二维码偶尔带多余空白，判定也要认（真正交给系统时会去掉首尾空白）
        assertTrue(isFidoLink("  \n$fidoPayload"))
        assertEquals("FIDO", detectBarcodeType(" $fidoPayload "))
        assertTrue(ScanRecord(rawContent = "  $fidoPayload", type = "TEXT").isOpenable)
    }

    @Test
    fun urlAlsoToleratesSurroundingWhitespace() {
        assertEquals("URL", detectBarcodeType("  https://example.com/x "))
        val record = ScanRecord(rawContent = " https://example.com", type = "URL")
        assertTrue(record.isUrl)
        assertTrue(record.isOpenable)
    }

    @Test
    fun oldHistoryRecordsStillRecognised() {
        // 旧版本把 FIDO 链接存成 TEXT，也要能打开喵~
        val legacy = ScanRecord(rawContent = fidoPayload, type = "TEXT")
        assertTrue(legacy.isFido)
        assertTrue(legacy.isOpenable)
    }

    @Test
    fun otherTypesUnchanged() {
        assertEquals("URL", detectBarcodeType("https://example.com"))
        assertEquals("WIFI", detectBarcodeType("WIFI:S:home;T:WPA;P:1234;;"))
        assertEquals("PHONE", detectBarcodeType("tel:+8613800000000"))
        // 长得像但不是链接的普通文本
        assertEquals("TEXT", detectBarcodeType("FIDO2 is a standard"))
        assertFalse(isFidoLink("https://example.com"))
        assertFalse(ScanRecord(rawContent = "hello", type = "TEXT").isOpenable)
    }
}