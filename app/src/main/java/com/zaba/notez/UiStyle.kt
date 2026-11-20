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

    /**
     * Terapkan Glassmorphism + 3D Clay pada mini player mengambang.
     * Menggunakan volume pill halus, inner highlight top-left, dan inner shadow bottom-right
     * yang lembut dan tidak mencolok ("tidak terlalu menonjol").
     */
    fun applyMiniPlayerGlass(card: MaterialCardView, seed: Int = 0x4D494E49) {
        val context = card.context
        val isLight = ThemePref.isLightTheme(ThemePref.get(context))
        val radiusPx = dp(context, 28f).toFloat()

        card.setCardBackgroundColor(Color.TRANSPARENT)
        card.strokeColor = Color.TRANSPARENT
        card.strokeWidth = 0
        card.cardElevation = dp(context, 4f).toFloat()
        card.maxCardElevation = dp(context, 6f).toFloat()
        card.preventCornerOverlap = false

        card.background = AuroraGlassDrawable(
            context = context,
            radiusPx = radiusPx,
            isLight = isLight,
            surfaceColor = surface(context),
            primaryColor = primary(context),
            outlineColor = outline(context),
            alpha = if (isLight) 175 else 205,
            isSelected = false,
            seed = seed,
            isClay = true
        )

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
 * aurora blooms with unique abstract asymmetry per card,
 * with optional subtle 3D clay volumetric pill bevels.
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
    private val seed: Int = 0,
    private val isClay: Boolean = false
) : Drawable() {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bloomPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glazePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val innerBevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val innerShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
        if (isClay) {
            innerShadowPaint.strokeWidth = innerBevelWidthPx * 1.5f
        }
        setupOrganicBlooms()
    }

    private fun setupOrganicBlooms() {
        val opt = ThemePref.optionOf(ThemePref.get(context))
        val rng = kotlin.random.Random(seed)

        if (opt.value == ThemePref.GITHUB_DARK) {
            // Khusus GitHub Dark: Monochromatic Slate Dev Glass (tanpa spektrum aurora warna-warni ramai)
            val ghAccent = Color.parseColor("#58A6FF")
            chosenColors = listOf(ghAccent)
            bloomList = listOf(
                OrganicBloom(
                    relX = 0.85f,
                    relY = 0.18f,
                    relRadius = 0.45f,
                    color = ghAccent,
                    alpha = 60
                )
            )
            return
        }

        val fullPalette: List<Int> = if (isLight) {
            when (opt.value) {
                ThemePref.MATCHA_CREAM_PAPER -> listOf(
                    Color.parseColor("#9ED872"), // Lime Mint
                    Color.parseColor("#8EBEF5"), // Biru Awan Pudar
                    Color.parseColor("#FCD14D"), // Kuning Pudar
                    Color.parseColor("#FBAC7A"), // Warm Peach Amber
                    Color.parseColor("#4D8852"), // Matcha Leaf
                    Color.parseColor("#60A370")  // Soft Sage Moss
                )
                ThemePref.PARCHMENT_AMBER -> listOf(
                    Color.parseColor("#D97706"), // Terracotta Amber
                    Color.parseColor("#FBBF24"), // Warm Honey
                    Color.parseColor("#A16207"), // Classic Leather
                    Color.parseColor("#F59E0B"), // Golden Ink
                    Color.parseColor("#78350F"), // Aged Oak
                    Color.parseColor("#CA8A04")  // Parchment Sun
                )
                ThemePref.ROSE_PINE_DAWN -> listOf(
                    Color.parseColor("#B4637A"), // Dusty Rose
                    Color.parseColor("#EA9D34"), // Soft Gold
                    Color.parseColor("#56949F"), // Pine Teal
                    Color.parseColor("#907AA9"), // Iris Violet
                    Color.parseColor("#D7827E"), // Blush Dawn
                    Color.parseColor("#286983")  // Deep Pine
                )
                ThemePref.NORD_POLAR_MIST -> listOf(
                    Color.parseColor("#38BDF8"), // Polar Cyan
                    Color.parseColor("#81A1C1"), // Arctic Blue
                    Color.parseColor("#88C0D0"), // Frost Teal
                    Color.parseColor("#B48EAD"), // Aurora Purple
                    Color.parseColor("#EBCB8B"), // Winter Sun
                    Color.parseColor("#A3BE8C")  // Polar Aurora Green
                )
                else -> {
                    val accent = UiStyle.accentTint(context)
                    val secondary = UiStyle.secondaryTint(context)
                    val coolShift = ColorUtils.blendARGB(accent, Color.parseColor("#38BDF8"), 0.40f)
                    val warmShift = ColorUtils.blendARGB(secondary, Color.parseColor("#FBBF24"), 0.45f)
                    val violetShift = ColorUtils.blendARGB(accent, Color.parseColor("#A855F7"), 0.35f)
                    val deepShift = ColorUtils.blendARGB(accent, Color.BLACK, 0.25f)
                    listOf(accent, coolShift, warmShift, violetShift, deepShift, secondary)
                }
            }
        } else {
            // Opsi 1: Universal Cosmic Polar Aurora (Mellow & Tenang, anti-kolintang) untuk semua tema gelap
            listOf(
                Color.parseColor("#00F5A0"), // Bioluminescent Emerald
                Color.parseColor("#38BDF8"), // Arctic Celestial Cyan
                Color.parseColor("#8B5CF6"), // Cosmic Amethyst
                Color.parseColor("#4F46E5"), // Deep Twilight Indigo
                Color.parseColor("#F472B6"), // Subtle Solar Flare Pink
                Color.parseColor("#FBBF24")  // Starlight Amber
            )
        }

        // Pilih 3-4 warna di light mode, atau 2-3 warna di dark mode agar tetap tenang
        val count = if (isLight) rng.nextInt(3, 5) else rng.nextInt(2, 4)
        chosenColors = fullPalette.shuffled(rng).take(count)

        // Sebar titik-titik bukit aurora secara abstrak dan asimetris (bukan garis lurus paralel)
        // Di dark mode: alpha dipangkas menjadi ~56-70 agar menjadi kabut kosmik lembut
        val baseAlpha = if (isLight) 140 else 62
        bloomList = chosenColors.map { color ->
            OrganicBloom(
                relX = rng.nextFloat() * 0.76f + 0.12f,
                relY = rng.nextFloat() * 0.76f + 0.12f,
                relRadius = if (isLight) rng.nextFloat() * 0.32f + 0.48f else rng.nextFloat() * 0.35f + 0.50f,
                color = color,
                alpha = baseAlpha + rng.nextInt(-6, 8)
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
        val topBaseAlpha = if (isLight) (if (isClay) 150 else 130) else (if (isClay) 230 else 220)
        val bottomBaseAlpha = if (isLight) (if (isClay) 100 else 80) else (if (isClay) 160 else 140)
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
        val glazeAlpha = if (isLight) 44 else 24
        glazePaint.shader = LinearGradient(
            0f, 0f, w * 0.85f, h * 0.85f,
            intArrayOf(
                ColorUtils.setAlphaComponent(Color.WHITE, glazeAlpha),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        // 4. Inner Bevel Highlight (Refleksi Sudut Dalam Kaca Fisik & 3D Clay)
        val bevelTopAlpha = if (isLight) (if (isClay) 95 else 85) else (if (isClay) 65 else 50)
        innerBevelPaint.shader = LinearGradient(
            0f, 0f, w * 0.70f, h * 0.70f,
            intArrayOf(
                ColorUtils.setAlphaComponent(Color.WHITE, bevelTopAlpha),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        // 4b. 3D Clay Inner Shadow (Gradasi Sudut Bawah-Kanan, sentuhan lembut 3D clay)
        if (isClay) {
            val shadowColor = if (isLight) Color.parseColor("#261B12") else Color.BLACK
            val shadowAlpha = if (isLight) 36 else 55
            innerShadowPaint.shader = LinearGradient(
                w * 0.35f, h * 0.35f, w, h,
                intArrayOf(
                    Color.TRANSPARENT,
                    ColorUtils.setAlphaComponent(shadowColor, (shadowAlpha * 0.35f).toInt()),
                    ColorUtils.setAlphaComponent(shadowColor, shadowAlpha)
                ),
                floatArrayOf(0f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
        }

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
        } else if (opt.value == ThemePref.GITHUB_DARK) {
            val specularWhite = ColorUtils.setAlphaComponent(Color.WHITE, 140)
            val ghOutline = outlineColor
            val ghAccent = Color.parseColor("#58A6FF")
            val subtleEdge = ColorUtils.blendARGB(ghOutline, ghAccent, 0.25f)
            intArrayOf(
                specularWhite,
                subtleEdge,
                ghOutline,
                ColorUtils.setAlphaComponent(ghOutline, 180)
            )
        } else {
            val specularWhite = ColorUtils.setAlphaComponent(Color.WHITE, 160)
            val chromaticEdge = ColorUtils.blendARGB(primaryColor, Color.WHITE, 0.35f)
            val subtleFrost = ColorUtils.setAlphaComponent(Color.WHITE, 40)
            intArrayOf(
                specularWhite,
                ColorUtils.setAlphaComponent(chromaticEdge, 140),
                ColorUtils.setAlphaComponent(chromaticEdge, 90),
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

        // 4. Inner Bevel Highlight (Efek Sudut Kaca Fisik & 3D Clay)
        val innerRadius = (radiusPx - strokeWidthPx).coerceAtLeast(0f)
        if (innerRadius > 0f) {
            canvas.drawRoundRect(innerBevelRect, innerRadius, innerRadius, innerBevelPaint)
            if (isClay) {
                canvas.drawRoundRect(innerBevelRect, innerRadius, innerRadius, innerShadowPaint)
            }
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
