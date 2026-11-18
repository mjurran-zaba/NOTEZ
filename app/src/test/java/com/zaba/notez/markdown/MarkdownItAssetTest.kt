package com.zaba.notez.markdown

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Cache markdown-it harus sekali per proses.
 *
 * Kenapa dijaga di level unit: di data perangkat `SHELL_BUILD` makan 426–1110 ms
 * untuk shell yang ukurannya identik (183559 chars) di setiap Activity. Satu
 * penyebabnya asset markdown-it (115 KB) dibaca ulang tiap renderer baru, dan
 * renderer dibuat ulang tiap Activity. Loader di test ini meniru biaya itu
 * sebagai hitungan, jadi kalau cache dilepas lagi angkanya langsung merah.
 */
class MarkdownItAssetTest {

    @After
    fun tearDown() {
        MarkdownItAsset.resetForTest()
    }

    @Test
    fun loadsAssetOnlyOncePerProcess() {
        var loads = 0
        val loader = {
            loads += 1
            "js-$loads"
        }
        assertEquals("js-1", MarkdownItAsset.get(loader))
        assertEquals("js-1", MarkdownItAsset.get(loader))
        assertEquals("asset tidak boleh dibaca dua kali", 1, loads)
        assertEquals(1, MarkdownItAsset.loadCount)
    }

    @Test
    fun exposesCachedSizeForDiagnostics() {
        assertEquals(0, MarkdownItAsset.cachedChars())
        MarkdownItAsset.get { "12345" }
        assertEquals(5, MarkdownItAsset.cachedChars())
    }
}
