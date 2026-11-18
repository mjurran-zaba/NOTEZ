package com.zaba.notez.diagnostics

/**
 * Satu kejadian breadcrumb global.
 *
 * [area] adalah bagian aplikasi yang mencatat (HOME, EDITOR, PERF, WEBVIEW, IO),
 * [message] adalah kejadiannya. Waktu memakai jam dinding supaya urutan antar
 * Activity tetap masuk akal; timeline performa tetap memakai uptime monotonic.
 */
data class BreadcrumbEntry(
    val wallClockMs: Long,
    val thread: String,
    val area: String,
    val message: String
)

/**
 * Log breadcrumb global: bukan milik satu catatan, bukan milik satu tracker.
 *
 * Kenapa terpisah dari [PerfSession]: ada kejadian yang terjadi sebelum tracker
 * dibuat (tap di daftar catatan, buka drawer, impor file) dan kejadian di
 * Activity lain saat tracker catatan tidak aktif. Kalau semuanya hanya masuk
 * sesi performa, kejadian itu hilang.
 *
 * Kelas ini sengaja tidak menyentuh Android API supaya bisa diuji di JVM murni.
 * Pemancaran ke logcat dipasang lewat [logger] oleh activity yang aktif
 * (lihat `installBreadcrumbLogcat`).
 */
object Breadcrumb {

    /** Batas keras supaya diagnosa lama tidak menumpuk tanpa batas di memori. */
    internal const val CAPACITY = 600

    private val entries = java.util.ArrayDeque<BreadcrumbEntry>(1)
    private var dropped = 0

    /** Sink opsional ke logcat. Null berarti breadcrumb hanya tersimpan di memori. */
    @Volatile
    var logger: ((BreadcrumbEntry) -> Unit)? = null

    fun add(area: String, message: String) {
        val entry = BreadcrumbEntry(
            wallClockMs = System.currentTimeMillis(),
            thread = Thread.currentThread().name,
            area = area,
            message = message
        )
        synchronized(entries) {
            entries.addLast(entry)
            while (entries.size > CAPACITY) {
                entries.removeFirst()
                dropped++
            }
        }
        logger?.invoke(entry)
    }

    /** Semua breadcrumb yang masih tersimpan, urut lama → baru. */
    fun snapshot(): List<BreadcrumbEntry> = synchronized(entries) { entries.toList() }

    /** Jumlah breadcrumb yang dibuang karena batas [CAPACITY]. */
    fun droppedCount(): Int = synchronized(entries) { dropped }

    fun clear() {
        synchronized(entries) {
            entries.clear()
            dropped = 0
        }
    }
}
