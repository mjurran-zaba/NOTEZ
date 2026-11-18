package com.zaba.notez.music

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guard leksikal untuk chrome panel Music (issue #22).
 *
 * Yang dijaga di sini tidak bisa diuji sebagai fungsi murni: apakah tombol
 * prev/next benar-benar ImageButton berikon vector dengan contentDescription,
 * bukan TextView berisi karakter ‹ ›. Membaca berkas sumber, bukan menjalankan
 * app — jadi ini bukan bukti runtime, hanya penjaga agar regresi gaya kembali.
 *
 * Catatan implementasi: tidak ada penghapusan komentar di sini karena yang
 * dicocokkan adalah atribut XML, bukan pola kode Kotlin. Kalau nanti guard ini
 * diperluas ke berkas .kt, penghapus komentar WAJIB memakai pemindaian state
 * machine yang menghormati literal string — penghapus regex naif pernah
 * melahap kode asli karena urutan garis-miring diikuti bintang di dalam
 * komentar terbaca sebagai pembuka komentar blok (insiden ZCODE 2026-08-13).
 *
 * Kotlin mendukung komentar blok BERSARANG, jadi pembuka kedua di dalam
 * komentar menutupi penutup pertama: komentar luar tidak pernah selesai dan
 * build gagal dengan "Unclosed comment". Jangan menuliskan urutan pembuka
 * komentar di dalam komentar, sekalipun niatnya hanya sebagai contoh.
 */
class MusicChromeGuardTest {

    private val layout: String = readRes("layout/include_music_drawer.xml")
    private val strings: String = readRes("values/strings.xml")

    @Test
    fun previousAndNextAreIconButtonsNotTextGlyphs() {
        for (id in listOf("music_previous", "music_next")) {
            val block = elementBlock(id)
            assertTrue("$id tidak ditemukan di include_music_drawer.xml", block.isNotEmpty())
            assertTrue("tombol $id harus ImageButton", block.startsWith("<ImageButton"))
            assertTrue(
                "tombol $id butuh android:contentDescription",
                block.contains("android:contentDescription=")
            )
            assertTrue(
                "tombol $id butuh ikon vector di res/drawable",
                block.contains("android:src=\"@drawable/ic_music_") ||
                    block.contains("android:src=\"@drawable/ic_lucide_")
            )
        }
    }

    @Test
    fun layoutHasNoChevronTextGlyphs() {
        assertFalse("chrome fungsional tidak boleh memakai karakter ‹", layout.contains("android:text=\"‹\""))
        assertFalse("chrome fungsional tidak boleh memakai karakter ›", layout.contains("android:text=\"›\""))
    }

    @Test
    fun musicDrawablesExistOnDisk() {
        val names = listOf(
            "ic_music_play",
            "ic_music_pause",
            "ic_music_prev",
            "ic_music_next",
            "ic_music_loop_all",
            "ic_music_once",
            "ic_music_shuffle"
        )
        for (name in names) {
            assertTrue("drawable $name hilang", File(resDir(), "drawable/$name.xml").isFile)
        }
    }

    @Test
    fun everyMusicContentDescriptionStringExists() {
        val names = listOf(
            "cd_music_previous",
            "cd_music_next",
            "cd_music_mode_repeat_all",
            "cd_music_mode_repeat_one",
            "cd_music_mode_shuffle"
        )
        for (name in names) {
            assertTrue("string $name hilang dari strings.xml", strings.contains("name=\"$name\""))
        }
    }

    /** Potongan teks satu elemen berdasarkan android:id-nya. */
    private fun elementBlock(id: String): String {
        val marker = "@+id/$id\""
        val markerIndex = layout.indexOf(marker)
        if (markerIndex < 0) return ""
        val elementStart = layout.lastIndexOf('<', markerIndex).coerceAtLeast(0)
        val selfClosing = layout.indexOf("/>", markerIndex)
        val elementEnd = if (selfClosing < 0) layout.length else selfClosing + 2
        return layout.substring(elementStart, elementEnd.coerceAtMost(layout.length))
    }

    /**
     * Direktori res modul app. Gradle menjalankan unit test dari direktori
     * modul, tapi jalur alternatif disediakan supaya guard tidak lolos diam-diam
     * kalau working directory berubah — kalau tidak ketemu, test gagal keras.
     */
    private fun resDir(): File {
        val candidates = listOf(File("src/main/res"), File("app/src/main/res"))
        return candidates.firstOrNull { it.isDirectory }
            ?: error(
                "Direktori res tidak ditemukan dari workingDir=${File(".").absolutePath}. " +
                    "Guard ini harus dijalankan dari direktori modul app/."
            )
    }

    private fun readRes(relativePath: String): String {
        val file = File(resDir(), relativePath)
        if (!file.isFile) {
            error(
                "Berkas ${file.path} tidak ada (workingDir=${File(".").absolutePath}). " +
                    "Guard membaca sumber, jadi jalur ini harus benar."
            )
        }
        return file.readText()
    }
}
