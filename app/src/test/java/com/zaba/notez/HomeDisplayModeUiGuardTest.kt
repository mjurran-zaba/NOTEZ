package com.zaba.notez

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guard struktural untuk UI mode tampilan (batch B2).
 *
 * Leksikal, bukan runtime: tidak membuktikan radio bisa disentuh, mode
 * benar-benar berubah, atau kartu Grid tidak bertumpuk. Itu UAT H05-H12.
 */
class HomeDisplayModeUiGuardTest {

    private val gridLayout: String = SourceScan.readRes("layout/item_note_grid.xml")
    private val mainLayout: String = SourceScan.readRes("layout/activity_main.xml")
    private val strings: String = SourceScan.readRes("values/strings.xml")
    private val activityCode: String =
        SourceScan.codeOnly(SourceScan.readMainFile("java/com/zaba/notez/MainActivity.kt"))

    @Test
    fun gridCardUsesSameIdsAndKeepsTrashTouchTarget() {
        for (id in listOf("note_card", "item_title", "item_preview", "item_meta", "item_delete")) {
            assertTrue("$id hilang dari item_note_grid.xml", gridLayout.contains("@+id/$id\""))
        }
        val trash = openingTag(gridLayout, "item_delete")
        assertTrue("trash Grid harus 48dp", trash.contains("android:layout_width=\"48dp\""))
        assertTrue("trash Grid harus 48dp", trash.contains("android:layout_height=\"48dp\""))
        assertTrue("trash Grid butuh contentDescription", trash.contains("android:contentDescription="))
    }

    @Test
    fun gridCardOwnsNoOuterMargin() {
        val card = openingTag(gridLayout, "note_card")
        assertTrue("note_card hilang dari item_note_grid.xml", card.isNotEmpty())
        assertFalse(
            "spacing Grid harus milik ItemDecoration, bukan margin luar kartu",
            card.contains("android:layout_margin")
        )
    }

    @Test
    fun gridCardLimitsTitleAndPreviewLines() {
        val title = openingTag(gridLayout, "item_title")
        assertTrue("judul Grid maksimal 2 baris", title.contains("android:maxLines=\"2\""))
        val preview = openingTag(gridLayout, "item_preview")
        assertTrue("preview Grid maksimal 3 baris", preview.contains("android:maxLines=\"3\""))
    }

    @Test
    fun drawerHasSingleSelectionGroupForDisplayMode() {
        val group = openingTag(mainLayout, "drawer_display_group")
        assertTrue("grup mode tampilan hilang", group.startsWith("<RadioGroup"))
        for (id in listOf("drawer_display_list", "drawer_display_grid")) {
            val tag = openingTag(mainLayout, id)
            assertTrue("$id hilang dari drawer", tag.startsWith("<RadioButton"))
            assertTrue("$id butuh label teks", tag.contains("android:text="))
        }
    }

    @Test
    fun displayModeLabelsExist() {
        for (name in listOf("drawer_display_title", "drawer_display_list", "drawer_display_grid")) {
            assertTrue("string $name hilang", strings.contains("name=\"$name\""))
        }
    }

    @Test
    fun activityPersistsAndAppliesDisplayMode() {
        assertTrue("activity harus membaca preference mode", activityCode.contains("HomeDisplayPref.get"))
        assertTrue("activity harus menyimpan preference mode", activityCode.contains("HomeDisplayPref.set"))
        assertTrue("activity harus memakai GridLayoutManager", activityCode.contains("GridLayoutManager"))
        assertTrue("span Grid harus lewat policy", activityCode.contains("HomeDisplayPolicy.spanCount"))
        assertTrue("dekorasi spacing harus dilepas", activityCode.contains("removeItemDecoration"))
        assertTrue("dekorasi spacing harus dipasang", activityCode.contains("addItemDecoration"))
    }

    private fun openingTag(xml: String, id: String): String {
        val marker = "@+id/$id\""
        val markerIndex = xml.indexOf(marker)
        if (markerIndex < 0) return ""
        val start = xml.lastIndexOf('<', markerIndex)
        val end = xml.indexOf('>', markerIndex)
        if (start < 0 || end < 0) return ""
        return xml.substring(start, end + 1)
    }
}
