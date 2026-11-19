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
                ColorUtils.setAlphaComponent(Color.WHITE, 205),
                ColorUtils.setAlphaComponent(outlineColor, 45)
            )
        } else {
            ColorUtils.compositeColors(
                ColorUtils.setAlphaComponent(Color.WHITE, 75),
                ColorUtils.setAlphaComponent(primaryColor, 110)
            )
        }

        setupBlooms()
    }

    private fun setupBlooms() {
        val opt = ThemePref.optionOf(ThemePref.get(context))
        if (opt.value == ThemePref.MATCHA_CREAM_PAPER) {
            // Palet 6 Warna Aurora Matcha Cream Paper (rekomendasi user + aksen tema)
            // Ditingkatkan saturasi & ke-jreng-annya agar berpendar mewah dan nyata
            val cLimeMint = Color.parseColor("#9ED872")
            val cCloudBlue = Color.parseColor("#8EBEF5")
            val cPaleYellow = Color.parseColor("#FCD14D")
            val cPeachAmber = Color.parseColor("#FBAC7A")
            val cMatchaLeaf = Color.parseColor("#4D8852")
            val cSageMoss = Color.parseColor("#60A370")

            bloomList = listOf(
                // 1. Fresh Lime Mint - Bukit Kiri-Atas (pucuk bercahaya)
                Bloom(0.10f, 0.14f, 0.72f, cLimeMint, 145),
                // 2. Biru Awan Pudar - Lembah Kabut Dingin Kanan-Atas (kontras es)
                Bloom(0.90f, 0.12f, 0.68f, cCloudBlue, 140),
                // 3. Kuning Pudar - Punggung Bukit Hangat Kanan-Tengah (pendaran madu cerah)
                Bloom(0.85f, 0.58f, 0.65f, cPaleYellow, 150),
                // 4. Warm Peach Amber - Pusaran Lembut Tengah-Kiri
                Bloom(0.25f, 0.52f, 0.58f, cPeachAmber, 130),
                // 5. Matcha Leaf - Jangkar Daun Bawah-Kiri
                Bloom(0.15f, 0.90f, 0.60f, cMatchaLeaf, 135),
                // 6. Soft Sage Moss - Kabut Teduh Bawah-Kanan
                Bloom(0.75f, 0.92f, 0.55f, cSageMoss, 125)
            )
        } else {
            val accent = UiStyle.accentTint(context)
            val secondary = UiStyle.secondaryTint(context)
            val coolShift = ColorUtils.blendARGB(accent, Color.parseColor("#38BDF8"), 0.35f)
            val warmShift = ColorUtils.blendARGB(secondary, Color.parseColor("#FCD34D"), 0.40f)
            val peachShift = ColorUtils.blendARGB(secondary, Color.parseColor("#FB7185"), 0.30f)
            val deepAccent = ColorUtils.blendARGB(accent, Color.BLACK, 0.20f)

            val baseAlpha = if (isLight) 125 else 105
            bloomList = listOf(
                Bloom(0.10f, 0.14f, 0.72f, accent, baseAlpha + 15),
                Bloom(0.90f, 0.12f, 0.68f, coolShift, baseAlpha + 10),
                Bloom(0.85f, 0.58f, 0.65f, warmShift, baseAlpha + 18),
                Bloom(0.25f, 0.52f, 0.58f, peachShift, baseAlpha),
                Bloom(0.15f, 0.90f, 0.60f, deepAccent, baseAlpha + 8),
                Bloom(0.75f, 0.92f, 0.55f, secondary, baseAlpha - 5)
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
        val topBaseAlpha = if (isLight) 140 else 200
        val bottomBaseAlpha = if (isLight) 80 else 115
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
                    ColorUtils.setAlphaComponent(b.color, (a * 0.55f).toInt()),
                    ColorUtils.setAlphaComponent(b.color, 0)
                ),
                floatArrayOf(0f, 0.52f, 1f),
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
