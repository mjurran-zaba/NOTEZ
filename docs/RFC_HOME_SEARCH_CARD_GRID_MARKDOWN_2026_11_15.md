# RFC — Home Empty Search, Card List/Grid, dan Kelengkapan Markdown

| Field | Nilai |
|---|---|
| Tanggal | 2026-11-15 (UTC) |
| Status | **DESIGNED — arah tiga jalur disetujui; dokumen rencana, belum implementasi** |
| Baseline checkout | `6aba90806ab29665497fb0de3683e0f0103e33a6` |
| Branch sesi | `arena/91eada52-notez` |
| Versi baseline | Konfigurasi aplikasi `0.1.7`, versionCode `8` |
| Target versi rilis | Belum ditentukan; tidak menaikkan versi lewat RFC ini |
| Pemilik keputusan produk | Zaqi |
| Deliverable sekarang | Rancangan dan rencana kerja Markdown; bukan patch aplikasi |
| Revisi | 2026-11-16 — klarifikasi §3.1 (akar kapsul tanpa klaim tinggi), §6.1 (drawer custom view), §6.4 (ownership spacing), §7.2 dan §3.3 (batas klaim capability), §8.1 (initial toolbar `GONE` di XML) |

> **Ringkasan approval**
>
> Pemilik produk menyetujui arah: (1) merapikan search saat Home kosong,
> (2) pilihan Card List/Card Grid di drawer Home, dan (3) audit serta
> pelengkapan Markdown dengan mengecualikan capability yang riskan.
> Detail di bawah membedakan **kontrak yang dipertahankan**, **rancangan
> rekomendasi**, dan **keputusan yang masih membutuhkan bukti/konfirmasi**.
> Persetujuan arah bukan persetujuan otomatis memasang dependency atau
> membuka seluruh kandidat fitur renderer.

---

## Daftar isi

1. Tujuan dan batas pekerjaan
2. Dasar pembacaan dan sumber kebenaran
3. Temuan baseline
4. Kontrak yang wajib dipertahankan
5. Jalur A — Search saat Home kosong
6. Jalur B — Card List / Card Grid
7. Jalur C — Audit dan kelengkapan Markdown
8. Rancangan teknis dan peta file
9. Tahapan implementasi dan dependensi
10. Strategi pengujian dan mutation
11. Matriks UAT perangkat
12. Risiko dan rollback
13. Gate penyelesaian dan rilis
14. Keputusan terbuka
15. Langkah berikutnya

---

## 1. Tujuan dan batas pekerjaan

### 1.1 Tujuan teramati

- Home tanpa catatan tidak menyisakan kapsul search tipis tanpa fungsi yang jelas.
- Pengguna dapat memilih List atau Grid dari drawer Home, tanpa mengubah data catatan.
- Markdown dilengkapi berdasarkan gap yang dibuktikan, bukan daftar fitur pemasaran.
- Input, autosave, Reading View, pencarian, hapus/undo, dan backup tetap aman.
- Perubahan dibuat kecil dan dapat diuji serta dibatalkan secara terpisah.

### 1.2 Non-goals

Tidak termasuk dalam RFC ini:

- redesign total Home, Editor, Settings, status bar, dialog tema, atau Music;
- mengganti ikon mata dengan kaca pembesar;
- menambahkan hamburger, bottom navigation, drawer hint, atau onboarding;
- mengganti native EditText menjadi live preview/CodeMirror/block editor;
- merombak preview teks pada kartu menjadi hasil render Markdown;
- Space Workspace, file explorer, tag, pin, archive, real Trash;
- perubahan Room schema atau format backup;
- pembenahan ownership tick Music;
- penghapusan `BRANCHING.md` sekarang: permintaan pengguna adalah **nanti**;
- refactor folder repo secara luas;
- publish, merge, tag, atau release otomatis.

### 1.3 Prinsip pembatasan scope

Ketiga jalur dapat direncanakan bersama, tetapi bukan satu patch besar.
Home dan renderer memiliki risiko berbeda. Kegagalan tambahan Markdown
harus bisa dibatalkan tanpa mencabut List/Grid yang sudah lolos pengujian.

---

## 2. Dasar pembacaan dan sumber kebenaran

### 2.1 Konteks yang sudah dibaca

Seluruh 38 file di `docs/` baseline telah dibaca sebelum RFC ini dibuat,
termasuk 35 Markdown dan 3 fixture JSON. Source yang diperiksa secara terarah:

- `MainActivity.kt`, `NoteAdapter.kt`, `NoteDao.kt`, `UiStyle.kt`;
- `activity_main.xml`, `item_note.xml`, layout Editor dan Music;
- bagian pipeline `MarkdownPreviewRenderer.kt`;
- konfigurasi aplikasi dan `.github/workflows/build.yml`.

**Batas:** ini bukan audit runtime menyeluruh renderer. Inventaris kapabilitas
pada jalur C masih perlu dipetakan ke implementasi aktif dan diuji per skenario.
Screenshot pengguna merupakan bukti visual, bukan bukti latency, lifecycle,
aksesibilitas, atau keberhasilan seluruh interaksi.

### 2.2 Dokumen terkait

| Dokumen lokal | Relevansi |
|---|---|
| [Home/Search/Theme Polish](RFC_HOME_SEARCH_THEME_POLISH_2026_09_29.md) | Mata sebagai identitas; visibilitas mengikuti total catatan |
| [Home Empty State](RFC_HOME_EMPTY_STATE_ABOUT_NOTEZ_V1_2026_09_29.md) | Welcome dan search no-result dibedakan |
| [FAB Floating](RFC_HOME_FAB_FLOATING_CLEANUP_2026_09_27.md) | FAB overlay; pertahankan trash + undo |
| [Theme System v2](RFC_THEME_SYSTEM_V2_APPEARANCE_POLISH_2026_09_28.md) | Curated themes, preference compatibility, readability |
| [Preview v2](RFC_MARKDOWN_PREVIEW_V2_GITHUB_OBSIDIAN_2026_09_26.md) | Native edit / local WebView read separation |
| [Preview v3](RFC_MARKDOWN_PREVIEW_V3_SAFE_HTML_SYNTAX_2026_09_29.md) | Safe HTML dan local syntax highlighting |
| [Preview v4](RFC_MARKDOWN_PREVIEW_V4_MARKDOWN_MAX_2026_09_29.md) | Highlight, sub/sup, footnote, definition list |
| [Raw HTML + Optional Cache](RFC_RAW_HTML_COMPAT_P1_OPTIONAL_ONLINE_CACHE_2026_09_29.md) | Kontrak online user-triggered yang lebih baru |
| [RFC v0.1.6](RFC-NOTEZv0.1.6-.md) | Pelajaran renderer fork blank, scope Space belum seluruhnya tersedia |
| [Release v0.1.7](RELEASE_NOTES_V0.1.7.md) | Kondisi rilis terdokumentasi yang lebih baru |

