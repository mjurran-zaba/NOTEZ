package com.zaba.notez.diagnostics

import android.os.SystemClock
import android.util.Log

class PerfTracker(private val noteId: Long?, initialTimeMs: Long? = null) {
    private var startedAt: Long = initialTimeMs ?: SystemClock.uptimeMillis()
    private var startedAtWallClock: Long = System.currentTimeMillis()
    private val events = mutableListOf<PerfEvent>()
    private var droppedEvents = 0
    private var finished = false

    /** Jenis sesi terakhir, supaya timeline render dan timeline editor tidak tertukar. */
    private var label: String = LABEL_RENDER

    init {
        mark("OPEN_T0", "note=$noteId")
    }

    /**
     * Catat satu titik di timeline.
     *
     * [detail] sengaja bebas format: isinya bisa ukuran karakter, hash, id
     * catatan, atau pesan error. Setiap marker juga disalin ke [Breadcrumb]
     * supaya urutan lintas Activity tetap terlihat walaupun sesi ini nanti
     * ditimpa sesi lain.
     */
    fun mark(name: String, detail: String = "") {
        val elapsed = SystemClock.uptimeMillis() - startedAt
        val thread = Thread.currentThread().name
        val event = PerfEvent(name, elapsed, thread, detail)
        synchronized(events) {
            if (events.size >= MAX_EVENTS) droppedEvents++ else events += event
        }
        Breadcrumb.add("PERF", "$name +${elapsed}ms$threadSuffix(thread)$detailSuffix(detail)")
        Log.d("NOTEZ_PERF", "$name +${elapsed}ms [$thread]$detailSuffix(detail)")
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
            resetSession(LABEL_RENDER)
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
        resetSession(LABEL_INTERACTION)
        mark(name)
    }

    private fun resetSession(newLabel: String) {
        synchronized(events) {
            startedAt = SystemClock.uptimeMillis()
            startedAtWallClock = System.currentTimeMillis()
            events.clear()
            droppedEvents = 0
            finished = false
            label = newLabel
        }
        mark("OPEN_T0", "note=$noteId label=$newLabel")
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
                    startedAtWallClock = startedAtWallClock,
                    noteId = noteId,
                    events = events.toList(),
                    label = label,
                    droppedEvents = droppedEvents
                )
            }
        } ?: return
        PerfStore.saveSession(session)
        Breadcrumb.add("PERF", "session ${session.label} disimpan: ${session.events.size} event, ${session.durationMs}ms")
    }

    private fun detailSuffix(detail: String): String =
        if (detail.isBlank()) "" else " · $detail"

    private fun threadSuffix(thread: String): String =
        if (thread == "main") "" else " [$thread]"

    private companion object {
        const val MARK_RENDER_CALLED = "RENDER_CALLED"
        const val LABEL_RENDER = "render"
        const val LABEL_INTERACTION = "edit"

        /**
         * Batas event per sesi. Breadcrumb global tetap merekam semua, jadi
         * pemotongan di sini tidak menghilangkan jejak, hanya meringkas timeline.
         */
        const val MAX_EVENTS = 400
    }
}
