package com.zaba.notez

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
 */
class HomeEmptySearchGuardTest {

    private val layout: String = SourceScan.readRes("layout/activity_main.xml")
    private val activityCode: String =
        SourceScan.codeOnly(SourceScan.readMainFile("java/com/zaba/notez/MainActivity.kt"))

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
        // itemCount boleh dipakai untuk hal lain (mis. clamp posisi scroll),
        // jadi yang diperiksa hanya badan keputusan chrome search.
        val body = functionBody("private fun renderSearchChrome(")
        assertTrue("renderSearchChrome tidak ditemukan", body.isNotEmpty())
        assertFalse(
            "chrome search tidak boleh diputuskan dari jumlah hasil filter (itemCount)",
            body.contains("itemCount")
        )
        assertTrue(
            "chrome search harus memakai HomeSearchPolicy.toolbarVisible",
            body.contains("HomeSearchPolicy.toolbarVisible")
        )
        assertFalse(
            "jalur lama updateSearchAvailability harus sudah digantikan",
            activityCode.contains("updateSearchAvailability")
        )
        assertTrue(
            "activity harus menyimpan total catatan dari observer presence",
            activityCode.contains("totalNotes =")
        )
    }

    /** Badan satu fungsi berdasarkan awal signature-nya (komentar/string sudah dibuang). */
    private fun functionBody(signature: String): String {
        val start = activityCode.indexOf(signature)
        if (start < 0) return ""
        val open = activityCode.indexOf('{', start)
        if (open < 0) return ""
        var depth = 0
        for (i in open until activityCode.length) {
            when (activityCode[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return activityCode.substring(open, i + 1)
                }
            }
        }
        return activityCode.substring(open)
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
}
