# Pelajaran emulasi ARMv7 dan sandbox terbatas

Daftar isi: 0. Konteks dan batas · 1. Sumber daya sandbox · 2. Diagnosis
jaringan · 3. Lapisan pembuktian di bawah emulator penuh · 4. Emulator Android
penuh ARM32 · 5. Kegagalan yang pernah terjadi · 6. Batas jujur

## 0. Konteks dan batas

Isi file ini dirangkum dari catatan satu proyek (Agustus 2026) yang bekerja di
sandbox Linux x86-64, RAM sekitar 1,9 GB, **tanpa `/dev/kvm`**. Angka, versi,
dan nama alat bisa berubah. Perlakukan sebagai titik awal, **ukur ulang di
lingkunganmu**, dan jangan menyalinnya sebagai fakta mutlak. Skrip dan path
spesifik proyek sengaja tidak dibawa ke sini.

## 1. Sumber daya sandbox

- **Disk adalah resource paling mudah dihabiskan.** Pernah satu image sistem
  ±1,8 GB diletakkan di direktori kerja yang ikut snapshot, ruang kerja
  penuh 100%, dan semua tool file/shell mati. Pelajaran:
  - letakkan image, AVD, cache Gradle eksperimental, dan arsip besar di
    direktori sementara **di luar** area kerja yang disnapshot;
  - ekstrak hanya file kecil yang dibutuhkan lalu **hapus** arsip/image;
  - jangan commit tree emulator;
  - cek `df` sebelum dan sesudah langkah besar.
- Direktori sementara bisa hilang saat sandbox restart. Buat pemasangan yang
  **idempotent** (bisa dijalankan ulang) dan simpan resepnya sebagai skrip.
- Jangan menjalankan build berat (Gradle) bersamaan dengan emulator.

## 2. Diagnosis jaringan: pecah dulu, baru simpulkan

"Tidak bisa diakses" harus dipecah sebelum dianggap diblokir:

- **Hostname vs sertifikat:** satu host memakai IP yang sama dengan host lain
  tetapi sertifikat milik host lain, sehingga muncul hostname mismatch. Solusinya
  memakai hostname yang benar, bukan menyimpulkan "diblokir".
- **User-Agent:** CDN dengan perlindungan bot dapat memberi 403 pada UA bawaan
  library (misalnya `Python-urllib/x.y`), tetapi 200 pada UA yang dikenal.
  Itu bukan blokir permanen.
- **Arsitektur/libc:** error `exec format error` atau symbol hilang berarti
  binary salah arsitektur/libc, bukan jaringan.
- Bandingkan dua jalur (host vs emulasi). Hasil berlawanan berarti
  verdict pertama keliru.

## 3. Lapisan pembuktian di bawah emulator penuh

Tiap lapisan menjawab pertanyaan yang berbeda; jangan menyamakannya.

| Lapisan | Apa itu | Membuktikan | Tidak membuktikan |
|---|---|---|---|
| A | Interpreter ARM32 **glibc** (user-mode emulation) | ISA 32-bit, logika resolver, versi Python sama | perilaku bionic/Android |
| B | Paket Termux (bionic, tapi versi runtime berbeda) | pustaka Termux | wheel/ekstensi yang dibangun untuk versi runtime lain (bisa gagal karena versi Cython/ABI beda) |
| C | Runtime **bionic** (linker + libc Android API >= 24) dengan **versi interpreter yang sama** dengan produksi, dijalankan lewat emulasi user-mode | import wheel native Android ARMv7 sungguhan, perilaku libc Android | JVM, Activity, UI, WebView, lifecycle |

Catatan teknis yang dilaporkan di lapisan C:

- Linker Android sebelum API 24 gagal membaca tabel hash modern
  (`DT_GNU_HASH`); gunakan linker dan libc dari image API 24 atau lebih baru.
  Ambil hanya file libc/linker yang diperlukan dari image, lalu hapus image.
- DNS bionic berbicara lewat socket UNIX ke daemon DNS Android
  (`/dev/socket/dnsproxyd`). Di emulasi user-mode, path socket itu harus ada di
  filesystem **host**; awalan root emulasi tidak berlaku untuk socket UNIX.
  Alternatifnya mengisi `/system/etc/hosts`.
