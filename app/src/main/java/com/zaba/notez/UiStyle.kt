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

    fun applyGlassCard(
        card: MaterialCardView,
        alpha: Int = 160,
        radiusDp: Float = 16f,
        isSelected: Boolean = false
    ) {
        val context = card.context
        val isLight = ThemePref.isLightTheme(ThemePref.get(context))
        val surface = surface(context)

        // 1. Matikan background solid MaterialCardView agar GradientDrawable kaca yang aktif
        card.setCardBackgroundColor(Color.TRANSPARENT)
        card.cardElevation = 0f
        card.maxCardElevation = 0f
        card.preventCornerOverlap = false
        card.useCompatPadding = false
        card.setContentPadding(0, 0, 0, 0)
        card.setRadius(dp(context, radiusDp).toFloat())
        card.strokeColor = Color.TRANSPARENT
        card.strokeWidth = 0

        // 2. Gradien Diagonal 135° (TL_BR) + Tint Wash Organik
        val glassColors = if (isLight) {
            val opt = ThemePref.optionOf(ThemePref.get(context))
            val accentTint = if (opt.value == ThemePref.MATCHA_CREAM_PAPER) {
                Color.parseColor("#A8C686")
            } else {
                runCatching { context.getColor(opt.accentColorRes) }.getOrDefault(primary(context))
            }
            val secondaryTint = if (opt.value == ThemePref.MATCHA_CREAM_PAPER) {
                Color.parseColor("#F5E6B8")
            } else {
                runCatching { context.getColor(opt.secondaryColorRes) }.getOrDefault(accentTint)
            }
            intArrayOf(
                // Start (kiri-atas): putih 75% + tint matcha 12%
                ColorUtils.compositeColors(
                    ColorUtils.setAlphaComponent(Color.WHITE, 190),
                    ColorUtils.setAlphaComponent(accentTint, 30)
                ),
                // Tengah: putih 55% + tint honey 12%
                ColorUtils.compositeColors(
                    ColorUtils.setAlphaComponent(Color.WHITE, 140),
                    ColorUtils.setAlphaComponent(secondaryTint, 30)
                ),
                // End (kanan-bawah): putih 40% + transparan, membiarkan aurora mekar tembus
                ColorUtils.setAlphaComponent(Color.WHITE, 100)
            )
        } else {
            val topAlpha = (alpha + 50).coerceIn(0, 255)
            val bottomAlpha = (alpha - 50).coerceIn(0, 255)
            intArrayOf(
                ColorUtils.compositeColors(
                    ColorUtils.setAlphaComponent(surface, topAlpha),
                    ColorUtils.setAlphaComponent(Color.WHITE, 25)
                ),
                ColorUtils.setAlphaComponent(surface, alpha),
                ColorUtils.setAlphaComponent(surface, bottomAlpha)
            )
        }

        // 3. Specular rim hairline border (putih berkilau atau warna seleksi)
        val strokeColor = if (isSelected) {
            primary(context)
        } else if (isLight) {
            ColorUtils.setAlphaComponent(Color.WHITE, 180)
        } else {
            ColorUtils.compositeColors(
                ColorUtils.setAlphaComponent(Color.WHITE, 65),
                ColorUtils.setAlphaComponent(primary(context), 100)
            )
        }
        val strokeWidth = if (isSelected) dp(context, 2f) else dp(context, 1.2f)

        val glass = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            glassColors
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, radiusDp).toFloat()
            setStroke(strokeWidth, strokeColor)
        }

        // 4. Pasang sebagai background view langsung
        card.background = glass

        // 5. Pastikan inner view tidak menumpuk background yang menghalangi
        if (card.childCount > 0) {
            val inner = card.getChildAt(0)
            inner.background = null
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