### 2.3 Cara menangani sejarah dokumen

- Status lama seperti “belum CI” tidak otomatis menggambarkan kondisi rilis terbaru.
- Larangan INTERNET historis telah direvisi untuk image cache user-triggered.
- Daftar rencana bukan bukti bahwa fiturnya tersedia.
- Contoh skrip di panduan bukan perintah yang boleh dieksekusi otomatis.
- Fakta source, hasil tes, laporan perangkat, dan keputusan produk diberi label terpisah.
- Perubahan eksternal teknis baru perlu sumber primer/version-specific; RFC ini tidak
  mengklaim riset dependency kandidat sudah dilakukan.

---

## 3. Temuan baseline

### 3.1 Home search

`MainActivity.onCreate()` memasang `UiStyle.roundedBackground(...)` ke
`home_toolbar` tanpa memeriksa apakah database kosong.

`updateSearchAvailability()` mengatur mata berdasarkan `hasAnyNotes`.
Saat ditutup, `search_input` memakai `INVISIBLE`, sehingga tetap menyumbang
ukuran layout. Ketika mata `GONE`, parent yang masih memiliki background
tetap membentuk kapsul kosong dengan tinggi hasil pengukuran child, bukan
hanya menyisakan stroke tanpa area.

Ada dua pengamatan data yang berbeda:

- observer total notes: menentukan apakah pencarian tersedia;
- observer hasil/query: mengisi adapter dan empty state hasil pencarian.

Jangan memakai `adapter.itemCount == 0` untuk menyimpulkan database kosong.
Query yang tidak cocok juga menghasilkan nol item.

### 3.2 Daftar kartu

- RecyclerView saat ini memakai `LinearLayoutManager`.
- Adapter memakai `item_note.xml` dengan judul, preview, metadata, serta trash 48dp.
- Cuplikan saat ini berasal dari dua baris source pertama, digabung dan dibatasi 120 karakter.
- `NoteDao` mengurutkan berdasarkan `updatedAt DESC`; mode tampilan tidak boleh menulis ulang timestamp.
- `NoteAdapter.submit()` saat ini memakai `notifyDataSetChanged()`.
- Styling kartu sudah dipengaruhi `UiStyle` dan binding adapter.

### 3.3 Markdown

Dokumen dan source terarah menunjukkan baseline berisi markdown-it lokal,
raw HTML allowlist, sanitizer, image placeholder/cache, heading anchors,
table wrappers, task-list enhancement, callout, highlight sederhana,
footnotes, definition list, serta code enhancement.

**Tidak boleh disimpulkan:** seluruh kombinasi, nesting, input malformed,
atau performa dokumen besar sudah lulus hanya karena fitur tersebut ada.

Inventaris awal berdasarkan dokumentasi dan inspeksi source terarah; dukungan
penuh, edge case, keamanan runtime, dan performa belum diverifikasi oleh audit ini.

---

## 4. Kontrak yang wajib dipertahankan

### 4.1 Pengalaman utama

- Mata sebagai identitas search; swipe drawer tetap.
- Welcome kuda, copy, Panduan NOTEZ, dan FAB tetap tersedia.
- FAB tetap mengambang; tidak menambah baris bawah palsu.
- Trash tetap discoverable, muted, dan mendukung undo yang sama.
- Catatan tetap Room-backed; ekspor file bukan migrasi Space.
- Editor native; Reading View lokal; autosave tidak dirombak.

### 4.2 Keamanan dan privasi

- Tidak ada request hanya karena note mengandung URL.
- Gambar remote melalui placeholder → konfirmasi domain → native download → cache lokal.
- Tidak ada remote JS/CSS/font/iframe atau auto-fetch tambahan.
- Tidak ada `addJavascriptInterface`.
- Tidak membuka arbitrary filesystem/content URI dari HTML catatan.
- Raw user `style`, `class`, `id`, dan handler event tidak menjadi capability bebas.
- HTML/JS pada contoh tes tetap data, bukan instruksi untuk dijalankan.

### 4.3 Data dan kompatibilitas

- Tidak mengubah Room schema/JSON backup.
- Tidak mengubah note ID, urutan query, source body, maupun timestamp saat pindah tampilan.
- Preference mode tampilan terpisah dari preference tema.
- Unknown preference fallback ke List, tanpa crash atau menghapus catatan.

---

## 5. Jalur A — Search saat Home kosong

### 5.1 Rancangan rekomendasi

**Database benar-benar kosong: sembunyikan seluruh `home_toolbar` dengan `GONE`.**
Bukan hanya mata, bukan kapsul transparan yang masih mengambil tinggi.

Alasan:

- tidak menyisakan outline tanpa aksi;
- tidak menambah pesan/hint baru;
- mempertahankan konsep search ketika catatan tersedia;
- perubahan terbatas pada state tampilan Home.

Ini rekomendasi konkret dari approval arah. Jika pengguna ternyata ingin
kapsul tetap terlihat, detail ini perlu direvisi sebelum implementasi A.

### 5.2 State table

| Total catatan | Query/hasil | Toolbar | Mata | Konten utama |
|---|---|---|---|---|
| Belum diketahui saat load | Belum diketahui | Awal `GONE` untuk mencegah flash kapsul | Tidak interaktif | Ikuti loading baseline; jangan tampilkan welcome palsu baru |
| 0 | Query kosong | `GONE` | Tersembunyi | Welcome NOTEZ |
| 0 | Query masih tersisa | Tutup/reset search, lalu `GONE` | Tersembunyi | Welcome setelah state konsisten |
| > 0 | Search tertutup | Tampilan baseline | Terlihat | List/Grid |
| > 0 | Query ada hasil | Tampilan baseline | Terlihat | Hasil List/Grid |
| > 0 | Query tidak ada hasil | Tetap terlihat | Terlihat | Search no-result, bukan welcome |

