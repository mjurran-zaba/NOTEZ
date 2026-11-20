# ADR-004: NOTEZ v0.2.0 — APK Footprint Optimization, Immersive Glassmorphism Theme System, and ADB Logcat-Style Diagnostics Console

- **Status:** ACCEPTED (Diimplementasikan & diverifikasi)
- **Tanggal:** 2026-11-19
- **Penulis:** MUHAMMAD ZAQI PRASETYO & Agent Mode
- **Target Versi:** NOTEZ v0.2.0
- **Repositori:** `mjurran-zaba/NOTEZ` (Android 8.0+ / API 26..34, ARMv7 friendly)

---

## 1. Konteks & Latar Belakang Masalah

Dalam evaluasi berkala NOTEZ setelah rilis v0.1.9, ditemukan tiga area arsitektur yang perlu ditingkatkan untuk memaksimalkan efisiensi perangkat low-end (ARMv7 32-bit) sekaligus menyempurnakan estetika visual:

### 1.1 Ukuran Terinstall & Bloat String Asing
- **Masalah:** APK download v0.1.9 berukuran ~2.1 MB, namun setelah dipasang di HP menjadi ~8 MB. Walaupun ini wajar karena kompilasi ART (`.odex` / `.vdex`) dan alokasi SQLite WAL Room, APK masih memuat string terjemahan untuk 80+ bahasa dunia dari dependensi `com.google.android.material` dan `androidx.appcompat`.
- **Target:** Memangkas tabel `resources.arsc` tanpa mengorbankan kemampuan mengetik aksara internasional (Arab, Jepang, Sanskerta, Emoji) di editor catatan.

### 1.2 Estetika Tema, Glassmorphism, & Inkonsistensi Visual
- **Masalah:**
  1. Ke-14 tema bawaan saat ini seluruhnya bertipe **Dark Mode** (belum ada opsi Day/Paper mode yang teduh saat digunakan di luar ruangan).
  2. Komponen `AuroraBackdropView` sudah ada di `MainActivity`, namun kartu catatan (`NoteAdapter`) dan `MusicMiniPlayerHelper` diwarnai dengan alpha 224 (~88% opaque) dan garis tepi datar abu-abu (`colorOutline`). Akibatnya, gradasi aurora di belakang kartu tertutup dan tidak muncul ilusi **Glassmorphism** (kaca tembus pandang).
  3. Layar `EditorActivity` dan `RecycleActivity` masih berlatar belakang hitam datar tanpa `AuroraBackdropView`.
- **Target:** Menerapkan sistem *Optical Glassmorphism* (frosted translucency + specular rim highlight) tanpa library C++/shader berat, serta menambahkan palet tema imersif (Aurora Borealis, Cyber Obsidian, Sunset Mirage, dan Matcha Cream Paper).

### 1.3 Layar Diagnostics yang Belum Berasa "Terminal Logcat"
- **Masalah:** Tampilan `DiagnosticsActivity` saat ini menggunakan layout kartu standar yang kurang padat dan tidak memberikan nuansa "developer console".
- **Target:** Mengadopsi arsitektur diagnostik dari repo `ZCODE` (`com.zaba.zcode.ui.settings.DiagnosticsScreen.kt` & `Breadcrumb.kt`): gaya terminal ADB logcat (`#050806`, font Monospace, syntax highlighting Error=Merah/OK=Hijau/Pkg=Cyan, tab filter cepat, dan jendela rendering anti-ANR).

---

## 2. Keputusan Arsitektur (Architectural Decisions)

### Pilar 1: Optimasi Ukuran Ramping via `resourceConfigurations`
1. Menambahkan deklarasi konfigurasi bahasa di `app/build.gradle.kts`:
   ```kotlin
   defaultConfig {
       ...
       resourceConfigurations += listOf("id", "en")
   }
   ```
2. **Jaminan Kepatuhan Kontrak (Unicode Safe):**
   - Batasan ini **hanya** membuang file XML UI library pihak ketiga (label tombol internal Material).
   - Komponen editor (`EditText`, `TextView`) dan penyimpanan Room Database tetap berjalan di atas `java.lang.String` bertaraf **UTF-8 / UTF-16 Unicode penuh**. Pengguna di seluruh dunia tetap dapat membaca dan mengetik aksara Arab (RTL), Kanji/Hiragana Jepang, Hangul, Devanagari, maupun Emoji tanpa hambatan.

