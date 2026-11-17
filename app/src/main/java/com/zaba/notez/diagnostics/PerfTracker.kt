package com.zaba.notez.diagnostics

import android.os.SystemClock
import android.util.Log

class PerfTracker(private val noteId: Long?, initialTimeMs: Long? = null) {
    private var startedAt: Long = initialTimeMs ?: SystemClock.uptimeMillis()
    private val events = mutableListOf<PerfEvent>()
    private var finished = false

    init {
        mark("OPEN_T0")
    }

    fun mark(name: String) {
        val elapsed = SystemClock.uptimeMillis() - startedAt
        synchronized(events) {
            events += PerfEvent(name, elapsed)
        }
        Log.d("NOTEZ_PERF", "$name +${elapsed}ms")
    }

    /**
     * Tandai awal satu sesi render.
     *
     * Render pertama setelah Activity dibuka tetap memakai sesi yang sudah ada,
     * supaya garis waktu open catatan (ACTIVITY_CREATED → DB_QUERY_DONE → … →
     * RENDER_CALLED) utuh dan bisa dibandingkan dengan baseline sebelumnya.
     *
     * Render berikutnya (mis. toggle view→edit→view) membuka sesi baru dengan
     * `startedAt` baru. Tanpa ini, delta toggle tercampur dengan garis waktu
     * pembukaan catatan dan `startedAt` tidak pernah di-reset.
     *
     * Kalau render sebelumnya belum selesai (belum sampai PAINT_REAL), sesi itu
     * ditutup dulu: dua render yang tumpang tindih tidak boleh berbagi sesi.
     */
    fun beginRenderSession() {
        val previousRenderOpen = synchronized(events) {
            !finished && events.any { it.name == MARK_RENDER_CALLED }
        }
        if (previousRenderOpen) {
            finishSession()
        }
        val startFresh = synchronized(events) { finished }
        if (startFresh) {
            synchronized(events) {
                startedAt = SystemClock.uptimeMillis()
                events.clear()
                finished = false
            }
            mark("OPEN_T0")
        }
        mark(MARK_RENDER_CALLED)
    }

    fun finishSession() {
        // Idempotent: satu sesi hanya disimpan sekali, meskipun jalur cache dan
        // jalur paint sama-sama memanggil finishSession().
        val eventsInSession = snapshotAndClose() ?: return
        val session = PerfSession(
            id = System.currentTimeMillis().toString(),
            startedAt = startedAt,
            noteId = noteId,
            events = eventsInSession
        )
        PerfStore.saveSession(session)
    }

    /** Salin event sekali saja; null kalau sesi ini sudah ditutup. */
    private fun snapshotAndClose(): List<PerfEvent>? = synchronized(events) {
        if (finished) {
            null
        } else {
            finished = true
            events.toList()
        }
    }

    private companion object {
        const val MARK_RENDER_CALLED = "RENDER_CALLED"
    }
}
