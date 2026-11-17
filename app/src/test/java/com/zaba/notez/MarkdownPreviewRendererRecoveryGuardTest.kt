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

    private val editorCode: String = SourceScan.codeOnly(
        SourceScan.readMainFile("java/com/zaba/notez/EditorActivity.kt")
    )

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
            body.contains("bodyEdit.setText(currentContent)")
        )
        assertTrue(
            "isi ulang hanya saat berbeda, supaya kursor tidak reset",
            body.contains("bodyEdit.text.toString() != currentContent")
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