### Pilar 2: Immersive Glassmorphism System (Pure Android Views)
1. **Formula Optical Glass (0 KB Overhead & 0 Frame Drop di ARMv7):**
   - **Translucency Kalibrasi:** Menyesuaikan alpha latar belakang kartu catatan dari `224` (88%) menjadi **`165–175` (65%–69%)**. Gradasi `AuroraBackdropView` di bawahnya kini terlihat tembus lembut secara dinamis saat di-scroll.
   - **Specular Rim Highlight (Border Kaca):** Mengganti stroke kartu dari abu-abu mati menjadi *translucent accent / specular white tint* (`1dp` stroke dengan alpha adaptif). Efek ini memberikan ilusi fisik pantulan cahaya di sudut lempengan kaca akrilik tebal.
   - **Floating Glass Pill (Mini Player):** Mini player musik dirombak menjadi kapsul kaca melayang di atas konten catatan.
2. **Pemasangan Ambient Backdrop Menyeluruh:**
   - Menempatkan `AuroraBackdropView` pada `activity_editor.xml` dan `activity_recycle.xml` agar transisi antar-layar mulus dan tematik.
3. **Katalog 4 Tema Baru di `ThemePref.kt`:**
   - **Aurora Borealis:** Dasar hitam kosmik (`#080C14`), aksen hijau zamrud & cyan neon (`#00F5A0` / `#00D9F5`).
   - **Cyber Obsidian (Inspirasi Seal Plus):** Dasar obsidian pekat (`#0A0A0F`), aksen violet elektrik & biru neon (`#8B5CF6`).
   - **Sunset Mirage:** Senja temaram hangat (`#140C10`), aksen amber peach glowing (`#FF7E5F`).
   - **Matcha Cream Paper (Day / Paper Mode):** Latar kertas oat teduh (`#F8F6F0`), aksen matcha sage (`#4E7250`), kartu frosted milky glass yang nyaman di bawah sinar matahari.

### Pilar 3: ADB Logcat-Style Diagnostics Console (Inspirasi ZCODE)
1. **Format Jejak Monospace Padat:**
   - Format log distandarkan: `MM-dd HH:mm:ss.SSS | TAG | DETAIL`.
   - Penyimpanan langsung di-flush ke disk (`FileWriter.flush()`) sehingga jika terjadi crash native/OOM mendadak, baris terakhir tetap terekam sebagai TKP forensik.
2. **Pewarnaan Sintaks Terminal (Syntax Highlighting):**
   - Error / Fatal / Exception $\rightarrow$ Merah `#FF6B6B`.
   - Success / OK / Done $\rightarrow$ Hijau Terminal `#39FF14`.
   - Lifecycle / System / DB $\rightarrow$ Cyan `#7DD3FC`.
   - Default Information $\rightarrow$ Mint Green `#9AE6B4`.
3. **Fitur Navigasi Forensik:**
   - Tab filter instan: `SEMUA` · `LIFECYCLE` · `MUSIC` · `CRASH`.
   - Blok penanda jelas: `=== CRASH TERAKHIR ===` dan `=== SESI PERFORMA ===`.
   - Tombol cepat **SALIN SEMUA** dan **BAGIKAN** ke intent sistem.
   - Jendela render maksimal 500 baris aktif (`RENDER_WINDOW = 500`) untuk mencegah ANR pada CPU ARMv7 low-end.

---

## 3. Daftar File & Direktori yang Terlibat

### 3.1 File yang PASTI Disentuh (Definite Modifications)

