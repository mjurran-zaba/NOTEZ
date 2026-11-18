package com.zaba.notez

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guard leksikal untuk issue #2 (Reading View perf).
 *
 * Yang dijaga:
 * - renderer mati tidak boleh meninggalkan cache state basi sehingga render()
 *   berikutnya lolos CACHE_HIT tanpa menggambar;
 * - applyMode(editing) harus mengisi bodyEdit dari currentContent supaya rotasi
 *   saat mode edit tidak menyimpan body kosong.
 *
 * Ini bukan bukti runtime: recovery penuh tetap butuh recreate WebView dan
 * verifikasi di perangkat. Lihat komentar di kode produksi.
 */
class MarkdownPreviewRendererRecoveryGuardTest {

    private val rendererCode: String = SourceScan.codeOnly(
        SourceScan.readMainFile("java/com/zaba/notez/markdown/MarkdownPreviewRenderer.kt")
    )

    /** Sumber mentah: dipakai kalau guard harus mencocokkan literal string. */
    private val editorRaw: String = SourceScan.readMainFile("java/com/zaba/notez/EditorActivity.kt")

    private val editorCode: String = SourceScan.codeOnly(editorRaw)

    @Test
    fun onRenderProcessGoneResetsShellCacheState() {
        val body = functionBody(rendererCode, "override fun onRenderProcessGone(")
        assertTrue("onRenderProcessGone tidak ditemukan", body.isNotEmpty())
        assertTrue("shell harus dibangun ulang", body.contains("shellLoaded = false"))
        assertTrue("hash render basi harus dibuang", body.contains("lastRenderHash = null"))
        assertTrue("tema shell basi harus dibuang", body.contains("shellTheme = null"))
        assertTrue("tema render basi harus dibuang", body.contains("lastRenderTheme = null"))
        assertTrue("pending source basi harus dibuang", body.contains("pendingSourceJson = null"))
        assertTrue("pending base basi harus dibuang", body.contains("pendingBaseJson = null"))
        assertTrue("pending cache basi harus dibuang", body.contains("pendingCacheJson = null"))
    }

    @Test
    fun onRenderProcessGoneDoesNotLoadUrlOnDeadView() {
        val body = functionBody(rendererCode, "override fun onRenderProcessGone(")
        assertTrue("onRenderProcessGone tidak ditemukan", body.isNotEmpty())
        assertFalse(
            "view dengan renderer mati tidak boleh dipakai loadUrl",
            body.contains("loadUrl(")
        )
        assertTrue(
            "marker performa harus tetap ada",
            body.contains("perfTracker?.mark(")
        )
    }

    @Test
    fun shellDoesNotRewriteScriptWithAmbiguousReplace() {
        // Regresi issue #2: buildShell() dulu mengganti semua "(function () {"
        // termasuk callback requestAnimationFrame, sehingga window.notezRender
        // tertimpa callback paint dan preview kosong untuk semua catatan.
        val raw = SourceScan.readMainFile("java/com/zaba/notez/markdown/MarkdownPreviewRenderer.kt")
        assertFalse(
            "shell tidak boleh diubah lewat replace() pola yang ambigu",
            raw.contains(".replace(\"(function () {\"")
        )
        val signature = "window.notezRender = function (sourceJson, githubRawBaseJson, cachedImagesJson) {"
        assertTrue("shell harus mendefinisikan window.notezRender langsung", raw.contains(signature))
        assertTrue(
            "window.notezRender hanya boleh didefinisikan sekali",
            raw.split(signature).size - 1 == 1
        )
        assertTrue(
            "source shell harus dari parameter, bukan hasil replace",
            raw.contains("var source = sourceJson;")
        )
    }

    @Test
    fun applyModePopulatesBodyFromCurrentContent() {
        val body = functionBody(editorCode, "private fun applyMode(")
        assertTrue("applyMode tidak ditemukan", body.isNotEmpty())
        assertTrue(
            "mode edit harus mengisi bodyEdit dari currentContent",
            body.contains("syncBodyText()")
        )
        val sync = functionBody(editorCode, "private fun syncBodyText(")
        assertTrue("syncBodyText tidak ditemukan", sync.isNotEmpty())
        val guardAt = sync.indexOf("bodyTextSynced == currentContent")
        val setTextAt = sync.indexOf("bodyEdit.setText(currentContent)")
        assertTrue("isi ulang hanya saat berbeda, supaya kursor tidak reset", guardAt >= 0)
        assertTrue("syncBodyText harus punya jalur skip", sync.contains("return"))
        assertTrue(
            "isi body hanya ditulis setelah cek isi yang sama",
            guardAt in 0 until setTextAt
        )
    }

    @Test
    fun switchToEditDoesNotRewriteIdenticalBody() {
        // Regresi delay masuk editor pada catatan 96 KB: setText selalu dipanggil
        // ulang walau isi EditText sudah sama, dan perbandingan memakai
        // text.toString() yang menyalin seluruh isi.
        val body = functionBody(editorCode, "private fun switchToEdit(")
        assertTrue("switchToEdit tidak ditemukan", body.isNotEmpty())
        assertFalse(
            "switchToEdit tidak boleh menyalin body tanpa cek isi",
            body.contains("bodyEdit.setText(")
        )
        assertTrue("switchToEdit harus memakai syncBodyText", body.contains("syncBodyText()"))
        assertFalse(
            "perbandingan isi tidak boleh mengalokasikan text.toString()",
            editorCode.contains("bodyEdit.text.toString() != currentContent")
        )
    }

