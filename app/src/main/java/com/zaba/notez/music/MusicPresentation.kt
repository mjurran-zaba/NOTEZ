package com.zaba.notez.music

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.zaba.notez.R

/**
 * Pemetaan mode playback musik ke ikon monoline Lucide dan label aksesibilitas.
 */
object MusicPresentation {

    @DrawableRes
    fun iconOf(mode: MusicPlaybackMode): Int = when (mode) {
        MusicPlaybackMode.REPEAT_ALL -> R.drawable.ic_lucide_repeat
        MusicPlaybackMode.REPEAT_ONE -> R.drawable.ic_lucide_repeat_1
        MusicPlaybackMode.SHUFFLE -> R.drawable.ic_lucide_shuffle
    }

    @StringRes
    fun contentDescriptionResOf(mode: MusicPlaybackMode): Int = when (mode) {
        MusicPlaybackMode.REPEAT_ALL -> R.string.cd_music_mode_repeat_all
        MusicPlaybackMode.REPEAT_ONE -> R.string.cd_music_mode_repeat_one
        MusicPlaybackMode.SHUFFLE -> R.string.cd_music_mode_shuffle
    }

    fun trackLine(title: String, artist: String): String {
        val safeTitle = title.trim()
        val safeArtist = artist.trim()
        return if (safeArtist.isEmpty()) safeTitle else "$safeTitle · $safeArtist"
    }

    fun artistLine(artist: String, displayName: String): String {
        val safeArtist = artist.trim()
        return if (safeArtist.isNotEmpty()) safeArtist else displayName.trim()
    }
}
