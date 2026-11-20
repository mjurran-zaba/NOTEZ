# ADR: NOTEZ UI Polish, Design System, & Anti-Slop Component Architecture

- **Status:** APPROVED (Siap Diimplementasikan)
- **Tanggal:** 18 November 2026
- **Pengambil Keputusan:** Lead Designer & Engineer (`muzape28-blip` / `zabaniyah221`) & Arena Agent
- **Target Versi:** NOTEZ v1.1.0+
- **Platform:** Android (API 26–35), Kotlin, Androidx Jetpack, Material Components 3, Media3

---

## 1. Konteks & Latar Belakang Masalah

Setelah optimasi performa dokumen besar (100 KB) dan eliminasi bottleneck perakitan shell HTML berhasil dicapai pada perangkat kelas entri (Infinix SMART 9 HD), audit antarmuka pengguna (UI) secara menyeluruh menemukan beberapa kelemahan struktural, inkonsistensi ergonomi, dan *anti-pattern* visual:

1. **"Ghost Navigation" & Affordance yang Gaib:**
   - Home (`activity_main.xml`) tidak memiliki tombol menu/hamburger. Pengguna baru tidak menyadari adanya Drawer (Pengaturan, Panduan, Diagnostics, Mode Grid/List).
   - Di Android 10+ dengan *Gesture Navigation*, gestur *edge-swipe* dari sisi kiri sering berbenturan dengan gestur sistem **BACK**, menyebabkan pengguna terlempar keluar aplikasi saat hendak membuka drawer.
   - Ikon pencarian (*Search*) saat ini menggunakan mata terbuka (`ic_eye`), yang bertentangan dengan *mental model* standar pengguna (mata diasosiasikan dengan preview/password, bukan pencarian).
2. **"Ranjau" Tombol Sampah Inline pada Kartu Catatan:**
   - Mode List (`item_note.xml`) dan Grid (`item_note_grid.xml`) menempatkan tombol sampah permanen 48x48dp (`item_delete`) di ujung kanan dan pojok bawah kartu. Area ini adalah zona utama sentuhan jempol kanan saat membuka catatan, mengakibatkan risiko *accidental deletion* yang tinggi.
3. **Kelelahan Visual Akibat Border Neon (*Stroke Glare*):**
   - Setiap kartu di-stroke menggunakan warna `colorPrimary` murni. Di layar dengan puluhan catatan, hal ini menciptakan polusi visual garis neon yang melelahkan mata.
4. **Kekakuan Grid Item:**
   - `item_note_grid.xml` mengunci `minLines="2"` pada judul dan `minLines="3"` pada preview, membuang ruang layar secara kaku saat catatan pendek atau kosong.
5. **Drawer Campur Aduk (Home vs. Editor):**
   - Pemutar musik lokal berada di Drawer Home dan Drawer Editor. Home yang seharusnya menjadi tempat *triage* cepat menjadi sesak, sementara Editor yang merupakan tempat *deep work* belum memiliki *companion mini player* persisten saat bernavigasi.
   - Fitur "Buka file" berada di dalam Drawer Editor, padahal pembukaan berkas eksternal (`.md` / `.txt`) secara alamiah merupakan alur masuk dari Home.
6. **Elemen UI Kuno & Teks Mentah:**
   - Layar Pengaturan (`activity_settings.xml`) menggunakan karakter unicode mentah `▲` dan `▼` untuk accordion, serta garis divider kaku.
   - Counter editor menempel mentah di bawah judul seperti debug string tanpa visual hierarchy.

---

## 2. Keputusan Desain & Arsitektur Komponen