- Jangan meng-export `PYTHONHOME` milik interpreter ARM sebelum menjalankan
  Python host, karena host akan memakai stdlib ARM dan gagal (misalnya
  `No module named '_socket'`).

## 4. Emulator Android penuh ARM32

Dipakai bila bug menyentuh jembatan Kotlin/Python, Activity, Compose, WebView,
PackageManager, atau lifecycle. Aturan dari pengalaman:

- Tanpa KVM, emulator tetap bisa jalan (lebih lambat, TCG). "Tidak ada KVM"
  bukan "tidak ada emulasi".
- Emulator SDK modern dapat menolak guest ARM32 dengan pesan arsitektur CPU
  `arm` tidak didukung. Proyek itu memakai emulator klasik versi lama
  (27.3.8) dan menguncinya; **jangan auto-update**. Verifikasi ketersediaannya
  saat kamu memakainya.
- Gunakan AVD terdaftar dengan layout SDK standar (`platform-tools/`,
  `system-images/android-<api>/.../armeabi-v7a`). Menjalankan engine
  langsung tanpa AVD hanya untuk diagnosis; ADB dan metadata sering tidak stabil.
- **Batasi RAM dengan dua pembatas:** opsi memori emulator **dan**
  argumen QEMU `-m`. Dengan satu pembatas saja, RSS bisa menembus 1,5 GB.
- **WebView butuh EGL pbuffer.** Mode GPU `off` membuat WebView Chromium
  abort walau headless. Pakai renderer perangkat lunak (SwiftShader);
  `-gpu off` hanya untuk boot/shell tanpa WebView.
- **minSdk produksi vs image resmi.** Image ARMv7 resmi berhenti di API
  rendah (24/25). APK produksi dengan minSdk lebih tinggi tidak bisa
  dipasang (`INSTALL_FAILED_OLDER_SDK`). Tidak ada bypass minSdk; jangan
  menurunkan minSdk produk demi emulator. Bangun varian **test-only** dengan
  minSdk rendah, dan label hasilnya jujur: boleh membuktikan Android/JVM/
  runtime, **bukan** DEVICE VERIFIED, dan tidak boleh dirilis.

## 5. Kegagalan yang pernah terjadi

| Percobaan | Gejala | Akar | Keputusan |
|---|---|---|---|
| Menyimpulkan "tidak bisa full Android" | berhenti di lapisan C | menyamakan tanpa-KVM dengan tanpa-emulasi | coba emulasi TCG |
| Emulator SDK terbaru + guest ARM32 | PANIC arsitektur CPU tidak didukung | mesin QEMU2 modern mencabut ARM32 | pin emulator klasik |
| AVD manual | `Broken AVD system path` | layout SDK tidak standar | buat layout standar |
| RAM bawaan | RSS ~1,55 GB, sandbox hampir habis | opsi memori diam-diam dinaikkan | dua pembatas RAM |
| `-gpu off` | WebView SIGABRT (EGL pbuffer) | WebView tetap butuh EGL | SwiftShader + RAM 512 MB |
| Pasang APK produksi di API 24 | `INSTALL_FAILED_OLDER_SDK` | minSdk produk > image | varian test-only |
| Mencari flag bypass minSdk | tidak ada | minSdk adalah kontrak kompatibilitas | berhenti mencari bypass |

Satu bukti kegunaan: emulator penuh menemukan bug (pembatalan ditelan oleh
jalur fallback lalu salah dilaporkan sebagai masalah kompatibilitas) yang lolos
411 test dan lapisan C. Jadi emulator penuh adalah gerbang nyata untuk
perubahan lifecycle/jembatan, bukan kosmetik.

## 6. Batas jujur

- Emulator dan lapisan di atas **bukan** perangkat nyata. Pengguna tetap perlu
  menguji di perangkat; laporkan hasil emulator dan UAT bersama, bukan
  menggantikan.
- Lapisan C tidak menjalankan JVM/UI/WebView.
- Setiap hasil diberi label lapisan yang benar (lihat skill
  `evidence-and-honesty`, `references/status-ladder.md`).
