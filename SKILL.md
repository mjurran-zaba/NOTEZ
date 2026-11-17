---
name: armv7-android-constraints
description: Panduan rekayasa APK Android untuk perangkat ARMv7 (armeabi-v7a, 32-bit) dan HP low-end/RAM kecil, termasuk saat membangun dan menguji lewat Termux/ACode atau sandbox terbatas. Mencakup identifikasi lingkungan (ABI, libc bionic vs glibc, API level), inferensi yang tidak valid, jebakan native library dan wheel Python (Chaquopy), tangga verifikasi termurah (host, emulator, perangkat), anggaran performa terukur, dan UX yang nyaman di layar kecil. Gunakan setiap kali user menyebut ARMv7, armeabi-v7a, 32-bit, HP kentang/low-end, RAM kecil, SIGSEGV/dlopen/UnsatisfiedLinkError, wheel/native lib di Android, emulator ARM, atau minta optimasi performa dan UX untuk perangkat lemah, walau tidak menyebut kata "ARMv7" secara eksplisit. English - Android 32-bit ARM constraints, bionic vs glibc, native wheels, low-end device performance and UX.
---

# Rekayasa Android untuk ARMv7 dan perangkat low-end

Perangkat ARMv7 dan HP berspek rendah gagal dengan cara yang tidak terlihat di
laptop developer: crash native tanpa stack Java, binary yang dibangun untuk
libc lain, memori habis, layar sempit. Asumsi dari mesin cepat sering salah di
sini. Aturan utama skill ini: **ukur dan uji pada lapisan yang paling dekat
dengan perangkat target, dan katakan jujur lapisan mana yang sudah diuji.**

## 1. Identifikasi lingkungan dulu

Sebelum merancang atau mendiagnosis, tulis fakta berikut (tanyakan atau ukur,
jangan menebak):

| Hal | Pertanyaan |
|---|---|
| OS & API | versi Android, API level, `minSdk`/`targetSdk` |
| ABI | `armeabi-v7a` saja? `abiFilters` di Gradle? |
| libc | bionic (Android) atau glibc (host/emulator Linux)? |
| Runtime | JVM/ART, WebView, native lib, Python embedded (Chaquopy) |
| Sumber dependensi | build dari source, wheel, atau prebuilt AAR? |
| Sumber daya | RAM, ruang disk, jumlah core, layar (dp) |
| Alat build | apa yang bisa jalan lokal vs hanya di CI |

## 2. Inferensi yang TIDAK valid

Jangan menyimpulkan hal berikut tanpa bukti di lingkungan target:

- "Berjalan di x86/ARM64 berarti berjalan di ARMv7."
- "Berjalan di Linux glibc berarti berjalan di Android bionic."
- "Emulator lulus berarti semua perangkat nyata lulus."
- "Build berhasil berarti runtime berhasil."
- "Import modul berhasil berarti fitur lengkap berfungsi."
- "Library ada di PyPI berarti ada wheel untuk Android ARMv7."
- "Test lulus pada mode fallback berarti dependensi asli teruji."
- "Update dari versi A ke B aman" tanpa menguji dua artefak nyata.

## 3. Jebakan native dan wheel

Catatan ini berasal dari pengalaman proyek, bukan dari verifikasi ulang saat
skill ditulis. **Cek ulang terhadap versi alat yang kamu pakai sebelum
memutuskan.**

- **Wheel Linux biasa (`manylinux*`) bukan wheel Android.** Wheel untuk
  glibc yang dimuat di bionic bisa berakhir sebagai crash native (SIGSEGV)
  yang tidak bisa ditangkap dengan `try/except`. Pakai wheel dengan tag
  Android (pola `android_<api>_armeabi_v7a`) atau build dari source untuk
  target yang benar.
- **Tolak secara eksplisit.** Di kode, deteksi arsitektur/libc yang tidak
  didukung dan gagal dengan pesan jelas; jangan membiarkan proses mati diam-diam.
