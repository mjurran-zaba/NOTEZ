package com.zaba.notez

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

/**
 * Custom RecyclerView that provides smooth, proportional fading edges matching
 * ScrollView's natural alpha-fade behavior.
 * Fades content into transparency so the Aurora theme background shines through
 * seamlessly without solid color overlays.
 */
class FadingRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    override fun getTopFadingEdgeStrength(): Float {
        val length = verticalFadingEdgeLength
        if (length <= 0) return 0f
        val offset = computeVerticalScrollOffset().coerceAtLeast(0)
        return (offset.toFloat() / length).coerceIn(0f, 1f)
    }

    override fun getBottomFadingEdgeStrength(): Float {
        val length = verticalFadingEdgeLength
        if (length <= 0) return 0f
        val offset = computeVerticalScrollOffset().coerceAtLeast(0)
        val extent = computeVerticalScrollExtent().coerceAtLeast(0)
        val range = computeVerticalScrollRange().coerceAtLeast(0)
        val remaining = range - (offset + extent)
        if (remaining <= 0) return 0f
        return (remaining.toFloat() / length).coerceIn(0f, 1f)
    }
}