### 5.3 Transisi penting

- Catatan pertama dibuat → toolbar dan mata tersedia setelah total notes teramati.
- Catatan terakhir dihapus → search ditutup/reset, keyboard ditutup bila aktif, toolbar hilang.
- Undo catatan terakhir → toolbar kembali tanpa mereset pilihan List/Grid.
- Query no-result → toolbar tidak hilang.
- Kembali dari Editor/Settings dan recreate tema → hitung state dari sumber yang benar.

### 5.4 Implementasi yang disarankan

- Gunakan satu fungsi render state search yang menerima state total-note presence.
- Derivasi visibilitas toolbar dan mata dari sumber yang sama.
- Jangan menambah observer database ketiga untuk menyelesaikan masalah visual.
- Hindari reset query berulang yang memicu pengumpulan flow secara tidak perlu.
- Tangani urutan emisi observer total dan query; tampilan akhir tidak boleh tersangkut pada welcome/no-result yang salah.
- Jangan mengubah styling search berisi catatan, focus, atau keyboard flow di luar kebutuhan empty transition.

### 5.5 Acceptance criteria A

- [ ] Tidak ada outline search/kapsul tipis saat total notes nol.
- [ ] Tidak ada ruang tinggi toolbar tersembunyi yang tidak disengaja.
- [ ] Welcome tetap proporsional setelah ruang atas berubah; portrait/landscape diuji.
- [ ] Mata kembali setelah ada catatan.
- [ ] Query no-result tidak menyembunyikan search.
- [ ] Delete terakhir/undo/rotasi/recreate tema tidak meninggalkan UI stale.
- [ ] Tidak menambah navigasi/hint/onboarding atau mengubah ikon mata.

---

## 6. Jalur B — Card List / Card Grid

### 6.1 Information architecture

Tambahkan kelompok tampilan **hanya di drawer Home**, di antara menu utilitas
Home dan Music. Editor tidak mendapat kelompok ini.

```text
NOTEZ

Panduan Markdown
Pengaturan
Diagnostics

Tampilan catatan
  (●) List
  ( ) Grid

Music
  ...
```

Notasi radio di atas adalah wireframe, bukan karakter font untuk implementasi.

Kelompok tampilan ditambahkan sebagai custom view pada `activity_main.xml`,
sebelum include Music. Gunakan kontrol native single-selection dengan label yang
bisa ditap, checked state, dan semantics radio yang terbaca TalkBack; tap seluruh
baris memilih mode, tanpa menghasilkan dua target aksesibilitas yang duplikat.
Selected state visual saja belum cukup.

### 6.2 Perilaku pilihan

| Aspek | Kontrak rancangan |
|---|---|
| Default install/upgrade tanpa preference | List |
| Persistensi | Simpan mode yang dipilih, berlaku setelah restart/recreate |
| Pilihan saat Home kosong | Tetap tersedia di drawer; tidak menciptakan catatan |
| Tap mode berbeda | Terapkan segera, drawer tetap terbuka agar pilihan terlihat |
| Tap mode yang sama | No-op; tidak rebuild adapter/layout yang tidak perlu |
| Search aktif | Query dan hasil dipertahankan |
| Urutan | Tetap `updatedAt DESC` |
| Tema | Palet tetap mengikuti tema aktif |
| Editor | Tidak ada perubahan mode edit/baca |
| Scroll | Pertahankan note jangkar teratas bila masih ada; fallback posisi valid terdekat |

### 6.3 Model preference

Rancangan kecil dan version-tolerant:

```kotlin
enum class NoteDisplayMode { LIST, GRID }

// Konsep API, bukan patch siap-tempel.
object HomeDisplayPref {
    fun get(context: Context): NoteDisplayMode
    fun set(context: Context, mode: NoteDisplayMode)
}
```

- Simpan string stabil `list` / `grid`, bukan ordinal enum.
- Nilai null/tidak dikenal → `LIST`.
- Tidak memasukkan preference ini ke JSON backup catatan tanpa keputusan terpisah.
- Clear app data mereset ke List; restart normal tidak.

### 6.4 Layout Grid

**Rekomendasi awal: grid reguler, bukan staggered/masonry.**

```text
┌─────────────────┐  ┌─────────────────┐
│ Judul catatan   │  │ Judul catatan   │
│ maksimal 2 baris│  │ maksimal 2 baris│
│                 │  │                 │
│ Cuplikan source │  │ Cuplikan source │
│ maksimal 3 baris│  │ maksimal 3 baris│
│                 │  │                 │
│ Tanggal         │  │ Tanggal         │
│ Jumlah karakter │  │ Jumlah karakter │
│             [🗑]│  │             [🗑]│
└─────────────────┘  └─────────────────┘
```

Simbol trash di wireframe hanya penanda lokasi; produksi memakai drawable
`ic_notez_trash`, bukan emoji.

Spesifikasi awal yang perlu dicocokkan di HP:

| Elemen | Rancangan |
|---|---|
| Kolom portrait HP | Target 2 jika lebar efektif memadai |
| Kolom maksimum fase pertama | 3 pada ruang lebih lebar; tidak diasumsikan dari orientasi saja |
| Lebar minimum kartu awal | 160dp efektif pada font normal; angka rancangan, bukan hasil ukur |
| Font besar/ruang sempit | Turun ke 1 kolom bila minimum lebar yang diuji tidak tercapai |
| Jarak antarkartu | Target 8dp sebagai jarak akhir antarkartu, bukan margin 8dp per kartu; dimiliki satu `ItemDecoration` |
| Padding isi | Target 12dp, mengikuti karakter kartu baseline |
| Judul | 16sp bold, maksimal 2 baris, ellipsize end |
| Preview | 13sp, maksimal 3 baris, source excerpt baseline dipertahankan |
| Metadata | 11–12sp sebagai titik awal; tanggal dan jumlah karakter tidak hilang |
| Trash | Area sentuh 48×48dp, terpisah dari metadata, bukan overlay judul |
| Tinggi | Seragam per baris pada font/config yang sama, tanpa tinggi pixel tetap |

