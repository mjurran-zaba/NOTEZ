# Uji pemicu dan efektivitas skill

Semua skill di koleksi ini berstatus DRAFT sampai lolos uji di bawah. Uji ini
sengaja sederhana supaya bisa dijalankan manual di Termux/ACode atau agent apa
pun yang mendukung folder skill.

## Cara menjalankan (±5 menit per skill)

1. Pasang folder `skills/` ke agent (lokasi bergantung agent).
2. Untuk tiap skill, kirim tiap kueri **"harus memicu"**. Catat apakah agent
   membaca skill itu (lihat log pemanggilan/baca file).
3. Kirim tiap kueri **"tidak boleh memicu"**. Skill tidak boleh terbaca.
4. Untuk 1-2 kueri terbaik per skill, nilai hasilnya: apakah jawaban/tindakan
   agent mengikuti aturan inti skill? Bandingkan dengan menjalankan kueri yang
   sama tanpa skill.
5. Hasil `lulus` bila: >= 2 dari 3 kueri positif memicu, 0 dari 2 kueri negatif
   memicu, dan perilaku lebih baik daripada tanpa skill. Catat hasil di
   `SKILLS.md` (kolom status) dan naikkan ke TESTED.
6. Kueri positif yang gagal memicu: perbaiki `description` (tambah kata pemicu
   yang dipakai pengguna). Kueri negatif yang salah memicu: persempit
   `description`.

## Kueri per skill

### evidence-and-honesty
Harus memicu:
- "Tulis ringkasan PR buat perbaikan bug upload ini, bilang udah beres ya."
- "Fix-nya udah jalan di HP? Tulis laporannya."
- "Draft handoff untuk sesi berikutnya, sertakan status verifikasi."
Tidak boleh memicu:
- "Jelaskan perbedaan List dan Set di Kotlin."
- "Buatkan warna tema gelap untuk aplikasi catatan."

### regression-guards-mutation
Harus memicu:
- "Bug crash null di parser ini udah gue benerin, tambahin test biar nggak balik lagi."
- "Tolong bikin guard buat mencegah import Compose ketinggalan."
- "Test ini beneran melindungi bug itu nggak? Buktikan."
Tidak boleh memicu:
- "Ubah warna tombol jadi biru."
- "Apa itu mutation di biologi?"

### armv7-android-constraints
Harus memicu:
- "Aplikasi crash SIGSEGV di HP lama 32-bit, padahal jalan di emulator."
- "Bisa nggak pakai wheel numpy dari PyPI di APK armeabi-v7a?"
- "Optimasi aplikasi ini biar enteng di HP RAM 2 GB."
Tidak boleh memicu:
- "Rancang API REST untuk toko online."
- "Cara bikin animasi transisi di web."

### verification-pyramid
Harus memicu:
- "Cukup nggak test lokal doang buat fitur download ini, atau perlu emulator?"
- "Tulis acceptance criteria buat fitur cancel."
- "Gimana cara ngetes kode retry tanpa nunggu 60 detik?"
Tidak boleh memicu:
- "Rapikan format README."
- "Terjemahkan kalimat ini ke Inggris."

### long-running-operations
Harus memicu:
- "Tombol Cancel nggak berhenti-berhenti, worker-nya masih jalan."
- "Naikin retry jadi 3 kali dan timeout 20 detik."
- "UI stuck pas instalasi paket, dua kali tap Start jadi dobel."
Tidak boleh memicu:
- "Bikin layout login dengan dua kolom."
- "Apa itu coroutine?" (pertanyaan definisi murni)

### research-before-design
Harus memicu:
- "Gimana aplikasi lain ngatur dependency resolver? Gue mau rancang sendiri."
- "Ada video tentang tool X, cocok nggak buat proyek gue?"
- "Pilih library cache yang bagus, bandingkan dulu."
Tidak boleh memicu:
- "Perbaiki typo di komentar ini."
- "Hitung 15% dari 240."

### blind-ci-diagnosis
Harus memicu:
- "CI merah tapi log nggak bisa dibuka, gimana nyari penyebabnya?"
- "Test baru gue nggak jalan di CI padahal lokal jalan."
- "Push ke .github/workflows ditolak, kenapa?"
Tidak boleh memicu:
- "Tambah halaman pengaturan baru di aplikasi."
- "Jelaskan apa itu Gradle."

### change-governance-handoff
Harus memicu:
- "Gue mau ubah mekanisme penyimpanan data, perlu RFC nggak?"
- "Boleh langsung lu ubah atau tanya dulu?"
- "Lanjutin kerjaan dari sesi kemarin, ini ringkasannya."
Tidak boleh memicu:
- "Ganti nama variabel x jadi count."
- "Tuliskan puisi pendek tentang hujan."

### android-compose-pitfalls
Harus memicu:
- "Force close pas keyboard muncul di layar landscape, ada TextField di dalam scroll."
- "Compose: var x by remember error di CI."
- "Editor CodeMirror di WebView, undo malah munculin isi file lain."
Tidak boleh memicu:
- "Buat halaman web statis dengan HTML dan CSS."
- "Apa bedanya Activity dan Fragment?" (definisi murni)

### android-release-signing
Harus memicu:
- "Gimana cara tanda tangani APK rilis lewat GitHub Actions dengan aman?"
- "Gue mau bikin build RC terpisah dari rilis produksi."
- "Keystore gue taruh di mana? Kirim aja ke lu ya."
Tidak boleh memicu:
- "Pasang aplikasi ini di HP lewat adb."
- "Tambahkan ikon aplikasi baru."

### constrained-sandbox-engineering
Harus memicu:
- "Sandbox bilang tidak bisa akses internet, jadi kita nyerah aja ya?"
- "Mau unduh system image 2 GB buat emulator, taruh mana?"
- "Disk hampir penuh, semua tool mati."
Tidak boleh memicu:
- "Rekomendasi laptop buat coding."
- "Buat fungsi untuk menghitung diskon."

## Catatan

- Kueri di atas contoh awal; tambah kueri dari percakapan nyata yang gagal
  memicu skill. Kueri negatif yang baik adalah "nyaris mirip tapi bukan"
  (kata kunci sama, kebutuhan berbeda).
- Pengujian ini mengukur pemicu dan kepatuhan dasar, belum kualitas jangka
  panjang. Ulangi saat skill diubah.
