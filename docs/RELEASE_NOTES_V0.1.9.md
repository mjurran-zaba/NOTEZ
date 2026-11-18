# NOTEZ v0.1.9

**Status:** Production release candidate.
**Version:** `versionName 0.1.9`, `versionCode 10`.
**Tag:** `v0.1.9`

---

## Ringkasan Perubahan (Highlights)

Rilis v0.1.9 berfokus pada penyempurnaan menyeluruh pengalaman pengguna (Delight & Usability), pengorganisasian catatan tingkat lanjut (Multi-Selection, Pin, Recycle Bin), kontrol pemutar musik yang lebih presisi, serta perbaikan visual logo dan animasi splash screen.

### 1. Manajemen Catatan: Multi-Selection & Contextual Action Bar
- **Long Press untuk Seleksi:** Menekan lama pada catatan (baik mode List maupun Grid) mengaktifkan mode multi-seleksi dengan Contextual Action Bar di bagian atas layar.
- **Pilih Semua / Batal:** Ikon seleksi cepat untuk memilih semua catatan sekaligus atau membatalkan pilihan.
- **Tindakan Massal (Batch Actions):** Mendukung penyematan (Pin/Unpin) massal dan pemindahan ke Tempat Sampah (Recycle) secara bersamaan.
- **Feedback Haptic & Indikator Visual:** Setiap perubahan seleksi disertai haptic feedback halus dan ikon centang terpadu.

### 2. Fitur Pin & Indikator Semat
- Catatan yang disematkan akan selalu berada di urutan teratas daftar catatan.
- Indikator pin bergaya Lucide Pushpin tampil elegan di pojok kartu catatan pada layout List maupun Grid.

### 3. Tempat Sampah (Recycle Bin) & Kebijakan Auto-Purge 30 Hari
- **Menu Drawer Recycle:** Halaman khusus Tempat Sampah yang terpisah untuk menampung catatan yang dihapus.
- **Restore & Permanent Delete:** Catatan dapat dipulihkan kembali ke daftar utama dengan sekali ketuk atau dihapus permanen.
- **Auto-Purge 30 Hari:** Kebijakan pembersihan otomatis di latar belakang saat aplikasi dibuka untuk catatan yang telah berada di Tempat Sampah lebih dari 30 hari.

### 4. Music Drawer & Mini-Player Polish
- **Seek -10s / +10s:** Tombol lompat mundur dan maju 10 detik dengan ikon Lucide monoline custom (`rotate-ccw-10` dan `rotate-cw-10`) di drawer musik.
- **Swipe-to-Dismiss Mini-Player:** Gesture swipe ke kiri pada bar mini-player kini bekerja responsif dan mulus untuk menutup pemutar mini.
- **Navigasi Modulo Track:** Swipe kanan/kiri untuk skip lagu sebelumnya/berikutnya dengan perlindungan indeks modulo yang aman dan tidak crash.
- **Haptic Feedback:** Setiap gesture swipe navigasi dan dismiss kini menghasilkan getaran haptic yang natural.
- **Sinkronisasi Speed Dial FAB:** Perbaikan pergeseran `translationY` Speed Dial FAB agar tetap terikat rapi dan tidak tumpang tindih saat mini-player aktif.

### 5. Splash Screen & Brand Identity Polish
- **Animasi 60/120fps Bebas Freeze:** Menghapus tugas berat sinkronisasi WebView dari antrean startup Main Thread, memastikan animasi splash berjalan mulus tanpa stutter.
- **Logo Tajam & Coretan Aksen Lengkap:** Menjamin pewarnaan aksen hijau neon (`splash_accent`) dan pendaran (`splash_glow`) terpasang sejak frame ke-0, sehingga logo kuda dan garis aksen di bawahnya tampil tajam, presisi, dan tidak buram.

### 6. Perbaikan Performa Markdown Preview & Renderer Recovery (Issue #2)
- Penanganan `onRenderProcessGone` untuk memulihkan tampilan preview Markdown secara instan jika render process WebView terhenti di perangkat dengan RAM terbatas.
- Penyesuaian layout dan initial visibility search bar (`home_toolbar`) untuk mencegah layout shift.

---

## Verifikasi & Kualitas

| Pengujian | Status | Catatan |
|---|---|---|
| **Unit Tests & Layout Guards** | **Lolos (100%)** | 75 dari 75 test suite lulus tanpa kegagalan. |
| **Pemeriksaan Statis XML & Kotlin** | **Lolos** | ID layout, constraint, dan drawable vector valid. |
| **UAT Pengguna** | **Dikonfirmasi** | Pengguna mengonfirmasi animasi splash, mini-player swipe, multi-select, dan drawer tampil sempurna dan nyaman digunakan. |
