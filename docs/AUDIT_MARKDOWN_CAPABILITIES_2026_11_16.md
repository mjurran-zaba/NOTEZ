# AUDIT — Kapabilitas Markdown NOTEZ (tahap C0)

| Field | Nilai |
|---|---|
| Tanggal | 2026-11-16 (UTC) |
| Baseline kode | `76198c6` (branch `fix/home-empty-search`) |
| Metode | Inspeksi source + dokumen; **tanpa** eksekusi WebView, fixture, atau perangkat |
| Status | Audit C0 selesai untuk inventaris; C1/C2 belum dikerjakan |
| Klasifikasi | `supported` / `partial` / `missing` / `intentionally restricted` / `unverified` |

> **Batas bukti.** Semua verdict di bawah berasal dari membaca kode
> `markdown/MarkdownPreviewRenderer.kt` (1.783 baris), `assets/markdown/`,
> dan `assets/help/markdown_guide.md`. Tidak ada satu pun skenario yang
> dijalankan. Label `supported` berarti "jalur kode dan aturan keamanannya
> ditemukan", bukan "sudah terbukti benar di perangkat". Sandbox pengembangan
> tidak punya JDK/Gradle/Android SDK, dan renderer butuh WebView Android,
> jadi verifikasi runtime tetap harus lewat CI + UAT perangkat.

## 1. Bukti yang dipakai

| Area | Lokasi |
|---|---|
| Parser + aturan | `MarkdownPreviewRenderer.kt:287` (`markdown-it` 15.0.2, `html:true`, `linkify:true`, `breaks:false`, `typographer:false`, enable `table`+`strikethrough`) |
| Allowlist raw HTML | `MarkdownPreviewRenderer.kt:182-218` |
| Allowlist HTML hasil render | `:219-264` |
| Allowlist class | `:265-281` |
| Sanitizer DOM | `:552-670` |
| Link policy | `:178-181` (`https?:`, `mailto:`, `tel:`, anchor `#slug`, `notez-image://load?src=`, cache `https://notez.local/cache/image/<sha256>`) |
| Footnote | ekstraksi `:731-770`, inline rule `:863-884` |
| Definition list | `:772-822` |
| Highlight/sub/sup | `:838-861` (`==`, `^`, `~` sebelum `emphasis`) |
| Code card + highlight | `:1022-1190` |
| Callout | `:1194-1200` (NOTE/TIP/IMPORTANT/WARNING/CAUTION) |
| Task list | `:976-982` |
| Tabel | wrapper `:960-966`, `colspan`/`rowspan` clamp `:477-483` |
| Asset pihak ketiga | `assets/markdown/THIRD_PARTY_MARKDOWN.md` (markdown-it 15.0.2, MIT, SHA tarball & JS) |

## 2. Matriks kapabilitas

| ID | Kapabilitas | Acuan | Bukti source | Klasifikasi | Risiko/catatan |
|---|---|---|---|---|---|
| MD-CORE-001 | Heading, paragraf, emphasis, list, quote, hr | CommonMark via markdown-it | `:287` | supported (source) | Nesting/Unicode/malformed belum diuji |
| MD-STRIKE-001 | Strikethrough `~~x~~` | GFM | `:287` | supported (source) | Benturan dengan `~sub~` perlu uji |
| MD-TABLE-001 | Tabel, alignment, lebar | GFM | `:287`, `:960-966`, `:477-483` | supported (source) | Tabel sangat lebar & scroll terisolasi belum diuji |
| MD-TASK-001 | Task list `- [ ]` / `- [x]` | GFM (view-only) | `:976-982` | partial (source) | Checkbox `disabled` + `aria-hidden`; nested task belum diuji; tidak menulis source |
| MD-CODE-001 | Fenced/inline code, label bahasa, scroll | CommonMark + enhancement | `:1022-1190` | supported (source) | Fence panjang & literal syntax di dalam kode belum diuji |
| MD-HL-001 | Syntax highlighting lokal | enhancement NOTEZ | `:1022-1190` | partial (source) | Hanya bahasa yang punya `codeRules`; fallback bahasa asing belum diverifikasi |
| MD-LINK-001 | Link, autolink, anchor lokal | GFM + policy NOTEZ | `:178-181`, `:886-900` | supported (source) | Skema di luar `https/http/mailto/tel` jadi `notez-unsafe-link`; obfuscation belum diuji |
| MD-CALLOUT-001 | Callout `[!NOTE]` dll. | GitHub-style, 5 jenis | `:1194-1200` | supported (source) | Multiline & nested quote belum diuji |
| MD-SUPSUB-001 | Highlight `==x==`, sup `^x^`, sub `~x~` | extension NOTEZ | `:838-861` | partial (source) | Escape delimiter, whitespace, benturan strikethrough belum diuji |
| MD-FOOT-001 | Footnote `[^1]` | extension NOTEZ | `:731-770`, `:863-884` | partial (source) | Multiline, ref berulang, backlink, fenced literal belum diuji |
| MD-DEFLIST-001 | Definition list | extension NOTEZ | `:772-822` | partial (source) | **Isi definisi di-`escapeHtml`**, jadi inline markdown di dalamnya tampil literal (lihat F1) |
| MD-HTML-001 | Raw HTML aman | allowlist NOTEZ | `:182-281`, `:552-670` | supported (source) | Dua lapis sanitasi (raw + DOM); nested/malformed/comment belum diuji |
| MD-IMG-001 | Gambar remote | placeholder + konfirmasi + cache | `:886-916`, `:178-181` | supported (source) | MIME/size/error/cancel/offline cache belum diuji |
| MD-IMG-002 | Gambar lokal (`file://`, `content://`) | — | tidak ada di source | missing (by design) | Butuh RFC attachment terpisah |
| MD-MATH-001 | Math/LaTeX | — | tidak ada | missing | Butuh studi C3 (dependency, ukuran, performa) |
| MD-MERMAID-001 | Mermaid | — | tidak ada | missing | Butuh studi C3 |
| MD-WIKI-001 | Wikilink/backlink | — | tidak ada | missing | Butuh model identitas dokumen |
| MD-PERF-001 | Performa dokumen besar | — | tidak ada pengukuran | unverified | Belum ada angka; jangan mengarang anggaran |
| MD-A11Y-001 | Aksesibilitas Reading View | — | tidak ada pengujian | unverified | Butuh TalkBack di perangkat |

