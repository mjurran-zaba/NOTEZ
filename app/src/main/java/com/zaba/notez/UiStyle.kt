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
 * - Multi-zone chromatic Aurora glass cards with organic abstract cloud diffusion
 * - Deterministic procedural seeding per card so every card is unique, living, and anti-slop
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
        isSelected: Boolean = false,
        seed: Int = card.hashCode()
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

        // 2. Pasang AuroraGlassDrawable dengan sebaran aurora abstrak organik acak (bukan garis pelangi)
        card.background = AuroraGlassDrawable(
            context = context,
            radiusPx = dp(context, radiusDp).toFloat(),
            isLight = isLight,
            surfaceColor = surface,
            primaryColor = primary(context),
            outlineColor = outline(context),
            alpha = alpha,
            isSelected = isSelected,
            seed = seed
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
 * Custom glass drawable that renders procedural, organic, non-linear
 * aurora blooms with unique abstract asymmetry per card.
 */
class AuroraGlassDrawable(
    private val context: Context,
    private val radiusPx: Float,
    private val isLight: Boolean,
    private val surfaceColor: Int,
    private val primaryColor: Int,
    private val outlineColor: Int,
    private val alpha: Int = 160,
    private val isSelected: Boolean = false,
    private val seed: Int = 0
) : Drawable() {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bloomPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glazePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val innerBevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val clipPath = Path()
    private val cardRect = RectF()
    private val strokeRect = RectF()
    private val innerBevelRect = RectF()

    private val strokeWidthPx = (if (isSelected) 2f else 1.2f) * context.resources.displayMetrics.density
    private val innerBevelWidthPx = 1.0f * context.resources.displayMetrics.density

    private data class OrganicBloom(
        val relX: Float,
        val relY: Float,
        val relRadius: Float,
        val color: Int,
        val alpha: Int
    )

    private var chosenColors: List<Int> = emptyList()
    private var bloomList: List<OrganicBloom> = emptyList()
    private var bloomShaders: List<RadialGradient> = emptyList()

    init {
        strokePaint.strokeWidth = strokeWidthPx
        innerBevelPaint.strokeWidth = innerBevelWidthPx
        setupOrganicBlooms()
    }

    private fun setupOrganicBlooms() {
        val opt = ThemePref.optionOf(ThemePref.get(context))
        val rng = kotlin.random.Random(seed)

        val fullPalette: List<Int> = if (opt.value == ThemePref.MATCHA_CREAM_PAPER) {
            // Palet 6 Warna Aurora Matcha Cream Paper (rekomendasi user + aksen tema)
            listOf(
                Color.parseColor("#9ED872"), // Lime Mint
                Color.parseColor("#8EBEF5"), // Biru Awan Pudar (Rekomendasi User)
                Color.parseColor("#FCD14D"), // Kuning Pudar (Rekomendasi User)
                Color.parseColor("#FBAC7A"), // Warm Peach Amber
                Color.parseColor("#4D8852"), // Matcha Leaf
                Color.parseColor("#60A370")  // Soft Sage Moss
            )
        } else {
            val accent = UiStyle.accentTint(context)
            val secondary = UiStyle.secondaryTint(context)
            val coolShift = ColorUtils.blendARGB(accent, Color.parseColor("#38BDF8"), 0.35f)
            val warmShift = ColorUtils.blendARGB(secondary, Color.parseColor("#FCD34D"), 0.40f)
            val peachShift = ColorUtils.blendARGB(secondary, Color.parseColor("#FB7185"), 0.30f)
            val deepAccent = ColorUtils.blendARGB(accent, Color.BLACK, 0.20f)
            listOf(accent, coolShift, warmShift, peachShift, deepAccent, secondary)
        }

        // Pilih 3 sampai 4 warna secara acak deterministik per kartu agar tidak menumpuk jadi becek
        val count = rng.nextInt(3, 5)
        chosenColors = fullPalette.shuffled(rng).take(count)

        // Sebar titik-titik bukit aurora secara abstrak dan asimetris (bukan garis lurus paralel)
        val baseAlpha = if (isLight) 140 else 115
        bloomList = chosenColors.map { color ->
            OrganicBloom(
                relX = rng.nextFloat() * 0.82f + 0.09f,
                relY = rng.nextFloat() * 0.82f + 0.09f,
                relRadius = rng.nextFloat() * 0.32f + 0.48f,
                color = color,
                alpha = baseAlpha + rng.nextInt(-15, 18)
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

        val bevelInset = strokeWidthPx + (innerBevelWidthPx / 2f)
        innerBevelRect.set(bounds.left + bevelInset, bounds.top + bevelInset, bounds.right - bevelInset, bounds.bottom - bevelInset)

        clipPath.reset()
        clipPath.addRoundRect(cardRect, radiusPx, radiusPx, Path.Direction.CW)

        // 1. Dasar Frosted Glass Transparan (menjaga keterbacaan teks tanpa memblokir warna)
        val baseColor = if (isLight) Color.WHITE else surfaceColor
        val topBaseAlpha = if (isLight) 130 else 195
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

        // 2. Pre-compute Radial Shaders untuk awan-awan aurora abstrak
        bloomShaders = bloomList.map { b ->
            val cx = w * b.relX
            val cy = h * b.relY
            val radius = maxDim * b.relRadius
            val a = b.alpha
            RadialGradient(
                cx, cy, radius,
                intArrayOf(
                    ColorUtils.setAlphaComponent(b.color, a),
                    ColorUtils.setAlphaComponent(b.color, (a * 0.45f).toInt()),
                    0
                ),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP
            )
        }

        // 3. Glaze pantulan kilap kaca diagonal 135 derajat
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

        // 4. Inner Bevel Highlight (Refleksi Sudut Dalam Kaca Fisik)
        val bevelTopAlpha = if (isLight) 85 else 50
        innerBevelPaint.shader = LinearGradient(
            0f, 0f, w * 0.70f, h * 0.70f,
            intArrayOf(
                ColorUtils.setAlphaComponent(Color.WHITE, bevelTopAlpha),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        // 5. Dynamic Prism Rim (Specular Refraction Border)
        val refractColor = if (isSelected) primaryColor else (chosenColors.firstOrNull() ?: primaryColor)
        val prismColors = if (isSelected) {
            intArrayOf(
                primaryColor,
                ColorUtils.setAlphaComponent(Color.WHITE, 240),
                primaryColor,
                ColorUtils.setAlphaComponent(primaryColor, 160)
            )
        } else if (isLight) {
            val specularWhite = ColorUtils.setAlphaComponent(Color.WHITE, 235)
            val chromaticEdge = ColorUtils.blendARGB(Color.WHITE, refractColor, 0.35f)
            val subtleFrost = ColorUtils.setAlphaComponent(Color.WHITE, 95)
            intArrayOf(
                specularWhite,
                ColorUtils.setAlphaComponent(chromaticEdge, 210),
                ColorUtils.setAlphaComponent(chromaticEdge, 140),
                subtleFrost
            )
        } else {
            val specularWhite = ColorUtils.setAlphaComponent(Color.WHITE, 180)
            val chromaticEdge = ColorUtils.blendARGB(primaryColor, Color.WHITE, 0.40f)
            val subtleFrost = ColorUtils.setAlphaComponent(Color.WHITE, 45)
            intArrayOf(
                specularWhite,
                ColorUtils.setAlphaComponent(chromaticEdge, 170),
                ColorUtils.setAlphaComponent(chromaticEdge, 110),
                subtleFrost
            )
        }
        strokePaint.shader = LinearGradient(
            0f, 0f, w, h,
            prismColors,
            floatArrayOf(0f, 0.28f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun draw(canvas: Canvas) {
        if (cardRect.isEmpty) return

        canvas.save()
        canvas.clipPath(clipPath)

        // 1. Dasar Frosted Glass
        canvas.drawRect(cardRect, basePaint)

        // 2. Awan-awan Aurora Abstrak Organik (Setiap kartu punya sebaran unik & hidup)
        for (shader in bloomShaders) {
            bloomPaint.shader = shader
            canvas.drawRect(cardRect, bloomPaint)
        }
        bloomPaint.shader = null

        // 3. Glaze Pantulan Kaca 135 Derajat
        canvas.drawRect(cardRect, glazePaint)

        // 4. Inner Bevel Highlight (Efek Sudut Kaca Fisik)
        val innerRadius = (radiusPx - strokeWidthPx).coerceAtLeast(0f)
        if (innerRadius > 0f) {
            canvas.drawRoundRect(innerBevelRect, innerRadius, innerRadius, innerBevelPaint)
        }

        canvas.restore()

        // 5. Dynamic Prism Specular Hairline Rim
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
