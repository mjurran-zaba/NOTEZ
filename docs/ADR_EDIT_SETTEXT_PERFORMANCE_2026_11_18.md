# ADR — Optimasi Performa `bodyEdit.setText` pada Catatan Besar 96 KB - 100 KB

**Status:** `IMPLEMENTED & CI VERIFIED — Menunggu Platform/Device Verification di Infinix SMART 9 HD`  
**Tanggal Revisi:** 2026-11-18  
**Konteks Issue:** [Issue #2: perf(preview): render Markdown 96 KB lambat di perangkat low-end](https://github.com/mjurran-zaba/NOTEZ/issues/2)  
**Target Perangkat Acuan:** Infinix SMART 9 HD (Helio G50, 8× Cortex-A53, RAM 4 GB, Android 14)  
**Versi Basis:** PR #5 (`fix/issue2-renderer-recovery-and-rotation`, commit `49afd1e`)

---

## 1. Masalah & Bukti Lapangan (Data Riil Perangkat)

Dari data dump Diagnostics nyata pada artifact debug PR #5 di Infinix SMART 9 HD:
- **Toggle Edit → View:** `8 – 10 ms` (Target < 600 ms **tercapai telak** via in-memory cache).
- **First Open Catatan 96 KB → Full Render:** `2.477 ms` (Target < 2.500 ms **tercapai** via `MarkdownItAsset` cache proses).
- **Warm Toggle Edit (teks sudah tersinkron):** `7 – 10 ms` (Sesi #1 & #3).

### Bottleneck Kritis yang Ditemukan di Lapangan:
Saat pertama kali pengguna menekan tombol Pensil (*Edit*) untuk membuka catatan besar (96 KB – 100 KB), UI thread mengalami freeze selama **5,9 detik**:

```text
# Sesi #1 (Note 34, 96 KB - Cold Edit):
EDIT_SETTEXT_START +7ms   · chars=96373
EDIT_SETTEXT_END   +4918ms Δ4911 · chars=96373  <-- 4,91 DETIK FREEZE
...
EDIT_VISIBLE       +4927ms Δ1
KEYBOARD_REQUESTED +5903ms Δ976                 <-- JEDA 976 MS (KURSOR BAWAH)
EDIT_LAYOUT_DONE   +5946ms Δ43
Total waktu masuk edit: 5.946 ms
```

---

## 2. Analisis Akar Masalah Teknis

Dari dekonstruksi waktu milidetik:
1. **Biang Kerok 1: `bodyEdit.setText(currentContent)` Makan 4.911 ms**
   - `EditText` di Android wajib menggunakan buffer tipe `EDITABLE`, yang otomatis menginstansiasi `DynamicLayout`.
   - `DynamicLayout` secara sinkron memanggil font shaping native HarfBuzz & Skia untuk seluruh 96.373 karakter sekaligus di main thread.
   - `AppCompatEditText` secara default menyalakan `EmojiCompat` (`EmojiTextWatcher`) yang menyisir 96k karakter mencari kode emoji dan membuat `EmojiSpan`.
   - Pada CPU hemat daya Helio G50 (Cortex-A53), kalkulasi ini membutuhkan waktu murni ~4,9 detik jika dilakukan sekaligus di main thread.
2. **Biang Kerok 2: Kursor Dipaksa ke Akhir Dokumen Makan 976 ms**
   - Menaruh kursor ke `bodyEdit.text.length` (karakter ke-96.373) memicu `bringPointIntoView` internal Android yang memaksa kalkulasi scroll vertikal ke baris ke-2.400 di viewport.

---

## 3. Solusi Arsitektur: Idle Pre-warm & Safe Optimization

1. **Idle Pre-warming di Balik Layar (Pangkas Freeze 4,9s Jadi < 10 ms):**
   - Saat pengguna membuka catatan, layar Reading View tampil di detik ke-2,7 (`PAINT_REAL`).
   - `MarkdownPreviewRenderer` mengirim callback `onPaintReal` ke `EditorActivity`.
   - Saat CPU menganggur (*idle*), `EditorActivity` menggunakan `Looper.myQueue().addIdleHandler` untuk menyinkronkan `bodyEdit` via `syncBodyText()` diam-diam di background.
   - Saat pengguna kemudian menekan tombol Pensil, teks sudah tersinkron (`bodyTextSynced == currentContent`).
   - Aplikasi langsung meluncur lewat jalur ekspres **`EDIT_TEXT_SKIP` yang cuma butuh 7–10 milidetik**!
2. **Perbaikan Kursor (Pangkas Jeda 976 ms):**
   - Kursor diatur ke posisi awal (`0`) secara default, bukan ke karakter ke-96.373.
   - Menghilangkan beban kalkulasi scroll ekstrem 2.400 baris.
3. **Optimasi Tipografi & Sistem:**
   - `android:breakStrategy="simple"` & `android:hyphenationFrequency="none"` (greedy line-breaker).
   - `android:importantForAutofill="no"` (mencegah inspeksi formulir pada catatan panjang).
   - `app:emojiCompatEnabled="false"` (mematikan scanning span berulang; seluruh emoji native Android 14 tetap tampil dan dapat diketik 100%).

---

## 4. Status Verifikasi

- **DESIGNED:** Terdokumentasi lengkap dalam ADR ini.
- **IMPLEMENTED:**
  - `app/src/main/res/layout/activity_editor.xml`
  - `app/src/main/java/com/zaba/notez/EditorActivity.kt`
  - `app/src/main/java/com/zaba/notez/markdown/MarkdownPreviewRenderer.kt`
  - `app/src/test/java/com/zaba/notez/EditorTextPerformanceGuardTest.kt`
- **LOCALLY VERIFIED:** Lolos uji guard leksikal & lolos pengujian mutasi `red -> green` pada semua invariant.
- **CI VERIFIED:** GitHub Actions **Run ID `44300318211`** (Commit `49afd1e`) SUKSES:
  - `testDebugUnitTest`: PASS.
  - `assembleDebug`: PASS.
  - Artefak: `notez-debug-pr-5` (SHA-256: `88fba2b096af996e978f73ebab62b41117f30677cef188a7618925a1c5c0d4b1`).
- **PLATFORM/DEVICE VERIFIED:** Menunggu hasil verifikasi nyata di Infinix SMART 9 HD.
