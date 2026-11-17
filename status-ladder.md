# Tangga status verifikasi

Daftar isi: 1. Level baku · 2. Varian lapisan bukti · 3. Aturan penulisan ·
4. Contoh kalimat · 5. Kesalahan umum

## 1. Level baku

| Level | Arti | Bukti minimum |
|---|---|---|
| DESIGNED | Perilaku dan kriteria penerimaan tertulis | dokumen desain/RFC |
| IMPLEMENTED | Kode ada | diff/commit |
| LOCALLY VERIFIED | Pemeriksaan lokal yang relevan lulus | output test, tanggal, perintah |
| CI VERIFIED | CI kanonik berhasil build/test | ID run dan job yang SUCCESS |
| PLATFORM/DEVICE VERIFIED | Runtime target atau perangkat nyata lulus skenario bernama | perangkat, OS/API, ABI, versi app, skenario, hasil |
| RELEASED | Artefak terverifikasi benar-benar didistribusikan | artefak + checksum yang sama dengan yang diuji |
| REGRESSION FOUND | Bukti baru membuka kembali klaim | bukti pembuka |

Status naik hanya dengan bukti baru. Status turun saat bukti lama dibatalkan
(misalnya dependensi berubah setelah uji perangkat).

## 2. Varian lapisan bukti

Beri nama lapisan yang benar-benar diuji. Contoh label yang sah:

- `BOOT VERIFIED` (emulator menyala, belum menjalankan skenario);
- `FULL-EMULATOR VERIFIED` (emulator penuh, bukan perangkat nyata);
- `BROWSER-HARNESS VERIFIED` (aset web diuji di browser terisolasi, bukan di
  WebView/host aslinya);
- `PRODUCTION SIGNED` (tanda tangan sesuai fingerprint yang diharapkan);
- `DEVICE OBSERVED` (teramati di perangkat tetapi bukan janji perilaku).

Label turunan tidak boleh dipakai untuk mengklaim level yang lebih tinggi.
Contoh: emulator hijau tidak berarti DEVICE VERIFIED.

## 3. Aturan penulisan

1. Tulis level tertinggi yang dibuktikan, bukan yang diharapkan.
2. Sebut target: perangkat/emulator, OS atau API level, ABI, versi app,
   kondisi jaringan, dan artefak (nama + checksum bila ada).
3. Pisahkan "diuji" dari "diperiksa": membaca README atau menonton video
   bukan menjalankan.
4. Draft, artifact CI, dan tag bukan rilis publik.
5. Ketahanan data antar-versi (update continuity) butuh dua versi nyata;
   jangan menyimpulkannya dari teori platform.

## 4. Contoh kalimat

- "Kode ada dan test lokal hijau (IMPLEMENTED + LOCALLY VERIFIED). CI belum
  dijalankan."
- "CI VERIFIED pada run 123 untuk commit abc. Belum diuji di perangkat."
- "Lulus 12 skenario di perangkat X (Android 14, armeabi-v7a, app 1.2.3).
  TalkBack belum diuji."
- "Browser-harness hijau pada aset yang identik byte-per-byte dengan yang
  dikirim. Ini bukan bukti perilaku WebView di perangkat."

## 5. Kesalahan umum

- Menyamakan "build berhasil" dengan "fitur berjalan".
- Menyamakan emulator dengan semua perangkat nyata.
- Menyamakan "import berhasil" dengan "fitur penuh berfungsi".
- Mempertahankan status lama setelah dependensi atau tanda tangan berubah.
- Mencatat status di banyak dokumen sehingga satu cepat basi. Simpan satu
  sumber kebenaran (catatan rilis atau laporan uji) dan tautkan dari tempat lain.
