package com.zaba.notez.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guard untuk pemetaan mode playback ke tampilan (issue #22 dan #23).
 *
 * Murni JVM: hanya menyentuh fungsi murni dan resource ID, jadi tidak butuh
 * Robolectric maupun Android runtime. Bukti tampilan sesungguhnya tetap
 * memerlukan perangkat.
 *
 * Riwayat bug yang dijaga:
 * 1. Ikon repeat-one tidak ada (yang dipakai cuma angka "1"), sementara label
 *    "Sekali" dan perilaku playlist juga tidak sinkron. Guard:
 *    everyModeHasIconLabelAndContentDescription + cycleVisitsEveryModeAndReturnsToStart.
 * 2. Enum berganti nama (ONCE -> REPEAT_ONE) sementara music_library.json
 *    menyimpan nama lama. Tanpa pemetaan, mode tersimpan pengguna hilang
 *    diam-diam. Guard: legacyModeNamesSurviveRename.
 * 3. Judul dirender dengan "${title} - ${artist}", sehingga judul/artis kosong
 *    menghasilkan pemisah menggantung. Guard: trackLineDoesNotLeaveDanglingSeparator.
 */
class MusicPresentationTest {

    @Test
    fun everyModeHasIconLabelAndContentDescription() {
        for (mode in MusicPlaybackMode.values()) {
            assertNotEquals("mode $mode belum punya ikon", 0L, MusicPresentation.iconOf(mode).toLong())
            assertNotEquals(
                "mode $mode belum punya content description",
                0L,
                MusicPresentation.contentDescriptionResOf(mode).toLong()
            )
            assertTrue("label mode $mode kosong", mode.label().isNotBlank())
        }
    }

    @Test
    fun modeLabelsAreDistinct() {
        val labels = MusicPlaybackMode.values().map { it.label() }
        assertEquals("label mode tidak boleh kembar", labels.size, labels.toSet().size)
    }

    @Test
    fun cycleVisitsEveryModeAndReturnsToStart() {
        val modes = MusicPlaybackMode.values()
        for (start in modes) {
            var current = start
            val visited = mutableListOf(current)
            repeat(modes.size) {
                current = current.next()
                visited += current
            }
            assertEquals("siklus harus kembali ke titik awal dari $start", start, current)
            assertEquals(
                "siklus harus mengunjungi semua mode",
                modes.toSet(),
                visited.toSet()
            )
        }
    }

    @Test
    fun legacyModeNamesSurviveRename() {
        // Nama inilah yang ditulis NOTEZ 0.1.6 ke music_library.json.
        assertEquals(MusicPlaybackMode.REPEAT_ALL, MusicPlaybackMode.fromPersisted("LOOP_ALL"))
        assertEquals(MusicPlaybackMode.REPEAT_ONE, MusicPlaybackMode.fromPersisted("ONCE"))
        assertEquals(MusicPlaybackMode.SHUFFLE, MusicPlaybackMode.fromPersisted("SHUFFLE"))
    }

    @Test
    fun currentModeNamesAreStable() {
        for (mode in MusicPlaybackMode.values()) {
            assertEquals(mode, MusicPlaybackMode.fromPersisted(mode.name))
        }
    }

    @Test
    fun unknownOrMissingModeFallsBackToRepeatAll() {
        assertEquals(MusicPlaybackMode.REPEAT_ALL, MusicPlaybackMode.fromPersisted(null))
        assertEquals(MusicPlaybackMode.REPEAT_ALL, MusicPlaybackMode.fromPersisted(""))
        assertEquals(MusicPlaybackMode.REPEAT_ALL, MusicPlaybackMode.fromPersisted("BUKAN_MODE"))
    }

    @Test
    fun trackLineDoesNotLeaveDanglingSeparator() {
        assertEquals("Catatan", MusicPresentation.trackLine("Catatan", ""))
        assertEquals("Catatan", MusicPresentation.trackLine("Catatan", "   "))
        assertEquals("Catatan · Budi", MusicPresentation.trackLine(" Catatan ", " Budi "))
        assertTrue(
            "judul kosong tidak boleh menyisakan pemisah",
            !MusicPresentation.trackLine("", "").contains("·")
        )
    }

    @Test
    fun artistLineFallsBackToFileNameOnlyWhenArtistBlank() {
        assertEquals("Budi", MusicPresentation.artistLine("Budi", "lagu.mp3"))
        assertEquals("lagu.mp3", MusicPresentation.artistLine("", "lagu.mp3"))
        assertEquals("lagu.mp3", MusicPresentation.artistLine("   ", "lagu.mp3"))
    }
}
