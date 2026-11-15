# ADR — Posisi Panel Music: Drawer atau Content (#25)

**Status:** `PROPOSED — menunggu keputusan pemilik produk`
**Tanggal:** 2026-11-15
**Konteks issue:** [#25 [P3] Tentukan posisi Music: drawer atau content](https://github.com/muzape28-blip/NOTEZ/issues/25)
**Versi saat keputusan dibuat:** `0.1.6` (versionCode 7)

---

## 1. Masalah

Panel Music sekarang hidup di dalam drawer dan **di-`<include>` dua kali**:
`activity_main.xml:292` dan `activity_editor.xml` (di dalam `drawer_panel`).
`MusicDrawerController` dibuat per-Activity, jadi ada dua instance yang hidup
berdampingan. Issue #25 menanyakan apakah Music seharusnya tinggal di drawer
seperti sekarang, atau pindah ke content / layar sendiri.

Ini bukan bug yang bisa ditambal tanpa keputusan produk, karena ketiganya
mengubah di mana pengguna menemukan musik dan apa yang tetap tersedia saat
sedang menulis catatan.

## 2. Fakta dari kode (bukan asumsi)

| Fakta | Bukti |
|---|---|
| Dua instance controller (Main + Editor), masing-masing membangun `MediaController` sendiri | `MusicDrawerController.init { connectController() }` |
| Tiap instance memasang `handler.postDelayed(tick, 1000L)` yang berjalan terus selama Activity hidup | `tick` di `MusicDrawerController` |
| Tick menulis ulang beberapa TextView **setiap detik** walaupun drawer tertutup dan tidak ada lagu | `updateUi()` di dalam `tick` |
| Playback sendiri tetap benar saat Activity mati karena dipegang `MusicPlaybackService` (MediaSessionService) | `MusicPlaybackService.kt`, `SessionToken` |
| Music hanya bisa diakses dari drawer; tidak ada entry point lain | `activity_main.xml`, `activity_editor.xml` |

Konsekuensi yang terukur dari bacaan kode: dua `MediaController` + dua tick
1000 ms. Ini pemborosan kerja UI, bukan kebocoran memori (controller dilepas di
`destroy()`), dan bukan penyebab crash yang terlihat.

## 3. Opsi

### Opsi A — Tetap di drawer (status quo) + perbaiki ownership tick

- **Plus:** tidak ada perubahan UX, Music tetap tersedia di Home **dan** Editor,
  satu `<include>` layout dipakai dua tempat, diff kecil.
- **Minus:** dua instance tetap ada; judul seksi "Music" harus tetap ringkas
  karena ruang drawer 300dp.
- **Kerja:** hentikan tick saat drawer tertutup atau saat `onPause`; mulai lagi
  saat terlihat. Tidak menyentuh layout.

### Opsi B — Pindah ke content di Home

- **Plus:** satu instance, area lebih luas, tidak berebut ruang dengan daftar menu.
- **Minus:** **Music hilang dari Editor** — pengguna yang menulis sambil dengar
  musik kehilangan kontrol; harus keluar dulu ke Home. Ini regresi UX nyata.
- **Kerja:** pindahkan `<include>`, ubah controller agar bisa dipakai tanpa
  drawer, sesuaikan `MusicDrawerController` (nama kelas pun jadi menyesatkan).

### Opsi C — Layar Music sendiri (`MusicActivity`)

- **Plus:** ownership paling jelas: satu Activity, satu `MediaController`,
  satu tick; bisa tumbuh (playlist, pencarian, antrean) tanpa menggemukkan drawer.
- **Minus:** menambah satu layar + entry point baru yang harus ditemukan
  pengguna; drawer tetap butuh ringkasan now-playing agar tidak "hilang".
- **Kerja:** Activity baru, layout baru, navigasi dari dua Activity yang ada,
  plus ringkasan kecil di drawer.

## 4. Rekomendasi

**Opsi A sekarang, Opsi C kalau Music tumbuh.**

Alasannya: masalah yang benar-benar terukur hari ini adalah **tick tanpa owner
yang jelas**, bukan tempat panelnya. Memindahkan panel (B/C) tidak menghilangkan
tick — ia hanya memindahkannya, dan B malah menghilangkan akses dari Editor.
Memperbaiki ownership tick menyelesaikan akar masalahnya dengan diff terkecil,
dan tidak menutup jalan ke Opsi C nanti.

Yang **tidak** disarankan: Opsi B, karena menghapus kemampuan mengontrol musik
saat menulis catatan — penggunaan yang justru paling masuk akal untuk aplikasi ini.

## 5. Konsekuensi bila Opsi A dipilih

- Tidak ada perubahan layout maupun UX.
- Perbaikan tick menjadi pekerjaan terpisah dengan acceptance criteria sendiri:
  tick berhenti saat drawer tertutup, berhenti saat `onPause`, dan hidup lagi
  saat drawer dibuka; tidak ada tulisan TextView saat tidak terlihat.
- Issue #25 ditutup dengan merujuk ADR ini, bukan dengan patch kode.

## 6. Yang membatalkan keputusan ini

- Ada kebutuhan fitur yang tidak muat di drawer (antrean, pencarian lagu,
  playlist banyak) → pindah ke Opsi C.
- Pengguna melaporkan Music sulit ditemukan → Opsi C, bukan B.
- Ditemukan bahwa dua instance controller menyebabkan bug nyata (bukan sekadar
  kerja boros) → naikkan prioritas Opsi C.
