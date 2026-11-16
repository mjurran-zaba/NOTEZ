# NOTEZ v0.1.8

**Status:** Production release, published.
**Version:** `versionName 0.1.8`, `versionCode 9`.
**Tag:** `v0.1.8` → commit `ad29cba` (commit bump versi).
**Signed APK dibangun dari:** commit `ddd9e29` (satu commit workflow di atas tag; tidak ada perubahan source aplikasi).
**CI run:** [43899527596](https://github.com/mjurran-zaba/NOTEZ/actions/runs/43899527596) — `Build NOTEZ Production — v0.1.8`, success (3m18s).
**Artifact:** `notez-v0.1.8-release`, 3.266.635 bytes.
**SHA-256 APK:** `20ff1efdd310ed2b521ff3ae9fd89a30434fdc9c38267f6afde5ed6e766db309`.
**GitHub Release:** <https://github.com/mjurran-zaba/NOTEZ/releases/tag/v0.1.8> — aset `app-release.apk`, SHA-256 sama dengan artifact CI.

---

## Changed

- **Home empty search (A1)** — saat belum ada catatan, seluruh toolbar search
  disembunyikan (`GONE`). Tidak ada kapsul kosong yang menyisakan ruang.
- **Card List / Card Grid (B1–B3)** — pilihan List/Grid di drawer Home. Default
  install/upgrade tetap List. Pilihan tersimpan sebagai string stabil, tidak
  mengubah isi, urutan, note ID, source body, maupun `updatedAt`. Grid adaptif
  1–3 kolom dengan spacing milik satu `ItemDecoration` dan inset tepi milik
  padding RecyclerView.
- **Markdown definition list (C1-A / F1)** — isi `<dd>` kini dirender dengan
  `md.renderInline`, sehingga `**tebal**`, `` `kode` ``, dan link di dalam
  definisi tampil sebagai format, bukan teks literal. Sanitizer dua lapis dan
  URL policy tidak dilonggarkan; tidak ada dependency baru.

## Documentation

- `docs/RFC_HOME_SEARCH_CARD_GRID_MARKDOWN_2026_11_15.md`
- `docs/AUDIT_MARKDOWN_CAPABILITIES_2026_11_16.md`

## Verifikasi

| Tingkat | Status | Bukti |
|---|---|---|
| IMPLEMENTED | ya | A1–B3 dan C1-A ada di source `main`. |
| LOCALLY VERIFIED | tidak | Sandbox pengembangan tanpa JDK/Gradle/Android SDK; hanya pemeriksaan statis dan `git diff --check`. |
| CI VERIFIED | ya | `Build NOTEZ` hijau untuk batch A1–B3 (PR #1) dan `Build NOTEZ Production — v0.1.8` run 43899527596 sukses untuk `testDebugUnitTest` + `assembleRelease` signed. |
| DEVICE VERIFIED | sebagian | Pemilik repo mengonfirmasi visual Home empty dan Card Grid "perfect", serta search collapsed (mata + slide-in) sesuai bug report. C1-A Markdown belum diuji perangkat. |
| RELEASED | ya | APK signed `app-release.apk` (3.266.635 bytes, SHA-256 di atas) diterbitkan sebagai aset GitHub Release `v0.1.8`. |

## Catatan proses

Run percobaan pertama (`43893516338`) gagal di `packageRelease` karena secret
keystore di repo masih kosong (`echo "" | base64 -d`), bukan karena keystore
lokal rusak. Setelah secret `NOTEZ_KEYSTORE_B64`, `NOTEZ_STORE_PASS`,
`NOTEZ_KEY_ALIAS`, dan `NOTEZ_KEY_PASS` diisi, run kedua sukses.

## Not included / sisa

- UAT perangkat untuk C1-A (inline markdown di definition list) belum dicatat
  per butir.
- Mutation proof red→green untuk C1-A belum dijalankan.
- C1-B dan C2 Markdown menunggu keputusan.
- Math / Mermaid / attachment / wikilink tetap di luar scope.
