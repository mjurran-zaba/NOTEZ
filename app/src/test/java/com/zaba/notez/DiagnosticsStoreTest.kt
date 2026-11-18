package com.zaba.notez

import com.zaba.notez.diagnostics.Breadcrumb
import com.zaba.notez.diagnostics.PerfEvent
import com.zaba.notez.diagnostics.PerfSession
import com.zaba.notez.diagnostics.PerfStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Guard penyimpanan diagnostics.
 *
 * Bug yang dilindungi: `PerfStore` hanya punya satu slot "sesi terakhir".
 * Render kedua yang cuma mengenai cache (`CACHE_HIT`, 3 event) menimpa sesi
 * render penuh sebelumnya, dan membuka catatan lain menimpa sesi catatan
 * sebelumnya. Akibatnya Diagnostics hanya menampilkan OPEN_T0 / RENDER_CALLED /
 * CACHE_HIT walaupun dua catatan besar sudah dibuka.
 *
 * Test ini JVM murni: tidak butuh Android runtime.
 */
class DiagnosticsStoreTest {

    @Before
    fun reset() {
        PerfStore.clear()
        Breadcrumb.clear()
        Breadcrumb.logger = null
    }

    @Test
    fun keepsEverySessionNewestFirst() {
        PerfStore.saveSession(session("catatan-96kb", "render", 2259))
        PerfStore.saveSession(session("catatan-100kb", "render", 3100))
        assertEquals(
            listOf("catatan-100kb", "catatan-96kb"),
            PerfStore.getSessions().map { it.id }
        )
        assertEquals("catatan-100kb", PerfStore.getLatestSession()?.id)

        // Sesi cache-hit kecil tidak boleh menghapus sesi besar di belakangnya.
        PerfStore.saveSession(session("cache-hit", "render", 7))
        assertEquals(3, PerfStore.getSessions().size)
        assertEquals(2259, PerfStore.getSessions()[2].durationMs)
    }

    @Test
    fun historyIsBounded() {
        repeat(PerfStore.CAPACITY + 5) { index ->
            PerfStore.saveSession(session("s$index", "render", 1))
        }
        assertEquals(PerfStore.CAPACITY, PerfStore.getSessions().size)
        assertEquals("s${PerfStore.CAPACITY + 4}", PerfStore.getSessions().first().id)
    }

    @Test
    fun sessionKeepsWallClockThreadAndDetail() {
        val event = PerfEvent(
            name = "CACHE_HIT",
            elapsedMs = 7,
            thread = "main",
            detail = "hash=abc"
        )
        val session = PerfSession(
            id = "1",
            startedAt = 100,
            startedAtWallClock = 1_700_000_000_000,
            noteId = 34,
            events = listOf(event)
        )
        // Tanggal harus berasal dari jam dinding; uptime 100 ms bukan tanggal 1970.
        assertTrue(session.startedAtWallClock > 1_600_000_000_000)
        assertEquals("abc", session.events.single().detail)
        assertEquals(7, session.durationMs)

        val defaults = PerfEvent(name = "RENDER_CALLED", elapsedMs = 0)
        assertEquals("main", defaults.thread)
        assertEquals("", defaults.detail)
        assertEquals(0, PerfSession("2", 0, 0, null, emptyList()).durationMs)
    }

    @Test
    fun breadcrumbKeepsOrderAndDropsOldest() {
        Breadcrumb.add("HOME", "TAP_NOTE id=34")
        Breadcrumb.add("EDITOR", "onCreate note=34")
        Breadcrumb.add("PERF", "PAINT_REAL +2259ms")

        val entries = Breadcrumb.snapshot()
        assertEquals(listOf("HOME", "EDITOR", "PERF"), entries.map { it.area })
        assertEquals("TAP_NOTE id=34", entries.first().message)
        assertEquals(0, Breadcrumb.droppedCount())

        val total = Breadcrumb.CAPACITY + 10
        repeat(total) { index -> Breadcrumb.add("TEST", "e$index") }
        val after = Breadcrumb.snapshot()
        assertEquals(Breadcrumb.CAPACITY, after.size)
        assertEquals("e10", after.first().message)
        // 3 breadcrumb awal + (total - CAPACITY) terbaru yang mendorong keluar.
        assertEquals(3 + total - Breadcrumb.CAPACITY, Breadcrumb.droppedCount())
    }

    private fun session(id: String, label: String, durationMs: Long) = PerfSession(
        id = id,
        startedAt = 0,
        startedAtWallClock = System.currentTimeMillis(),
        noteId = 34,
        label = label,
        events = listOf(PerfEvent("PAINT_REAL", durationMs))
    )
}
