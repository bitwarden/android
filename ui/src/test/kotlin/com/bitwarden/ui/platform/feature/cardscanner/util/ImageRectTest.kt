package com.bitwarden.ui.platform.feature.cardscanner.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImageRectTest {

    private val outer = ImageRect(left = 0, top = 0, right = 100, bottom = 100)

    @Test
    fun `contains returns true when other is fully inside`() {
        assertTrue(outer.contains(ImageRect(10, 10, 90, 90)))
    }

    @Test
    fun `contains returns true when edges align with this rectangle's edges`() {
        assertTrue(outer.contains(ImageRect(0, 0, 100, 100)))
    }

    @Test
    fun `contains returns false when other extends past the left edge`() {
        assertFalse(outer.contains(ImageRect(-1, 10, 50, 50)))
    }

    @Test
    fun `contains returns false when other extends past the top edge`() {
        assertFalse(outer.contains(ImageRect(10, -1, 50, 50)))
    }

    @Test
    fun `contains returns false when other extends past the right edge`() {
        assertFalse(outer.contains(ImageRect(10, 10, 101, 50)))
    }

    @Test
    fun `contains returns false when other extends past the bottom edge`() {
        assertFalse(outer.contains(ImageRect(10, 10, 50, 101)))
    }

    @Test
    fun `intersects returns true when rectangles overlap on a corner`() {
        assertTrue(outer.intersects(ImageRect(50, 50, 150, 150)))
    }

    @Test
    fun `intersects returns true when one rectangle is fully inside the other`() {
        assertTrue(outer.intersects(ImageRect(10, 10, 90, 90)))
    }

    @Test
    fun `intersects returns false when other lies entirely to the left`() {
        assertFalse(outer.intersects(ImageRect(-50, 10, 0, 50)))
    }

    @Test
    fun `intersects returns false when other lies entirely to the right`() {
        assertFalse(outer.intersects(ImageRect(100, 10, 150, 50)))
    }

    @Test
    fun `intersects returns false when other lies entirely above`() {
        assertFalse(outer.intersects(ImageRect(10, -50, 50, 0)))
    }

    @Test
    fun `intersects returns false when other lies entirely below`() {
        assertFalse(outer.intersects(ImageRect(10, 100, 50, 150)))
    }
}
