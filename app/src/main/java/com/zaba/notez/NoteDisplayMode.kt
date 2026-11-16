package com.zaba.notez

/**
 * Mode presentasi daftar catatan di Home.
 *
 * Nilai yang disimpan adalah [persistedValue] (string stabil), bukan ordinal
 * enum: ordinal berubah bila urutan enum diubah, dan preference pengguna lama
 * akan terbaca sebagai mode yang salah tanpa error.
 */
enum class NoteDisplayMode(val persistedValue: String) {
    LIST("list"),
    GRID("grid");

    companion object {
        /**
         * Nilai null atau tidak dikenal jatuh ke [LIST] tanpa crash, sesuai
         * kontrak "unknown preference fallback ke List".
         */
        fun fromPersisted(raw: String?): NoteDisplayMode =
            values().firstOrNull { it.persistedValue == raw } ?: LIST
    }
}