### A. Sistem Ikon: Lucide Monoline Vector (Obsidian Style)
Diputuskan untuk **menolak penggunaan emoji** pada UI chrome dan mengadopsi standar desain visual **Lucide Icons** (set ikon resmi Obsidian.md):
- **Spesifikasi:** Kanvas `24x24dp`, `strokeWidth = 2dp`, `strokeLineCap = round`, `strokeLineJoin = round`, tanpa *flat fill*.
- **Warna Dinamis:** Setiap ikon dikonversi menjadi `<vector>` XML native Android dan di-tinting secara dinamis melalui `?attr/colorOnSurfaceVariant` atau `?attr/colorPrimary`.
- **Daftar Pemetaan Ikon:**
  - `panel-left`: Pembuka drawer / sidebar di Home Top Bar.
  - `search`: Kaca pembesar pencarian di Home Top Bar.
  - `plus` / `x`: Morphing FAB utama.
  - `file-plus`: Aksi "Catatan Baru" pada Speed Dial FAB.
  - `folder-open`: Aksi "Buka File (.md / .txt)" pada Speed Dial FAB.
  - `book-open`: Menu Panduan Markdown.
  - `sliders-horizontal`: Menu Pengaturan.
  - `activity`: Menu Diagnostics.
  - `layout-grid` / `layout-list`: Pengganti mode tampilan.
  - `arrow-left`: Navigasi kembali pada Editor, Settings, dan Guide.
  - `save` & `file-down`: Aksi simpan file di Editor.
  - `palette`, `database`, `file-up`, `folder-sync`, `shield-check`, `trash-2`, `tag`: Menu di Pengaturan.
  - `chevron-down` & `chevron-right`: Indikator accordion dan trailing navigasi.

---

### B. Top App Bar & Drawer Toggle (`panel-left`)
- **Struktur Baris Atas Home:**
  - Kiri: Tombol `panel-left` (40x40dp, touch target 48dp).
  - Tengah: Teks/logo subtil NOTEZ dengan note counter halus.
  - Kanan: Tombol Lucide `search`.
- **Animasi Sinkronisasi Gestur Drawer:**
  - Tombol `panel-left` dikaitkan langsung dengan `onDrawerSlide(slideOffset)` dari `DrawerLayout`. Saat drawer diusap dengan jari, ikon berputar halus dari 0° hingga 180° secara kontinu mengikuti posisi ujung jari.
  - Dilengkapi *micro-spring press bounce* (120ms, scale `1.0 -> 0.88 -> 1.0`) dengan haptic click feedback ringan saat diketuk.

---

### C. Pemindahan "Buka File" & FAB Speed Dial (Opsi A)
- **Migrasi Alur:** Fitur "Buka file" (`menu_file_open`) dihapus dari Drawer Editor dan dipindahkan seutuhnya ke FAB Home.
- **Interaksi Tap FAB:**
  1. Mengetuk FAB `+` tidak lagi langsung membuat catatan, melainkan membuka **M3 Floating Speed Dial**.
  2. Ikon FAB utama berputar 45° secara halus (`+` bertransformasi menjadi `✕`).
  3. Latar belakang meredup dengan scrim halus (*dim 35%*).
  4. Dua kapsul pill meluncur naik bertingkat (*staggered 30ms*, `OvershootInterpolator`):
     - **Pill 1 (Bawah):** `[ file-plus ] Catatan Baru` $\to$ Membuat catatan baru di database Room dan membuka Editor.
     - **Pill 2 (Atas):** `[ folder-open ] Buka File (.md / .txt)` $\to$ Membuka SAF picker Android (`text/*`, `application/octet-stream`), mengimpor teks, dan mengikat URI untuk persistensi.
  5. Mengetuk area kosong atau mengetuk tombol `✕` menutup speed dial dan mengembalikan FAB ke rotasi 0°.

---

### D. Pemisahan Konteks Drawer (Separation of Concerns)
1. **Drawer Home (`MainActivity`):**
   - Komponen pemutar musik (`include_music_drawer`) **dihapus seutuhnya**.
   - Murni berfungsi sebagai **Navigasi Aplikasi**: Panduan Markdown, Pengaturan, Diagnostics, Mode Tampilan List/Grid.
   - Ramping, cepat, dan bebas dari scroll berlebih.
2. **Drawer Editor (`EditorActivity`):**
   - Berfungsi sebagai **Workspace & Focus Companion**:
     - Bagian Atas: Operasi Berkas (*Simpan File*, *Simpan Sebagai...*).
     - Bagian Bawah: *Focus Sound Hub* (Koleksi musik lokal, tambah lagu, mode putar shuffle/loop).

