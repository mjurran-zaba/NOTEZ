package com.zaba.notez

import android.content.Context

/**
 * Preference mode tampilan daftar Home.
 *
 * Sengaja memakai berkas SharedPreferences sendiri, terpisah dari berkas
 * "notez" milik [ThemePref]: mode tampilan bukan bagian dari tema, dan
 * mengganti tema tidak boleh menyentuh mode (atau sebaliknya).
 *
 * Preference ini tidak masuk JSON backup catatan; clear app data mereset ke
 * [NoteDisplayMode.LIST], restart normal tidak.
 */
object HomeDisplayPref {

    private const val PREF = "notez_home_display"
    private const val KEY = "display_mode"

    /** Selalu mengembalikan mode valid; nilai hilang/asing menjadi LIST. */
    fun get(context: Context): NoteDisplayMode {
        val raw = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY, null)
        return NoteDisplayMode.fromPersisted(raw)
    }

    fun set(context: Context, mode: NoteDisplayMode) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, mode.persistedValue)
            .apply()
    }
}
