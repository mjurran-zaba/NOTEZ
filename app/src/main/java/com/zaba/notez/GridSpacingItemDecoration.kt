package com.zaba.notez

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Satu-satunya pemilik jarak antarkartu pada mode Grid.
 *
 * Layout `item_note_grid.xml` sengaja tanpa margin luar; inset tepi daftar
 * tetap milik padding RecyclerView. Hasil akhir jarak antarkartu mendekati
 * [spacingPx] (pembulatan pembagian integer per kolom), dan dekorasi ini harus
 * dilepas saat pindah ke List supaya tidak menumpuk saat mode diganti berulang.
 */
class GridSpacingItemDecoration(private val spacingPx: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val manager = parent.layoutManager as? GridLayoutManager
        val position = parent.getChildAdapterPosition(view)
        if (manager == null || position == RecyclerView.NO_POSITION) {
            outRect.set(0, 0, 0, 0)
            return
        }
        val span = manager.spanCount.coerceAtLeast(1)
        val column = position % span
        outRect.left = spacingPx * column / span
        outRect.right = spacingPx * (span - 1 - column) / span
        outRect.top = if (position < span) 0 else spacingPx / 2
        outRect.bottom = spacingPx / 2
    }
}
