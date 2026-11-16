# NOTEZ v0.1.8

**Status:** Production release candidate — per ubah di bawah belum dirilis; build signed harus diverifikasi sebelum publish.

**Version:** `versionName 0.1.8`, `versionCode 9`.

**Tag:** `v0.1.8` (dijadwalkan pada commit yang dirilis).

### Changed

- **Home empty search (A1)** — saat belum ada catatan, toolbar search disembunyikan sepenuhnya (`GONE`). Mata tidak muncul; kapsul kosong tidak menyisakan ruang.
- **Tampilan Card List / Card Grid (B1–B3)** — pilihan List/Grid ditambahkan di drawer Home. Default install/upgrade tetap List. Pilihan tersimpan, tidak mengubah isi, urutan, atau `updatedAt` catatan. Grid adaptif (1–3 kolom) dengan spacing terpisah dan inset tepi yang dikendalikan oleh RecyclerView.
- **Markdown definition list — inline formatting (C1-A / F1)** — isi definisi definition list kini memproses inline markdown (`**tebal**`, `` `kode` ``, link) seperti bagian dokumen lain, bukan tampil literal. Sanitizer dua lapis dan kebijakan URL tetap berlaku; tidak ada dependency atau kemampuan baru yang dibuka.

### Not included / sisa gerakan

- Verifikasi runtime Markdown C1-A (fixture red→green dan UAT perangkat) — belum dilakukan; perubahan bersifat source-level sebelum pengujian perangkat.
- Fitur Markdown tambahan (C1-B, C2) — menunggu keputusan.
- Math / Mermaid / attachment / wikilink — masih di luar scope sampai ada keputusan/kelayakan terpisah.

### Verification

Waiting CI (`testDebugUnitTest`, `assembleDebug`) dan UAT perangkat sebelum rilis.

### Known issues

- (kosong — diisi setelah UAT.)

### References

- `docs/RFC_HOME_SEARCH_CARD_GRID_MARKDOWN_2026_11_15.md`
- `docs/AUDIT_MARKDOWN_CAPABILITIES_2026_11_16.md`
