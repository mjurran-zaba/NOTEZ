package com.zaba.notez

import android.content.Context

/** Pilihan tema NOTEZ. Disimpan di SharedPreferences, diterapkan via setTheme(). */
object ThemePref {
    const val OLED = 0
    const val GITHUB_DARK = 1
    const val COBALT2 = 2
    const val NOTEZ_YOU_DARK = 3
    const val FADE_CHOCO_MATCHA = 4
    const val GLOOMY_SAKURA_NIGHT = 5
    const val RASPBERRY_NIGHT = 6
    const val BLUE_MOON_CHEESE = 7
    const val NOTEZ_YOU_WARM = 8
    const val GLOOMY_LAVENDER = 9
    const val DARK_FOREST = 10
    const val TOKYO_NIGHT = 11
    const val KAWAII_CATPUCINN = 12
    const val GLOOME_DARK_SUNSET = 13
    const val AURORA_BOREALIS = 14
    const val CYBER_OBSIDIAN = 15
    const val SUNSET_MIRAGE = 16
    const val MATCHA_CREAM_PAPER = 17
    const val PARCHMENT_AMBER = 18
    const val ROSE_PINE_DAWN = 19
    const val NORD_POLAR_MIST = 20

    private const val PREF = "notez"
    private const val KEY = "theme"

    data class ThemeOption(
        val value: Int,
        val name: String,
        val description: String,
        val styleRes: Int,
        val backgroundColorRes: Int,
        val surfaceColorRes: Int,
        val textColorRes: Int,
        val secondaryColorRes: Int,
        val accentColorRes: Int,
        val outlineColorRes: Int,
        val dangerColorRes: Int
    )

