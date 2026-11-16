package com.zaba.notez

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guard untuk hitungan kolom Grid (batch B2).
 *
 * Murni JVM. Ini menjaga kebijakan "jangan paksa dua kolom bila lebar minimum
 * belum tercapai" dan batas maksimum fase pertama. Bukti visual (judul tidak
 * terpotong, trash tidak tertimpa) tetap UAT H11-H12.
 */
class HomeDisplayPolicyTest {

    private val minCard = 160
    private val gap = 8

    @Test
    fun narrowWindowFallsBackToOneColumn() {
        assertEquals(1, HomeDisplayPolicy.spanCount(300, minCard, gap))
    }

    @Test
    fun twoColumnsWhenMinimumWidthIsMet() {
        assertEquals(2, HomeDisplayPolicy.spanCount(400, minCard, gap))
    }

    @Test
    fun threeColumnsOnWideWindow() {
        assertEquals(3, HomeDisplayPolicy.spanCount(600, minCard, gap))
    }

    @Test
    fun neverExceedsMaximumColumns() {
        assertEquals(3, HomeDisplayPolicy.spanCount(5000, minCard, gap))
        assertEquals(2, HomeDisplayPolicy.spanCount(5000, minCard, gap, maxColumns = 2))
    }

    @Test
    fun spacingIsChargedToEachGap() {
        // 2 kolom tanpa gap butuh 320px; dengan gap 8px butuh 328px.
        assertEquals(2, HomeDisplayPolicy.spanCount(322, minCard, spacingPx = 0))
        assertEquals(1, HomeDisplayPolicy.spanCount(322, minCard, spacingPx = 8))
    }

    @Test
    fun invalidWidthFallsBackToOneColumn() {
        assertEquals(1, HomeDisplayPolicy.spanCount(0, minCard, gap))
        assertEquals(1, HomeDisplayPolicy.spanCount(-100, minCard, gap))
        assertEquals(1, HomeDisplayPolicy.spanCount(800, 0, gap))
    }
}
