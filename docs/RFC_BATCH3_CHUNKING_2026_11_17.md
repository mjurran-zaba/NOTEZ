# RFC — Batch 3: Render Bertahap / Chunking (Issue #2)

**Status:** `DRAFT — menunggu approval Zaqi`  
**Tujuan:** first paint 96 KB < 1.5 s, full render < 2.5 s, tidak ada long task > 100 ms saat scroll.

## Konsep
- Render blok awal dulu → paint.
- Sisanya per chunk ~20–40 blok / ~6–8 KB, target < 10 ms per chunk.
- Output harus identik dengan render penuh (fixture paritas).

## Prasyarat
- Fixture paritas 10/50/96/200 KB (`docs/fixtures/`).
- Batch 2 (shell reuse) stabil.
- Cache Batch 4 tidak merusak paritas.

## File disentuh
- `MarkdownPreviewRenderer.kt` (render orchestration, chunk scheduling)
- JS renderer (chunk insertion, sanitizer per chunk)

## Risiko
- Output drift; long task; sanitizer bypass per chunk.

## Acceptance
- [ ] First paint 96 KB < 1.5 s (device uji).
- [ ] Full render < 2.5 s.
- [ ] Output identik (fixture paritas).
- [ ] Tidak ada long task > 100 ms saat scroll.
EOF