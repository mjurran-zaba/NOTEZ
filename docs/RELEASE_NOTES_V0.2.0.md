# NOTEZ v0.2.0

**Status:** Production release, published.
**Version:** `versionName 0.2.0`, `versionCode 11`.
**Tag:** `v0.2.0`
**GitHub Release:** <https://github.com/mjurran-zaba/NOTEZ/releases/tag/v0.2.0> — aset `NOTEZ-v0.2.0-release.apk`.

---

## Ringkasan Perubahan Utama (Highlights)

Rilis v0.2.0 merupakan lompatan besar (*major milestone*) untuk estetika visual, performa APK footprint, dan pengalaman diagnostik di NOTEZ:

### 1. Dedicated Theme Gallery Screen & Auroramorphism Grid
- **Galeri Tema Layar Penuh:** Menggantikan dialog pop-up bertumpuk di pengaturan dengan layar Theme Gallery mandiri (2 kolom grid).
- **Desain Kartu Auroramorphism & Glassmorphism:** Setiap thumbnail tema menampilkan 6-zone gradient aurora nyata yang serasi dengan palet warnanya, dibungkus efek frosted glass dan specular highlight.
- **Tampilan Minimalis & Bersih:** Thumbnail visual murni dan nama tema tanpa teks deskripsi yang memadati layar.
- **Instant Apply & Haptic Feedback:** Sentuhan pada kartu tema langsung menerapkan tema secara instan tanpa perlu tombol konfirmasi tambahan.

### 2. Snug 14dp Topbar Bottom Edge Fade & Dynamic Theme Bloom
- **Snug Edge Transition:** Fading edge dinamis 14dp terpasang presisi menempel di tepi bawah topbar (`mepet`), hanya muncul saat konten di-scroll dan menghilang sempurna di posisi resting 0.
- **Dynamic Aurora Bloom:** Warna fading edge beradaptasi secara dinamis dengan warna dominan aurora tema aktif, menciptakan transisi scroll yang lembut dan imersif.
- **Pembersihan Siluet Bug:** Menghilangkan pemotongan siluet kartu pada Resting State, menjaga margin alami kartu catatan tetap bernapas.

### 3. Standardisasi Topbar & Visual Polish
- **Standar 44dp Topbar:** Menyelaraskan seluruh topbar (Home, Editor, Recycle, Guide, Theme Gallery, Diagnostics, Settings) ke tinggi seragam 44dp yang ringkas dan modern.
- **Adaptive Drawer & Branding:** Logo kuda 2.5x lebih tajam dengan coretan aksen neon, serta animasi chalkboard GIF adaptif di drawer navigasi.

### 4. Footprint Optimization & Unicode Preservation
- **Ramping dengan `resourceConfigurations`:** Membatasi string library eksternal ke `id` dan `en`, memangkas ukuran `resources.arsc` secara signifikan.
- **100% Unicode Safe:** Tetap mendukung rendering dan penulisan teks aksara internasional (Arab RTL, Jepang Kanji/Kana, Devanagari, Emoji) tanpa hambatan.

### 5. Diagnostics Terminal Logcat Console
- **Tampilan ADB Logcat Monospace:** Desain konsol modern berlatar belakang gelap `#050806` dengan pewarnaan syntax (Error=Merah, Info=Hijau, Pkg=Cyan).
- **Filter Cepat & Jendela Anti-ANR:** Triage diagnostik performa dan breadcrumb secara instan tanpa lag.
