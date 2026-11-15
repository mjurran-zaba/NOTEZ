# RFC — Perbaikan Issue Terbuka NOTEZ (#19–#26)

**Status:** `APPROVED (keputusan §9, 2026-11-15) — EKSEKUSI MENUNGGU PERINTAH "GASS", BELUM ADA KODE PRODUKSI YANG DIUBAH`
**Tanggal:** 2026-11-15
**Branch kerja:** `freebuff/f3a36116d74294a9536e1919`
**Base commit:** `e57c146` (*Change greeting from 'Hello World' to 'Goodbye World'*)
**Target versi:** `0.1.7` (versionCode `8`)
**Penulis:** Buffy (agent), atas permintaan Zaqi

---

## 0. Ringkasan

Delapan issue masih terbuka di GitHub. Hasil pembacaan kode menunjukkan:

- **3 issue valid dan penyebabnya sudah ketemu di kode** (#19, #20, #23).
- **2 issue valid tapi akar masalahnya satu kelas** (#22 dan #23 → mode playback tidak punya satu sumber kebenaran).
- **1 issue kemungkinan besar sudah selesai** (#21) → kandidat ditutup.
- **1 issue keputusan produk, bukan bug** (#25) → perlu ADR, bukan patch.
- **1 issue proses** (#26) → guard, bukan perbaikan fitur.

RFC ini memetakan **file yang pasti disentuh**, **konsep perubahan**, **acceptance criteria**, dan **batas verifikasi yang bisa gw klaim** di lingkungan ini.

---

## 1. Ruang Lingkup

### Termasuk
| Batch | Issue | Jenis perubahan |
|---|---|---|
| A | #19 | Fitur baru: Buka File di menu FILE |
| B | #22 + #23 | Bug fix kelas: glyph / label / behavior mode playback |
| C | #20 | Bug fix: duplikasi judul di panel Music |
| D | #24 | Perbaikan hierarchy & aksesibilitas panel Music |
| E | #21 | Verifikasi lalu tutup (tanpa ubah kode) |
| F | #26 | Guard proses + guard test |

### Non-goals (sengaja TIDAK dikerjakan)
- **Tidak** memindahkan panel Music ke content / activity terpisah — itu #25, keputusan produk, di luar RFC ini (lihat §12).
- **Tidak** menambah intent-filter `ACTION_VIEW` di manifest **kecuali** disetujui (§9 pertanyaan 2), karena itu memperluas permukaan `exported` aplikasi.
- **Tidak** menyentuh Room schema, backup format, atau `NoteFileTargetStore` API.
- **Tidak** upgrade dependency apa pun kecuali `junit` untuk test baru (§9 pertanyaan 3).
- **Tidak** refactor `MusicPlaybackService`, `PerfTracker`, atau `MarkdownPreviewRenderer`.

---

## 2. Batas Verifikasi (jujur, sebelum mulai)

| Layer | Status di sandbox ini |
|---|---|
| Source inspection | ✅ Sudah dilakukan, semua temuan di RFC ini berbasis `file:line` |
| Compile (`assembleDebug`) | ❌ **Tidak bisa** — tidak ada JDK, tidak ada Android SDK, `ANDROID_HOME` unset |
| Unit test | ❌ **Belum ada infrastruktur test sama sekali** — `app/src` hanya `main/`, tanpa `testImplementation`/`junit` di `app/build.gradle.kts` |
| Device / emulator | ❌ Tidak ada `adb`, tidak ada emulator |

**Artinya:** setelah RFC ini di-approve, bila toolchain tidak dipasang, hasil kerja maksimal berstatus **IMPLEMENTED** — bukan **LOCALLY VERIFIED**. Klaim "sudah jalan" tidak akan gw buat.

Opsi (keputusan di §9 pertanyaan 1):
- ~~**(a)** Pasang JDK 17 + Android SDK (platform 34, build-tools 34) → ±2.3 GB unduhan.~~ **TIDAK DIPILIH.**
- **(b) ✅ DIPILIH (2026-11-15)** — build & unit test **hanya lewat GitHub Actions**. Konsekuensi yang diterima: tidak ada gate lokal, status maksimal dari sisi agent adalah **IMPLEMENTED** sampai workflow CI dijalankan dan hijau. Tidak ada JDK/SDK yang dipasang di sandbox.

---

## 3. Peta File

### 3.1 PASTI disentuh

| File | Batch | Perubahan |
|---|---|---|
| `app/src/main/java/com/zaba/notez/music/MusicDrawerController.kt` | B, C, D | Ambil glyph/label/CD dari satu sumber; hapus duplikasi judul; rapikan visibility & aksesibilitas |
| `app/src/main/java/com/zaba/notez/music/MusicLibraryStore.kt` | B | Semantik `MusicPlaybackMode` + migrasi nama mode lama dari JSON |
| `app/src/main/java/com/zaba/notez/music/MusicPresentation.kt` | B, C | **FILE BARU** — fungsi murni: label, ikon, contentDescription, formatter judul |
| `app/src/main/res/layout/include_music_drawer.xml` | B, D | `‹`/`›` → `ImageButton`; id untuk label "Koleksi musik"; padding konsisten; heading aksesibilitas |
| `app/src/main/res/drawable/ic_music_once.xml` | B | Ganti glyph "1" jadi ikon repeat-one yang benar |
| `app/src/main/res/drawable/ic_music_prev.xml` | B | **FILE BARU** — ikon skip-previous |
| `app/src/main/res/drawable/ic_music_next.xml` | B | **FILE BARU** — ikon skip-next |
| `app/src/main/res/values/strings.xml` | B, D | `cd_*` untuk prev/next/mode, label seksi |
| `app/src/main/res/layout/activity_editor.xml` | A | Item menu `menu_file_open` |
| `app/src/main/java/com/zaba/notez/EditorActivity.kt` | A | Launcher `OpenDocument`, baca URI, buat catatan, set target file |
| `app/build.gradle.kts` | F | `testImplementation("junit:junit:4.13.2")` — ✅ disetujui (lihat §9-3) |
| `app/src/test/java/com/zaba/notez/music/MusicPresentationTest.kt` | F | **FILE BARU** — guard JVM murni (label/ikon/CD/siklus mode/migrasi/formatter) |
| `app/src/test/java/com/zaba/notez/music/MusicChromeGuardTest.kt` | F | **FILE BARU** — guard leksikal Kotlin atas `include_music_drawer.xml` & drawable |
| `docs/RFC_...` (file ini) | — | Diubah ke `APPROVED` setelah ACC |

### 3.2 MUNGKIN disentuh (tergantung keputusan)

| File | Kapan | Alasan |
|---|---|---|
| `app/src/main/AndroidManifest.xml` | Jika §9-2 = ya | `<intent-filter>` `ACTION_VIEW` untuk `.md` |
| `app/src/main/java/com/zaba/notez/MainActivity.kt` | Jika §9-2 = ya, atau #25 = pindah ke content | Titik masuk buka-file eksternal / lokasi panel |
| `.github/workflows/build.yml` | F — ✅ disetujui (§9-2 lanjutan) | Tambah `testDebugUnitTest` ke langkah build agar guard benar-benar dieksekusi CI |
| `docs/ADR_MUSIC_LAYOUT_2026_11_15.md` | #25 | **FILE BARU** — ADR keputusan drawer vs content |
| `docs/UAT_*_2026_11_15.md` | Setelah implementasi | Bukti verifikasi sesuai konvensi repo |

### 3.3 TIDAK disentuh
`AppDatabase.kt`, `Note.kt`, `NoteDao.kt`, `BackupHelper.kt`, `MarkdownPreviewRenderer.kt`, `ThemePref.kt`, `UiStyle.kt`, `MusicPlaybackService.kt`, `NoteFileTargetStore.kt`, `settings.gradle.kts`, `gradle.properties`.

---

## 4. Batch A — Issue #19 `[P0]` Menu FILE belum mendukung Buka File

### 4.1 Bukti
- `activity_editor.xml` hanya punya `menu_file_save` (baris 149) dan `menu_file_save_as` (baris 163). Tidak ada item "Buka".
- `EditorActivity.kt:106-113` hanya menyambung dua listener itu.
- `AndroidManifest.xml` tidak punya `ACTION_VIEW` / `ACTION_SEND` filter; `MainActivity` hanya `MAIN`/`LAUNCHER`.
- Sudah ada `saveAsLauncher` (`CreateDocument("text/markdown")`, `EditorActivity.kt:76`) dan `NoteFileTargetStore` → infrastruktur file **setengah jadi**: bisa tulis, belum bisa baca.

### 4.2 Konsep
Buka file = **impor jadi catatan baru yang langsung terikat ke file sumber**, memakai `NoteFileTargetStore.setTargetUri(noteId, uri)` yang sudah ada. Jadi setelah dibuka, "Simpan file" langsung menulis balik ke file yang sama — konsisten dengan perilaku Save yang sekarang.

Alur:
```
Menu FILE → "Buka file..." → OpenDocument picker
   → baca stream (IO, batas 5 MiB)
   → deteksi judul dari nama file
   → flush save catatan yang sedang terbuka
   → dao.upsert(Note(title, content, now)) → noteId baru
   → NoteFileTargetStore.setTargetUri(noteId, uri) + persistable read/write
   → relayout editor ke catatan baru, mode VIEW
```

### 4.3 Langkah edit

**(1) `activity_editor.xml`** — sisipkan item baru **sebelum** `menu_file_save`:

```xml
<TextView
    android:id="@+id/menu_file_open"
    android:layout_width="match_parent"
    android:layout_height="48dp"
    android:background="?attr/selectableItemBackground"
    android:clickable="true"
    android:focusable="true"
    android:gravity="center_vertical"
    android:paddingStart="20dp"
    android:paddingEnd="20dp"
    android:text="@string/menu_file_open"
    android:textColor="?attr/colorOnSurface"
    android:textSize="15sp" />
```

**(2) `strings.xml`** — tambah:
```xml
<string name="menu_file_open">Buka file...</string>
<string name="open_file_failed">Gagal membuka file</string>
<string name="open_file_too_large">File terlalu besar (maks 5 MB)</string>
<string name="open_file_done">File dibuka sebagai catatan baru</string>
```

**(3) `EditorActivity.kt`** — tambah launcher + handler:

```kotlin
private val openFileLauncher =
    registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) importFromUri(uri)
    }

private fun handleOpenFile() {
    openFileLauncher.launch(arrayOf("text/*", "application/octet-stream"))
}

private fun importFromUri(uri: Uri) {
    // 1. Jangan kehilangan edit yang belum tersimpan di catatan sekarang.
    saveBlocking(currentTitle, currentContent)
    lifecycleScope.launch(Dispatchers.IO) {
        val imported = runCatching { readMarkdown(uri) }.getOrNull()
        if (imported == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@EditorActivity, R.string.open_file_failed, Toast.LENGTH_LONG).show()
            }
            return@launch
        }
        if (imported.content.length > MAX_IMPORT_BYTES) { /* toast too large */ return@launch }
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        val newId = dao.upsert(Note(title = imported.title, content = imported.content,
                                   updatedAt = System.currentTimeMillis()))
        NoteFileTargetStore.setTargetUri(this@EditorActivity, newId, uri)
        withContext(Dispatchers.Main) { bindImportedNote(newId, imported) }
    }
}
```

`readMarkdown(uri)` — helper baru, semua exception ditangkap:
- `displayName` via `OpenableColumns.DISPLAY_NAME` (pola sama dengan `MusicLibraryStore.readMetadata`).
- Judul = nama file tanpa ekstensi; kalau kosong → `R.string.untitled_note`.
- Isi = `openInputStream(uri)` → `readBytes()` dengan **batas keras** `MAX_IMPORT_BYTES = 5 * 1024 * 1024` untuk mencegah OOM (AGENTS §13, RAM perangkat user bukan RAM sandbox).
- Decode UTF-8; bila `MalformedInputException` → fallback `ISO_8859_1` (lebih baik tampil rusak daripada gagal total).

`bindImportedNote(...)` — set `noteId`, `currentTitle`, `currentContent`, `titleEdit`/`bodyEdit` teks, `loaded=true`, `applyMode(false)`, toast sukses.

**(4) Wiring** di `onCreate`, sejajar listener lain:
```kotlin
findViewById<View>(R.id.menu_file_open)?.setOnClickListener {
    drawer.closeDrawer(GravityCompat.START)
    handleOpenFile()
}
```

### 4.4 Acceptance criteria
- [ ] Menu FILE punya tiga item: Buka file, Simpan file, Simpan sebagai.
- [ ] Memilih `.md` dari Downloads membuka isinya sebagai catatan baru di mode VIEW.
- [ ] Setelah dibuka, "Simpan file" menulis balik ke file yang sama **tanpa** dialog.
- [ ] Catatan lama tetap utuh; edit yang belum tersimpan ter-flush lebih dulu.
- [ ] File > 5 MB ditolak dengan pesan jelas, app tidak crash/OOM.
- [ ] URI yang provider-nya tidak mendukung persistable permission tidak membuat crash (ditangkap `runCatching`).
- [ ] Rotasi layar setelah impor tidak kehilangan `noteId`.

### 4.5 Risiko & rollback
| Risiko | Mitigasi |
|---|---|
| OOM saat file besar | Batas 5 MB + baca di IO |
| `noteId` baru tidak konsisten dengan autosave yang jalan | `saveBlocking` sebelum ganti `noteId`; generation counter yang sudah ada tetap jadi pengaman |
| Provider aneh (Google Drive, SAF) | Semua SAF call dibungkus `runCatching` |
| Rollback | Batch A murni aditif: hapus item menu + launcher → kode kembali ke kondisi semula |

---

## 5. Batch B — Issue #22 + #23 glyph, label, behavior playback

### 5.1 Bukti
1. `ic_music_once.xml` path-nya hanya bentuk angka **"1"** (`M10,5h4v14h-3V8l-2,1V6z`) — bukan ikon repeat-one.
2. `include_music_drawer.xml`: tombol prev/next adalah `TextView` dengan karakter teks `‹` dan `›` (30sp bold), **tanpa `contentDescription`**. Ini persis yang dilarang AGENTS §12 (chrome fungsional harus glyph stabil, bukan karakter font).
3. `MusicDrawerController.kt:211-232` — mode `ONCE` memaksa playlist jadi **satu item** + `REPEAT_MODE_OFF`, tapi `playNext()`/`playPrevious()` untuk `ONCE` memakai `wrapIndex()` yang justru **pindah ke lagu lain**.
4. `MusicDrawerController.kt:276-283` — ikon & label dipetakan di `updateUi()`, sedangkan **perilaku** dipetakan terpisah di `syncPlaylist()`. Dua sumber kebenaran → bisa divergen (dan sudah divergen).
5. `mode.contentDescription` di-hardcode `"Music mode"` (layout), tidak ikut state.

### 5.2 Konsep
**Satu sumber kebenaran.** Semua yang bergantung pada mode (label, ikon, contentDescription, dan penerapan ke `Player`) pindah ke `MusicPlaybackMode`. UI hanya membaca hasilnya; tidak ada `when(mode)` lagi di controller.

**Perubahan kontrak UX yang perlu ACC:** mode ketiga sekarang bernama `ONCE` dengan label "Sekali" — ambigu. Issue #22 sendiri menyebut "Repeat / Repeat One / Shuffle", jadi gw usulkan semantik standar pemutar musik:

| Mode baru | Label | `Player` | Perilaku next/prev |
|---|---|---|---|
| `REPEAT_ALL` | Ulangi semua | `REPEAT_MODE_ALL`, shuffle off, playlist penuh | pindah track, berputar |
| `REPEAT_ONE` | Ulangi satu | `REPEAT_MODE_ONE`, shuffle off, playlist penuh | pindah track (manual), otomatis mengulang track yang sama |
| `SHUFFLE` | Acak | `REPEAT_MODE_ALL`, shuffle on, playlist penuh | pindah track acak |

Ini **mengubah perilaku** `ONCE` (yang sekarang berhenti setelah satu lagu) menjadi **mengulang lagu yang sama**. Konsekuensi: user yang sudah terbiasa "Sekali" akan merasakan beda. Alternatif konservatif ada di §9 pertanyaan 4.

### 5.3 Langkah edit

**(1) `MusicLibraryStore.kt`** — perluas enum, jaga kompatibilitas JSON:

```kotlin
enum class MusicPlaybackMode {
    REPEAT_ALL, REPEAT_ONE, SHUFFLE;

    fun next(): MusicPlaybackMode = when (this) {
        REPEAT_ALL -> REPEAT_ONE
        REPEAT_ONE -> SHUFFLE
        SHUFFLE -> REPEAT_ALL
    }

    /** Label yang dilihat user. Satu-satunya sumber teks mode. */
    fun label(): String = when (this) {
        REPEAT_ALL -> "Ulangi semua"
        REPEAT_ONE -> "Ulangi satu"
        SHUFFLE -> "Acak"
    }

    /** Penerapan ke player. Satu-satunya sumber perilaku mode. */
    fun applyTo(player: Player, items: List<MediaItem>, startIndex: Int) {
        when (this) {
            REPEAT_ALL -> { player.shuffleModeEnabled = false; player.repeatMode = Player.REPEAT_MODE_ALL }
            REPEAT_ONE -> { player.shuffleModeEnabled = false; player.repeatMode = Player.REPEAT_MODE_ONE }
            SHUFFLE   -> { player.shuffleModeEnabled = true;  player.repeatMode = Player.REPEAT_MODE_ALL }
        }
        player.setMediaItems(items, startIndex, C.TIME_UNSET)
    }

    companion object {
        /** Migrasi nama mode lama: "ONCE" (v0.1.6) → REPEAT_ONE. */
        fun fromPersisted(raw: String?): MusicPlaybackMode = when (raw) {
            "ONCE" -> REPEAT_ONE          // legacy v0.1.6
            "LOOP_ALL" -> REPEAT_ALL      // legacy v0.1.6
            else -> runCatching { valueOf(raw.orEmpty()) }.getOrDefault(REPEAT_ALL)
        }
    }
}
```

`read()` di `MusicLibraryStore` diubah agar memakai `fromPersisted(root.optString("playbackMode"))` — **tanpa** mengubah `VERSION` (nama field & struktur JSON tetap, hanya nilainya yang dipetakan). Ini mencegah library user yang sudah ada jatuh ke default karena `valueOf("LOOP_ALL")`/`valueOf("ONCE")` gagal.

**(2) `MusicPresentation.kt`** (baru) — pemetaan tampilan, terpisah supaya bisa diuji tanpa Android:

```kotlin
object MusicPresentation {
    @DrawableRes fun iconOf(mode: MusicPlaybackMode): Int
    fun contentDescriptionOf(mode: MusicPlaybackMode): String   // "Mode Ulangi satu. Ketuk untuk ganti."
    fun nowPlayingLine(title: String, artist: String): String   // "Title" atau "Title · Artist"
    fun subtitleLine(title: String, artist: String): String
    fun libraryLine(title: String, artist: String): String      // "Title" + artist di baris kedua
}
```

**(3) `MusicDrawerController.kt`**
- `private val previous: ImageButton`, `next: ImageButton` (tipe berubah).
- `syncPlaylist()` menyusut jadi:
  ```kotlin
  val items = snapshot.tracks.map { it.toMediaItem() }
  val index = snapshot.tracks.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: 0
  if (items.isEmpty()) { c.clearMediaItems(); return }
  snapshot.playbackMode.applyTo(c, items, index)
  c.prepare()
  if (keepPlaying) c.play()
  ```
- `playNext()`/`playPrevious()`: hapus cabang `when (snapshot.playbackMode)`; pakai `hasNextMediaItem()`/`seekToNextMediaItem()` dengan fallback `seekTo(0)` / `seekTo(lastIndex)` untuk semua mode. Dengan `REPEAT_MODE_ONE`, `seekToNextMediaItem()` tetap pindah track secara manual — perilaku yang user harapkan.
- `updateUi()`: ambil ikon & CD & label dari `MusicPresentation`, hapus `when` lokal.

**(4) `include_music_drawer.xml`** — ganti dua `TextView` jadi:

```xml
<ImageButton
    android:id="@+id/music_previous"
    android:layout_width="52dp"
    android:layout_height="44dp"
    android:background="?attr/selectableItemBackgroundBorderless"
    android:contentDescription="@string/cd_music_previous"
    android:src="@drawable/ic_music_prev"
    android:tint="?attr/colorPrimary" />
```
(idem untuk `music_next`, `ic_music_next`.) Ukuran tetap 52×44dp agar target sentuh ≥ 44dp dan layout tidak bergeser (mitigasi #26).

**(5) Drawable**
- `ic_music_prev.xml` / `ic_music_next.xml`: path Material `skip_previous` / `skip_next`.
- `ic_music_once.xml` → rename isi jadi repeat-one (loop + angka 1 kecil), tetap `fillColor="#FFFFFFFF"` karena di-tint.

**(6) `strings.xml`**
```xml
<string name="cd_music_previous">Lagu sebelumnya</string>
<string name="cd_music_next">Lagu berikutnya</string>
<string name="cd_music_play">Putar musik</string>
<string name="cd_music_pause">Jeda musik</string>
```

### 5.4 Acceptance criteria
- [ ] Tiga mode tampil dengan ikon yang tidak ambigu: loop, repeat-one, shuffle.
- [ ] Label di bawah ikon selalu sama dengan perilaku player (verifikasi manual: putar lagu, habis → perilaku sesuai label).
- [ ] Tidak ada gabungan ikon/label/perilaku yang bertentangan (guard test di §8).
- [ ] Pengguna lama dengan `music_library.json` berisi `"LOOP_ALL"`/`"ONCE"` tidak kehilangan mode tersimpan.
- [ ] TalkBack membacakan ketiga tombol dengan nama yang benar; mode dibacakan ikut state.
- [ ] Tidak ada karakter teks `‹`/`›` di chrome fungsional.

### 5.5 Risiko & rollback
| Risiko | Mitigasi |
|---|---|
| Migrasi mode gagal → library user reset | `fromPersisted()` menangani dua nama lama + fallback aman; perubahan perilaku JSON diuji manual dengan file contoh |
| Perubahan UX "Sekali" → "Ulangi satu" | Butuh ACC (§9-4); alternatif konservatif tersedia |
| `REPEAT_MODE_ONE` + `seekToNextMediaItem()` | Perlu uji di device: pastikan next manual tidak memantul balik ke lagu yang sama |
| Rollback | Seluruh batch adalah satu commit; revert mengembalikan enum + layout |

---

## 6. Batch C — Issue #20 judul tampil berulang

### 6.1 Bukti
`MusicDrawerController.updateUi()` (baris 238-244) merender judul di **tiga** tempat:
1. `subtitle` = `"title - artist"`
2. `nowTitle` + `nowArtist`
3. `renderLibrary()` tiap item = `"title - artist"`

Ketiganya aktif bersamaan saat drawer expanded → judul yang sama tampil berulang.

### 6.2 Konsep
Bukan "hapus satu baris", tapi **hapus duplikasi sumber definisi**:
- `subtitle` = ringkasan **hanya saat collapsed** (satu baris, menggantikan now-playing yang tak terlihat). Saat expanded → `GONE`, karena now-playing sudah ada.
- Blok now-playing = satu-satunya tempat judul besar.
- Item library = judul sebagai baris utama, artis sebagai baris kedua (bukan digabung `" - "`), dan **tidak** mengulang judul lagu yang sedang diputar di subtitle.
- Semua format teks lewat `MusicPresentation`, jadi tidak ada lagi `"${a} - ${b}"` yang tersebar.

### 6.3 Langkah edit
- `MusicPresentation.nowPlayingLine/subtitleLine/libraryLine` dibuat sebagai fungsi murni.
- `updateUi()`:
  ```kotlin
  subtitle.text = if (!expanded) currentTrack?.let { MusicPresentation.subtitleLine(it.title, it.artist) }.orEmpty() else ""
  subtitle.isVisible = !expanded && subtitle.text.isNotBlank()
  nowTitle.text = currentTrack?.title ?: "Belum ada musik lokal"
  ```
- `renderLibrary()`: `item.text` pakai `libraryLine`, plus `contentDescription` gabungan.

### 6.4 Acceptance criteria
- [ ] Saat drawer collapsed: judul muncul **1×** (di subtitle).
- [ ] Saat drawer expanded: judul muncul **1×** di now-playing; subtitle hilang.
- [ ] Tiap item library menampilkan judul dan artis sekali, tanpa tanda hubung ganda.
- [ ] Track tanpa artist tidak menampilkan `" - "` menggantung.

---

## 7. Batch D — Issue #24 hierarchy panel Music

### 7.1 Bukti
- `music_header_subtitle` berada **di luar** `music_submenu` → info lagu tetap tampil walau panel tertutup.
- Label "Koleksi musik" adalah `TextView` **tanpa `id`** → tidak bisa disembunyikan saat koleksi kosong; muncul bersamaan dengan `music_empty` (dua pesan kosong sekaligus).
- Inden tidak konsisten: header 18dp, subtitle 20dp, isi 8dp; ada `LinearLayout` pembungkus tanpa id dengan padding `4dp/6dp`.
- Seksi "Music" dan "Koleksi musik" tidak ditandai sebagai heading aksesibilitas.

### 7.2 Konsep
Struktur target (satu arah, satu inden):

```
Music                    ← heading, 18dp, 54dp tinggi
  └ subtitle             ← ringkasan collapsed (lihat Batch C)
  └ [controls]           ← prev / play-pause / next, 44dp target
  └ [mode row]           ← ikon + label mode
  └ Tambah musik
  └ Koleksi musik        ← heading, hanya tampil bila ada isi
     └ list item
  └ Belum ada musik lokal ← hanya bila kosong
```

### 7.3 Langkah edit
- Beri `@+id/music_library_label`, set `isVisible = tracks.isNotEmpty()`.
- Samakan inden isi ke 18dp (header & isi sejajar); hapus padding ganda pada pembungkus.
- `ViewCompat.setAccessibilityHeading(header, true)` dan sama untuk `music_library_label` (dukung minSdk 26).
- `music_empty` di-set `isVisible = tracks.isEmpty()` (sudah ada di kode, tapi belum mencakup label).

### 7.4 Acceptance criteria
- [ ] Panel kosong tidak menampilkan "Koleksi musik" sama sekali.
- [ ] Panel terisi menampilkan heading "Koleksi musik" + daftar, tanpa pesan "Belum ada musik lokal".
- [ ] Semua elemen isi sejajar pada inden yang sama.
- [ ] TalkBack bisa navigasi per heading.

---

## 8. Batch E + F — Tutup #21, guard #26

### 8.1 #21 — kandidat ditutup (tanpa ubah kode)
`MusicLibraryStore.readMetadata()` (baris 179-212) sudah memisahkan:
- `displayName` = nama file dari `OpenableColumns.DISPLAY_NAME`
- `title` = tag `METADATA_KEY_TITLE`, fallback nama file tanpa ekstensi
- `artist` = tag `METADATA_KEY_ARTIST`, boleh kosong

Artinya metadata sudah dipisahkan dari nama file. **Belum LOCALLY VERIFIED** karena butuh file audio nyata + build. Rencana: verifikasi manual di device dengan satu file bertag dan satu file tanpa tag; kalau cocok → tutup issue dengan komentar bukti.

### 8.2 #26 — guard agar layout tidak rusak lagi
Issue ini soal proses, jadi guard-nya dua lapis:

1. **Guard test (Batch F).** Karena repo belum punya test sama sekali:
   - `app/build.gradle.kts`: `testImplementation("junit:junit:4.13.2")` — sama persis dengan yang dipakai ZCODE; tanpa Android runtime, tanpa Robolectric.
   - **`MusicPresentationTest.kt` (JVM murni):**
     - setiap konstanta `MusicPlaybackMode` punya ikon ≠ 0, label tidak blank, CD tidak blank (gagal kalau nanti ada mode baru tanpa tampilan);
     - `next()` membentuk siklus penuh dan kembali ke titik awal dalam 3 langkah;
     - `fromPersisted("ONCE") == REPEAT_ONE` dan `fromPersisted("LOOP_ALL") == REPEAT_ALL` (guard migrasi);
     - `nowPlayingLine`/`subtitleLine` tidak menghasilkan `" - "` menggantung saat artist kosong (guard #20).
   - **`MusicChromeGuardTest.kt` (leksikal, Kotlin):** membaca `app/src/main/res/layout/include_music_drawer.xml` dari `workingDir` modul (Gradle menjalankan test dari direktori `app/`) dan meng-assert:
     - `music_previous` & `music_next` adalah `ImageButton` dengan `android:contentDescription` dan `android:src`, **bukan** `TextView`;
     - tidak ada `android:text="‹"` atau `android:text="›"` di layout mana pun;
     - setiap drawable mode yang dirujuk `MusicPresentation` benar-benar ada sebagai file di `res/drawable/`;
     - test **gagal keras** kalau file XML tidak ditemukan (jangan lolos diam-diam karena path salah).
   - **Catatan implementasi dari ZCODE:** penghapus komentar untuk guard leksikal **wajib** memakai pemindaian state machine yang menghormati literal string. Penghapus naif `/* */` lalu `//` pernah melahap 19.949 karakter kode asli karena `text/*` di dalam komentar. NOTEZ sendiri akan memuat `arrayOf("text/*", ...)` dari Batch A — jadi jebakan ini nyata di repo ini.
   - Mutation protocol: balikkan satu mapping (mis. hapus `REPEAT_ONE -> ...`) → test harus **merah**; ubah `<ImageButton` jadi `<TextView` di layout → lexical guard harus **merah**; kembalikan → **hijau**.
2. **Guard proses.** Checklist layout di `docs/` untuk setiap perubahan UI: portrait & landscape, target sentuh ≥ 44dp, tidak mengubah tinggi komponen tetangga, TalkBack. Ditambahkan ke template UAT agar #26 tidak terulang.

Registrasi CI — **sudah diverifikasi, dan ada masalah yang harus diperbaiki:** `.github/workflows/build.yml` NOTEZ saat ini hanya menjalankan `gradle assembleDebug`. **Tidak ada `testDebugUnitTest`**, sehingga test baru akan menjadi kode mati. Perbaikan (✅ disetujui, §9-6): ubah langkah build menjadi `gradle testDebugUnitTest assembleDebug --console=plain`.

Sebagai pembanding, di ZCODE hanya `test_zcode_fase0.py`, `test_zcode_package_runtime.py`, `tools/check.sh`, dan `gradle testDebugUnitTest` yang benar-benar dipanggil CI; empat file guard besar di sana (`test_zcode_kotlin_guards.py` 5.497 baris, `test_zcode_security_guards.py`, `test_zcode_license_guards.py`, `test_v1021_commit_and_save_guards.py`) tidak terdaftar di workflow mana pun. Pola ZCODE yang diadopsi adalah **strukturnya**, bukan status registrasinya.

---

## 9. Keputusan (sudah dijawab Zaqi, 2026-11-15)

| # | Pertanyaan | Keputusan Zaqi (2026-11-15) |
|---|---|---|
| 1 | Build di sandbox atau di CI? | **CI GitHub Actions saja.** Tidak ada JDK/SDK di sandbox. |
| 2 | Intent-filter `ACTION_VIEW` untuk `.md`? | **Tidak.** Menu FILE saja. |
| 3 | Tambah `junit:junit:4.13.2` sebagai `testImplementation`? | **Boleh.** |
| 4 | Semantik mode ketiga? | **Ikuti ikon** → `REPEAT_ONE` / "Ulangi satu" (loop, repeat-one, shuffle). |
| 5 | Guard leksikal ditulis Kotlin atau Python ala ZCODE? | **Kotlin (JVM test).** Satu bahasa, satu perintah Gradle, tanpa toolchain Python baru. |
| 6 | Tambah `testDebugUnitTest` ke `.github/workflows/build.yml`? | **Boleh.** Tanpa ini, test baru jadi kode mati (AGENTS §6). |
| 7 | Perbaiki 4 guard ZCODE yang tidak terdaftar CI? | **Ditunda.** Fokus NOTEZ. |

---

## 10. Urutan eksekusi & dependency

```
Batch A (#19)  ── independen, bisa duluan
Batch B (#22+#23) ─┐
Batch C (#20)  ────┤ B harus lebih dulu: C memakai MusicPresentation dari B
Batch D (#24)  ────┘ D butuh C (visibilitas subtitle) dan B (ikon baru)
Batch E (#21)  ── verifikasi manual, setelah build tersedia
Batch F (#26)  ── setelah B & C, karena guard menguji keduanya
```

Satu batch = satu commit koheren (AGENTS §16). Commit message mengikuti gaya repo: `fix(music): ...`, `feat(editor): ...`, `test(music): ...`.

---

## 11. Rencana verifikasi

### Gate otomatis (jika §9-1 = pasang toolchain)
```
./gradlew :app:assembleDebug          # kompilasi
./gradlew :app:testDebugUnitTest      # guard unit test
./gradlew :app:lintDebug              # regresi resource/aksesibilitas dasar
```
Setiap perintah dijalankan dengan exit status diperiksa, bukan dipotong `head`.

### Manual di device (harus Zaqi, atau emulator bila tersedia)
| Batch | Skenario |
|---|---|
| A | Buka `catatan.md` dari Downloads; edit; Simpan file; cek file asli berubah; buka file 10 MB → ditolak |
| B | Putar lagu di tiap mode sampai habis; cek perilaku = label; TalkBack pada 3 tombol |
| C | Buka panel collapsed vs expanded; hitung kemunculan judul |
| D | Panel kosong vs terisi; bandingkan inden |
| E | File audio bertag ID3 vs tanpa tag |
| Semua | Portrait + landscape, ukuran layar kecil |

### Status yang akan gw klaim
Hanya setelah gate lolos: **LOCALLY VERIFIED (sandbox, emulator/device tidak tersedia)** — dan kalau device tidak ada, itu gw tulis eksplisit, bukan disembunyikan.

---

## 12. #25 — di luar RFC ini

`#25` menanyakan posisi Music: drawer atau content. Itu **keputusan produk**, dan pilihannya mengubah lifecycle:

- **Sekarang:** `MusicDrawerController` dibuat per-Activity (dua instance: Main + Editor), masing-masing memasang `handler.post(tick)` 1 detik dan terus menulis ulang TextView walaupun drawer tertutup dan tidak ada lagu.
- **Jika pindah ke content:** satu instance, tapi Music hilang dari EditorActivity.
- **Jika activity terpisah:** satu `MediaController` dengan owner jelas, tapi menambah satu layar.

Rekomendasi gw: **tetap drawer**, tapi perbaiki ownership tick (berhenti saat drawer tertutup/tidak terlihat) sebagai perbaikan terpisah, dan tulis ADR berisi keputusan itu. RFC terpisah, tidak dicampur ke batch ini.

---

## 13. Checklist sebelum implementasi

- [x] Tujuh pertanyaan §9 dijawab (2026-11-15).
- [x] RFC ini diubah statusnya jadi `APPROVED`.
- [ ] Perintah **GASS** dari Zaqi diterima.
- [ ] `git status` bersih, branch dipastikan benar.
- [ ] `.github/workflows/build.yml` dibaca (untuk Batch F).
- [ ] Tidak ada secret yang masuk repo selama pengerjaan.

---

## 14. Setelah approval — handoff yang akan gw tulis

Sesuai AGENTS §18: branch + commit, status workspace, apa yang berubah dan kenapa, hasil gate (buatan/merah), bukti mutation test, limits yang tersisa, rollback point, dan next command.
