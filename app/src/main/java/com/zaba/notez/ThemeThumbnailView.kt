package com.zaba.notez

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils

/**
 * Custom View that renders a miniature visual representation of a NOTEZ theme,
 * combining authentic Auroramorphism background blooms with Glassmorphism card surfaces.
 */
class ThemeThumbnailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var themeOption: ThemePref.ThemeOption? = null
    private var isActiveTheme: Boolean = false

    private val clipPath = Path()
    private val cardRect = RectF()
    private val miniCardRect = RectF()
    private val pillRect = RectF()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bloomPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val miniCardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val density = resources.displayMetrics.density

    fun bind(option: ThemePref.ThemeOption, isActive: Boolean) {
        this.themeOption = option
        this.isActiveTheme = isActive
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val opt = themeOption ?: return

        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        val cornerRadius = 13f * density

        val isLight = ThemePref.isLightTheme(opt.value)
        val baseColor = context.getColor(opt.backgroundColorRes)
        val accentColor = context.getColor(opt.accentColorRes)
        val secondaryColor = context.getColor(opt.secondaryColorRes)
        val surfaceColor = context.getColor(opt.surfaceColorRes)
        val textColor = context.getColor(opt.textColorRes)

        // 1. Clip canvas to rounded thumbnail shape
        cardRect.set(0f, 0f, w, h)
        clipPath.reset()
        clipPath.addRoundRect(cardRect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.clipPath(clipPath)

        // 2. Base theme background
        bgPaint.color = baseColor
        canvas.drawRect(cardRect, bgPaint)

        // 3. Bloom 1 (top-left accent bloom)
        val cx1 = w * 0.20f
        val cy1 = h * 0.22f
        val r1 = (w * 0.60f).coerceAtLeast(1f)
        val alpha1 = if (isLight) 115 else 145
        bloomPaint.shader = RadialGradient(
            cx1, cy1, r1,
            intArrayOf(
                ColorUtils.setAlphaComponent(accentColor, alpha1),
                ColorUtils.setAlphaComponent(accentColor, (alpha1 * 0.45f).toInt()),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx1, cy1, r1, bloomPaint)

        // 4. Bloom 2 (bottom-right secondary bloom)
        val cx2 = w * 0.82f
        val cy2 = h * 0.78f
        val r2 = (w * 0.50f).coerceAtLeast(1f)
        val alpha2 = if (isLight) 80 else 105
        bloomPaint.shader = RadialGradient(
            cx2, cy2, r2,
            intArrayOf(
                ColorUtils.setAlphaComponent(secondaryColor, alpha2),
                ColorUtils.setAlphaComponent(secondaryColor, (alpha2 * 0.40f).toInt()),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx2, cy2, r2, bloomPaint)
        bloomPaint.shader = null

        // 5. Mini Glass Card
        val padX = w * 0.08f
        val padY = h * 0.17f
        miniCardRect.set(padX, padY, w - padX, h - padY)
        val miniRadius = 8f * density

        val miniCardAlpha = if (isLight) 195 else 45
        val miniCardBase = if (isLight) Color.WHITE else surfaceColor
        miniCardPaint.color = ColorUtils.setAlphaComponent(miniCardBase, miniCardAlpha)
        canvas.drawRoundRect(miniCardRect, miniRadius, miniRadius, miniCardPaint)

        // Mini Card Specular Stroke
        strokePaint.strokeWidth = 1f * density
        strokePaint.color = ColorUtils.setAlphaComponent(Color.WHITE, if (isLight) 120 else 45)
        canvas.drawRoundRect(miniCardRect, miniRadius, miniRadius, strokePaint)

        // Mini Card Note Title Pill
        val pillLeft = miniCardRect.left + (8f * density)
        val pillTop = miniCardRect.top + (10f * density)
        val pillWidth = miniCardRect.width() * 0.38f
        val pillHeight = 4.5f * density
        pillRect.set(pillLeft, pillTop, pillLeft + pillWidth, pillTop + pillHeight)
        pillPaint.color = ColorUtils.setAlphaComponent(accentColor, 235)
        canvas.drawRoundRect(pillRect, pillHeight / 2f, pillHeight / 2f, pillPaint)

        // Mini Card Text Line 1
        val line1Top = pillTop + (10f * density)
        val line1Width = miniCardRect.width() * 0.72f
        val lineHeight = 2.5f * density
        pillRect.set(pillLeft, line1Top, pillLeft + line1Width, line1Top + lineHeight)
        pillPaint.color = ColorUtils.setAlphaComponent(textColor, if (isLight) 140 else 170)
        canvas.drawRoundRect(pillRect, lineHeight / 2f, lineHeight / 2f, pillPaint)

        // Mini Card Text Line 2
        val line2Top = line1Top + (7f * density)
        val line2Width = miniCardRect.width() * 0.50f
        pillRect.set(pillLeft, line2Top, pillLeft + line2Width, line2Top + lineHeight)
        pillPaint.color = ColorUtils.setAlphaComponent(textColor, if (isLight) 90 else 115)
        canvas.drawRoundRect(pillRect, lineHeight / 2f, lineHeight / 2f, pillPaint)

        // 6. Outer Border & Active Badge
        if (isActiveTheme) {
            strokePaint.strokeWidth = 2.2f * density
            strokePaint.color = accentColor
            canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, strokePaint)

            // Active Badge in top-right
            val badgeRadius = 9.5f * density
            val badgeX = w - badgeRadius - (5f * density)
            val badgeY = badgeRadius + (5f * density)
            pillPaint.color = accentColor
            canvas.drawCircle(badgeX, badgeY, badgeRadius, pillPaint)

            // Checkmark in badge
            val checkColor = if (isLight) Color.BLACK else Color.WHITE
            checkPaint.color = checkColor
            checkPaint.strokeWidth = 1.8f * density
            val path = Path().apply {
                moveTo(badgeX - (3.8f * density), badgeY)
                lineTo(badgeX - (1.0f * density), badgeY + (3.0f * density))
                lineTo(badgeX + (4.0f * density), badgeY - (2.8f * density))
            }
            canvas.drawPath(path, checkPaint)
        } else {
            strokePaint.strokeWidth = 1f * density
            strokePaint.color = ColorUtils.setAlphaComponent(Color.WHITE, if (isLight) 40 else 24)
            canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, strokePaint)
        }
    }
}
