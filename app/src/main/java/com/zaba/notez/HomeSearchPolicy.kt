package com.zaba.notez

/**
 * Keputusan murni untuk chrome search di Home.
 *
 * Dipisahkan dari MainActivity supaya bisa diuji di JVM tanpa Android runtime,
 * dan supaya "belum diketahui" (observer belum mengirim emisi) tidak pernah
 * dianggap sama dengan "database kosong".
 *
 * Sumber kebenarannya adalah total catatan, bukan jumlah hasil filter: query
 * yang tidak cocok juga menghasilkan nol item dan tidak boleh menyembunyikan
 * search.
 */
internal object HomeSearchPolicy {

    /**
     * @param totalNotes jumlah seluruh catatan di database, atau null bila
     *   observer belum pernah mengirim emisi.
     * @return true hanya bila sudah terbukti ada minimal satu catatan.
     */
    fun toolbarVisible(totalNotes: Int?): Boolean = (totalNotes ?: 0) > 0
}
