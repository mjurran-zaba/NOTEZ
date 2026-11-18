# NOTEZ v0.1.7

**Status:** Production release, published.
**Version:** `versionName 0.1.7`, `versionCode 8`.
**Published:** 2026-11-15T13:40:43Z.
**Tag:** `v0.1.7` → commit `bfa2619`.
**Signed APK built from:** commit `bfa2619` (merge `ZAQIxNOTEZ` di `9a50dfc`).
**CI run:** [43719866287](https://github.com/muzape28-blip/NOTEZ/actions/runs/43719866287) — `release`, success.
**Artifact:** `notez-v0.1.7-release`, 2.317.187 bytes.
**SHA-256 APK:** `17ee200439a6cca954ca031c2188606a519e54ac1894d9be1ad155b4c3345066`.
**Signer:** `CN=ZABA, OU=NOTEZ, O=ZABA` (sesuai sertifikat rilis repo).
**GitHub Release:** <https://github.com/muzape28-blip/NOTEZ/releases/tag/v0.1.7> — aset `notez-v0.1.7.apk` (3.261.459 bytes, SHA-256 sama dengan artifact CI).

---

## Added

- **Buka file (issue #19)** — menu FILE dapat membuka berkas Markdown dari
  penyimpanan sebagai catatan baru. Batas impor 5 MB, decode UTF-8 dengan
  fallback ISO-8859-1, dan URI sumber diingat lewat `NoteFileTargetStore` supaya
  "Simpan file" menulis balik ke berkas asli tanpa dialog.
- **Unit test pertama di repo (issue #26)** — `junit:junit:4.13.2`, plus
  `MusicPresentationTest` (JVM murni) dan `MusicChromeGuardTest` (guard leksikal
  atas layout dan resource).
- **Alur branching terdokumentasi** — `docs/BRANCHING.md`: `main` = rilis,
  `ZAQIxNOTEZ` = integrasi, pekerjaan per isu lewat PR.

## Changed

- **Mode playback musik (issue #22, #23)** — `MusicPlaybackMode` sekarang
  `REPEAT_ALL` / `REPEAT_ONE` / `SHUFFLE` dengan `applyTo(player)` sebagai
  satu-satunya sumber perilaku. Ikon repeat-one digambar ulang (`ic_music_once`),
  tombol sebelumnya/berikutnya memakai ikon Material (`ic_music_prev`,
  `ic_music_next`) menggantikan karakter `‹` `›`, dan label mode memakai
  `MusicPresentation` (teks + ikon + content description di satu tempat).
- **Nama mode lama tetap terbaca** — `fromPersisted()` memetakan `LOOP_ALL` /
  `ONCE` dari `music_library.json` versi sebelumnya.
- **Panel Music (issue #24)** — semua teks rata pada 18dp, heading aksesibilitas
  dipasang lewat `ViewCompat` (berlaku juga di API 26/27), label "Koleksi musik"
  disembunyikan saat kosong.
- **Subtitle panel Music (issue #20)** — hanya tampil saat panel collapsed.
- **Rotasi saat impor** — `noteId` dan status editing disimpan di
  `onSaveInstanceState`, sehingga rotasi tidak mengembalikan ke catatan lama.
- **CI** — `Build NOTEZ` berjalan pada push ke `main` dan `ZAQIxNOTEZ`, selalu
  menjalankan `testDebugUnitTest` sebelum `assembleDebug`, dan mengunggah APK
  debug dari semua build dengan nama `notez-debug-<ref>`. `release.yml`
  menjalankan unit test sebelum `assembleRelease` dan hanya bisa dijalankan dari
  `main`.

## Documentation

- `docs/RELEASE_NOTES_V0.1.7.md` (dokumen ini).
- `docs/RFC_FIX_OPEN_ISSUES_MUSIC_AND_FILE_2026_11_15.md` — rencana dan keputusan.
- `docs/ADR_MUSIC_LAYOUT_2026_11_15.md` — keputusan posisi panel Music (issue #25).

## Verifikasi

| Tingkat | Status | Bukti |
|---|---|---|
| IMPLEMENTED | ya | Semua perubahan di atas ada di `main`. |
| LOCALLY VERIFIED | ya | XML well-formed, brace/paren seimbang, guard leksikal disimulasikan, mutation test guard (TextView + chevron → merah, dipulihkan → hijau), pemindai komentar bersarang atas 29 berkas Kotlin. |
| CI VERIFIED | ya | `Build NOTEZ` di `main`/`bfa2619` success (`testDebugUnitTest` + `assembleDebug`), dan `release.yml` success dengan langkah `Run unit tests (guard)` → `Build signed release APK` → `Verify signature + badging` → `Shred keystore` semuanya hijau. |
| DEVICE VERIFIED | sebagian | APK debug dipasang di perangkat oleh pemilik repo dan dinyatakan sesuai. Skenario UAT terperinci belum dicatat per butir. |
| RELEASED | ya | Signed APK `notez-v0.1.7-release` terunggah dari run 43719866287 dan diterbitkan sebagai aset GitHub Release `v0.1.7`; tag menunjuk commit `bfa2619`. Distribusi lewat Play Store belum dilakukan. |

## Not included / sisa

- **Issue #21** (metadata audio vs nama berkas) — sudah ditelusuri di kode
  (`readMetadata()` memisahkan title/artist/album), tetapi belum ditutup karena
  belum ada verifikasi perangkat dengan berkas audio bertag dan tanpa tag.
- **Issue #25** — keputusan tercatat di ADR; perbaikan ownership tick Music
  (tick berjalan terus walau panel tertutup) adalah pekerjaan terpisah.
- `app/src/main/res/layout/item_guide_toc.xml:47` memakai chevron teks seperti
  kelas bug #22, tetapi berada di luar cakupan tiket ini dan belum diubah.

## Risiko yang diketahui

- `REPEAT_MODE_ONE` + `seekToNextMediaItem()` pada Media3 belum diuji langsung;
  jika tombol "berikutnya" memantul ke lagu yang sama, perbaikannya di
  `MusicPlaybackService.playNext()`.
