# SKILLS: Perpustakaan Skill Global untuk Coding Agent

Koleksi skill yang bisa dipakai lintas proyek dan lintas agent. Fokus: coding,
pengembangan APK Android, desain UI dan kenyamanan UX, dan rekayasa untuk
perangkat ARMv7/low-end. Skill mengikuti format Agent Skills: satu folder
per skill, berisi `SKILL.md` dengan frontmatter `name` dan `description`.

File ini adalah **indeks dan registry**. Isi skill ada di `skills/<nama>/`.
Kontrak perilaku agent ada di `AGENTS.md`, bukan di sini.

Terakhir diperbarui: 2026-11-17.

## Cara pakai

1. Baca `description` tiap skill untuk menentukan yang relevan dengan tugas.
2. Buka `SKILL.md` skill itu; buka `references/` hanya bila diperlukan.
3. Jalankan `scripts/` sebagaimana diinstruksikan skill. Baca skrip dulu.
4. Lapisan proyek (`docs/`) bisa menambah aturan khusus proyek. Aturan proyek
   tidak mengubah skill; urutan prioritas ditentukan di `AGENTS.md`.

## Aturan koleksi

**Format**
- Folder `skills/<nama>/SKILL.md`. `name` huruf kecil, angka, tanda hubung,
  sama persis dengan nama folder.
- `description` menjelaskan apa yang dilakukan **dan kapan dipakai**,
  dwibahasa (Indonesia + English) agar mudah terpicu. Maksimal 1024 karakter.
- Isi `SKILL.md` di bawah 500 baris. Detail panjang masuk `references/`;
  skrip ke `scripts/`. Tautan referensi satu tingkat dari `SKILL.md`.
- Tulis perintah dan penjelasan alasannya, bukan sekadar larangan.

**Isi**
- Tidak ada hash commit, ID run CI, nomor versi rilis, hitungan test, atau
  status per rilis di dalam skill. Itu milik catatan rilis/laporan uji.
- Tidak ada data pribadi, nama akun, perangkat spesifik, atau kredensial.
- Satu skill, satu tujuan. Kategori campuran dipecah.
- Setiap skill dengan skrip: skrip harus sudah dijalankan dan hasil ujinya
  dicatat sebelum status naik dari DRAFT.

**Skill dari sumber luar**
- Provenance wajib: repo, commit SHA yang di-pin, tanggal audit.
- Lisensi dicek per folder skill. Tanpa lisensi jelas: `LINKED` saja
  (ditautkan, tidak disalin).
- Isi dibaca penuh sebelum diadopsi: skrip, perintah shell, instruksi yang
  menimpa aturan lain.
- Status: `LINKED` (hanya tautan), `ADOPTED` (disalin utuh dengan LICENSE asli),
  `ADAPTED` (diubah; catat perubahan dan sertakan LICENSE asli).
- Skill yang dimodifikasi tidak lagi boleh mengaku sebagai versi upstream.

**Status skill milik sendiri:** `DRAFT` (ditulis, belum diuji dengan agent),
`TESTED` (dijalankan pada skenario nyata, hasil tercatat), `PLANNED`
(belum ditulis). Cara menguji pemicu dan efektivitas: `tests/trigger-queries.md`.

## A. Skill milik sendiri

| Skill | Tujuan | Status | Asal |
|---|---|---|---|
| `evidence-and-honesty` | Kejujuran, tangga status, format laporan | DRAFT | SKILLS.md lama (Peraturan 1-2, 13, 14.3, 15.1) |
| `regression-guards-mutation` | Guard regresi + mutation check + skrip bantu | DRAFT (skrip diuji 4 skenario) | SKILLS.md lama (SKILL 2, 13.4) |
| `armv7-android-constraints` | ABI/libc, wheel native, verifikasi, performa, UX layar kecil | DRAFT | SKILLS.md lama (SKILL 3, 5, 13, 17) |
| `verification-pyramid` | Memilih bukti termurah yang cukup | DRAFT | SKILL 13, 5.3 |
| `long-running-operations` | Operasi panjang, timeout, pembatalan | DRAFT | SKILL 12 |
| `research-before-design` | Riset pembanding sebelum desain | DRAFT | SKILL 11, 24.1-24.2 |
| `blind-ci-diagnosis` | Diagnosis tanpa compiler lokal | DRAFT | SKILL 1, 27, 28.3, 28.5 |
| `change-governance-handoff` | RFC/ADR proporsional, matriks otonomi, handoff | DRAFT | SKILL 10, 14, 15 |
| `android-compose-pitfalls` | Jebakan Compose, WebView, editor | DRAFT | SKILL 4, 17, 18, 19 |
| `android-release-signing` | Signing dan promosi rilis tanpa hash/ID | DRAFT | SKILL 25, 26 |
| `constrained-sandbox-engineering` | Kerja di sandbox terbatas | DRAFT | SKILL 5.2-5.7, 24.3 |

