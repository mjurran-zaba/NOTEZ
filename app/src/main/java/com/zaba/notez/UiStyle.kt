package com.zaba.notez

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import androidx.core.graphics.ColorUtils
import com.google.android.material.R as MaterialR
import com.google.android.material.card.MaterialCardView

/**
 * Theme-aware appearance helpers implementing authentic Auroramorphism & Glassmorphism:
 * - Multi-zone chromatic Aurora glass cards with organic directional blending
 * - Specular hairline rims mimicking real frosted glass edges
 * - High-contrast text legibility with tactile specular light reflection
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

    fun accentTint(context: Context): Int {
        val opt = ThemePref.optionOf(ThemePref.get(context))
        return if (opt.value == ThemePref.MATCHA_CREAM_PAPER) {
            Color.parseColor("#A8C686")
        } else {
            runCatching { context.getColor(opt.accentColorRes) }.getOrDefault(primary(context))
        }
    }

    fun secondaryTint(context: Context): Int {
        val opt = ThemePref.optionOf(ThemePref.get(context))
        return if (opt.value == ThemePref.MATCHA_CREAM_PAPER) {
            Color.parseColor("#F5E6B8")
        } else {
            runCatching { context.getColor(opt.secondaryColorRes) }.getOrDefault(accentTint(context))
        }
    }

    fun applyGlassCard(
        card: MaterialCardView,
        alpha: Int = 160,
        radiusDp: Float = 16f,
        isSelected: Boolean = false
    ) {
        val context = card.context
        val isLight = ThemePref.isLightTheme(ThemePref.get(context))
        val surface = surface(context)

        // 1. Matikan background solid MaterialCardView agar custom AuroraGlassDrawable aktif
        card.setCardBackgroundColor(Color.TRANSPARENT)
        card.cardElevation = 0f
        card.maxCardElevation = 0f
        card.preventCornerOverlap = false
        card.useCompatPadding = false
        card.setContentPadding(0, 0, 0, 0)
        card.setRadius(dp(context, radiusDp).toFloat())
        card.strokeColor = Color.TRANSPARENT
        card.strokeWidth = 0

        // 2. Pasang AuroraGlassDrawable dengan 6 zona spektrum aurora abstrak bergelombang
        card.background = AuroraGlassDrawable(
            context = context,
            radiusPx = dp(context, radiusDp).toFloat(),
            isLight = isLight,
            surfaceColor = surface,
            primaryColor = primary(context),
            outlineColor = outline(context),
            alpha = alpha,
            isSelected = isSelected
        )

        // 3. Pastikan inner view tidak menumpuk background yang menghalangi
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

/**
 * Custom glass drawable that renders 6 distinct organic aurora zones
 * via flowing multi-stop linear gradient coupled with organic polar wave hills.
 */
