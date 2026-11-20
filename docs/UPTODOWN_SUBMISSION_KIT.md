# Uptodown Developer Submission Kit - NOTEZ

Dokumen panduan dan teks siap pakai untuk pendaftaran aplikasi NOTEZ di Uptodown Developer Console (`developers.uptodown.com`).

---

## 1. Data Teknis Aplikasi

| Kolom | Nilai |
| :--- | :--- |
| Application Name | `NOTEZ` |
| Package Name | `com.zaba.notez` |
| Version Name | `0.2.0` |
| Version Code | `11` |
| Primary Category | Productivity |
| Secondary Category | Tools |
| License | Open Source (GNU General Public License v3.0) |
| Price | Free / Gratis |
| Website / Repository | `https://github.com/mjurran-zaba/NOTEZ` |
| Developer / Studio Name | `MJURRAN` |
| Contact Email | `mjurran@gmail.com` |
| Download APK URL | `https://github.com/mjurran-zaba/NOTEZ/releases/download/v0.2.0/app-release.apk` |

---

## 2. Deskripsi Singkat (Short Description / Tagline)

Maksimal 80 - 100 karakter.

### Versi Bahasa Indonesia
> Aplikasi catatan Markdown offline-first dengan pemutar audio fokus lokal tanpa pelacakan.

### Versi Bahasa Inggris
> Fast, offline-first Markdown notes app with local focus audio and zero tracking.

---

## 3. Deskripsi Lengkap (Full Description)

### Versi Bahasa Indonesia

NOTEZ adalah aplikasi catatan pribadi berbasis teks Markdown yang dibangun dengan arsitektur offline-first. Aplikasi ini memprioritaskan kecepatan akses, kesederhanaan visual, dan privasi penuh tanpa ketergantungan pada server pihak ketiga.

Fitur Utama:

1. Editor & Pratinjau Markdown
Mendukung sintaks Markdown standar (heading, list, table, blockquote, task list, code block) yang diproses langsung di perangkat menggunakan engine lokal tanpa memuat resource eksternal.

2. Penyimpanan Sepenuhnya Offline
Seluruh catatan disimpan secara lokal menggunakan database SQLite Room terenkapsulasi. Fitur ekspor dan impor memanfaatkan Android Storage Access Framework (SAF) sehingga pengguna memiliki kendali penuh atas file catatan mereka sendiri (.json dan .md).

3. Pemutar Musik Fokus Lokal (Focus Music)
Dilengkapi pemutar audio latar belakang berbasis AndroidX Media3 untuk memutar berkas audio lokal saat menulis atau membaca, tanpa iklan dan tanpa streaming internet.

4. Tema Tampilan Minimalis
Pilihan tema kontras tinggi yang dirancang untuk kenyamanan membaca dalam jangka waktu lama, termasuk GitHub Dark, GitHub Light, dan varian tema gelap hemat daya.

5. Privasi Mutlak
Tidak memerlukan pendaftaran akun, tidak mengandung pelacak analitik, tidak menampilkan iklan, dan tidak mengirimkan data catatan ke internet. Berlisensi bebas di bawah GNU GPL-3.0.

---

### Versi Bahasa Inggris

NOTEZ is an offline-first Markdown notes and reading application designed for speed, clarity, and uncompromising privacy. It functions entirely on-device without external server dependencies.

Core Features:

1. Markdown Editor & Clean Reader
Supports standard Markdown formatting (headings, lists, tables, code blocks, task lists) rendered locally via an isolated internal renderer with zero CDN or third-party script tracking.

2. Pure Offline Storage
All notes reside on your device inside a local SQLite database. Import and export operations rely strictly on the Android Storage Access Framework (SAF), giving you direct ownership of your plain text files.

3. Integrated Focus Audio
Features a built-in background audio player powered by AndroidX Media3 for local ambient playback while writing, with full lockscreen and notification controls.

4. Streamlined Theme System
Features high-contrast, distraction-free color palettes tailored for readability, including GitHub Dark, GitHub Light, and power-saving dark modes.

5. Zero Telemetry & Open Source
No account creation, no analytics SDKs, no behavioral tracking, and no advertisements. Free and open source under the GNU General Public License v3.0.

---

## 4. Catatan Rilis (Changelog v0.2.0)

- Rilis produksi signed APK v0.2.0.
- Penambahan panel diagnostik performa rendering.
- Sinkronisasi rasio gulir antara mode edit dan pratinjau baca.
- Peningkatan persistensi pemutar audio fokus di latar belakang sistem.
- Pembersihan dependensi dan optimalisasi ukuran biner APK.

---

## 5. Berkas Aset Visual

Semua aset berikut berada di direktori `NOTEZ/docs/assets/`:

- **Icon 512x512 PNG**: `docs/assets/icon_512.png`
- **Tangkapan Layar 1 (Daftar Catatan)**: `docs/assets/Screenshot_20261120-213708.png`
- **Tangkapan Layar 2 (Tampilan Grid & Mini Player)**: `docs/assets/Screenshot_20261120-213725.png`
- **Tangkapan Layar 3 (Mode Pratinjau Markdown)**: `docs/assets/Screenshot_20261120-213801.png`
- **Tangkapan Layar 4 (Mode Editor Teks)**: `docs/assets/Screenshot_20261120-213809.png`
- **Tangkapan Layar 5 (Drawer Musik Fokus)**: `docs/assets/Screenshot_20261120-213843.png`
- **Tangkapan Layar 6 (Galeri Tema Tampilan)**: `docs/assets/Screenshot_20261120-213854.png`
