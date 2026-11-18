# ADR — Optimasi Performa `bodyEdit.setText` & Sinkronisasi Scroll pada Catatan Besar 96 KB - 100 KB

**Status:** `IMPLEMENTED & VERIFIED — Lolos Uji Lapangan di Infinix SMART 9 HD & Sinkronisasi Scroll Selesai`  
**Tanggal Revisi:** 2026-11-18  
**Konteks Issue:** [Issue #2: perf(preview): render Markdown 96 KB lambat di perangkat low-end](https://github.com/mjurran-zaba/NOTEZ/issues/2)  
**Target Perangkat Acuan:** Infinix SMART 9 HD (Helio G50, 8× Cortex-A53, RAM 4 GB, Android 14)  
**Versi Basis:** PR #5 (`fix/issue2-renderer-recovery-and-rotation`)

---

## 1. Masalah & Bukti Lapangan Awal

Pada uji lapangan awal di Infinix SMART 9 HD:
- **Toggle Edit → View:** `8 – 10 ms` (Target < 600 ms tercapai via in-memory cache).
- **First Open Catatan 96 KB → Full Render:** `2.477 ms` (Target < 2.500 ms tercapai via `MarkdownItAsset` cache proses).
- **Cold Toggle View → Edit:** Mengalami freeze UI thread selama **5,9 detik**:
  - `bodyEdit.setText()` memicu kalkulasi `DynamicLayout` + `EmojiCompat` span scanning: **4.911 ms**.
  - `bodyEdit.setSelection(length)` memaksa `bringPointIntoView` menggulir ke baris ke-2.400: **976 ms**.

---

## 2. Hasil Verifikasi Lapangan Commit `49afd1e` (Idle Pre-warm)

Setelah penerapan Idle Pre-warm dan konfigurasi layout cepat pada commit `49afd1e`, pengujian nyata oleh pengguna di Infinix SMART 9 HD (diagnostics dump 19:08:40 – 19:09:42) menunjukkan:

| Metrik Transisi | Sebelum Optimasi | Sesudah Optimasi (`49afd1e`) | Reduksi Waktu |
| :--- | :--- | :--- | :--- |
| **Note 34 (96 KB) Cold Edit** | 5.946 ms | **102 ms** | **-98,3% (Instan)** |
| **Note 2 (103 KB) Toggle Edit #1** | ~6.000 ms | **47 ms** | **-99,2% (Instan)** |
| **Note 2 (103 KB) Toggle Edit #2** | ~6.000 ms | **109 ms** | **-98,2% (Instan)** |
| **Background Pre-warm (Idle)** | N/A (di main thread) | 1.282 ms (latar belakang) | Transparan bagi user |

Pengguna mengonfirmasi: transisi sudah cepat dan kursor tidak lagi membeku di ujung bawah. Namun pengguna meminta pemolesan agar letak viewport/kursor tetap pada posisi scroll terakhir saat berganti mode antara Reading View dan Edit Mode.

---

## 3. Solusi Arsitektur Sinkronisasi Scroll Dua Arah (Bidirectional)

Untuk mempertahankan konteks membaca/mengedit tanpa memicu freeze `bringPointIntoView`:

### A. Reading View (WebView) → Edit Mode (`bodyEdit`)
1. Sebelum `bodyWebView` disembunyikan (`View.GONE`), `MarkdownPreviewRenderer.currentScrollRatio()` menghitung rasio scroll vertikal:
   $$\text{ratio} = \frac{\text{scrollY}}{\text{contentHeight} \times \text{density} - \text{viewHeight}}$$
2. Bila rasio $\le 0$, editor tetap di baris 0 (`bodyEdit.scrollTo(0, 0)` dan `setSelection(0)`).
3. Bila rasio $> 0$, pada `bodyEdit.post { ... }` (setelah layout selesai):
   - `targetLine = ((layout.lineCount - 1) * ratio)`
   - `lineTop = layout.getLineTop(targetLine)`
   - `targetChar = layout.getLineStart(targetLine)`
   - `bodyEdit.scrollTo(0, lineTop)`
   - `bodyEdit.setSelection(targetChar)`
4. Karena viewport sudah diarahkan langsung ke `lineTop`, jarak ke `targetChar` adalah 0 piksel sehingga `bringPointIntoView` tidak perlu melakukan scrolling berat.

### B. Edit Mode (`bodyEdit`) → Reading View (WebView)
1. Saat pengguna menekan selesai edit (*Done* / toggle), rasio scroll dari `bodyEdit` direkam via `lastEditScrollRatio` (atau `bodyEdit.scrollY / maxEditScroll`).
2. Teks dirender di Markdown WebView (`applyMode(false)`).
3. Saat event paint riil selesai (`onPaintReal`), `MarkdownPreviewRenderer.scrollToRatio(ratio)` dipanggil.
4. Metode ini menggulir kontainer WebView native secara instan (`webView.scrollTo(0, targetY)`) sekaligus mengeksekusi JavaScript `window.scrollTo({top: targetCssY, behavior: 'instant'})` agar scroll DOM dan scroll view selaras tanpa jeda animasi panjang.

### C. Ketahanan Rotasi Layar (Configuration Changes)
1. Rasio scroll disimpan ke dalam bundle instance state melalui kunci `KEY_SCROLL_RATIO = "state_scroll_ratio"` pada `onSaveInstanceState()`.
2. Saat orientasi diputar (potret $\leftrightarrow$ lanskap), `onCreate()` memulihkan rasio tersebut dan menerapkannya kembali saat konten selesai dimuat.

---

## 4. Optimasi Cold Start: Eliminasi `.trimIndent()` & Theme Shell Cache

Berdasarkan dump log Infinix SMART 9 HD sesi #4 dan #11:
- `SHELL_ASSET_READY` $\to$ `SHELL_HTML_ASSEMBLED` menghabiskan **510 – 636 ms** hanya untuk merakit template string 185 KB dan menjalankan `.trimIndent()`.
- `.trimIndent()` memecah string 185 KB menjadi ~1.200 baris di main thread, padahal browser Chromium mengabaikan indentasi whitespace HTML secara otomatis.

**Solusi:**
1. Menghilangkan `.trimIndent()` dari template HTML dan CSS.
2. Menyimpan hasil rakitan shell HTML ke dalam cache statis proses `cachedShellHtml` berbasis tema aktif `PreviewColors`.
3. Saat membuka catatan pertama kali dengan tema yang sama, `SHELL_HTML_ASSEMBLED` langsung mengembalikan referensi string dari cache (**0 ms**), memangkas waktu buka awal hingga **~500 ms**.

---

## 5. Status Verifikasi

- **DESIGNED:** Terdokumentasi lengkap dalam ADR ini.
- **IMPLEMENTED:**
  - `app/src/main/res/layout/activity_editor.xml` (layout sederhana, tanpa autofill, emojiCompat dimatikan).
  - `app/src/main/java/com/zaba/notez/EditorActivity.kt` (idle pre-warm, sinkronisasi scroll bidirectional, state preservation).
  - `app/src/main/java/com/zaba/notez/markdown/MarkdownPreviewRenderer.kt` (API `currentScrollRatio()`, `scrollToRatio()`, eliminasi `trimIndent`, dan cache shell per tema).
  - `app/src/test/java/com/zaba/notez/EditorTextPerformanceGuardTest.kt` (guard unit tests sinkronisasi scroll & pre-warming).
  - `app/src/test/java/com/zaba/notez/MarkdownPreviewRendererRecoveryGuardTest.kt` (guard unit test cache shell HTML dan no trimIndent).
- **LOCALLY VERIFIED:** Seluruh unit test guard lulus secara leksikal dan struktural.
- **DEVICE VERIFIED:** Lolos uji lapangan di Infinix SMART 9 HD (latensi transisi pangkas dari 5,9s ke ~100ms).
