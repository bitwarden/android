package com.bitwarden.ui.platform.feature.cardscanner.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PanVoteBufferTest {

    @Test
    fun `record returns null for the first occurrence of a PAN`() {
        val buffer = PanVoteBuffer()
        assertNull(buffer.record(pan = "4111111111111111"))
    }

    @Test
    fun `record returns the PAN once it has been seen in two of three frames`() {
        val buffer = PanVoteBuffer()
        val pan = "4111111111111111"

        assertNull(buffer.record(pan = pan))
        assertEquals(pan, buffer.record(pan = pan))
    }

    @Test
    fun `record returns null when only a single frame contains a PAN`() {
        val buffer = PanVoteBuffer()
        val pan = "4111111111111111"

        assertNull(buffer.record(pan = pan))
        assertNull(buffer.record(pan = null))
        assertNull(buffer.record(pan = null))
        // The original PAN has now been pushed out of the window.
        assertNull(buffer.record(pan = null))
    }

    @Test
    fun `record returns null when a PAN appears once and then disappears for two frames`() {
        val buffer = PanVoteBuffer()
        val pan = "4111111111111111"

        assertNull(buffer.record(pan = pan))
        assertNull(buffer.record(pan = null))
        assertNull(buffer.record(pan = null))
    }

    @Test
    fun `record does not confirm two different PANs that each appeared only once`() {
        val buffer = PanVoteBuffer()

        assertNull(buffer.record(pan = "4111111111111111"))
        assertNull(buffer.record(pan = "5500000000000004"))
        assertNull(buffer.record(pan = "378282246310005"))
    }

    @Test
    fun `record confirms a PAN seen in two non-consecutive frames within the window`() {
        val buffer = PanVoteBuffer()
        val pan = "4111111111111111"

        assertNull(buffer.record(pan = pan))
        assertNull(buffer.record(pan = null))
        assertEquals(pan, buffer.record(pan = pan))
    }

    @Test
    fun `record continues to emit the same PAN on every subsequent frame it is seen`() {
        val buffer = PanVoteBuffer()
        val pan = "4111111111111111"

        assertNull(buffer.record(pan = pan))
        assertEquals(pan, buffer.record(pan = pan))
        assertEquals(pan, buffer.record(pan = pan))
        assertEquals(pan, buffer.record(pan = pan))
    }
}