class AuroraGlassDrawable(
    private val context: Context,
    private val radiusPx: Float,
    private val isLight: Boolean,
    private val surfaceColor: Int,
    private val primaryColor: Int,
    private val outlineColor: Int,
    private val alpha: Int = 160,
    private val isSelected: Boolean = false
) : Drawable() {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val auroraFlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hillAPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hillBPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glazePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val clipPath = Path()
    private val cardRect = RectF()
    private val strokeRect = RectF()

    private val strokeWidthPx = (if (isSelected) 2f else 1.2f) * context.resources.displayMetrics.density

    init {
        strokePaint.strokeWidth = strokeWidthPx
        strokePaint.color = if (isSelected) {
            primaryColor
        } else if (isLight) {
            ColorUtils.compositeColors(
                ColorUtils.setAlphaComponent(Color.WHITE, 205),
                ColorUtils.setAlphaComponent(outlineColor, 45)
            )
        } else {
            ColorUtils.compositeColors(
                ColorUtils.setAlphaComponent(Color.WHITE, 75),
                ColorUtils.setAlphaComponent(primaryColor, 110)
            )
        }
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        if (bounds.width() <= 0 || bounds.height() <= 0) return

        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()

        cardRect.set(bounds)
        val inset = strokeWidthPx / 2f
        strokeRect.set(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset)

        clipPath.reset()
        clipPath.addRoundRect(cardRect, radiusPx, radiusPx, Path.Direction.CW)

        // 1. Dasar Frosted Glass Transparan (menjaga kontras teks tanpa menutup warna aurora)
        val baseColor = if (isLight) Color.WHITE else surfaceColor
        val topBaseAlpha = if (isLight) 120 else 190
        val bottomBaseAlpha = if (isLight) 70 else 110
        basePaint.shader = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(
                ColorUtils.setAlphaComponent(baseColor, topBaseAlpha),
                ColorUtils.setAlphaComponent(baseColor, bottomBaseAlpha)
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        // 2. Aliran 6 Spektrum Warna Aurora (Multi-Stop Linear: setiap warna punya zona tersendiri, blend mulus tanpa bertumpuk jadi becek)
        val opt = ThemePref.optionOf(ThemePref.get(context))
        val (auroraColors, cHillA, cHillB) = if (opt.value == ThemePref.MATCHA_CREAM_PAPER) {
            val cLimeMint = Color.parseColor("#9ED872")
            val cCloudBlue = Color.parseColor("#8EBEF5")
            val cPaleYellow = Color.parseColor("#FCD14D")
            val cPeachAmber = Color.parseColor("#FBAC7A")
            val cMatchaLeaf = Color.parseColor("#4D8852")
            val cSageMoss = Color.parseColor("#60A370")

            Triple(
                intArrayOf(
                    ColorUtils.setAlphaComponent(cLimeMint, 145),   // Stop 0.00: Lime Mint (Kiri-Atas)
                    ColorUtils.setAlphaComponent(cCloudBlue, 140),  // Stop 0.20: Biru Awan Pudar
                    ColorUtils.setAlphaComponent(cPaleYellow, 155), // Stop 0.42: Kuning Pudar (Puncak Terang)
                    ColorUtils.setAlphaComponent(cPeachAmber, 135), // Stop 0.62: Warm Peach Amber
                    ColorUtils.setAlphaComponent(cMatchaLeaf, 140), // Stop 0.82: Matcha Leaf
                    ColorUtils.setAlphaComponent(cSageMoss, 130)   // Stop 1.00: Soft Sage (Kanan-Bawah)
                ),
                cCloudBlue,
                cPaleYellow
            )
        } else {
            val accent = UiStyle.accentTint(context)
            val secondary = UiStyle.secondaryTint(context)
            val coolShift = ColorUtils.blendARGB(accent, Color.parseColor("#38BDF8"), 0.35f)
            val warmShift = ColorUtils.blendARGB(secondary, Color.parseColor("#FCD34D"), 0.40f)
            val peachShift = ColorUtils.blendARGB(secondary, Color.parseColor("#FB7185"), 0.30f)
            val deepAccent = ColorUtils.blendARGB(accent, Color.BLACK, 0.20f)
            val a = if (isLight) 130 else 110

            Triple(
                intArrayOf(
                    ColorUtils.setAlphaComponent(accent, a),
                    ColorUtils.setAlphaComponent(coolShift, a - 5),
                    ColorUtils.setAlphaComponent(warmShift, a + 10),
                    ColorUtils.setAlphaComponent(peachShift, a - 10),
                    ColorUtils.setAlphaComponent(deepAccent, a),
                    ColorUtils.setAlphaComponent(secondary, a - 15)
                ),
                coolShift,
                warmShift
            )
        }

        val auroraPositions = floatArrayOf(0.0f, 0.20f, 0.42f, 0.62f, 0.82f, 1.0f)
        auroraFlowPaint.shader = LinearGradient(
            0f, h * 0.15f,
            w, h * 0.85f,
            auroraColors,
            auroraPositions,
            Shader.TileMode.CLAMP
        )

        // 3. Kontur Bukit Aurora Organik (Memberi gelombang/lembah alami aurora kutub bergelombang)
        // Bukit A: Puncak Awan Dingin di atas kiri-tengah
        val radiusHillA = (h * 1.15f).coerceAtLeast(w * 0.45f)
        hillAPaint.shader = RadialGradient(
            w * 0.30f, 0f, radiusHillA,
            intArrayOf(
                ColorUtils.setAlphaComponent(cHillA, 115),
                ColorUtils.setAlphaComponent(cHillA, 55),
                0
            ),
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP
        )

        // Bukit B: Punggung Madu Hangat di bawah kanan-tengah
        val radiusHillB = (h * 1.20f).coerceAtLeast(w * 0.48f)
        hillBPaint.shader = RadialGradient(
            w * 0.55f, h, radiusHillB,
            intArrayOf(
                ColorUtils.setAlphaComponent(cHillB, 125),
                ColorUtils.setAlphaComponent(cHillB, 60),
                0
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP
        )

        // 4. Glaze pantulan kaca diagonal 135 derajat
        val glazeAlpha = if (isLight) 44 else 22
        glazePaint.shader = LinearGradient(
            0f, 0f, w * 0.85f, h * 0.85f,
            intArrayOf(
                ColorUtils.setAlphaComponent(Color.WHITE, glazeAlpha),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun draw(canvas: Canvas) {
        if (cardRect.isEmpty) return

        canvas.save()
        canvas.clipPath(clipPath)

        // 1. Dasar Frosted Glass
        canvas.drawRect(cardRect, basePaint)

        // 2. Aliran 6 Spektrum Warna Aurora (Linear Multi-Stop, zona warna terdefinisi)
        canvas.drawRect(cardRect, auroraFlowPaint)

        // 3. Kontur Bukit & Ombak Aurora Organik
        canvas.drawRect(cardRect, hillAPaint)
        canvas.drawRect(cardRect, hillBPaint)

        // 4. Glaze Pantulan Kaca 135 Derajat
        canvas.drawRect(cardRect, glazePaint)

        canvas.restore()

        // 5. Specular Hairline Rim
        canvas.drawRoundRect(strokeRect, radiusPx, radiusPx, strokePaint)
    }

    override fun setAlpha(alpha: Int) {
        basePaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        basePaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
