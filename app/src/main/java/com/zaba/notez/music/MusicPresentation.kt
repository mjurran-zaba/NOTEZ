package com.zaba.notez.music

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.zaba.notez.R

/**
 * Satu-satunya tempat mode playback dipetakan ke tampilan.
 *
 * Controller tidak boleh punya `when (mode)` sendiri untuk ikon, label, atau
 * content description. Dulu ikon dipetakan di `updateUi()` sementara perilaku
 * dipetakan di `syncPlaylist()`, dan keduanya sempat berbeda arti (issue #22
 * dan #23): ikon "repeat one" tidak ada, label "Sekali" bertabrakan dengan
 * perilaku yang tetap bisa lompat lagu.
 */
object MusicPresentation {

    @DrawableRes
    fun iconOf(mode: MusicPlaybackMode): Int = when (mode) {
        MusicPlaybackMode.REPEAT_ALL -> R.drawable.ic_music_loop_all
        MusicPlaybackMode.REPEAT_ONE -> R.drawable.ic_music_once
        MusicPlaybackMode.SHUFFLE -> R.drawable.ic_music_shuffle
    }

    @StringRes
    fun contentDescriptionResOf(mode: MusicPlaybackMode): Int = when (mode) {
        MusicPlaybackMode.REPEAT_ALL -> R.string.cd_music_mode_repeat_all
        MusicPlaybackMode.REPEAT_ONE -> R.string.cd_music_mode_repeat_one
        MusicPlaybackMode.SHUFFLE -> R.string.cd_music_mode_shuffle
    }

    /** "Judul" atau "Judul · Artis", tanpa pemisah menggantung saat artis kosong. */
    fun trackLine(title: String, artist: String): String {
        val safeTitle = title.trim()
        val safeArtist = artist.trim()
        return if (safeArtist.isEmpty()) safeTitle else "$safeTitle · $safeArtist"
    }

    /** Baris kedua now-playing: nama artis, atau nama berkas saat tag artis kosong. */
    fun artistLine(artist: String, displayName: String): String {
        val safeArtist = artist.trim()
        return if (safeArtist.isNotEmpty()) safeArtist else displayName.trim()
    }
}
