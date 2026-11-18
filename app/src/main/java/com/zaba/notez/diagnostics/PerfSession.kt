package com.zaba.notez.diagnostics

data class PerfSession(
    val id: String,
    /** Uptime saat sesi mulai; dipakai untuk menghitung delta antar event. */
    val startedAt: Long,
    /** Jam dinding saat sesi mulai; dipakai untuk menampilkan tanggal. */
    val startedAtWallClock: Long,
    val noteId: Long?,
    val events: List<PerfEvent>,
    /** "render" untuk Reading View, "edit" untuk sesi masuk mode edit. */
    val label: String = "render",
    /** Event yang dibuang karena batas [PerfTracker] (bukan karena filenya kosong). */
    val droppedEvents: Int = 0
) {
    /** Panjang sesi = waktu event terakhir; 0 kalau sesi tidak punya event. */
    val durationMs: Long get() = events.lastOrNull()?.elapsedMs ?: 0L
}