**Trade-off:** grid menampilkan lebih banyak kartu berdampingan, bukan lebih
banyak teks per kartu. Nama panjang dapat terpotong; isi lengkap tetap di Editor.
Jangan mengecilkan font atau target sentuh demi memaksakan dua kolom.

Kepemilikan spacing:

- **List:** pertahankan margin baseline `item_note.xml`.
- **Grid:** `item_note_grid.xml` tanpa margin luar kartu.
- **Spacing antar-kartu Grid:** dimiliki satu `ItemDecoration`.
- **Inset tepi daftar:** dimiliki padding RecyclerView, dihitung bersama dekorasi.
- Saat switch mode, decoration lama dilepas/diganti—jangan ditambahkan berulang.

### 6.5 Pemilihan jumlah kolom

- Hitung dari lebar RecyclerView yang sudah diukur, kurangi padding dan spacing.
- Bandingkan dengan minimum lebar kartu yang telah diuji.
- Batasi 1–3 untuk fase pertama.
- Untuk font besar, gunakan ambang yang lebih konservatif berdasarkan UAT;
  jangan menganggap dp saja menjamin teks muat.
- Hitung ulang saat ukuran window/orientasi berubah, bukan di setiap frame/binding.
- Mode preference tetap `grid` meskipun ruang saat itu hanya cukup satu kolom.

### 6.6 Interaksi dan aksesibilitas

- Tap body kartu → buka note yang benar.
- Tap trash → hapus note itu saja, tidak ikut membuka Editor.
- Undo tetap mekanisme existing; tidak menambah confirm/long-press-only.
- Radio selection terbaca oleh TalkBack; jangan hanya mengandalkan warna.
- Traversal grid: kiri ke kanan, lalu baris berikutnya, sesuai urutan visual.
- Jangan menjadikan content description kartu satu teks panjang yang menghilangkan akses trash.
- Bottom padding transparan cukup agar kartu terakhir dapat digeser melewati FAB.
- Switch mode tidak membuat note baru, memicu save, atau mengubah `updatedAt`.

### 6.7 Menjaga posisi dan identitas

Saat pindah mode:

1. Rekam ID note pertama yang terlihat dan offset yang masuk akal.
2. Ganti presentasi/layout manager pada main thread.
3. Setelah layout baru siap, temukan ID yang sama dalam dataset terkini.
4. Pulihkan posisi; jika note hilang, clamp ke posisi terdekat yang valid.
5. Batalkan/abaikan pemulihan basi jika dataset atau mode kembali berubah.

Jangan memindahkan state mentah LinearLayoutManager ke GridLayoutManager
seolah keduanya kontraknya sama. Saat query berubah, scroll-to-top boleh
mengikuti baseline; switch tampilan sendiri tidak boleh menghapus query.

### 6.8 Acceptance criteria B

- [ ] Pilihan List/Grid berada di drawer Home saja dan dapat dipilih satu per satu.
- [ ] List tetap default dan tampilan baseline tidak diregresikan.
- [ ] Pilihan tersimpan setelah restart, rotasi, dan ganti tema.
- [ ] Grid adaptif; tidak ada judul/metadata/trash saling menimpa.
- [ ] Teks besar tidak dipaksa masuk dua kolom dengan font diperkecil.
- [ ] Query, hasil, urutan, note ID, source body, timestamp, dan data tetap sama.
- [ ] Tap kartu/trash benar di kedua mode setelah delete/undo/switch cepat.
- [ ] Posisi scroll dipertahankan secara best effort berbasis identitas note.
- [ ] Kartu terakhir dapat diakses tanpa tertutup FAB secara permanen.
- [ ] Unknown preference fallback aman; Editor/Music/backup tetap normal.

---

## 7. Jalur C — Audit dan kelengkapan Markdown

### 7.1 Definisi “lengkap”

Bukan janji kompatibel dengan seluruh GitHub/Obsidian atau semua dialect.
Targetnya adalah daftar kapabilitas yang eksplisit, teruji, aman, dan berguna
untuk catatan pengguna. Fitur ditambahkan berdasarkan nilai dan bukti.

Empat jenis pekerjaan dipisahkan:

1. **Syntax/compatibility:** parser dan hasil format.
2. **Reader convenience:** copy code, outline, navigasi.
3. **Document/workspace:** resolusi file lokal, wikilink, backlink.
4. **Active capability:** script, embed, jaringan, akses file.

Tidak semuanya boleh disebut “tinggal menambah Markdown”.

### 7.2 Inventaris awal — belum verdict audit

| Area | Baseline terdokumentasi/source terarah | Hal yang perlu dibuktikan |
|---|---|---|
| Heading/paragraf/emphasis/list/quote | Tersedia | Nesting, escaping, malformed input, Unicode |
| GFM table/alignment | Tersedia | Pipe escaped, inline code, tabel sangat lebar, scroll terisolasi |
| Task list | Tampilan baca/view-only | Nested tasks dan tidak memodifikasi source |
| Fenced/inline code | Tersedia | Fence panjang, literal syntax di dalam kode, overflow, header tetap |
| Syntax highlighting | Lokal ringan untuk label umum | Unknown language fallback, HTML escaping, large blocks |
| Link/autolink/anchor | Tersedia dengan policy URL | Duplicate heading, Unicode slug, encoded/malicious schemes |
| Callout | Lima jenis GitHub-style terdokumentasi | Multiline, nested quote, jenis tak dikenal, code literal |
| Highlight/sub/sup | Rule lokal sederhana | Delimiter escape, benturan strikethrough, nesting, whitespace |
| Footnote | Implementasi lokal | Multiline, ref berulang, label tak dikenal, fenced literal, backlink |
| Definition list | Preprocess lokal | Definisi multiline, beberapa term, code literal |
| Safe HTML | Allowlist diperluas | Atribut, nested/malformed HTML, comment, URL obfuscation |
| Gambar remote | Placeholder + konfirmasi/cache | Cancel/error, cache missing, MIME/size, relative path context |
| Gambar lokal | Belum tersedia menurut docs | Model attachment dan izin perlu RFC terpisah |
| Mermaid/math | Di luar scope terdahulu | Kelayakan lokal, security, lisensi, biaya perangkat belum diteliti |
| Wikilink/backlink | Belum tersedia menurut docs | Resolusi identitas dokumen/workspace belum dirancang |

