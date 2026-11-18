package com.zaba.notez.markdown

/**
 * Cache proses untuk `assets/markdown/markdown-it.umd.min.js` (115 KB).
 *
 * Kenapa ada: sebelumnya asset ini dipegang `by lazy` di dalam renderer, dan
 * renderer dibuat ulang setiap Activity. Efeknya setiap buka catatan membaca +
 * meng-inflate asset yang isinya tidak pernah berubah, di main thread, di dalam
 * rentang SHELL_BUILD. Data perangkat: SHELL_BUILD 426–1110 ms untuk shell yang
 * ukurannya identik (183559 chars) di semua catatan.
 *
 * Isi asset tidak berubah selama proses hidup, jadi satu salinan per proses aman
 * dan cukup (~115 KB). [loadCount] dan [cachedChars] dipakai marker Diagnostics
 * supaya memperbaiki atau meregresi cache ini kelihatan di timeline, bukan
 * cuma di test.
 */
internal object MarkdownItAsset {

    private var cached: String? = null

    /** Berapa kali asset benar-benar dibaca dari disk. */
    var loadCount: Int = 0
        private set

    @Synchronized
    fun get(loader: () -> String): String {
        cached?.let { return it }
        val loaded = loader()
        cached = loaded
        loadCount += 1
        return loaded
    }

    /** Ukuran isi yang sedang di-cache; 0 kalau belum pernah dibaca. */
    @Synchronized
    fun cachedChars(): Int = cached?.length ?: 0

    /** Hanya untuk test: lupakan cache dan hitungan pembacaan. */
    @Synchronized
    fun resetForTest() {
        cached = null
        loadCount = 0
    }
}