- **Dukungan 32-bit bisa hilang pada versi baru.** Catatan proyek menyebut
  Chaquopy berhenti menyediakan wheel 32-bit pada Python 3.12+
  (lihat https://chaquo.com/chaquopy/ dan catatan rilisnya). Verifikasi di
  rilis terbaru sebelum memilih versi Python atau menghapus ABI 32-bit.
- **`abiFilters` harus mencerminkan perangkat nyata.** Menghapus
  `armeabi-v7a` demi ukuran berarti menutup perangkat 32-bit; itu keputusan
  produk, bukan optimasi kecil.
- **Ukuran dan pemuatan.** Tiap native lib menambah ukuran APK dan memori
  saat dimuat; ukur, jangan menebak.

## 4. Tangga verifikasi termurah

Naikkan tingkat hanya bila lapisan sebelumnya tidak cukup membuktikan klaim.
Mulai dari yang paling murah:

1. **Test di host** untuk logika murni (cepat, tidak membuktikan ABI/libc).
2. **Binary ARM glibc via emulasi user-mode** (menangkap bug 32-bit, tidak
   menangkap bug bionic).
3. **Lingkungan bionic ARM** bila tersedia (lebih dekat ke Android).
4. **Emulator Android penuh ARM32** (menangkap manifest, izin, WebView, siklus
   hidup; lambat dan butuh banyak RAM/disk).
5. **Perangkat nyata** (satu-satunya yang membuktikan perilaku perangkat itu).

Pelajaran emulator dan sandbox yang rinci ada di
`references/emulation-lessons.md`; baca bila kamu harus menyalakan emulator
atau bekerja di sandbox dengan RAM/disk kecil. Setiap hasil harus diberi label
lapisan yang benar (lihat skill `evidence-and-honesty`).

## 5. Anggaran performa harus terukur

- Tetapkan anggaran per perangkat target: waktu start dingin, memori puncak
  (RSS), ukuran APK, dan responsivitas input. **Isi angka dari pengukuran di
  perangkat target, bukan dari tebakan.**
- Ukur sebelum dan sesudah tiap optimasi, pada build yang sama dan kondisi yang
  sama. Optimasi tanpa angka adalah klaim tanpa bukti.
- Hindari pekerjaan di thread UI, alokasi besar saat start, dan dependensi
  yang menarik runtime besar demi satu fungsi kecil.
- Untuk logika yang panjang atau berat, pertimbangkan tombol batal, batas
  waktu, dan batas memori yang eksplisit.
- Untuk animasi: gunakan pendekatan yang deterministik dan ringan; jangan
  menambah jeda palsu demi tampilan.

## 6. UX yang nyaman di layar kecil dan perangkat lemah

- Rancang untuk lebar kecil (sekitar 360dp) dan uji portrait, landscape, serta
  dengan keyboard terbuka. Konten penting tidak boleh tertutup.
- Target sentuh minimal 48dp (rekomendasi Material/Android). Beri jarak antar
  target yang cukup.
- Beri umpan balik cepat untuk aksi yang lambat (indikator progres,
  status yang jujur), jangan membuat UI tampak membeku.
- Pertahankan kontras dan ukuran teks yang terbaca; dukung ukuran font sistem.
- Pakai ikon vektor atau gambar yang konsisten untuk elemen fungsional;
  hindari emoji sebagai kontrol utama karena tampilannya bergantung perangkat.
- Pastikan fitur utama tetap berguna saat memori terbatas (simpan state,
  pulih dengan baik saat proses dimatikan sistem).

## 7. Checklist sebelum menyatakan "didukung ARMv7"

- [ ] ABI dan libc target tertulis dan dicocokkan dengan artefak.
- [ ] Tidak ada native lib/wheel yang dibuat untuk libc atau ABI lain.
- [ ] Lapisan verifikasi tertinggi yang benar-benar dijalankan disebut.
- [ ] Angka performa (start, memori, ukuran) berasal dari pengukuran.
- [ ] UX diuji pada layar kecil dan orientasi berbeda.
- [ ] Yang belum diuji (perangkat nyata, TalkBack, update antar-versi)
      disebut sebagai batas klaim.