Label “tersedia” di sini tidak setara dengan “semua edge case verified”.

Inventaris awal berdasarkan dokumentasi dan inspeksi source terarah; dukungan
penuh, edge case, keamanan runtime, dan performa belum diverifikasi oleh audit ini.

### 7.3 Output audit wajib

Buat laporan terpisah, kandidat nama:

`docs/AUDIT_MARKDOWN_CAPABILITIES_2026_11_15.md`

Jika audit dilakukan pada tanggal lain, gunakan tanggal pelaksanaan sebenarnya.

Setiap entri berisi:

| Field | Isi wajib |
|---|---|
| ID | Mis. `MD-TABLE-001` |
| Kebutuhan | Contoh penggunaan yang ingin didukung |
| Dialect/acuan | CommonMark, GFM, atau extension NOTEZ; versi/link primer |
| Input minimal | Source yang dapat direproduksi |
| Expected | HTML/DOM/interaksi yang diharapkan |
| Actual | Hasil yang benar-benar diamati |
| Jalur aktif | File/fungsi/parser rule/enhancer terkait |
| Klasifikasi | Supported / partial / missing / intentionally restricted / unverified |
| Risiko | Security, CPU/RAM, APK, maintenance, compatibility |
| Keputusan | Fix / add / defer / reject beserta alasan |
| Bukti | Tes, mutasi, browser/host/device, artifact/versi |

Audit tidak boleh mengubah `unverified` menjadi `missing` tanpa menguji,
atau mengubah `missing` menjadi komitmen implementasi otomatis.

### 7.4 Urutan prioritas

**C0 — Inventaris dan reproduksi**

- Identifikasi versi markdown-it dari asset dan catatan lisensi yang benar-benar ada.
- Peta parser → sanitizer → enhancer → WebView → native navigation/cache.
- Cocokkan guide/fixtures dengan implementasi aktif.
- Kumpulkan contoh pengguna selain fixture sintetis, tanpa memasukkan isi privat ke repo.

**C1 — Perbaiki fitur yang sudah dijanjikan**

- Utamakan bug compatibility yang reproducible.
- Tambah permanent regression guard dan buktikan red → green.
- Tangani kelas masalah, misalnya fence handling lintas preprocessors,
  bukan hanya satu contoh yang kebetulan gagal.
- Jangan mengklaim parser sederhana setara plugin upstream penuh tanpa bukti.

**C2 — Tambahan lokal berisiko rendah**

- Pilih setelah C0/C1: sintaks berguna yang tidak membuka network/file/script.
- Tentukan grammar dan benturan delimiter sebelum menulis rule.
- Reader convenience, misalnya copy code, ditandai terpisah dari syntax.
- Tidak otomatis menambahkan semua kandidat sekaligus.

**C3 — Studi kelayakan fitur berat**

Math/LaTeX dan Mermaid dapat dievaluasi, bukan otomatis disetujui atau ditolak.
Untuk masing-masing perlu keputusan terpisah:

- library/version/paket tepat, lisensi, maintenance, dependency dan install hooks;
- asset seluruhnya lokal; tidak ada CDN/runtime download;
- security mode, link/click handler, HTML label, URL dan fitur aktif dibatasi;
- batas ukuran/kompleksitas input serta proteksi agar UI tidak terjebak render;
- ukuran APK, startup, CPU/RAM di perangkat sasaran;
- fallback source/code yang tetap dapat dibaca bila gagal/tidak didukung;
- apakah sandbox render sungguh memberikan isolasi atau hanya defense-in-depth;
- bukti target WebView, bukan hanya demo desktop.

Jika biaya lebih besar dari manfaat atau kontrol risikonya tidak cukup,
fitur tetap dibatasi dan limitasi dijelaskan dengan jujur.

### 7.5 Capability yang tetap ditolak dalam scope ini

- Arbitrary JavaScript dalam note.
- Iframe/embed web bebas, autoplay audio/video.
- Remote scripts/styles/fonts atau auto-fetch gambar.
- CSS bebas yang dapat menutupi UI/mengubah batas interaksi.
- Akses file/content URI arbitrer dari raw HTML.
- Raw SVG aktif tanpa kebijakan sanitizer/rendering terpisah.
- Plugin/theme executable yang diunduh pengguna.

Penolakan ini tidak melarang contoh kode tampil sebagai teks. Contoh
`<script>` harus tetap bisa didokumentasikan secara inert.

### 7.6 Performa dan kegagalan

- Ukur baseline cold/warm render untuk dokumen pendek, sedang, panjang.
- Laporkan input size, device, Android/WebView, build dan kondisi tes.
- Tetapkan anggaran numerik setelah pengukuran; jangan mengarang target ms/MB sekarang.
- Render gagal tidak boleh menghilangkan source asli.
- Jangan menghidupkan eksperimen pool/shell reuse dari panduan lama sebagai bagian diam-diam dari fitur syntax.
- Jika library tidak dapat dibatasi/cancel dengan aman, desain ownership/fallback terpisah diperlukan sebelum integrasi.

### 7.7 Dokumentasi dan fixture

Setiap batch renderer yang diterima harus memperbarui:

- Panduan Markdown lokal: cara tulis, contoh, batasan;
- fixture regression otomatis;
- fixture impor UAT bila berguna, dengan ID nonkonflik yang ditetapkan saat dibuat;
- matriks capability;
- hasil verifikasi per artifact;
- release notes hanya bila benar-benar masuk kandidat rilis.

Jangan menimpa fixture historis v2/v3/v4 hanya agar tampak cocok dengan renderer terbaru.

### 7.8 Acceptance criteria C

