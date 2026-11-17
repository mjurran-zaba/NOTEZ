#!/usr/bin/env bash
# mutation_check.sh - buktikan bahwa sebuah test/guard benar-benar bisa gagal.
#
# Pemakaian:
#   mutation_check.sh --file PATH --sed 's/lama/baru/' -- PERINTAH_TEST [ARG...]
#
# Alur:
#   1. baseline : PERINTAH_TEST harus LULUS pada kode saat ini
#   2. mutasi   : terapkan ekspresi sed ke --file
#   3. uji      : PERINTAH_TEST harus GAGAL (kalau lulus, guard palsu)
#   4. pulihkan : kembalikan file asli, pastikan identik
#   5. akhir    : PERINTAH_TEST harus LULUS lagi
#
# Kode keluar: 0 = guard terbukti; 1 = guard palsu; 2 = baseline/pemulihan
# bermasalah; 64 = salah pakai.
# Catatan: mutasi yang menghasilkan error sintaks juga "gagal"; periksa output
# agar kegagalan benar-benar berasal dari asersi, bukan dari kompilasi.

set -u

# Cache berbasis mtime (mis. .pyc Python menyimpan mtime dalam detik + ukuran)
# bisa memakai kode lama bila file termutasi seukuran dan diubah di detik yang
# sama. Matikan penulisan bytecode dan beri jeda 1 detik sebelum tiap tulis.
export PYTHONDONTWRITEBYTECODE=1
tick() { sleep 1; }

FILE=""; EXPR=""
while [ $# -gt 0 ]; do
  case "$1" in
    --file) FILE="${2:-}"; shift 2 ;;
    --sed)  EXPR="${2:-}"; shift 2 ;;
    --)     shift; break ;;
    *)      echo "argumen tidak dikenal: $1" >&2; exit 64 ;;
  esac
done

if [ -z "$FILE" ] || [ -z "$EXPR" ] || [ $# -eq 0 ]; then
  echo "pakai: $0 --file PATH --sed 'EKSPRESI' -- PERINTAH_TEST [ARG...]" >&2
  exit 64
fi
[ -f "$FILE" ] || { echo "file tidak ada: $FILE" >&2; exit 64; }

BACKUP="$(mktemp)"
cp -p -- "$FILE" "$BACKUP" || exit 2
restore() { cp -p -- "$BACKUP" "$FILE"; rm -f -- "$BACKUP"; }
trap restore EXIT INT TERM

run_tests() { "$@" >/tmp/mutation_check.out 2>&1; }

echo "[1/5] baseline: test harus lulus"
if ! run_tests "$@"; then
  echo "GAGAL: baseline sudah merah. Perbaiki dulu sebelum menguji guard." >&2
  tail -n 15 /tmp/mutation_check.out >&2
  exit 2
fi

echo "[2/5] terapkan mutasi: $EXPR"
MUTATED="$(mktemp)"
sed -e "$EXPR" -- "$FILE" > "$MUTATED" || { echo "sed gagal" >&2; exit 64; }
if cmp -s -- "$FILE" "$MUTATED"; then
  echo "GAGAL: ekspresi sed tidak mengubah file (tidak ada yang cocok)." >&2
  rm -f -- "$MUTATED"
  exit 64
fi
tick
cp -- "$MUTATED" "$FILE"; rm -f -- "$MUTATED"

echo "[3/5] test harus gagal pada kode termutasi"
if run_tests "$@"; then
  echo "GUARD PALSU: test tetap lulus setelah mutasi." >&2
  RESULT=1
else
  echo "ok: test merah. Cuplikan kegagalan (pastikan dari asersi, bukan sintaks):"
  tail -n 8 /tmp/mutation_check.out
  RESULT=0
fi

echo "[4/5] pulihkan file asli"
tick
cp -p -- "$BACKUP" "$FILE"
if ! cmp -s -- "$BACKUP" "$FILE"; then
  echo "GAGAL: file tidak identik setelah pemulihan." >&2
  exit 2
fi

echo "[5/5] test harus lulus lagi"
if ! run_tests "$@"; then
  echo "GAGAL: test merah setelah pemulihan." >&2
  tail -n 15 /tmp/mutation_check.out >&2
  exit 2
fi

if [ "$RESULT" -eq 0 ]; then echo "HASIL: guard terbukti (merah saat mutasi, hijau saat pulih)."; fi
exit "$RESULT"
