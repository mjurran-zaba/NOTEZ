package com.zaba.notez.diagnostics

/**
 * Riwayat sesi performa di memori (terbaru di depan).
 *
 * Sebelumnya hanya satu sesi terakhir yang disimpan. Akibatnya render kedua yang
 * hanya mengenai cache (`CACHE_HIT`, 3 event) menimpa sesi render penuh tepat
 * sebelumnya, dan membuka catatan lain menimpa sesi catatan sebelumnya — itulah
 * sebabnya Diagnostics hanya menampilkan OPEN_T0 / RENDER_CALLED / CACHE_HIT.
 */
object PerfStore {

    /** Cukup untuk beberapa catatan + toggle, tanpa tumbuh tanpa batas. */
    internal const val CAPACITY = 80

    private val sessions = java.util.ArrayDeque<PerfSession>(CAPACITY)

    fun saveSession(session: PerfSession) {
        synchronized(sessions) {
            sessions.addFirst(session)
            while (sessions.size > CAPACITY) sessions.removeLast()
        }
    }

    /** Semua sesi tersimpan, terbaru di indeks 0. */
    fun getSessions(): List<PerfSession> = synchronized(sessions) { sessions.toList() }

    fun getLatestSession(): PerfSession? = synchronized(sessions) { sessions.peekFirst() }

    fun clear() {
        synchronized(sessions) { sessions.clear() }
    }
}