- [ ] Audit mencakup fitur aktif, parsial, belum tersedia, pembatasan, dan unverified.
- [ ] Setiap fitur baru terpilih memiliki grammar, acceptance criteria, non-goals, dan risiko.
- [ ] Source note tidak berubah akibat render.
- [ ] Contoh syntax di fenced/inline code tetap literal.
- [ ] Sanitizer dan URL policy tidak dilonggarkan diam-diam.
- [ ] Tidak ada request otomatis atau dependency baru tanpa review/approval.
- [ ] Bug fix memiliki guard yang terbukti gagal pada bug yang dimaksud.
- [ ] Browser proof tidak diklaim sebagai Android/device proof.
- [ ] Guide, capability matrix, dan hasil UAT sesuai artifact yang diuji.

---

## 8. Rancangan teknis dan peta file

### 8.1 Home

| File | Perubahan rencana |
|---|---|
| `MainActivity.kt` | State empty toolbar, wiring pilihan mode, layout manager, scroll anchor |
| `NoteAdapter.kt` | Presentasi berdasarkan mode dan binding identitas yang aman |
| `activity_main.xml` | Initial toolbar state dan pilihan mode di drawer Home |
| `item_note.xml` | Pertahankan baseline List; ubah hanya jika dibutuhkan oleh shared contract |
| `item_note_grid.xml` (baru) | Susunan Grid terpisah agar List tidak dipenuhi conditional layout |
| `HomeDisplayPref.kt` (baru) | Persist mode dan unknown-value fallback |
| `HomeDisplayPolicy.kt` (opsional baru) | Fungsi murni mode/kolom/state agar mudah diuji |
| `values/strings.xml` | Label dan deskripsi aksesibilitas baru |
| `values/dimens.xml` (jika diperlukan) | Hanya dimensi baru yang benar-benar dipakai bersama |

`home_toolbar` diberi `android:visibility="gone"` di XML sebagai initial state.
Runtime baru menampilkannya setelah total catatan diketahui lebih dari nol, untuk
mencegah flash kapsul sebelum observer pertama. Konsekuensinya: pada database
berisi catatan, toolbar baru muncul setelah emisi pertama. Itu trade-off yang kita
pilih untuk state awal—bukan janji bahwa seluruh layout pasti bebas pergeseran.

Rekomendasi: gunakan satu adapter dengan view type sesuai mode dan dua layout,
bukan dua dataset atau dua DAO. Jika memakai stable IDs, implementasikan dan
uji konsistensinya; jangan sekadar mengaktifkan flag.

`DiffUtil/ListAdapter` hanya dipertimbangkan bila diperlukan untuk update/anchor
correctness; bukan refactor wajib demi label “rapi”. Mengubah adapter ownership
lebih luas harus dijelaskan pada rencana batch sebelum eksekusi.

### 8.2 Markdown

| File/area | Perubahan rencana |
|---|---|
| `markdown/MarkdownPreviewRenderer.kt` | Audit jalur aktif dahulu; patch terfokus setelah kandidat dipilih |
| `markdown/RemoteImageCache.kt` | Review batas integrasi; tidak diubah tanpa kebutuhan dan persetujuan |
| `assets/markdown/THIRD_PARTY_MARKDOWN.md` | Verifikasi pin/license; update hanya jika asset berubah |
| `assets/help/markdown_guide.md` | Sesuaikan dengan fitur yang terbukti tersedia |
| `app/src/test/...` | Unit/structural guards yang benar-benar terdaftar di command CI |
| `docs/AUDIT_MARKDOWN_CAPABILITIES_*.md` | Temuan dan keputusan audit |
| `docs/UAT_HOME_LIST_GRID_*.md` | Checklist serta hasil Home per build |
| `docs/UAT_MARKDOWN_COMPLETENESS_*.md` | Skenario renderer terpilih per build |

### 8.3 Area yang dijaga tidak berubah

Room entities/schema, backup format, theme IDs, playback service, package/app ID,
release signing, branch sesi, dan permission manifest tidak berubah lewat pekerjaan Home.

---

## 9. Tahapan implementasi dan dependensi

| Tahap | Pekerjaan | Output/gate |
|---|---|---|
| P0 | Review RFC, konfirmasi detail visual dan kandidat audit | Kontrak cukup jelas; belum patch |
| A1 | Guard state search kosong, implementasi toolbar | Focused red → green, static diff/XML |
| A2 | CI dan UAT search empty | Bukti layout dan transisi perangkat |
| B1 | Mode model/preference + unit test | Default/persist/fallback benar |
| B2 | Layout Grid + adapter/layout manager + pilihan drawer | UI implemented; data tidak berubah |
| B3 | Hardening scroll/filter/delete/undo/rotation | Guard, mutation, CI dan UAT |
| C0 | Audit renderer lengkap dan klasifikasi gap | Capability report; pilih batch berikutnya |
| C1 | Fix satu kelas bug yang terbukti | Fixture red → green, regression suite |
| C2 | Tambah fitur lokal terpilih | Per-batch review risiko dan approval bila kontrak berubah |
| C3 | Studi fitur berat bila dibutuhkan | RFC/keputusan terpisah; bukan janji implementasi |
| F | Review keseluruhan dan laporan | Bukti akhir, batasan, rollback, next step |

Dependensi:

```text
P0 → A1 → A2
P0 → B1 → B2 → B3
P0 → C0 → pilih kandidat → C1/C2
                         └→ studi C3 bila layak
A2 + B3 + batch C yang benar-benar dipilih → regression final
```

C0 berupa pembacaan/eksperimen terisolasi dapat dilakukan independen dari
Home. Jangan menulis file renderer yang sama secara paralel. Jangan menjalankan
beberapa tooling berat bersamaan tanpa mengukur resource.

Commit direncanakan sebagai unit reversible: search, preference, grid UI,
hardening, audit docs, dan masing-masing kelas perbaikan renderer.
Tidak perlu commit/push hanya untuk menyatakan RFC selesai dibaca.

---

## 10. Strategi pengujian dan mutation

### 10.1 Home — fungsi murni

Test cases minimum:

- mode absent/valid/unknown;
- total unknown/zero/nonzero vs result zero/nonzero;
- span count ruang sempit/normal/lebar serta font besar;
- anchor note masih ada/sudah terhapus/dataset kosong;
- pemilihan mode tidak mengubah dataset.