---

### E. Floating Glass Mini Player (Adopsi DNA ArchiveTune)
Mengadopsi pola *Floating Island* dari `ArchiveTune` (`moe.rukamori.archivetune.ui.player.MiniPlayer`):
1. **Bentuk & Penempatan:**
   - Kapsul melayang (*Floating Pill*) setinggi **56dp** dengan `radius = 28dp`, `marginHorizontal = 12dp`, `marginBottom = 12dp`.
   - Menggunakan `UiStyle.applyGlassCard` (translucent surface `alpha = 224` + stroke halus 1dp alpha 18%), menyatu secara organik dengan pendaran `AuroraBackdropView`.
2. **Anatomi Internal:**
   - **Kiri (40x40dp):** Ikon musik dibungkus *circular stroke progress indicator* (2.5dp warna `colorPrimary`) yang terisi melingkar sesuai rasio `position / duration`.
   - **Tengah (Flex 1):** Judul lagu berjalan (`13sp`, bold, single-line marquee) dan nama artis/file (`11sp`, `colorOnSurfaceVariant`).
   - **Kanan:** Tombol transportasi kompak (Prev 32dp, Play/Pause 38dp filled primary circle, Next 32dp).
3. **Gestur Taktil:**
   - *Swipe Horizontal* kiri-kanan pada kapsul untuk aksi cepat *Next / Previous track*.
   - *Tap Body* kapsul untuk membuka koleksi musik di Drawer Editor.
4. **Orkestrasi Layar:**
   - **Di Home:** Saat musik aktif, kapsul melayang di atas navigation bar. FAB `+` secara otomatis bergeser naik setinggi `64dp` secara paralel (*slide-up animator*).
   - **Di Editor:** Aktif melayang saat membaca (View Mode). Saat masuk Edit Mode dan keyboard virtual (IME) naik, kapsul otomatis **fade-out** agar tidak mempersempit area pandang ketik pada HP berlayar ringkas, dan fade-in kembali saat keyboard turun.

---

### F. Redesain Anatomi Kartu Catatan (List & Grid)
1. **Eliminasi Tombol Sampah Inline:**
   - Tombol `item_delete` 48x48dp dihapus total dari layout kartu.
   - **Aksi Hapus di List:** Menggunakan gestur **Swipe-to-Delete** (geser kiri dengan indikator merah halus + ikon Lucide `trash-2` dan Snackbar Undo).
   - **Aksi Hapus di Grid:** Menggunakan **Long-Press** dengan haptic feedback dan dialog konfirmasi/opsi.
2. **Tipografi & Spacing Berjenjang:**
   - Judul: `15sp` SemiBold (`colorOnSurface`).
   - Margin atas: `4dp`.
   - Preview: `13sp` Regular (`colorOnSurfaceVariant` alpha 78%), `maxLines = 2`, `lineSpacingExtra = 2dp`.
   - Margin atas: `8dp`.
   - Meta (tanggal & karakter): `11sp` (`colorOnSurfaceVariant` alpha 60%).
3. **Stroke Halus Menggantikan Neon:**
   - Border kartu diganti dari `colorPrimary` menjadi `colorOutline` transparan (`alpha ~18%`), memberikan estetika lempengan kaca gelap (*slate glass*) yang tenang.
4. **Grid Adaptif (Content-Aware Height):**
   - Menghapus pembatasan kaku `minLines="2"` dan `minLines="3"`. Kartu menyesuaikan tinggi secara alami berdasarkan isi catatan tanpa ada ruang kosong artifisial.

---

### G. Dynamic Theme Accent pada Lambang Kuda NOTEZ
- Garis aksen kilat pada lambang kuda (`ic_notez_horse_splash_accent.png` dan `splash_glow`) serta garis bawah logo vektor (`ic_notez_fg.xml`) tidak lagi di-hardcode ke warna hijau `#39FF14`.
- Lapisan aksen diberi filter warna dinamis `PorterDuff.Mode.SRC_IN` berbasis tema aktif (`ThemePref.optionOf(theme).accentColorRes`).
- Garis kuda bertransformasi harmonis:
  - **GitHub Dark:** Hijau Neon (`#2EA043`).
  - **Cobalt2:** Cyan Neon / Electric Blue (`#00E5FF`).
  - **Gloomy Sakura Night:** Sakura Pink (`#FF79C6`).
  - **Gloomy Lavender:** Lavender Violet (`#BD93F9`).
  - **Cozy Earth / Choco Matcha:** Warm Matcha Gold.
  - **OLED / Notez You Dark:** High-contrast White/Violet.

