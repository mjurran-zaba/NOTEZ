package com.zaba.notez

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guard leksikal untuk optimasi performa `bodyEdit.setText` pada dokumen besar (96 KB+).
 *
 * Sesuai ADR `docs/ADR_EDIT_SETTEXT_PERFORMANCE_2026_11_18.md`.
 *
 * Mencegah regresi:
 * 1. Line breaking strategy Knuth-Plass (`high_quality`) atau hyphenation aktif yang
 *    dapat menyebabkan DynamicLayout freeze 2.8 - 5.0 detik saat pertama kali masuk mode edit.
 * 2. Autofill traversal pada dokumen puluhan ribu karakter yang memicu inspect view hierarchy.
 * 3. Text watcher autosave tidak sengaja terpicu selama `syncBodyText`.
 */
class EditorTextPerformanceGuardTest {

    private val editorXml: String = SourceScan.readRes("layout/activity_editor.xml")
    private val editorRaw: String = SourceScan.readMainFile("java/com/zaba/notez/EditorActivity.kt")
    private val editorCode: String = SourceScan.codeOnly(editorRaw)

    @Test
    fun editBodyXmlDeclaresSimpleBreakStrategy() {
        val editBodyTag = extractTag(editorXml, "<EditText", "android:id=\"@+id/edit_body\"")
        assertTrue("Tag edit_body harus ditemukan di XML", editBodyTag.isNotEmpty())
        assertTrue(
            "edit_body harus memakai breakStrategy=\"simple\"",
            editBodyTag.contains("android:breakStrategy=\"simple\"")
        )
    }

    @Test
    fun editBodyXmlDeclaresHyphenationFrequencyNone() {
        val editBodyTag = extractTag(editorXml, "<EditText", "android:id=\"@+id/edit_body\"")
        assertTrue("Tag edit_body harus ditemukan di XML", editBodyTag.isNotEmpty())
        assertTrue(
            "edit_body harus memakai hyphenationFrequency=\"none\"",
            editBodyTag.contains("android:hyphenationFrequency=\"none\"")
        )
    }

    @Test
    fun editBodyXmlDeclaresImportantForAutofillNo() {
        val editBodyTag = extractTag(editorXml, "<EditText", "android:id=\"@+id/edit_body\"")
        assertTrue("Tag edit_body harus ditemukan di XML", editBodyTag.isNotEmpty())
        assertTrue(
            "edit_body harus mematikan autofill dengan importantForAutofill=\"no\"",
            editBodyTag.contains("android:importantForAutofill=\"no\"")
        )
    }

    @Test
    fun editorActivityProgrammaticallyEnforcesSimpleLayoutAndNoAutofill() {
        assertTrue(
            "EditorActivity harus menyetel breakStrategy simple",
            editorCode.contains("bodyEdit.breakStrategy = Layout.BREAK_STRATEGY_SIMPLE")
        )
        assertTrue(
            "EditorActivity harus menyetel hyphenationFrequency none",
            editorCode.contains("bodyEdit.hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE")
        )
        assertTrue(
            "EditorActivity harus menyetel importantForAutofill no",
            editorCode.contains("bodyEdit.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO")
        )
    }

    @Test
    fun syncBodyTextWrapsSetTextWithSuppressAutosave() {
        val syncBody = functionBody(editorCode, "private fun syncBodyText()")
        assertTrue("syncBodyText() harus ada di EditorActivity", syncBody.isNotEmpty())
        val suppressStart = syncBody.indexOf("suppressAutosave = true")
        val setTextAt = syncBody.indexOf("bodyEdit.setText(currentContent)")
        val finallyAt = syncBody.indexOf("finally")
        val suppressRestore = syncBody.indexOf("suppressAutosave = prevSuppress")

        assertTrue("suppressAutosave = true harus dipanggil", suppressStart >= 0)
        assertTrue("setText harus dipanggil", setTextAt >= 0)
        assertTrue("finally block harus membungkus pemulihan suppressAutosave", finallyAt >= 0)
        assertTrue("suppressAutosave harus dipulihkan di finally", suppressRestore >= 0)

        assertTrue("suppressAutosave = true harus sebelum setText", suppressStart < setTextAt)
        assertTrue("setText harus sebelum finally", setTextAt < finallyAt)
        assertTrue("finally harus sebelum suppressRestore", finallyAt < suppressRestore)
    }

    private fun extractTag(xml: String, tagStart: String, identifier: String): String {
        var idx = 0
        while (idx < xml.length) {
            val start = xml.indexOf(tagStart, idx)
            if (start < 0) return ""
            val end = xml.indexOf("/>", start)
            val close = xml.indexOf(">", start)
            val tagEnd = if (end != -1 && (close == -1 || end < close)) end + 2 else if (close != -1) close + 1 else xml.length
            val chunk = xml.substring(start, tagEnd)
            if (chunk.contains(identifier)) {
                return chunk
            }
            idx = start + tagStart.length
        }
        return ""
    }

    private fun functionBody(code: String, signature: String): String {
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
