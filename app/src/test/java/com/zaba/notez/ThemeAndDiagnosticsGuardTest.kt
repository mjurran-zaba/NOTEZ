package com.zaba.notez

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression & Contract Guards for ADR-004:
 * 1. Pillar 1: APK Footprint Optimization (resourceConfigurations)
 * 2. Pillar 2: Curated Themes & Glassmorphism Backdrop
 * 3. Pillar 3: ADB Logcat Diagnostics Console
 */
class ThemeAndDiagnosticsGuardTest {

    @Test
    fun resourceConfigurationsGuard() {
        val candidates = listOf(File("build.gradle.kts"), File("app/build.gradle.kts"))
        val buildGradle = candidates.firstOrNull { it.isFile }?.readText()
            ?: error("app/build.gradle.kts not found")
        assertTrue(
            "build.gradle.kts must constrain resourceConfigurations to id and en",
            buildGradle.contains("resourceConfigurations += listOf(\"id\", \"en\")")
        )
    }

    @Test
    fun fourCuratedThemesInThemePref() {
        assertEquals("AURORA_BOREALIS must be 14", 14, ThemePref.AURORA_BOREALIS)
        assertEquals("CYBER_OBSIDIAN must be 15", 15, ThemePref.CYBER_OBSIDIAN)
        assertEquals("SUNSET_MIRAGE must be 16", 16, ThemePref.SUNSET_MIRAGE)
        assertEquals("MATCHA_CREAM_PAPER must be 17", 17, ThemePref.MATCHA_CREAM_PAPER)

        val optionValues = ThemePref.OPTIONS.map { it.value }
        assertTrue("ThemePref.OPTIONS must contain AURORA_BOREALIS", optionValues.contains(14))
        assertTrue("ThemePref.OPTIONS must contain CYBER_OBSIDIAN", optionValues.contains(15))
        assertTrue("ThemePref.OPTIONS must contain SUNSET_MIRAGE", optionValues.contains(16))
        assertTrue("ThemePref.OPTIONS must contain MATCHA_CREAM_PAPER", optionValues.contains(17))
        assertEquals("Total theme options must be 18", 18, ThemePref.OPTIONS.size)
    }

    @Test
    fun themeXmlAndColorResourcesExist() {
        val colors = SourceScan.readRes("values/colors.xml")
        val themes = SourceScan.readRes("values/themes.xml")

        val expectedThemes = listOf(
            "aurora_borealis",
            "cyber_obsidian",
            "sunset_mirage",
            "matcha_cream_paper"
        )

        for (theme in expectedThemes) {
            assertTrue("colors.xml must contain ${theme}_bg", colors.contains("${theme}_bg"))
            assertTrue("colors.xml must contain ${theme}_surface", colors.contains("${theme}_surface"))
            assertTrue("colors.xml must contain ${theme}_accent", colors.contains("${theme}_accent"))
        }

        assertTrue("themes.xml must define Theme.Notez.AuroraBorealis", themes.contains("name=\"Theme.Notez.AuroraBorealis\""))
        assertTrue("themes.xml must define Theme.Notez.CyberObsidian", themes.contains("name=\"Theme.Notez.CyberObsidian\""))
        assertTrue("themes.xml must define Theme.Notez.SunsetMirage", themes.contains("name=\"Theme.Notez.SunsetMirage\""))
        assertTrue("themes.xml must define Theme.Notez.MatchaCreamPaper", themes.contains("name=\"Theme.Notez.MatchaCreamPaper\""))
    }

    @Test
    fun backdropExpansionGuard() {
        val editorLayout = SourceScan.readRes("layout/activity_editor.xml")
        val recycleLayout = SourceScan.readRes("layout/activity_recycle.xml")

        assertTrue("activity_editor.xml must contain editor_aurora", editorLayout.contains("editor_aurora"))
        assertTrue("activity_recycle.xml must contain recycle_aurora", recycleLayout.contains("recycle_aurora"))
    }

    @Test
    fun diagnosticsTerminalConsoleGuard() {
        val diagLayout = SourceScan.readRes("layout/activity_diagnostics.xml")
        val diagSource = SourceScan.readMainFile("java/com/zaba/notez/diagnostics/DiagnosticsActivity.kt")

        assertTrue("activity_diagnostics.xml must have terminal_card", diagLayout.contains("terminal_card"))
        assertTrue("activity_diagnostics.xml must have filter_container", diagLayout.contains("filter_container"))
        assertTrue("activity_diagnostics.xml must have btn_load_more for windowing", diagLayout.contains("btn_load_more"))
        assertTrue("activity_diagnostics.xml must have btn_share", diagLayout.contains("btn_share"))

        assertTrue("DiagnosticsActivity must define RENDER_WINDOW_DEFAULT = 500", diagSource.contains("RENDER_WINDOW_DEFAULT = 500"))
        assertTrue("DiagnosticsActivity must have LogFilter enum", diagSource.contains("enum class LogFilter"))
        assertTrue("DiagnosticsActivity must format as adb logcat tag", diagSource.contains("\$level/NOTEZ_"))
    }

    @Test
    fun diagnosticsActivityLifecycleInitializationOrderGuard() {
        val diagSource = SourceScan.readMainFile("java/com/zaba/notez/diagnostics/DiagnosticsActivity.kt")
        val indexOfFindBtnLoadMore = diagSource.indexOf("btnLoadMore = findViewById")
        val indexOfApplyScreenPolishCall = diagSource.indexOf("applyScreenPolish()")
        assertTrue("btnLoadMore must be found in onCreate", indexOfFindBtnLoadMore > 0)
        assertTrue("applyScreenPolish must be called in onCreate", indexOfApplyScreenPolishCall > 0)
        assertTrue(
            "applyScreenPolish() must be called AFTER btnLoadMore initialization to prevent UninitializedPropertyAccessException",
            indexOfApplyScreenPolishCall > indexOfFindBtnLoadMore
        )
    }
}
