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
 * Theme-aware appearance helpers implementing authentic Auroramorphism & Glassmorphism:
 * - Translucent frosted glass card body (alpha 145) allowing Aurora backdrop to glow through
 * - Crisp specular white hairline rim (1.2dp) for optical glass definition
 * - Zero elevation to keep background aurora rendering smooth and predictable
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

    fun applyGlassCard(card: MaterialCardView, alpha: Int = 160, radiusDp: Float = 16f) {
        val context = card.context
        val isLight = ThemePref.isLightTheme(ThemePref.get(context))
        val surface = surface(context)
        val primary = primary(context)
        val outline = outline(context)

        val glassStroke = if (isLight) {
            ColorUtils.compositeColors(
                ColorUtils.setAlphaComponent(Color.WHITE, 190),
                ColorUtils.setAlphaComponent(outline, 70)
            )
        } else {
            ColorUtils.compositeColors(
                ColorUtils.setAlphaComponent(Color.WHITE, 65),
                ColorUtils.setAlphaComponent(primary, 100)
            )
        }

        val cardAlpha = if (isLight) alpha.coerceIn(140, 185) else alpha.coerceIn(160, 225)
        card.setCardBackgroundColor(ColorUtils.setAlphaComponent(surface, cardAlpha))
        card.setStrokeColor(glassStroke)
        card.strokeWidth = dp(context, 1.2f)
        card.setRadius(dp(context, radiusDp).toFloat())
        card.cardElevation = 0f
        card.maxCardElevation = 0f
        card.preventCornerOverlap = false
        card.useCompatPadding = false
        card.setContentPadding(0, 0, 0, 0)

        if (card.childCount > 0) {
            val inner = card.getChildAt(0)
            if (inner.background is GradientDrawable) {
                val pStart = inner.paddingStart
                val pTop = inner.paddingTop
                val pEnd = inner.paddingEnd
                val pBottom = inner.paddingBottom
                inner.background = null
                inner.setPaddingRelative(pStart, pTop, pEnd, pBottom)
            }
        }
    }

    fun applyDrawerGlass(view: View, alpha: Int = 230) {
        view.setBackgroundColor(ColorUtils.setAlphaComponent(surface(view.context), alpha.coerceIn(0, 255)))
    }

    fun roundedBackground(
        context: Context,
        fillAlpha: Int = 94,
        strokeAlpha: Int = 72,
        radiusDp: Float = 14f
    ): GradientDrawable {
        val isLight = ThemePref.isLightTheme(ThemePref.get(context))
        val actualFillAlpha = if (isLight) (fillAlpha * 1.35f).toInt().coerceIn(0, 240) else fillAlpha.coerceIn(0, 255)
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, radiusDp).toFloat()
            setColor(ColorUtils.setAlphaComponent(surface(context), actualFillAlpha))
            setStroke(dp(context, 1f), ColorUtils.setAlphaComponent(primary(context), strokeAlpha.coerceIn(0, 255)))
        }
    }

    fun dp(context: Context, value: Float): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()
}