---

## 3. Rencana Eksekusi & Tahapan Implementasi

| Fase | Cakupan Kerja | Target Berkas Terkait |
| :--- | :--- | :--- |
| **Fase 1** | **Home Refresh, Lucide Icons, & FAB Speed Dial**<br>• Konversi & impor aset Lucide XML.<br>• Pembersihan menu musik dari Drawer Home.<br>• Implementasi Top Bar Home (`panel-left` + `search`).<br>• Implementasi Speed Dial FAB (Catatan Baru & Buka File SAF).<br>• Tinting dinamis garis kuda splash & empty state. | `res/drawable/ic_lucide_*.xml`<br>`res/layout/activity_main.xml`<br>`MainActivity.kt` |
| **Fase 2** | **Redesain Kartu Catatan (List & Grid)**<br>• Hapus tombol sampah inline.<br>• Implementasi Swipe-to-Delete pada `RecyclerView` List.<br>• Refactor padding, margin berjenjang, dan outline glass.<br>• Hapus `minLines` kaku pada Grid item. | `res/layout/item_note.xml`<br>`res/layout/item_note_grid.xml`<br>`NoteAdapter.kt` |
| **Fase 3** | **Floating Mini Player (ArchiveTune Architecture)**<br>• Layout kapsul 56dp glass pill.<br>• Indikator progress melingkar & kontrol transportasi.<br>• Gestur swipe horizontal (skip track).<br>• Sinkronisasi translasi FAB di Home & IME fade di Editor. | `res/layout/layout_mini_player.xml`<br>`MusicPlaybackController.kt`<br>`MainActivity.kt`, `EditorActivity.kt` |
| **Fase 4** | **Editor Polish & Settings Lucide Refactor**<br>• Header Editor baru dengan Lucide `arrow-left` & pill counter.<br>• Refactor baris Settings dengan Lucide icons & animated chevrons.<br>• Poles kartu koleksi musik di Drawer Editor. | `res/layout/activity_editor.xml`<br>`res/layout/activity_settings.xml`<br>`EditorActivity.kt`, `SettingsActivity.kt` |

---

## 4. Evaluasi Risiko & Mitigasi

1. **Kompatibilitas HP Kelas Entri (Helio G50 / 3GB RAM):**
   - Seluruh animasi menggunakan `ValueAnimator` / `ObjectAnimator` bawaan Android dengan durasi singkat (120–180ms), tanpa library pihak ketiga yang membebani heap memori.
   - Vektor Lucide memiliki ukuran sangat kecil (~500 bytes per file) dan dirender hardware-accelerated.
2. **Integritas Uji Regresi (Regression Guards):**
   - Struktur data catatan, format penyimpanan Room, dan optimasi shell HTML (ADR sebelumnya) tidak mengalami perubahan skema data, menjaga stabilitas performa catatan besar (100 KB).

---

## 5. Addendum: Visual Polish & Mellow Atmospheric Aurora (v0.3.2)

1. **Eliminasi Siluet Hitam Topbar & Bottombar:**
   - Menghapus atribut `android:requiresFadingEdge="vertical"` dan `android:fadingEdgeLength="36dp"` dari `activity_main.xml` dan `activity_recycle.xml`.
   - Mengembalikan kejernihan scrolling daftar catatan tanpa bayangan hitam yang menghalangi konten di bawah header/footer.

2. **Universal Cosmic Polar Aurora (Mellow / Anti-Kolintang):**
   - Mengadopsi palet kosmik polar seragam yang tenang (`#00F5A0`, `#38BDF8`, `#8B5CF6`) dengan alpha diturunkan ke kisaran `56–70` dan radius difusi melebar (`0.50–0.85`).
   - Meniadakan efek garis tajam atau blok warna-warni berlebih ("xylophone/glockenspiel") pada kartu bertumpuk di tema gelap.

