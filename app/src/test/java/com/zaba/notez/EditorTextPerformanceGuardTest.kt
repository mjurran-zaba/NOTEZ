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
 * 4. EmojiCompat text scanner yang menyisir seluruh karakter untuk mencari emoji spans.
 * 5. Kursor dipaksa lari ke ujung akhir dokumen (baris 2400) yang memicu bringPointIntoView 1 detik.
 * 6. Menjamin idle pre-warming pada saat Reading View aktif agar toggle edit instan (< 10 ms).
 */
class EditorTextPerformanceGuardTest {

    private val editorXml: String = SourceScan.readRes("layout/activity_editor.xml")
    private val editorRaw: String = SourceScan.readMainFile("java/com/zaba/notez/EditorActivity.kt")
    private val editorCode: String = SourceScan.codeOnly(editorRaw)
    private val rendererCode: String = SourceScan.codeOnly(
        SourceScan.readMainFile("java/com/zaba/notez/markdown/MarkdownPreviewRenderer.kt")
    )

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
    fun editBodyXmlDeclaresEmojiCompatDisabled() {
        val editBodyTag = extractTag(editorXml, "<EditText", "android:id=\"@+id/edit_body\"")
        assertTrue("Tag edit_body harus ditemukan di XML", editBodyTag.isNotEmpty())
        assertTrue(
            "edit_body harus menonaktifkan emojiCompat scanning dengan app:emojiCompatEnabled=\"false\"",
            editBodyTag.contains("app:emojiCompatEnabled=\"false\"")
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
        assertTrue(
            "EditorActivity harus menyetel isEmojiCompatEnabled false",
            editorCode.contains("isEmojiCompatEnabled = false")
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

    @Test
    fun editorActivityHooksIdlePrewarmOnPaintReal() {
        assertTrue(
            "MarkdownPreviewRenderer harus menyediakan callback onPaintReal",
            rendererCode.contains("var onPaintReal:")
        )
        assertTrue(
            "EditorActivity harus mendaftarkan handler onPaintReal untuk idle pre-warm",
            editorCode.contains("markdownPreview.onPaintReal =")
        )
        assertTrue(
            "Pre-warm harus menggunakan IdleHandler agar tidak mengganggu rendering aktif",
            editorCode.contains("addIdleHandler")
        )
        assertTrue(
            "Idle pre-warm harus memanggil syncBodyText",
            editorCode.contains("syncBodyText()")
        )
    }

    @Test
    fun switchToEditPlacesCursorAtStartNotEnd() {
        val body = functionBody(editorCode, "private fun switchToEdit()")
        assertTrue("switchToEdit() harus ada", body.isNotEmpty())
        assertFalse(
            "Kursor tidak boleh dipaksa ke akhir dokumen panjang (memaksa scroll 2400 baris)",
            body.contains("bodyEdit.setSelection(bodyEdit.text?.length ?: 0)")
        )
        assertTrue(
            "Kursor harus diset ke posisi awal (0)",
            body.contains("bodyEdit.setSelection(0)")
        )
    }

    @Test
    fun markdownPreviewRendererProvidesScrollSynchronizationApis() {
        assertTrue(
            "MarkdownPreviewRenderer harus menyediakan currentScrollRatio()",
            rendererCode.contains("fun currentScrollRatio(): Float")
        )
        assertTrue(
            "MarkdownPreviewRenderer harus menyediakan scrollToRatio(ratio: Float)",
            rendererCode.contains("fun scrollToRatio(ratio: Float)")
        )
    }

    @Test
    fun editorActivitySynchronizesScrollPositionBidirectionally() {
        val switchToEdit = functionBody(editorCode, "private fun switchToEdit()")
        assertTrue("switchToEdit harus ada", switchToEdit.isNotEmpty())
        assertTrue(
            "switchToEdit harus membaca rasio scroll dari MarkdownPreviewRenderer",
            switchToEdit.contains("markdownPreview.currentScrollRatio()")
        )
        assertTrue(
            "switchToEdit harus menggulir editor ke lineTop posisi baca",
            switchToEdit.contains("bodyEdit.scrollTo(0, lineTop)")
        )

        val switchToView = functionBody(editorCode, "private fun switchToView()")
        assertTrue("switchToView harus ada", switchToView.isNotEmpty())
        assertTrue(
            "switchToView harus menyimpan rasio scroll untuk dipulihkan",
            switchToView.contains("pendingScrollRatio =")
        )

        assertTrue(
            "onPaintReal harus memulihkan rasio scroll ke MarkdownPreviewRenderer",
            editorCode.contains("markdownPreview.scrollToRatio(ratio)")
        )
    }

    @Test
    fun editorActivityPreservesScrollRatioAcrossConfigurationChange() {
        assertTrue(
            "EditorActivity harus mendefinisikan KEY_SCROLL_RATIO",
            editorRaw.contains("const val KEY_SCROLL_RATIO = \"state_scroll_ratio\"")
        )
        val onSave = functionBody(editorCode, "override fun onSaveInstanceState(")
        assertTrue("onSaveInstanceState harus menyimpan KEY_SCROLL_RATIO", onSave.contains("outState.putFloat(KEY_SCROLL_RATIO"))

        val onCreate = functionBody(editorCode, "override fun onCreate(")
        assertTrue("onCreate harus membaca KEY_SCROLL_RATIO", onCreate.contains("state.getFloat(KEY_SCROLL_RATIO"))
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
