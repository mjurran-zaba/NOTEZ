package com.zaba.notez

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guard untuk pemetaan mode tampilan (batch B1).
 *
 * Murni JVM: hanya [NoteDisplayMode.fromPersisted]. Persistensi ke disk
 * (SharedPreferences lewat [HomeDisplayPref]) tidak dibuktikan di sini karena
 * butuh Android runtime; itu masuk UAT H06 di perangkat.
 *
 * Literal "list"/"grid" diuji sebagai kontrak penyimpanan, bukan detail acak:
 * nilai itulah yang tertulis di preference pengguna dan harus tetap terbaca
 * setelah rename atau reorder enum.
 */
class NoteDisplayModeTest {

    @Test
    fun missingPreferenceFallsBackToList() {
        assertEquals(NoteDisplayMode.LIST, NoteDisplayMode.fromPersisted(null))
    }

    @Test
    fun unknownOrEmptyValueFallsBackToList() {
        assertEquals(NoteDisplayMode.LIST, NoteDisplayMode.fromPersisted(""))
        assertEquals(NoteDisplayMode.LIST, NoteDisplayMode.fromPersisted("BUKAN_MODE"))
        assertEquals(NoteDisplayMode.LIST, NoteDisplayMode.fromPersisted("LIST"))
        assertEquals(NoteDisplayMode.LIST, NoteDisplayMode.fromPersisted(" grid "))
    }

    @Test
    fun everyModeRoundTripsThroughPersistedValue() {
        for (mode in NoteDisplayMode.values()) {
            assertEquals(
                "mode $mode tidak terbaca kembali dari nilai tersimpannya",
                mode,
                NoteDisplayMode.fromPersisted(mode.persistedValue)
            )
        }
    }

    @Test
    fun persistedValuesAreStableAndDistinct() {
        assertEquals("list", NoteDisplayMode.LIST.persistedValue)
        assertEquals("grid", NoteDisplayMode.GRID.persistedValue)
        val values = NoteDisplayMode.values().map { it.persistedValue }
        assertEquals("nilai tersimpan tidak boleh kembar", values.size, values.toSet().size)
    }
}