Preference disk persistence membutuhkan Android integration/device test atau
abstraksi store yang sesuai; unit test parser saja tidak membuktikan persistensi.

### 10.2 Home — structural guards

Guard dapat memeriksa ID, label, layout Grid, trash 48dp, dan wiring dasar.
Namun guard lexical tidak membuktikan kontrol dapat disentuh atau menu membuka
aksi yang benar. Gabungkan dengan UAT nyata. Strip komentar jika memeriksa source.

### 10.3 Markdown — lapisan bukti

```text
source inspection
→ parser/rule fixture
→ sanitizer/DOM browser harness
→ Android WebView integration
→ perangkat sasaran
```

Harness browser, bila dipakai, harus tooling pinned dan profil terisolasi,
tanpa login/secret, tanpa upload telemetry, dengan network allowlist/blocked
sesuai skenario. Bersihkan profil/cache/proses; dokumentasikan cleanup.

Ekstraksi JS dari Kotlin harus menjaga kesetaraan interpolasi/asset dengan
HTML yang dikirim aplikasi. Harness yang mengetes salinan renderer berbeda
bukan bukti bahwa source produksi benar.

### 10.4 Mutation minimum

| Guard | Mutasi sengaja | Hasil yang wajib |
|---|---|---|
| Empty search | Gunakan filtered result count sebagai total | Query no-result test gagal |
| Empty toolbar | Hilangkan aturan parent toolbar hidden | Structural/runtime scenario gagal sesuai lapisan tes |
| Mode fallback | Unknown diarahkan Grid/crash | Unit test fallback gagal |
| Grid span | Paksa kolom tanpa cek lebar | Narrow-window policy test gagal |
| Identitas kartu | Wiring ID/posisi dibuat salah pada harness yang tepat | Open/delete identity scenario gagal |
| Renderer | Reintroduksi bug yang diperbaiki | Fixture gagal untuk alasan yang benar |
| Security | Izinkan salah satu atribut/scheme terlarang di harness terisolasi | Guard sanitizer gagal tanpa network/eksekusi berbahaya |

Pulihkan setiap mutasi, review diff, lalu jalankan focused dan full relevant suite.
Jangan meninggalkan mutasi di source. Tidak semua skenario layout bisa dibuktikan
unit test; laporkan yang hanya dibuktikan lewat device dengan jujur.

### 10.5 CI canonical saat ini

Workflow baseline menjalankan:

```bash
gradle testDebugUnitTest assembleDebug --console=plain
```

- Test JVM baru pada source set yang benar ikut gate tersebut.
- Harness JS/browser tambahan **tidak otomatis** ikut CI. Jika disepakati,
  registrasikan command/tooling-nya dan review supply chain sebelum mengklaim CI verified.
- Jangan memasang Android toolchain besar secara diam-diam; keputusan historis build lewat CI dipertahankan kecuali direvisi.
- `git diff --check` dan XML parse adalah pemeriksaan lokal, bukan kompilasi Kotlin.

---

## 11. Matriks UAT perangkat

### 11.1 Identitas pengujian

Setiap laporan harus mencatat:

```text
Commit/build/artifact:
Debug atau signed release:
Device dan RAM:
Android / WebView version:
Ukuran/orientasi window:
Font scale / display size:
Tema:
Jumlah/ukuran catatan uji:
Langkah, expected, actual:
Screenshot/video bila perlu:
```

Jangan menebak spesifikasi perangkat dari judul catatan atau screenshot.

### 11.2 Home

| ID | Skenario | Expected |
|---|---|---|
| H01 | Nol catatan | Welcome ada, kapsul dan mata tidak ada |
| H02 | Buat note pertama, kembali | Search tersedia; note muncul |
| H03 | Query tanpa hasil | Search tetap ada; no-result bukan welcome |
| H04 | Hapus note terakhir lalu undo | Toolbar hilang/kembali sesuai total; undo bekerja |
| H05 | Pilih Grid di drawer | Terpilih jelas, hasil berubah, drawer tetap terbuka |
| H06 | Restart setelah pilih Grid | Preference tetap Grid |
| H07 | List ↔ Grid saat filter aktif | Query dan dataset tetap; tidak menyimpan ulang note |
| H08 | Judul panjang, source HTML/URL, Unicode, isi kosong | Batas baris benar; source aman dan tidak dirender di kartu |
| H09 | Buka dan hapus kartu setelah scroll/switch cepat | Note yang dituju benar; trash tidak membuka Editor |
| H10 | Scroll paling bawah | Kartu terakhir/trash dapat dijangkau di sekitar FAB |
| H11 | Portrait, landscape, window sempit | Kolom menyesuaikan tanpa clipping |
| H12 | Font besar dan display size besar | Tidak overlap; fallback satu kolom bila perlu |
| H13 | Ganti tema, kembali, rotasi | Mode tetap; palet konsisten; tidak crash |
| H14 | TalkBack | Label/selected state/trash/traversal dapat dipahami |
| H15 | Catatan banyak | Scroll/switch tidak memburuk secara mengganggu; ukur jika ada gejala |

Gunakan backup/test dataset terpisah; jangan clear data atau menghapus semua
catatan pribadi tanpa backup dan izin pengguna.

### 11.3 Markdown

- Re-run fixture historis relevan, dengan expected disesuaikan pada kontrak
  versi yang diuji, bukan menganggap semua snapshot lama masih aturan aktif.
- Tabel dan code lebar: scroll hanya area konten yang sesuai, halaman tidak melebar.
- Literal Markdown/HTML dalam code tidak diproses sebagai syntax aktif.
- Toggle edit/read berulang, edit lalu baca, rotasi, tema, keyboard/selection.
- Unsafe HTML/scheme tetap inert; tidak ada network saat sekadar membuka note.
- Image confirmation cancel/confirm/error/offline cache tetap benar.
- Fitur baru diuji dengan input minimal, campuran fitur, malformed, dan dokumen panjang.
- Music dan export/import tetap smoke-tested tanpa mengubah perilakunya.

---

## 12. Risiko dan rollback

