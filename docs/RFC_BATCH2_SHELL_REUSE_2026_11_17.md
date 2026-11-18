# RFC — Batch 2: Shell Reuse WebView (Issue #2)

**Status:** `DRAFT — menunggu approval Zaqi`  
**Tanggal:** 2026-11-17  
**Branch kerja (usulan):** `perf/issue-2-batch2-shell-reuse` (base `kilo/astute-wind-xhx` atau `main`)  
**Target:** toggle edit↔view < 600 ms di perangkat uji.

---

## 1. Masalah
Setiap `render()` memanggil `loadDataWithBaseURL` dengan HTML penuh (CSS + 115 KB markdown-it + JS renderer + source). Akibatnya shell di-parse ulang tiap toggle edit→view (~0.83 s sekarang). Target < 0.6 s.

## 2. Konsep
- Muat **shell sekali per-Activity** (CSS + markdown-it + JS renderer, tanpa `source`).
- Render berikutnya via `evaluateJavascript("window.notezRender(sourceJson, baseJson, cacheJson)", ...)` → update DOM tanpa reload.
- DOM dibersihkan + disanitasi ulang tiap render (keamanan tetap).
- Theme berubah → shell dibuat ulang (CSS berubah).
- Tangani `onRenderProcessGone` (sudah ada).

## 3. File yang disentuh
- `MarkdownPreviewRenderer.kt` (render, buildShell, buildContent, evaluateJavascript, shellLoaded flag, theme check)
- JS inlined: bungkus render logic jadi `window.notezRender(...)`.

## 4. Risiko
- Refactor JS besar; tanpa fixture paritas output bisa drift.
- Lifecycle WebView (destroy, theme change).
- Sanitizer tetap berlaku.

## 5. Acceptance
- [ ] Toggle edit↔view < 600 ms.
- [ ] Output identik dengan baseline (fixture paritas).
- [ ] Theme change reload shell.
- [ ] Sanitizer/URL policy tidak berubah.

## 6. Kapan dikerjakan
Setelah fixture paritas tersedia dan approval. Tidak digabung ke batch sekarang tanpa bukti.
