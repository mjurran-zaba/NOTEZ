package com.zaba.notez

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guard untuk chrome search Home saat database kosong (batch A1 RFC Home
 * Empty Search / Card List-Grid / Markdown).
 *
 * Murni JVM: hanya fungsi murni, tanpa Robolectric maupun Android runtime.
 * Bukti bahwa toolbar benar-benar hilang dari layar tetap memerlukan UAT
 * perangkat (H01-H04); test ini hanya menjaga keputusan `unknown`/0/>0.
 */
class HomeSearchPolicyTest {

    @Test
    fun toolbarHiddenBeforeFirstObserverEmission() {
        assertFalse(
            "toolbar tidak boleh tampil sebelum total catatan diketahui",
            HomeSearchPolicy.toolbarVisible(null)
        )
    }

    @Test
    fun toolbarHiddenWhenDatabaseEmpty() {
        assertFalse(
            "toolbar harus tersembunyi saat total catatan nol",
            HomeSearchPolicy.toolbarVisible(0)
        )
    }

    @Test
    fun toolbarVisibleWhenAnyNoteExists() {
        assertTrue("toolbar harus tampil saat ada catatan", HomeSearchPolicy.toolbarVisible(1))
        assertTrue("toolbar harus tampil saat banyak catatan", HomeSearchPolicy.toolbarVisible(500))
    }

    @Test
    fun queryWithoutResultMustNotBeTreatedAsEmptyDatabase() {
        // Simulasi RFC: database berisi 5 catatan, query tidak menghasilkan apa pun.
        // Angka hasil filter (0) tidak boleh dipakai sebagai total catatan.
        val totalNotes = 5
        val filteredResultCount = 0
        assertFalse("premis test salah", filteredResultCount > 0)
        assertTrue(
            "query no-result tidak boleh menyembunyikan search",
            HomeSearchPolicy.toolbarVisible(totalNotes)
        )
    }
}