| Risiko | Mitigasi | Rollback |
|---|---|---|
| Welcome bergeser setelah toolbar hilang | UAT visual; jangan ubah copy/logo sekaligus | Revert batch A |
| Dua observer Home menghasilkan state sementara salah | Shared state policy dan tes transisi/emisi | Pulihkan wiring sebelum batch A |
| Grid terlalu sempit | Minimum width, font-scale UAT, 1-column fallback | Paksa presentasi List tanpa menghapus note |
| Trash menghapus note salah | Identitas binding, rapid-update tests, undo | Revert adapter/Grid batch |
| Switch mode melompat atau callback stale | ID anchor + mode/dataset validation | Disable anchor restoration sementara jika aman |
| Preference baru invalid | Stable string + fallback List | Abaikan key baru; jangan clear app data |
| Tambahan syntax memakan source literal | Fence/escape/nesting fixtures | Revert rule/batch terisolasi |
| Sanitizer melemah | Security fixtures, red → green, primary-source review | Pulihkan sanitizer dan blok fitur baru |
| Library/diagram membebani HP | Kelayakan/measurement sebelum integrasi | Tidak memasang atau menonaktifkan fitur |
| Dokumentasi salah mengklaim support | Matrix berbukti dan guide sync | Koreksi klaim, bukan menutup-nutupi limitasi |

Rollback aplikasi tidak memakai reset destruktif workspace atau clear database.
Setiap commit yang kelak dibuat harus dapat direvert terpisah di branch sesi
yang sedang berlaku. Jangan menyalin perintah pindah branch dari panduan historis.

---

## 13. Gate penyelesaian dan rilis

### 13.1 Status RFC saat ditulis

| Tingkat | Status |
|---|---|
| DESIGNED | Ya — rancangan dan rencana ini |
| IMPLEMENTED | Ya untuk A1-B3 (search chrome, model/preference mode, Grid, hardening). Belum untuk C1/C2/C3 |
| LOCALLY VERIFIED | Tidak — sandbox tanpa JDK/Gradle/Android SDK; hanya pemeriksaan statis + `git diff --check` |
| CI VERIFIED | Belum ada; menunggu push + PR |
| PLATFORM/DEVICE VERIFIED | Tidak ada klaim perangkat baru |
| RELEASED | Tidak; baseline rilis lama bukan bukti fitur baru |

### 13.2 Gate implementasi

- [ ] Detail terbuka yang memengaruhi perilaku disepakati.
- [ ] Acceptance criteria dipilih per batch, bukan semua checkbox langsung dianggap scope rilis.
- [ ] Regression guards dan mutation relevan lulus.
- [ ] Diff, mode file, resource, dependency, secret/cache diperiksa.
- [ ] CI canonical lulus untuk source commit yang tepat.
- [ ] UAT perangkat tercatat dengan keterbatasan yang nyata.
- [ ] Guide/capability matrix sesuai hasil.
- [ ] Tidak ada perubahan data atau capability di luar persetujuan.

### 13.3 Rilis nanti

Target versi, PR base, strategi merge, dan urutan publikasi diputuskan terpisah.
Pengguna meminta penghapusan `BRANCHING.md` **nanti**; dokumen itu belum dihapus
atau diam-diam digantikan RFC ini. Ketegangan urutan rilis pada dokumen lama perlu
diselesaikan sebelum operasi rilis, bukan saat menulis rancangan UI.

Tidak ada push, PR, merge, workflow release, signing, tag, atau distribusi
sebagai efek samping pembuatan dokumen ini.

### 13.4 Catatan implementasi 2026-11-16

Batch A1-B3 sudah di-commit lokal di branch `fix/home-empty-search`
(`75a09a8`, `d833906`, `548e7dd`, `76198c6`); belum di-push, belum ada CI,
belum ada UAT. Audit tahap C0 ada di
`docs/AUDIT_MARKDOWN_CAPABILITIES_2026_11_16.md` dan belum mengubah renderer.
Kandidat C1 menunggu keputusan sebelum dikerjakan.

---

## 14. Keputusan terbuka

| ID | Detail | Rekomendasi | Dampak jika alternatif dipilih |
|---|---|---|---|
| D01 | Search parent saat nol note | `GONE` seluruh toolbar | Jika kapsul tetap diminta, perlu ukuran/visual state berbeda |
| D02 | Bentuk Grid | Reguler, bukan masonry | Masonry menambah kompleksitas tinggi/scroll dan urutan visual |
| D03 | Drawer setelah pilih mode | Tetap terbuka, selected state langsung berubah | Menutup drawer lebih cepat melihat hasil, tapi menghilangkan feedback pilihan di tempat |
| D04 | Rentang kolom | Adaptif 1–3, target 2 portrait normal | Fixed dua kolom berisiko pada font besar/window sempit |
| D05 | Kerapatan Grid | Judul 2 baris, source preview 3, metadata dan trash tetap | Lebih ringkas mengorbankan informasi; perlu UAT |
| D06 | Fitur Markdown tambahan pertama | Dipilih setelah audit C0 | Daftar fitur sekarang berisiko menduplikasi support atau membuka scope terlalu luas |
| D07 | Math/Mermaid/attachment/link antar-note | Feasibility/RFC terpisah | Memasukkan langsung menambah dependency/security/storage contract |

Angka ukuran dan wireframe adalah rancangan untuk divalidasi, bukan klaim
pixel-perfect berdasarkan screenshot. Detail yang sudah dikonfirmasi pengguna
nanti dicatat di bagian ini tanpa mengubah sejarah persetujuan secara diam-diam.

---

## 15. Langkah berikutnya

1. Review D01–D05 sebagai satu paket rekomendasi Home.
2. Jika disepakati, kerjakan A terlebih dahulu sebagai batch kecil; lanjut B.
3. Jalankan C0 untuk memilih scope Markdown berdasarkan bukti.
4. Bahas setiap kandidat berisiko/dependency sebelum implementasi.
5. Catat hasil build dan perangkat pada laporan baru, bukan mengganti rencana
   ini menjadi klaim bahwa semua fitur sudah selesai.

**Kesimpulan:** rapikan search kosong, tambahkan pilihan List/Grid tanpa
mengubah isi/alur catatan, lalu lengkapi Markdown secara bertahap dengan
keamanan, performa perangkat, dan kejujuran status sebagai gate utama.