3. **Spesialisasi GitHub Dark (Monochromatic Slate Dev Glass):**
   - Menghilangkan spektrum aurora warna-warni pada tema GitHub Dark.
   - Menggunakan estetika developer slate monokromatik murni (`#161B22`, border `#30363D`) dengan titik micro-glow aksen biru `#58A6FF` tunggal pada sudut kanan atas kartu.

4. **Keterbacaan Tipografi Kontras Tinggi (Dark Mode):**
   - Judul Catatan: `#F0F6FC` (White Pearl crisp).
   - Pratinjau Teks: `#DAE2EA` (Silver Crisp).
   - Metadata / Tanggal: `#9EA7B0` (Soft Silver).

5. **Drawer Header & FAB Polish:**
   - Header drawer diperkaya dengan logo emblem kuda NOTEZ (`ic_notez_horse_splash_horse` 48dp), branding "Local-First Markdown", dan divider garis halus.
   - Elevasi FAB disesuaikan ke `4dp` (pressed `6dp`) agar menyatu harmonis dengan kedalaman clay glass kartu.

---

## 6. Addendum: Adaptive Chalkboard Header, 2.5x Horse Footer, Ultra-Compact Topbar, & Vibrant Slate Glass (v0.3.3)

1. **Universal Slate Dev Glass (Lebih Jreng & Fade Smooth):**
   - Mengadopsi arsitektur monokromatik slate dev glass GitHub Dark ke **seluruh 16 tema gelap**.
   - Setiap tema gelap memakai satu *micro-glow* aksen khas tema (`relX = 0.86`, `relY = 0.16`) dengan opasitas dinaikkan ke `alpha = 100` (*lebih jreng/hidup*) dipadu gradasi eksponensial 4-stop (`0f, 0.35f, 0.70f, 1f`) untuk pembauran yang sangat halus (*fade smooth*, anti-kolintang/pelangi).
   - Border kartu menggunakan `outlineColor` tema dipadu garis kilap *specular highlight* putih (`alpha 150`) dan campuran aksen primer di sisi atas.

2. **Adaptive Chalkboard Drawer Header (.GIF - Opsi B):**
   - Menggunakan animasi tulisan tangan spidol/kapur `notez_rough` yang disesuaikan secara dinamis dengan 6 kelompok tema (*Dark Slate, Dark Indigo, Dark Purple, Dark Green, Dark Berry, dan Light Paper*).
   - Durasi tulis: **2,5 detik** (64 frame pada 34ms).
   - Ujung kapur/pena aktif menyala dengan pendaran warna aksen khas tema saat menggores huruf.
   - Perilaku Putar: **One-shot** (memutar sekali lalu menetap pada frame final NOTEZ); jika drawer ditutup dan dibuka kembali, animasi langsung dimulai ulang dari frame awal via `onDrawerOpened`.
   - Tata Letak Header: Tanpa *corner radius* (*edge-to-edge*, `layout_width="match_parent"`, `layout_height="140dp"`), memenuhi lebar header drawer 280dp secara mulus.

3. **Drawer Footer Emblem Kuda Diperbesar 2.5x:**
   - Logo kuda kecil 48dp di header atas dihapus.
   - Logo emblem kuda NOTEZ di footer bawah diperbesar **2,5 kali** (dari `64dp` menjadi **`160dp`**), lengkap dengan layer glow pendaran aksen dinamis (`drawer_footer_glow`, `drawer_footer_horse`, `drawer_footer_accent`).

4. **Ultra-Compact Topbar Home (44dp):**
   - Ketinggian topbar Home dipangkas menjadi **`44dp`** (menggantikan 52dp/48dp).
   - Tombol drawer dan pencarian disesuaikan ke `40dp x 40dp` (padding `8dp`), judul "NOTEZ" `17sp` tebal, dan `search_pill` ramping `36dp` (corner radius `18dp`).
   - Tanpa *bottombar* tambahan, memaksimalkan area tampilan daftar catatan.


