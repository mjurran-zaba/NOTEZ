# Model Branching NOTEZ

Dokumen ini mencatat kesepakatan yang disetujui pemilik repo (2026-11-15)
tentang alur kerja Git NOTEZ. Tujuan: `main` selalu dalam keadaan releasable,
dan integrasi tidak pernah diuji dengan cara "push langsung ke main".

## Peran branch

| Branch | Peran | Siapa yang boleh merge ke sini |
|---|---|---|
| `main` | Rilis. Selalu releasable. Tidak ada perubahan debug-only. | Hanya dari `ZAQIxNOTEZ`, lewat PR, setelah CI hijau. |
| `ZAQIxNOTEZ` | Integrasi. Semua pekerjaan berkumpul dan diuji di sini. | Dari `fix/*`, `feat/*`, `docs/*`, lewat PR. |
| `fix/*`, `feat/*`, `docs/*` | Satu isu / satu tujuan. Umur pendek. | — (PR ke `ZAQIxNOTEZ`). |

## Aturan merge

1. **Selalu PR**, jangan push langsung ke `main` atau `ZAQIxNOTEZ`. PR yang
   menarget `ZAQIxNOTEZ` otomatis menjalankan workflow `Build NOTEZ` (unit test
   + build debug APK).
2. **Base PR = `ZAQIxNOTEZ`**, bukan `main`. `main` hanya menerima PR dari
   `ZAQIxNOTEZ`.
3. **CI hijau wajib** sebelum merge. Workflow menjalankan `testDebugUnitTest`
   sebelum `assembleDebug`; guard yang tidak dijalankan CI bukan guard.
4. **Satu commit per batch** (lihat RFC/ADR), pesan commit mengikuti gaya repo.
5. **Jangan force-push** branch yang sudah punya PR terbuka.

## Rilis (main)

Rilis bukan sifat branch, tapi sifat build. "Main dalam keadaan release" berarti:

1. `gradle.properties`: `notez.versionName` dan `notez.versionCode` sudah
   dinaikkan.
2. `docs/RELEASE_NOTES_Vx.y.z.md` ditulis dengan bukti verifikasi.
3. `.github/workflows/release.yml`: token konfirmasi `BUILD-vx.y.z` disesuaikan
   dengan versi baru, dan workflow menjalankan `testDebugUnitTest` sebelum
   `assembleRelease`.
4. Dispatch manual `release.yml` dengan input `confirm=BUILD-vx.y.z`.
5. Workflow membangun APK **signed**, memverifikasi signature + badging,
   men-shred keystore, lalu mengunggah artifact `notez-vx.y.z-release`.
6. Tag `vx.y.z` dibuat di commit yang dirilis.
7. PR `ZAQIxNOTEZ → main` dibuka, CI hijau, lalu merge.

`Build NOTEZ` mengunggah APK **debug** dari semua build (`main`, `ZAQIxNOTEZ`,
dan PR) dengan nama artifact `notez-debug-<ref>`. Itu artefak verifikasi supaya
hasil integrasi bisa langsung dipasang di perangkat, **bukan** artefak rilis.
Satu-satunya artefak rilis adalah `notez-vx.y.z-release` dari `release.yml`
(signed, sudah diverifikasi `apksigner`).

## Realignment (satu kali, 2026-11-15)

Sebelum kesepakatan ini, `main` (4 commit) dan `ZAQIxNOTEZ` (7 commit) sudah
diverge: satu-satunya perbedaan konten adalah `docs/RELEASE_NOTES_V0.1.6.md`
yang ada di `ZAQIxNOTEZ` tapi tidak di `main`. Untuk menyelaraskan:

- PR `main → ZAQIxNOTEZ` dibuka untuk membawa commit yang hanya ada di `main`
  (#17 UI/UX POLISHED dan revisi README).
- Setelah itu, `main` dan `ZAQIxNOTEZ` harus berbagi merge-base yang sama, dan
  semua pekerjaan lanjut via PR ke `ZAQIxNOTEZ`.

## CI trigger

| Event | Workflow | Yang dijalankan |
|---|---|---|
| push ke `main` / `ZAQIxNOTEZ` | `Build NOTEZ` | `testDebugUnitTest` + `assembleDebug` |
| pull_request (base apa pun) | `Build NOTEZ` | `testDebugUnitTest` + `assembleDebug` |
| `workflow_dispatch` (manual) | `Build NOTEZ Production` | `testDebugUnitTest` + `assembleRelease` signed |

## Hal yang tidak boleh

- Push langsung ke `main` atau `ZAQIxNOTEZ`.
- Merge tanpa CI hijau.
- Mengubah token konfirmasi rilis tanpa menaikkan versi.
- Membangun rilis dari branch selain `main`.
