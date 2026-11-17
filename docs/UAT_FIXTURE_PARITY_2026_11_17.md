# UAT — Fixture Paritas (Issue #2)

## Tujuan
Menyediakan baseline render untuk 4 ukuran dokumen agar Batch 3 (chunking) dan Batch 4 (cache) bisa dibuktikan output identik.

## Fixture
- `docs/fixtures/10k.md` (~10.8 KB)
- `docs/fixtures/50k.md` (~54 KB)
- `docs/fixtures/96k.md` (~97 KB)
- `docs/fixtures/200k.md` (~202 KB)

## Cara capture baseline (sebelum Batch 3)
1. Buka catatan, paste isi fixture.
2. Toggle ke view, tunggu render selesai.
3. Screenshot/HTML hasil render (atau hash DOM) sebagai baseline.
4. Catat marker diagnostics: `FIRST_PAINT`, `FULL_RENDER`, `RENDER_JS_END`.

## Setelah Batch 3/4
1. Ulangi dengan fixture yang sama.
2. Bandingkan output render dengan baseline (harus identik).
3. Catat marker baru (chunk time, cache hit).
4. Jika ada drift → rollback batch.

## Catatan
- Jangan ubah sanitizer/URL policy.
- Theme berubah → shell reload, bukan cache.
EOF