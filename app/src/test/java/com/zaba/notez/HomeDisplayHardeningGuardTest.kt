package com.zaba.notez

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guard hardening mode tampilan (batch B3).
 *
 * Leksikal: menjaga invariant yang mudah hilang saat refactor, bukan bukti
 * runtime. Skenario cepat (delete/undo/switch beruntun, rotasi, TalkBack)
 * tetap UAT H05-H14.
 */
class HomeDisplayHardeningGuardTest {

    private val code: String =
        SourceScan.codeOnly(SourceScan.readMainFile("java/com/zaba/notez/MainActivity.kt"))

    @Test
    fun switchingPresentationNeverWritesNotes() {
        val body = functionBody("private fun applyDisplayMode(")
        assertTrue("applyDisplayMode tidak ditemukan", body.isNotEmpty())
        assertFalse("switch mode tidak boleh menyentuh DAO", body.contains("dao."))
        assertFalse("switch mode tidak boleh memicu save", body.contains("upsert"))
        assertFalse("switch mode tidak boleh menghapus note", body.contains("delete("))
    }

    @Test
    fun staleScrollRestoreIsCancelled() {
        val body = functionBody("private fun restoreScrollAnchor(")
        assertTrue("restoreScrollAnchor tidak ditemukan", body.isNotEmpty())
        assertTrue("pemulihan basi harus dibatalkan lewat token", body.contains("scrollRestoreToken"))
    }

    @Test
    fun scrollTargetComesFromSharedPolicy() {
        val body = functionBody("private fun restoreScrollAnchor(")
        assertTrue(
            "clamp index harus lewat HomeDisplayPolicy.anchorIndex",
            body.contains("HomeDisplayPolicy.anchorIndex")
        )
    }

    @Test
    fun modeIsRestoredBeforeFirstRender() {
        val body = functionBody("override fun onCreate(")
        assertTrue("onCreate tidak ditemukan", body.isNotEmpty())
        assertTrue(
            "mode harus dibaca dari preference saat activity dibuat",
            body.contains("HomeDisplayPref.get")
        )
        assertTrue(
            "mode harus diterapkan saat activity dibuat",
            body.contains("applyDisplayMode(")
        )
    }

    @Test
    fun gridSpacingDecorationIsNeverLeftBehind() {
        val body = functionBody("private fun applyDisplayMode(")
        val removeAt = body.indexOf("removeItemDecoration")
        val addAt = body.indexOf("addItemDecoration")
        assertTrue("dekorasi lama harus dilepas", removeAt >= 0)
        assertTrue("dekorasi baru harus dipasang", addAt >= 0)
        assertTrue("dekorasi harus dilepas sebelum yang baru dipasang", removeAt < addAt)
    }

    /** Badan satu fungsi berdasarkan awal signature-nya. */
    private fun functionBody(signature: String): String {
        val start = code.indexOf(signature)
        if (start < 0) return ""
        val open = code.indexOf('{', start)
        if (open < 0) return ""
        var depth = 0
        for (i in open until code.length) {
            when (code[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return code.substring(open, i + 1)
                }
            }
        }
        return code.substring(open)
    }
}