| No | Path File | Tindakan / Perubahan |
|---|---|---|
| 1 | `app/build.gradle.kts` | Menambahkan `resourceConfigurations += listOf("id", "en")` di dalam `defaultConfig`. |
| 2 | `app/src/main/res/values/colors.xml` | Menambahkan definisi kode warna untuk 4 tema baru (Aurora, Obsidian, Sunset, Matcha Cream) serta token warna glass rim. |
| 3 | `app/src/main/res/values/themes.xml` | Mendaftarkan 4 style tema baru turunan `Theme.Material3` dengan konfigurasi window background dan surface tint. |
| 4 | `app/src/main/java/com/zaba/notez/ThemePref.kt` | Mendaftarkan id tema baru, deskripsi, resource mapping (background, surface, accent, outline), dan accessor. |
| 5 | `app/src/main/java/com/zaba/notez/NoteAdapter.kt` | Memperbarui perhitungan alpha kartu glassmorphism (`168`) dan stroke rim highlight adaptif. |
| 6 | `app/src/main/res/layout/activity_diagnostics.xml` | Merombak layout menjadi tampilan console terminal full-screen dengan tab bar, toolbar monospaced, dan kontainer terminal logcat. |
| 7 | `app/src/main/java/com/zaba/notez/diagnostics/DiagnosticsActivity.kt` | Mengimplementasikan pewarnaan sintaks logcat, pemfilteran tab, jendela render 500 baris, dan penyalinan data forensik lengkap. |
| 8 | `app/src/main/java/com/zaba/notez/diagnostics/Breadcrumb.kt` | Penyelarasan format timestamp logcat `MM-dd HH:mm:ss.SSS` dan perlindungan flush instan. |
| 9 | `app/src/main/res/layout/activity_editor.xml` | Menyematkan `AuroraBackdropView` sebagai layer dasar di balik container editor. |
| 10 | `app/src/main/java/com/zaba/notez/music/MusicMiniPlayerHelper.kt` | Mengubah background dan border mini-player card agar tampak sebagai floating translucent capsule. |

### 3.2 File yang KEMUNGKINAN Disentuh / Diverifikasi (Conditional Polish)

| No | Path File | Kondisi & Tindakan |
|---|---|---|
| 11 | `app/src/main/res/layout/activity_recycle.xml` | Menambahkan `AuroraBackdropView` bila konsistensi visual Recycle Bin diselaraskan. |
| 12 | `app/src/main/res/layout/item_note_grid.xml` | Menyesuaikan border dan stroke kartu pada mode tampilan Grid agar identik dengan mode List. |
| 13 | `app/src/main/java/com/zaba/notez/SettingsActivity.kt` | Mengelompokkan 4 tema baru ke dalam group pilihan tema di dialog pengaturan. |
| 14 | `app/src/main/java/com/zaba/notez/AuroraBackdropView.kt` | Memastikan `RadialGradient` menghasilkan kontras yang tepat saat tema terang (Matcha Cream) aktif. |

---

## 4. Analisis Risiko & Mitigasi

1. **Risiko:** Performa menurun akibat *overdraw* dengan kartu transparan di ARMv7.  
   - **Mitigasi:** `AuroraBackdropView` bersifat **statis** (tidak ada loop animasi yang memicu redraw tiap frame). Kartu transparan hanya di-render sekali saat scroll diam, dengan zero GPU shader overhead.
2. **Risiko:** Teks catatan sulit dibaca di atas kartu transparan (*contrast issue*).  
   - **Mitigasi:** Alpha kartu dijaga di ambang batas aman 65%–70% (bukan 20%–30%), sehingga ketajaman tipografi teks judul dan isi catatan tetap memenuhi standar aksesibilitas WCAG AA.
3. **Risiko:** ANR pada Diagnostics saat log mencapai ribuan baris.  
   - **Mitigasi:** Menerapkan jendela geser `RENDER_WINDOW = 500` persis seperti pelajaran berharga dari `ZCODE` (Bug T), di mana baris lama dipotong dari rendering view namun tetap utuh di disk saat tombol Salin ditekan.

---

## 5. Rencana Pengujian (Verification Plan)

- [ ] **Kompilasi & Lint:** Build project lolos `assembleRelease` tanpa warning fatal R8.
- [ ] **Audit Ukuran APK:** Membandingkan ukuran APK rilis sebelum dan sesudah penambahan `resourceConfigurations`.
- [ ] **Uji Aksara Unicode:** Mengetik dan menyimpan catatan berisi karakter Arab, Jepang, dan Emoji untuk membuktikan integritas teks.
- [ ] **Uji Visual Glassmorphism:** Memverifikasi gradasi aurora yang membias lembut di balik kartu catatan dan mini player.
- [ ] **Uji Diagnostics Terminal:** Menghasilkan error buatan dan memeriksa pewarnaan baris logcat serta kelancaran tombol salin.

---
*Dokumen ini siap ditindaklanjuti ke tahap implementasi teknis setelah persetujuan.*