    val OPTIONS = arrayOf(
        ThemeOption(
            value = GITHUB_DARK,
            name = "GitHub Dark",
            description = "Markdown/readme vibe yang seimbang.",
            styleRes = R.style.Theme_Notez_GithubDark,
            backgroundColorRes = R.color.github_bg,
            surfaceColorRes = R.color.github_surface,
            textColorRes = R.color.github_text,
            secondaryColorRes = R.color.github_secondary,
            accentColorRes = R.color.github_accent,
            outlineColorRes = R.color.github_outline,
            dangerColorRes = R.color.github_danger
        ),
        ThemeOption(
            value = NOTEZ_YOU_DARK,
            name = "NOTEZ You Dark",
            description = "Material-ish dark yang kalem dan private.",
            styleRes = R.style.Theme_Notez_NotezYouDark,
            backgroundColorRes = R.color.notez_you_bg,
            surfaceColorRes = R.color.notez_you_surface,
            textColorRes = R.color.notez_you_text,
            secondaryColorRes = R.color.notez_you_secondary,
            accentColorRes = R.color.notez_you_accent,
            outlineColorRes = R.color.notez_you_outline,
            dangerColorRes = R.color.notez_you_danger
        ),
        ThemeOption(
            value = NOTEZ_YOU_WARM,
            name = "NOTEZ You Warm",
            description = "Hangat seperti lampu meja malam.",
            styleRes = R.style.Theme_Notez_NotezYouWarm,
            backgroundColorRes = R.color.notez_you_warm_bg,
            surfaceColorRes = R.color.notez_you_warm_surface,
            textColorRes = R.color.notez_you_warm_text,
            secondaryColorRes = R.color.notez_you_warm_secondary,
            accentColorRes = R.color.notez_you_warm_accent,
            outlineColorRes = R.color.notez_you_warm_outline,
            dangerColorRes = R.color.notez_you_warm_danger
        ),
        ThemeOption(
            value = FADE_CHOCO_MATCHA,
            name = "Fade Choco Matcha",
            description = "Cokelat lembut dengan aksen matcha.",
            styleRes = R.style.Theme_Notez_FadeChocoMatcha,
            backgroundColorRes = R.color.fade_choco_matcha_bg,
            surfaceColorRes = R.color.fade_choco_matcha_surface,
            textColorRes = R.color.fade_choco_matcha_text,
            secondaryColorRes = R.color.fade_choco_matcha_secondary,
            accentColorRes = R.color.fade_choco_matcha_accent,
            outlineColorRes = R.color.fade_choco_matcha_outline,
            dangerColorRes = R.color.fade_choco_matcha_danger
        ),
        ThemeOption(
            value = BLUE_MOON_CHEESE,
            name = "Blue Moon Cheese",
            description = "Biru bulan dengan aksen cheese glow.",
            styleRes = R.style.Theme_Notez_BlueMoonCheese,
            backgroundColorRes = R.color.blue_moon_cheese_bg,
            surfaceColorRes = R.color.blue_moon_cheese_surface,
            textColorRes = R.color.blue_moon_cheese_text,
            secondaryColorRes = R.color.blue_moon_cheese_secondary,
            accentColorRes = R.color.blue_moon_cheese_accent,
            outlineColorRes = R.color.blue_moon_cheese_outline,
            dangerColorRes = R.color.blue_moon_cheese_danger
        ),
        ThemeOption(
            value = RASPBERRY_NIGHT,
            name = "Raspberry Night",
            description = "Berry gelap yang manis tapi tetap elegan.",
            styleRes = R.style.Theme_Notez_RaspberryNight,
            backgroundColorRes = R.color.raspberry_night_bg,
            surfaceColorRes = R.color.raspberry_night_surface,
            textColorRes = R.color.raspberry_night_text,
            secondaryColorRes = R.color.raspberry_night_secondary,
            accentColorRes = R.color.raspberry_night_accent,
            outlineColorRes = R.color.raspberry_night_outline,
            dangerColorRes = R.color.raspberry_night_danger
        ),
        ThemeOption(
            value = GLOOMY_SAKURA_NIGHT,
            name = "Gloomy Sakura Night",
            description = "Sakura malam, soft tapi gloomy.",
            styleRes = R.style.Theme_Notez_GloomySakuraNight,
            backgroundColorRes = R.color.gloomy_sakura_night_bg,
            surfaceColorRes = R.color.gloomy_sakura_night_surface,
            textColorRes = R.color.gloomy_sakura_night_text,
            secondaryColorRes = R.color.gloomy_sakura_night_secondary,
            accentColorRes = R.color.gloomy_sakura_night_accent,
            outlineColorRes = R.color.gloomy_sakura_night_outline,
            dangerColorRes = R.color.gloomy_sakura_night_danger
        ),
        ThemeOption(
            value = GLOOMY_LAVENDER,
            name = "Gloomy Lavender",
            description = "Lavender gelap yang calm dan dreamy.",
            styleRes = R.style.Theme_Notez_GloomyLavender,
            backgroundColorRes = R.color.gloomy_lavender_bg,
            surfaceColorRes = R.color.gloomy_lavender_surface,
            textColorRes = R.color.gloomy_lavender_text,
            secondaryColorRes = R.color.gloomy_lavender_secondary,
            accentColorRes = R.color.gloomy_lavender_accent,
            outlineColorRes = R.color.gloomy_lavender_outline,
            dangerColorRes = R.color.gloomy_lavender_danger
        ),
        ThemeOption(
            value = GLOOME_DARK_SUNSET,
            name = "Gloome Dark Sunset",
            description = "Sunset redup dengan aksen oranye hangat.",
            styleRes = R.style.Theme_Notez_GloomeDarkSunset,
            backgroundColorRes = R.color.gloome_dark_sunset_bg,
            surfaceColorRes = R.color.gloome_dark_sunset_surface,
            textColorRes = R.color.gloome_dark_sunset_text,
            secondaryColorRes = R.color.gloome_dark_sunset_secondary,
            accentColorRes = R.color.gloome_dark_sunset_accent,
            outlineColorRes = R.color.gloome_dark_sunset_outline,
            dangerColorRes = R.color.gloome_dark_sunset_danger
        ),
        ThemeOption(
            value = DARK_FOREST,
            name = "Dark Forest",
            description = "Hijau hutan gelap yang adem.",
            styleRes = R.style.Theme_Notez_DarkForest,
            backgroundColorRes = R.color.dark_forest_bg,
            surfaceColorRes = R.color.dark_forest_surface,
            textColorRes = R.color.dark_forest_text,
            secondaryColorRes = R.color.dark_forest_secondary,
            accentColorRes = R.color.dark_forest_accent,
            outlineColorRes = R.color.dark_forest_outline,
            dangerColorRes = R.color.dark_forest_danger
        ),
        ThemeOption(
            value = TOKYO_NIGHT,
            name = "Tokyo Night",
            description = "Biru kota malam dengan neon soft.",
            styleRes = R.style.Theme_Notez_TokyoNight,
            backgroundColorRes = R.color.tokyo_night_bg,
            surfaceColorRes = R.color.tokyo_night_surface,
            textColorRes = R.color.tokyo_night_text,
            secondaryColorRes = R.color.tokyo_night_secondary,
            accentColorRes = R.color.tokyo_night_accent,
            outlineColorRes = R.color.tokyo_night_outline,
            dangerColorRes = R.color.tokyo_night_danger
        ),
        ThemeOption(
            value = KAWAII_CATPUCINN,
            name = "Kawaii Catpucinn",
            description = "Mocha kawaii dengan pastel malam.",
            styleRes = R.style.Theme_Notez_KawaiiCatpucinn,
            backgroundColorRes = R.color.kawaii_catpucinn_bg,
            surfaceColorRes = R.color.kawaii_catpucinn_surface,
            textColorRes = R.color.kawaii_catpucinn_text,
            secondaryColorRes = R.color.kawaii_catpucinn_secondary,
            accentColorRes = R.color.kawaii_catpucinn_accent,
            outlineColorRes = R.color.kawaii_catpucinn_outline,
            dangerColorRes = R.color.kawaii_catpucinn_danger
        ),
        ThemeOption(
            value = OLED,
            name = "OLED Black",
            description = "Hitam fokus dengan aksen hijau NOTEZ.",
            styleRes = R.style.Theme_Notez,
            backgroundColorRes = R.color.oled_bg,
            surfaceColorRes = R.color.oled_surface,
            textColorRes = R.color.oled_text,
            secondaryColorRes = R.color.oled_secondary,
            accentColorRes = R.color.oled_accent,
            outlineColorRes = R.color.oled_outline,
            dangerColorRes = R.color.oled_danger
        ),
        ThemeOption(
            value = COBALT2,
            name = "Cobalt2",
            description = "Playful coder theme dengan aksen kuning.",
            styleRes = R.style.Theme_Notez_Cobalt2,
            backgroundColorRes = R.color.cobalt_bg,
            surfaceColorRes = R.color.cobalt_surface,
            textColorRes = R.color.cobalt_text,
            secondaryColorRes = R.color.cobalt_secondary,
            accentColorRes = R.color.cobalt_accent,
            outlineColorRes = R.color.cobalt_outline,
            dangerColorRes = R.color.cobalt_danger
        ),
        ThemeOption(
            value = AURORA_BOREALIS,
            name = "Aurora Borealis",
            description = "Hijau zamrud & cyan kosmik dengan kabut kaca imersif.",
            styleRes = R.style.Theme_Notez_AuroraBorealis,
            backgroundColorRes = R.color.aurora_borealis_bg,
            surfaceColorRes = R.color.aurora_borealis_surface,
            textColorRes = R.color.aurora_borealis_text,
            secondaryColorRes = R.color.aurora_borealis_secondary,
            accentColorRes = R.color.aurora_borealis_accent,
            outlineColorRes = R.color.aurora_borealis_outline,
            dangerColorRes = R.color.aurora_borealis_danger
        ),
        ThemeOption(
            value = CYBER_OBSIDIAN,
            name = "Cyber Obsidian",
            description = "Obsidian pekat dengan aksen ungu neon elektrik.",
            styleRes = R.style.Theme_Notez_CyberObsidian,
            backgroundColorRes = R.color.cyber_obsidian_bg,
            surfaceColorRes = R.color.cyber_obsidian_surface,
            textColorRes = R.color.cyber_obsidian_text,
            secondaryColorRes = R.color.cyber_obsidian_secondary,
            accentColorRes = R.color.cyber_obsidian_accent,
            outlineColorRes = R.color.cyber_obsidian_outline,
            dangerColorRes = R.color.cyber_obsidian_danger
        ),
        ThemeOption(
            value = SUNSET_MIRAGE,
            name = "Sunset Mirage",
            description = "Senja hangat dengan pantulan amber dan persik.",
            styleRes = R.style.Theme_Notez_SunsetMirage,
            backgroundColorRes = R.color.sunset_mirage_bg,
            surfaceColorRes = R.color.sunset_mirage_surface,
            textColorRes = R.color.sunset_mirage_text,
            secondaryColorRes = R.color.sunset_mirage_secondary,
            accentColorRes = R.color.sunset_mirage_accent,
            outlineColorRes = R.color.sunset_mirage_outline,
            dangerColorRes = R.color.sunset_mirage_danger
        ),
        ThemeOption(
            value = MATCHA_CREAM_PAPER,
            name = "Matcha Cream Paper",
            description = "Mode siang/kertas teduh dengan aksen daun matcha.",
            styleRes = R.style.Theme_Notez_MatchaCreamPaper,
            backgroundColorRes = R.color.matcha_cream_paper_bg,
            surfaceColorRes = R.color.matcha_cream_paper_surface,
            textColorRes = R.color.matcha_cream_paper_text,
            secondaryColorRes = R.color.matcha_cream_paper_secondary,
            accentColorRes = R.color.matcha_cream_paper_accent,
            outlineColorRes = R.color.matcha_cream_paper_outline,
            dangerColorRes = R.color.matcha_cream_paper_danger
        ),
        ThemeOption(
            value = PARCHMENT_AMBER,
            name = "Parchment Amber",
            description = "Kertas buku klasik hangat dengan aksen kulit terracotta.",
            styleRes = R.style.Theme_Notez_ParchmentAmber,
            backgroundColorRes = R.color.parchment_amber_bg,
            surfaceColorRes = R.color.parchment_amber_surface,
            textColorRes = R.color.parchment_amber_text,
            secondaryColorRes = R.color.parchment_amber_secondary,
            accentColorRes = R.color.parchment_amber_accent,
            outlineColorRes = R.color.parchment_amber_outline,
            dangerColorRes = R.color.parchment_amber_danger
        ),
        ThemeOption(
            value = ROSE_PINE_DAWN,
            name = "Rosé Pine Dawn",
            description = "Nuansa kedai kopi sore yang aesthetic, blush, dan dusty rose.",
            styleRes = R.style.Theme_Notez_RosePineDawn,
            backgroundColorRes = R.color.rose_pine_dawn_bg,
            surfaceColorRes = R.color.rose_pine_dawn_surface,
            textColorRes = R.color.rose_pine_dawn_text,
            secondaryColorRes = R.color.rose_pine_dawn_secondary,
            accentColorRes = R.color.rose_pine_dawn_accent,
            outlineColorRes = R.color.rose_pine_dawn_outline,
            dangerColorRes = R.color.rose_pine_dawn_danger
        ),
        ThemeOption(
            value = NORD_POLAR_MIST,
            name = "Nord Polar Mist",
            description = "Kebiruan kabut salju Skandinavia yang dingin, bersih, dan segar.",
            styleRes = R.style.Theme_Notez_NordPolarMist,
            backgroundColorRes = R.color.nord_polar_mist_bg,
            surfaceColorRes = R.color.nord_polar_mist_surface,
            textColorRes = R.color.nord_polar_mist_text,
            secondaryColorRes = R.color.nord_polar_mist_secondary,
            accentColorRes = R.color.nord_polar_mist_accent,
            outlineColorRes = R.color.nord_polar_mist_outline,
            dangerColorRes = R.color.nord_polar_mist_danger
        )
    )

    val NAMES: Array<String>
        get() = OPTIONS.map { it.name }.toTypedArray()

    fun isLightTheme(value: Int): Boolean =
        value in setOf(MATCHA_CREAM_PAPER, PARCHMENT_AMBER, ROSE_PINE_DAWN, NORD_POLAR_MIST)

    fun get(context: Context): Int =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(KEY, GITHUB_DARK)

    fun set(context: Context, value: Int) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putInt(KEY, value).apply()
    }

    fun optionOf(value: Int): ThemeOption =
        OPTIONS.firstOrNull { it.value == value } ?: OPTIONS.first { it.value == GITHUB_DARK }

    fun nameOf(value: Int): String = optionOf(value).name

    fun styleOf(value: Int): Int = optionOf(value).styleRes
}
