package com.zaba.notez

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import androidx.core.graphics.ColorUtils
import com.google.android.material.R as MaterialR
import com.google.android.material.card.MaterialCardView

/**
 * Small, theme-aware appearance helpers used by the native surfaces.
 *
 * NOTEZ does not use a blur dependency for its glass effect. Instead, the
 * cards use a translucent theme surface, a quiet outline, and the aurora
 * backdrop underneath. It keeps scrolling and older devices predictable.
 */
object UiStyle {
    fun resolveColor(context: Context, attr: Int, fallback: Int): Int {
        val value = TypedValue()
        if (!context.theme.resolveAttribute(attr, value, true)) return fallback
        return when {
            value.resourceId != 0 -> runCatching { context.getColor(value.resourceId) }.getOrDefault(fallback)
            value.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT -> value.data
            else -> fallback
        }
    }

    fun surface(context: Context): Int = resolveColor(context, MaterialR.attr.colorSurface, Color.WHITE)

    fun primary(context: Context): Int = resolveColor(context, MaterialR.attr.colorPrimary, Color.WHITE)

    fun outline(context: Context): Int = resolveColor(context, MaterialR.attr.colorOutline, Color.WHITE)

    fun windowBackground(context: Context): Int =
        resolveColor(context, android.R.attr.windowBackground, surface(context))

    fun applyGlassCard(card: MaterialCardView, alpha: Int = 185, radiusDp: Float = 18f) {
        val context = card.context
        val surface = surface(context)
        val primary = primary(context)
        val outline = outline(context)
        val glassStroke = ColorUtils.compositeColors(
            ColorUtils.setAlphaComponent(primary, 55),
            ColorUtils.setAlphaComponent(outline, 70)
        )
        card.setCardBackgroundColor(ColorUtils.setAlphaComponent(surface, alpha.coerceIn(0, 255)))
        card.setStrokeColor(glassStroke)
        card.strokeWidth = dp(context, 1f)
        card.setRadius(dp(context, radiusDp).toFloat())
        card.cardElevation = 0f
        card.maxCardElevation = 0f
        card.preventCornerOverlap = false
        card.useCompatPadding = false
        card.setContentPadding(0, 0, 0, 0)
    }

    fun applyDrawerGlass(view: View, alpha: Int = 230) {
        view.setBackgroundColor(ColorUtils.setAlphaComponent(surface(view.context), alpha.coerceIn(0, 255)))
    }

    fun roundedBackground(
        context: Context,
        fillAlpha: Int = 94,
        strokeAlpha: Int = 72,
        radiusDp: Float = 14f
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(context, radiusDp).toFloat()
        setColor(ColorUtils.setAlphaComponent(surface(context), fillAlpha.coerceIn(0, 255)))
        setStroke(dp(context, 1f), ColorUtils.setAlphaComponent(primary(context), strokeAlpha.coerceIn(0, 255)))
    }

    fun dp(context: Context, value: Float): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()
}
