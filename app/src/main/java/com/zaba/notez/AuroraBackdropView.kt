package com.zaba.notez

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils

/**
 * Ambient background implementing authentic Aurora UI principles:
 * - 3 organic luminous blooms placed strategically across the canvas
 * - Rich chromatic saturation that radiates behind frosted glass cards
 * - Preserves ~35% of the frame as neutral valley to prevent visual fatigue
 * - Static radial shaders to maintain optimal 60fps scrolling performance
 */
class AuroraBackdropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val isLight = ThemePref.isLightTheme(ThemePref.get(context))
    private val themeOpt = ThemePref.optionOf(ThemePref.get(context))
    private val baseColor = UiStyle.windowBackground(context)

    private val accentColor = runCatching {
        context.getColor(themeOpt.accentColorRes)
    }.getOrDefault(UiStyle.primary(context))

    private val secondaryColor = runCatching {
        context.getColor(themeOpt.secondaryColorRes)
    }.getOrDefault(accentColor)

    // Third harmonic bloom: blend accent with secondary hue for chromatic richness
    private val harmonicColor = if (isLight) {
        ColorUtils.blendARGB(accentColor, secondaryColor, 0.45f)
    } else {
        ColorUtils.compositeColors(
            ColorUtils.setAlphaComponent(accentColor, 180),
            secondaryColor
        )
    }

    init {
        isClickable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(baseColor)

        val width = width.toFloat().coerceAtLeast(1f)
        val height = height.toFloat().coerceAtLeast(1f)
        val maxDim = width.coerceAtLeast(height)

        // Bloom 1: Header / Top-left light bloom (accent color, illuminates top bar & header)
        val radius1 = (maxDim * 0.75f).coerceAtLeast(260f)
        val alpha1 = if (isLight) 120 else 135
        drawAurora(canvas, width * 0.16f, height * 0.12f, radius1, accentColor, alpha1)

        // Bloom 2: Mid-right atmospheric aura (secondary color, balances list body)
        val radius2 = (maxDim * 0.70f).coerceAtLeast(240f)
        val alpha2 = if (isLight) 98 else 110
        drawAurora(canvas, width * 0.94f, height * 0.46f, radius2, secondaryColor, alpha2)

        // Bloom 3: Bottom-left ground glow (harmonic bloom, anchors footer & floating actions)
        val radius3 = (maxDim * 0.62f).coerceAtLeast(200f)
        val alpha3 = if (isLight) 82 else 92
        drawAurora(canvas, width * 0.32f, height * 0.90f, radius3, harmonicColor, alpha3)
    }

    private fun drawAurora(
        canvas: Canvas,
        x: Float,
        y: Float,
        radius: Float,
        color: Int,
        alpha: Int
    ) {
        paint.shader = RadialGradient(
            x,
            y,
            radius,
            intArrayOf(
                color.withAlpha(alpha),
                color.withAlpha((alpha * 0.52f).toInt()),
                color.withAlpha(0)
            ),
            floatArrayOf(0f, 0.46f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x, y, radius, paint)
        paint.shader = null
    }

    private fun Int.withAlpha(alpha: Int): Int = (this and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
}
