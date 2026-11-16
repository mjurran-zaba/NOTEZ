package com.zaba.notez

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guard struktural untuk search Home saat database kosong (batch A1).
 *
 * Ini membaca sumber (XML dan Kotlin), bukan menjalankan app. Guard leksikal
 * tidak membuktikan toolbar benar-benar hilang atau muncul di perangkat, jadi
 * UAT H01-H04 tetap wajib. Yang dijaga di sini adalah invariant yang mudah
 * diregresikan tanpa sadar:
 *
 * 1. `home_toolbar` memulai layout dengan visibility="gone" agar tidak ada
 *    kapsul yang sempat tampil sebelum observer pertama.
 * 2. `search_input` tetap "invisible" (bukan "gone"), supaya search masih bisa
 *    dibuka mata ketika catatan tersedia.
 * 3. Sumber activity memutuskan chrome search dari total catatan, bukan dari
 *    jumlah hasil filter (`itemCount`), dan tidak lagi memakai jalur lama.
 *
 * Catatan implementasi: berkas Kotlin diperiksa setelah komentar dan literal
 * string dihapus. Penghapusnya state machine sederhana yang menghormati raw
 * string dan blok komentar bersarang; pencocokan pola mentah pernah menelan
 * kode asli karena urutan garis-miring diikuti bintang di dalam komentar
 * terbaca sebagai pembuka komentar blok.
 */
class HomeEmptySearchGuardTest {

    private val layout: String = read("layout/activity_main.xml", "res")
    private val activitySource: String = read("java/com/zaba/notez/MainActivity.kt", "main")

    @Test
    fun toolbarStartsGoneInLayout() {
        val tag = openingTag("home_toolbar")
        assertTrue("home_toolbar tidak ditemukan di activity_main.xml", tag.isNotEmpty())
        assertTrue("home_toolbar harus LinearLayout", tag.startsWith("<LinearLayout"))
        assertTrue(
            "home_toolbar butuh android:visibility=\"gone\" sebagai initial state",
            tag.contains("android:visibility=\"gone\"")
        )
    }

    @Test
    fun searchInputStaysInvisibleSoSearchCanBeReopened() {
        val tag = openingTag("search_input")
        assertTrue("search_input tidak ditemukan", tag.isNotEmpty())
        assertTrue(
            "search_input harus tetap invisible agar search bisa dibuka kembali",
            tag.contains("android:visibility=\"invisible\"")
        )
    }

    @Test
    fun searchToggleStartsGoneInLayout() {
        val tag = openingTag("search_toggle")
        assertTrue("search_toggle tidak ditemukan", tag.isNotEmpty())
        assertTrue(
            "search_toggle harus mulai gone",
            tag.contains("android:visibility=\"gone\"")
        )
    }

    @Test
    fun activityDecidesChromeFromTotalNotesNotFilteredCount() {
        val code = codeOnly(activitySource)
        assertFalse(
            "chrome search tidak boleh diputuskan dari jumlah hasil filter (itemCount)",
            code.contains("itemCount")
        )
        assertFalse(
            "jalur lama updateSearchAvailability harus sudah digantikan",
            code.contains("updateSearchAvailability")
        )
        assertTrue(
            "activity harus memakai HomeSearchPolicy.toolbarVisible",
            code.contains("HomeSearchPolicy.toolbarVisible")
        )
        assertTrue(
            "activity harus menyimpan total catatan dari observer presence",
            code.contains("totalNotes =")
        )
    }

    /** Tag pembuka satu elemen berdasarkan android:id-nya. */
    private fun openingTag(id: String): String {
        val marker = "@+id/$id\""
        val markerIndex = layout.indexOf(marker)
        if (markerIndex < 0) return ""
        val start = layout.lastIndexOf('<', markerIndex)
        val end = layout.indexOf('>', markerIndex)
        if (start < 0 || end < 0) return ""
        return layout.substring(start, end + 1)
    }

    /**
     * Hapus komentar dan literal string agar pencocokan hanya mengenai kode.
     * Panjang keluaran dijaga sama supaya posisi baris tidak bergeser.
     */
    private fun codeOnly(source: String): String {
        val out = StringBuilder(source.length)
        var i = 0
        while (i < source.length) {
            val c = source[i]
            when {
                c == '/' && i + 1 < source.length && source[i + 1] == '/' -> {
                    while (i < source.length && source[i] != '\n') {
                        out.append(' ')
                        i++
                    }
                }
                c == '/' && i + 1 < source.length && source[i + 1] == '*' -> {
                    var depth = 0
                    while (i < source.length) {
                        when {
                            source[i] == '/' && i + 1 < source.length && source[i + 1] == '*' -> {
                                depth++
                                out.append("  ")
                                i += 2
                            }
                            source[i] == '*' && i + 1 < source.length && source[i + 1] == '/' -> {
                                depth--
                                out.append("  ")
                                i += 2
                                if (depth == 0) break
                            }
                            else -> {
                                out.append(if (source[i] == '\n') '\n' else ' ')
                                i++
                            }
                        }
                    }
                }
                c == '"' && source.startsWith("\"\"\"", i) -> {
                    out.append("\"\"\"")
                    i += 3
                    while (i < source.length && !source.startsWith("\"\"\"", i)) {
                        out.append(if (source[i] == '\n') '\n' else ' ')
                        i++
                    }
                    if (i < source.length) {
                        out.append("\"\"\"")
                        i += 3
                    }
                }
                c == '"' -> {
                    out.append('"')
                    i++
                    while (i < source.length && source[i] != '"' && source[i] != '\n') {
                        if (source[i] == '\\' && i + 1 < source.length) {
                            out.append("  ")
                            i += 2
                        } else {
                            out.append(if (source[i] == '\n') '\n' else ' ')
                            i++
                        }
                    }
                    if (i < source.length && source[i] == '"') {
                        out.append('"')
                        i++
                    }
                }
                c == '\'' -> {
                    out.append('\'')
                    i++
                    while (i < source.length && source[i] != '\'' && source[i] != '\n') {
                        if (source[i] == '\\' && i + 1 < source.length) {
                            out.append("  ")
                            i += 2
                        } else {
                            out.append(if (source[i] == '\n') '\n' else ' ')
                            i++
                        }
                    }
                    if (i < source.length && source[i] == '\'') {
                        out.append('\'')
                        i++
                    }
                }
                else -> {
                    out.append(c)
                    i++
                }
            }
        }
        return out.toString()
    }

    /**
     * Berkas sumber modul app. Gradle menjalankan unit test dari direktori
     * modul, tapi jalur alternatif disediakan supaya guard tidak lolos
     * diam-diam kalau working directory berubah.
     */
    private fun read(relativePath: String, kind: String): String {
        val candidates = if (kind == "res") {
            listOf(File("src/main/res/$relativePath"), File("app/src/main/res/$relativePath"))
        } else {
            listOf(File("src/main/$relativePath"), File("app/src/main/$relativePath"))
        }
        val file = candidates.firstOrNull { it.isFile }
            ?: error(
                "Berkas $relativePath tidak ditemukan dari workingDir=${File(".").absolutePath}."
            )
        return file.readText()
    }
}
