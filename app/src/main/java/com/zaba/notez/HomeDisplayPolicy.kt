package com.zaba.notez

/**
 * Hitungan kolom Grid yang murni (tanpa Android), supaya bisa diuji di JVM.
 *
 * Aturannya sengaja konservatif: jangan pernah memaksa dua kolom hanya karena
 * layar lebar menurut dp, dan jangan pernah mengecilkan kartu di bawah lebar
 * minimum yang sudah diuji. Lebar minimum di sini adalah input, bukan konstanta
 * tersembunyi, supaya ambang untuk font besar bisa diubah dari pemanggil.
 */
internal object HomeDisplayPolicy {

    /** Lebar kartu minimum yang dipakai fase pertama (angka rancangan RFC). */
    const val MIN_CARD_WIDTH_DP = 160

    /** Batas kolom fase pertama. */
    const val MAX_COLUMNS = 3

    /**
     * @param availableWidthPx lebar area daftar setelah padding RecyclerView.
     * @param minCardWidthPx lebar minimum satu kartu pada font/config saat ini.
     * @param spacingPx jarak akhir antarkartu yang disediakan dekorasi.
     * @param maxColumns batas atas kolom (minimal 1).
     * @return 1..maxColumns, aman untuk lebar tidak valid (0/negatif).
     */
    fun spanCount(
        availableWidthPx: Int,
        minCardWidthPx: Int,
        spacingPx: Int = 0,
        maxColumns: Int = MAX_COLUMNS
    ): Int {
        val limit = maxColumns.coerceAtLeast(1)
        if (availableWidthPx <= 0 || minCardWidthPx <= 0) return 1
        val gap = spacingPx.coerceAtLeast(0)
        var columns = 1
        while (columns < limit) {
            val next = columns + 1
            val needed = minCardWidthPx * next + gap * (next - 1)
            if (availableWidthPx >= needed) columns = next else break
        }
        return columns
    }

    /**
     * Pilih posisi scroll setelah mode berganti.
     *
     * @param exactIndex index note jangkar di dataset terkini, atau negatif
     *   bila note sudah tidak ada.
     * @param fallbackPosition posisi jangkar saat mode sebelumnya.
     * @param itemCount jumlah item terkini.
     * @return index valid, atau -1 bila dataset kosong.
     */
    fun anchorIndex(exactIndex: Int, fallbackPosition: Int, itemCount: Int): Int {
        if (itemCount <= 0) return -1
        if (exactIndex in 0 until itemCount) return exactIndex
        return fallbackPosition.coerceIn(0, itemCount - 1)
    }
}
