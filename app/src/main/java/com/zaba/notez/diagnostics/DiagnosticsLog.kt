package com.zaba.notez.diagnostics

import android.util.Log

/**
 * Arahkan breadcrumb global ke logcat.
 *
 * Dipanggil dari Activity yang aktif. Breadcrumb sendiri tetap kelas Kotlin murni
 * supaya bisa diuji tanpa Android runtime.
 */
fun installBreadcrumbLogcat() {
    if (Breadcrumb.logger != null) return
    Breadcrumb.logger = { entry ->
        Log.d("NOTEZ_BREADCRUMB", "[${entry.area}] ${entry.thread}: ${entry.message}")
    }
}