    @Test
    fun switchToEditDefersCursorFocusAndKeyboardUntilVisible() {
        // bringPointIntoView pada view GONE memaksa layout dokumen penuh sebelum
        // view diukur, jadi kursor/fokus/keyboard harus dijadwalkan setelahnya.
        val body = functionBody(editorCode, "private fun switchToEdit(")
        assertTrue("switchToEdit tidak ditemukan", body.isNotEmpty())
        val setSelectionAt = body.indexOf("bodyEdit.setSelection(")
        val postAt = body.indexOf("bodyEdit.post {")
        assertTrue("kursor harus dipasang", setSelectionAt >= 0)
        assertTrue("kursor harus dipasang setelah view terlihat", postAt in 0 until setSelectionAt)
        assertTrue(
            "fokus dan keyboard juga di dalam post",
            body.indexOf("showKeyboard(bodyEdit)") > postAt
        )
    }

    @Test
    fun editorInteractionSessionIsPersisted() {
        // Sebelum perbaikan, marker EDIT_* hanya menumpuk di memori: sesi render
        // ditutup di PAINT_REAL, dan sesi berikutnya menghapus daftar event tanpa
        // pernah menyimpannya.
        val tracker = SourceScan.codeOnly(
            SourceScan.readMainFile("java/com/zaba/notez/diagnostics/PerfTracker.kt")
        )
        val begin = functionBody(tracker, "fun beginInteractionSession(")
        assertTrue("beginInteractionSession tidak ditemukan", begin.isNotEmpty())
        assertTrue("sesi baru harus mereset garis waktu", begin.contains("resetSession("))
        // Reset dipusatkan di resetSession: satu tempat untuk render dan edit,
        // supaya label, jam dinding, dan daftar event tidak bisa lupa di-reset.
        val reset = functionBody(tracker, "private fun resetSession(")
        assertTrue("resetSession tidak ditemukan", reset.isNotEmpty())
        assertTrue("sesi baru harus membuka ulang sesi", reset.contains("finished = false"))
        assertTrue("sesi baru harus punya garis waktu sendiri", reset.contains("events.clear()"))
        assertTrue(
            "label sesi harus ikut diganti",
            reset.contains("label = newLabel")
        )

        val switchToEdit = functionBody(editorCode, "private fun switchToEdit(")
        assertTrue(
            "sesi edit harus ditutup supaya tersimpan",
            switchToEdit.contains("perfTracker?.finishSession()")
        )
        // codeOnly mengosongkan literal string, jadi nama marker dicek di sumber
        // mentah; pemanggilannya dicek di kode yang sudah dibersihkan komentar.
        assertTrue(
            "sesi edit harus dibuka saat pensil ditekan",
            editorRaw.contains("beginInteractionSession(\"EDIT_CLICK\")")
        )
        assertTrue(
            "pemanggilan beginInteractionSession harus ada di kode",
            editorCode.contains("beginInteractionSession(")
        )
    }

    @Test
    fun editorBodyIsBoundedToViewport() {
        // wrap_content di dalam ScrollView memaksa TextView mengukur tinggi
        // seluruh dokumen sebelum bisa tampil.
        val layout = SourceScan.readRes("layout/activity_editor.xml")
        assertFalse(
            "body editor tidak boleh dibungkus ScrollView lagi",
            layout.contains("@+id/edit_body_scroll")
        )
        val start = layout.indexOf("@+id/edit_body\"")
        assertTrue("EditText body tidak ditemukan", start >= 0)
        val tagEnd = layout.indexOf("/>" , start)
        assertTrue("tag EditText body tidak tertutup", tagEnd > start)
        val tag = layout.substring(start, tagEnd)
        assertTrue(
            "EditText body harus setinggi viewport, bukan wrap_content",
            tag.contains("android:layout_height=\"match_parent\"")
        )
    }

    @Test
    fun editorDestroyFlushesOpenSession() {
        // Data perangkat: impor 100 KB ditutup sebelum PAINT_REAL, jadi sesi
        // render-nya hilang sama sekali dari Diagnostics. Activity yang mati
        // harus menyimpan sesi yang masih terbuka, bukan menunggu PAINT_REAL.
        // Dibaca dari sumber mentah: codeOnly mengosongkan literal string, jadi
        // nama marker "ACTIVITY_DESTROY" tidak akan ketemu di sana.
        val body = functionBody(editorRaw, "override fun onDestroy(")
        assertTrue("onDestroy tidak ditemukan", body.isNotEmpty())
        val markAt = body.indexOf("mark(\"ACTIVITY_DESTROY\"")
        val finishAt = body.indexOf("perfTracker?.finishSession()")
        assertTrue("ACTIVITY_DESTROY harus tetap dicatat", markAt >= 0)
        assertTrue("sesi yang masih terbuka harus disimpan saat destroy", finishAt >= 0)
        assertTrue(
            "penutupan sesi terjadi setelah marker destroy supaya urutannya benar",
            markAt < finishAt
        )
    }

    @Test
    fun shellDeclaresInlineFaviconSoWebViewDoesNotRequestIt() {
        // Data perangkat: satu-satunya WEBVIEW_SUBRESOURCE_ERROR adalah
        // ERR_CACHE_MISS untuk https://notez.local/favicon.ico yang diminta
        // Chromium sendiri. Ikon inline menghapus permintaan itu di sumbernya.
        val raw = SourceScan.readMainFile("java/com/zaba/notez/markdown/MarkdownPreviewRenderer.kt")
        val icon = "<link rel=\"icon\" href=\"data:,\">"
        assertTrue("shell harus mendeklarasikan ikon inline", raw.contains(icon))
        val iconAt = raw.indexOf(icon)
        assertTrue(
            "ikon inline harus berada di dalam <head>",
            iconAt in 0 until raw.indexOf("</head>")
        )
    }

    /** Badan satu fungsi berdasarkan awal signature-nya. */
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