## 3. Temuan source-level

**F1 — Definition list kehilangan format inline.** `:808-809` membangun `<dd>`
dari `escapeHtml(definition)`. Akibatnya `**tebal**`, `` `kode` ``, atau link di
dalam definisi tampil sebagai teks mentah, berbeda dari sisa dokumen. Ini
temuan dari kode, bukan dari render; belum ada fixture yang membuktikannya.

**F2 — Ekstraksi footnote/deflist sadar fence.** `nextFenceState`/`markdownFenceMarker`
(`:718-730`) membuat baris di dalam fenced code tidak diperlakukan sebagai
definisi. Perilaku ini benar secara desain, tapi belum ada regression guard.

**F3 — `colspan`/`rowspan` dibatasi.** Nilai di-clamp ke 1..12 (`:477-483`),
mencegah tabel raksasa dari HTML pengguna. Batas ini bukan kontrak yang
didokumentasikan di guide.

**F4 — Sanitasi dua lapis.** `html_inline`/`html_block` melewati
`sanitizeRawHtml` (`:912-918`), lalu seluruh DOM hasil render melewati
`sanitizeRenderedDom` (`:628-658`). Tidak ada `addJavascriptInterface`, dan
tidak ada handler event yang diizinkan.

**F5 — Cache gambar dikunci ke host internal.** `safeCachedImageLink`
(`:181`) hanya menerima `https://notez.local/cache/image/<64 hex>`, sehingga
URL cache palsu dari catatan tidak bisa dipakai.

**F6 — Task list murni tampilan.** Checkbox `disabled` + `aria-hidden`
(`:976-982`); tidak ada jalur tulis balik ke source note.

## 4. Yang belum diverifikasi (jangan diklaim lulus)

- Semua kombinasi nesting (list di dalam callout, code di dalam table, dsb.).
- Input malformed dan HTML bersarang.
- Performa render dokumen pendek/sedang/panjang (cold dan warm).
- Perilaku WebView di Android 8–14 dan WebView versi lama.
- TalkBack, seleksi teks, dan scroll area kode/tabel di perangkat.
- Perilaku gambar remote saat cancel, error, offline, dan cache hilang.
- Kesetaraan guide (1.805 baris) dengan perilaku nyata per fitur.

## 5. Kandidat C1 (perbaikan, belum dikerjakan)

| Kandidat | Isi | Kenapa belum dikerjakan |
|---|---|---|
| C1-A | Render inline markdown di `<dd>` definition list (perbaiki F1) | Menyentuh jalur render + sanitasi; butuh fixture red→green dan UAT, tidak bisa dibuktikan di sandbox ini |
| C1-B | Pastikan fallback bahasa asing pada code card tetap aman dan literal | Perilaku `codeRules` untuk bahasa tak dikenal belum dipetakan |

Keduanya menunggu keputusan. Tidak ada perubahan renderer yang dilakukan pada
audit ini.

## 6. Kandidat C2 (fitur lokal berisiko rendah, setelah C1)

- Tombol "copy" pada code card (reader convenience, bukan syntax).
- Penanda scroll untuk tabel lebar.
- Outline heading (heading anchor sudah ada, `:886-900`).

Math/Mermaid/attachment/wikilink tetap di luar scope sampai ada keputusan
D07 dan studi kelayakan terpisah.

## 7. Kesimpulan

Inventaris ini cukup untuk memilih batch C berikutnya, tetapi belum cukup
untuk mengklaim kompatibilitas penuh. Satu bug source-level yang jelas
(F1) sudah teridentifikasi dan bisa jadi batch C1 pertama bila disetujui.