Catatan: nomor SKILL merujuk ke SKILLS.md lama (khusus proyek). Bagian yang
sangat khusus proyek (peta kode, checklist rilis, jalan buntu, SKILL 7, 8, 9,
16, 20-23) belum diekstrak dan dipindah ke `docs/` proyek, bukan dihapus.

## B. Skill dari sumber luar

Audit 2026-11-16, tingkat SKILL.md. **Belum** mencakup isi `references/` dan
`scripts/` repo-repo ini. Lisensi dibaca dari file LICENSE di repo.

| Repo | Commit SHA (pin) | Lisensi | Rencana |
|---|---|---|---|
| obra/superpowers | 22f0b463e7a5e579904dd6211a520321035526f7 | MIT | ADAPT (lihat catatan) |
| chrisbanes/skills | fec6823e4dd0bcf82c2fe77896bc81d44c29200b | Apache-2.0 | ADOPT sebagian |
| android/skills | c72942584fff970dac86ced78b367fce0c441e52 | Apache-2.0 (per folder) | ADOPT sebagian |
| Kotlin/kotlin-agent-skills | 087f5689c1934135ed58718cdf524525e452b7e9 | Apache-2.0 | ADOPT/LINKED sebagian |
| anthropics/skills | 1780c5e5a552fd4d1c9d6a7b0cab26b0c2bef064 | Apache-2.0 (per folder) | ADOPT `skill-creator`; `frontend-design` untuk web |

Semua baris di atas berstatus **RENCANA**: belum ada isi yang disalin ke koleksi ini.

**Rencana per repo**
- **obra/superpowers (ADAPT):** ambil `systematic-debugging`,
  `test-driven-development`, `verification-before-completion`, `writing-skills`.
  Jangan ambil `using-superpowers` dan `brainstorming` apa adanya: mewajibkan
  pemanggilan skill sebelum respons apa pun (termasuk pertanyaan klarifikasi)
  dan bentrok dengan prinsip otonomi proporsional di `AGENTS.md`. Skill saling
  merujuk dengan prefiks `superpowers:`; hilangkan agar mandiri.
- **android/skills (ADOPT sebagian):** `jetpack-compose-performance`,
  `anr-audit-and-fix`, `r8-analyzer`, `android-profiler`, `edge-to-edge`,
  `adaptive`, `android-permissions-security`, `android-intent-security`,
  `testing-setup`. Lewati wear, tv, xr, cars, play, device-ai (LINKED).
  Peringatan: `android-cli` memasang alat lewat `curl | bash` dan daftar
  platformnya tidak mencakup ARMv7.
- **chrisbanes/skills (ADOPT sebagian):** skill Compose dan Kotlin
  (`compose-state-and-effects`, `compose-performance`, `compose-focus-navigation`,
  `kotlin-concurrency-and-flow`, dan sejenisnya). Lewati skill alur kerja
  khusus GitHub.
- **Kotlin/kotlin-agent-skills:** ambil yang relevan (mis. migrasi AGP);
  lewati CocoaPods/iOS (memuat `rm -rf` dan tidak relevan).
- **anthropics/skills:** `skill-creator` untuk menulis/menguji skill;
  `frontend-design` berorientasi web, bukan Android native.

**Celah yang ditemukan audit**
- Tidak ada skill Tier 1 yang membahas ARMv7/32-bit/low-end. Diisi skill
  milik sendiri (`armv7-android-constraints`).
- Cakupan UX mobile dan aksesibilitas Tier 1 tipis. Kandidat Tier 2 (belum
  diaudit, lisensi belum terlihat): RubenGlez/mobile-design,
  awesome-skills/mobile-app-design, ghaida/intent (`include`),
  HoangNguyen0403/agent-skills-standard (`android-performance`).

## C. Lapisan proyek

Aturan dan pelajaran khusus satu proyek tidak masuk skill global. Letakkan di
`docs/` proyek itu (peta kode, checklist rilis, jalan buntu, log uji,
catatan rilis). SKILLS.md lama proyek ZCODE menjadi sumber dokumen tersebut.

## D. Riwayat audit

| Tanggal | Cakupan | Catatan |
|---|---|---|
| 2026-11-16 | SKILLS.md lama; 103 SKILL.md dari 5 repo | Hasil: 3 skill pilot dibuat; skrip mutation_check diuji (4 skenario) |
| 2026-11-17 | Ekstraksi 8 skill milik sendiri dari SKILLS.md lama | Semua DRAFT; lolos validasi format dan referensi silang; belum diuji dengan agent (lihat `tests/trigger-queries.md`) |
