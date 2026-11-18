package com.zaba.notez.diagnostics

data class PerfEvent(
    val name: String,
    val elapsedMs: Long,
    /** Thread pencatat: "main", "DefaultDispatcher-worker-1", dst. */
    val thread: String = "main",
    /** Detail tambahan: ukuran byte, hash, URL, pesan error. */
    val detail: String = ""
)
