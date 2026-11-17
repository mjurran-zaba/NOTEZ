package com.zaba.notez.diagnostics

import android.os.SystemClock
import android.util.Log

class PerfTracker(private val noteId: Long?, initialTimeMs: Long? = null) {
    private var startedAt: Long = initialTimeMs ?: SystemClock.uptimeMillis()
    private val events = mutableListOf<PerfEvent>()
    private var finished = false

    /** Jenis sesi terakhir, supaya timeline render dan timeline editor tidak tertukar. */
    private var label: String = LABEL_RENDER

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
                label = LABEL_RENDER
            }
            mark("OPEN_T0")
        }
        mark(MARK_RENDER_CALLED)
    }

    /**
     * Mulai sesi interaksi (mis. masuk mode edit) dengan garis waktu sendiri.
     *
     * Sesi render ditutup di PAINT_REAL. Tanpa sesi baru di sini, marker
     * EDIT_CLICK/EDIT_TEXT_SET/EDIT_VISIBLE/EDIT_LAYOUT_DONE hanya menumpuk di
     * memori lalu dibuang saat sesi render berikutnya dimulai — itulah sebabnya
     * diagnosa lama berhenti di PAINT_REAL dan tidak punya angka editor.
     *
     * Sesi render yang belum selesai (mis. user menekan pensil sebelum paint)
     * disimpan dulu, bukan dibuang; sesi yang sudah ditutup tidak disimpan dua
     * kali karena [finishSession] idempoten.
     */
    fun beginInteractionSession(name: String) {
        val previousOpen = synchronized(events) { !finished && events.size > 1 }
        if (previousOpen) {
            finishSession()
        }
        synchronized(events) {
            startedAt = SystemClock.uptimeMillis()
            events.clear()
            finished = false
            label = LABEL_INTERACTION
        }
        mark(name)
    }

    fun finishSession() {
        // Idempotent: satu sesi hanya disimpan sekali, meskipun jalur cache,
        // jalur paint, dan penutup sesi interaksi sama-sama memanggilnya.
        val session = synchronized(events) {
            if (finished) {
                null
            } else {
                finished = true
                PerfSession(
                    id = System.currentTimeMillis().toString(),
                    startedAt = startedAt,
                    noteId = noteId,
                    events = events.toList(),
                    label = label
                )
            }
        } ?: return
        PerfStore.saveSession(session)
    }

    private companion object {
        const val MARK_RENDER_CALLED = "RENDER_CALLED"
        const val LABEL_RENDER = "render"
        const val LABEL_INTERACTION = "edit"
    }
}
