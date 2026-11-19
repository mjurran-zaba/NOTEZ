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
 * Custom glass drawable that renders 6 abstract organic aurora zones
 * with smooth radial feathering & refraction over translucent frosted glass.
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
    private val bloomPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glazePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val clipPath = Path()
    private val cardRect = RectF()
    private val strokeRect = RectF()

    private val strokeWidthPx = (if (isSelected) 2f else 1.2f) * context.resources.displayMetrics.density

    private data class Bloom(
        val relX: Float,
        val relY: Float,
        val relRadius: Float,
        val color: Int,
        val alpha: Int
    )

    private var bloomList: List<Bloom> = emptyList()
    private var radialShaders: List<RadialGradient> = emptyList()

    init {
        strokePaint.strokeWidth = strokeWidthPx
        strokePaint.color = if (isSelected) {
            primaryColor
        } else if (isLight) {
            ColorUtils.compositeColors(
                ColorUtils.setAlphaComponent(Color.WHITE, 190),
                ColorUtils.setAlphaComponent(outlineColor, 60)
            )
        } else {
            ColorUtils.compositeColors(
                ColorUtils.setAlphaComponent(Color.WHITE, 65),
                ColorUtils.setAlphaComponent(primaryColor, 100)
            )
        }

        setupBlooms()
    }

    private fun setupBlooms() {
        val opt = ThemePref.optionOf(ThemePref.get(context))
        if (opt.value == ThemePref.MATCHA_CREAM_PAPER) {
            // Palet 6 Warna Aurora Matcha Cream Paper (rekomendasi user + aksen tema)
            // 1. Fresh Lime Mint: #A8C686
            // 2. Biru Awan Pudar (Rekomendasi User): #A2C2E8
            // 3. Kuning Pudar (Rekomendasi User): #F7E1A0
            // 4. Warm Peach Amber: #F8CCA8
            // 5. Matcha Leaf: #5E8C61
            // 6. Soft Sage Moss: #729B79
            val cLimeMint = Color.parseColor("#A8C686")
            val cCloudBlue = Color.parseColor("#A2C2E8")
            val cPaleYellow = Color.parseColor("#F7E1A0")
            val cPeachAmber = Color.parseColor("#F8CCA8")
            val cMatchaLeaf = Color.parseColor("#5E8C61")
            val cSageMoss = Color.parseColor("#729B79")

            bloomList = listOf(
                // 1. Fresh Lime Mint - Bukit Kiri-Atas (pucuk bercahaya)
                Bloom(0.12f, 0.16f, 0.70f, cLimeMint, 88),
                // 2. Biru Awan Pudar - Lembah Kabut Dingin Kanan-Atas (kontras es)
                Bloom(0.88f, 0.14f, 0.65f, cCloudBlue, 82),
                // 3. Kuning Pudar - Punggung Bukit Hangat Kanan-Tengah (pendaran madu)
                Bloom(0.82f, 0.60f, 0.62f, cPaleYellow, 90),
                // 4. Warm Peach Amber - Pusaran Lembut Tengah-Kiri
                Bloom(0.30f, 0.54f, 0.55f, cPeachAmber, 72),
                // 5. Matcha Leaf - Jangkar Daun Bawah-Kiri
                Bloom(0.18f, 0.90f, 0.58f, cMatchaLeaf, 78),
                // 6. Soft Sage Moss - Kabut Teduh Bawah-Kanan
                Bloom(0.72f, 0.92f, 0.52f, cSageMoss, 68)
            )
        } else {
            val accent = UiStyle.accentTint(context)
            val secondary = UiStyle.secondaryTint(context)
            val coolShift = ColorUtils.blendARGB(accent, Color.parseColor("#38BDF8"), 0.35f)
            val warmShift = ColorUtils.blendARGB(secondary, Color.parseColor("#FCD34D"), 0.40f)
            val peachShift = ColorUtils.blendARGB(secondary, Color.parseColor("#FB7185"), 0.30f)
            val deepAccent = ColorUtils.blendARGB(accent, Color.BLACK, 0.20f)

            val baseAlpha = if (isLight) 78 else 58
            bloomList = listOf(
                Bloom(0.12f, 0.16f, 0.70f, accent, baseAlpha + 10),
                Bloom(0.88f, 0.14f, 0.65f, coolShift, baseAlpha),
                Bloom(0.82f, 0.60f, 0.62f, warmShift, baseAlpha + 12),
                Bloom(0.30f, 0.54f, 0.55f, peachShift, baseAlpha - 5),
                Bloom(0.18f, 0.90f, 0.58f, deepAccent, baseAlpha),
                Bloom(0.72f, 0.92f, 0.52f, secondary, baseAlpha - 10)
            )
        }
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        if (bounds.width() <= 0 || bounds.height() <= 0) return

        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        val maxDim = w.coerceAtLeast(h)

        cardRect.set(bounds)
        val inset = strokeWidthPx / 2f
        strokeRect.set(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset)

        clipPath.reset()
        clipPath.addRoundRect(cardRect, radiusPx, radiusPx, Path.Direction.CW)

        val baseColor = if (isLight) Color.WHITE else surfaceColor
        val topBaseAlpha = if (isLight) 185 else 210
        val bottomBaseAlpha = if (isLight) 115 else 125
        basePaint.shader = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(
                ColorUtils.setAlphaComponent(baseColor, topBaseAlpha),
                ColorUtils.setAlphaComponent(baseColor, bottomBaseAlpha)
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        radialShaders = bloomList.map { b ->
            val cx = w * b.relX
            val cy = h * b.relY
            val radius = maxDim * b.relRadius
            val a = b.alpha
            RadialGradient(
                cx, cy, radius,
                intArrayOf(
                    ColorUtils.setAlphaComponent(b.color, a),
                    ColorUtils.setAlphaComponent(b.color, (a * 0.48f).toInt()),
                    ColorUtils.setAlphaComponent(b.color, 0)
                ),
                floatArrayOf(0f, 0.46f, 1f),
                Shader.TileMode.CLAMP
            )
        }

        val glazeAlpha = if (isLight) 48 else 24
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

        // 2. 6 Zona Spektrum Aurora Abstrak
        for (shader in radialShaders) {
            bloomPaint.shader = shader
            canvas.drawRect(cardRect, bloomPaint)
        }
        bloomPaint.shader = null

        // 3. Glaze Specular Kaca 135 Derajat
        canvas.drawRect(cardRect, glazePaint)

        canvas.restore()

        // 4. Specular Hairline Rim
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
