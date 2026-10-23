package com.x8bit.bitwarden.ui.vault.feature.item.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VaultItemPamRequestFormTest {

    @Test
    fun `durationOptionsMinutes should never exceed the maximum`() {
        val form = FORM.copy(durationMinutes = "60", maxDurationMinutes = 480L)

        assertEquals(listOf(15L, 30L, 60L, 120L, 240L, 480L), form.durationOptionsMinutes)
    }

    @Test
    fun `durationOptionsMinutes should offer the maximum when it is a short custom value`() {
        val form = FORM.copy(durationMinutes = "30", maxDurationMinutes = 90L)

        assertEquals(listOf(15L, 30L, 60L, 90L), form.durationOptionsMinutes)
    }

    @Test
    fun `durationOptionsMinutes should include the selected duration`() {
        val form = FORM.copy(durationMinutes = "45", maxDurationMinutes = 120L)

        assertEquals(listOf(15L, 30L, 45L, 60L, 120L), form.durationOptionsMinutes)
    }

    @Test
    fun `durationOptionsMinutes should stop at one day when there is no maximum`() {
        val form = FORM.copy(durationMinutes = "60", maxDurationMinutes = 0L)

        assertEquals(
            listOf(15L, 30L, 60L, 120L, 240L, 480L, 1_440L),
            form.durationOptionsMinutes,
        )
    }

    @Test
    fun `durationOptionsMinutes should not offer a very long maximum`() {
        val form = FORM.copy(durationMinutes = "60", maxDurationMinutes = 525_600L)

        assertFalse(525_600L in form.durationOptionsMinutes)
        assertEquals(1_440L, form.durationOptionsMinutes.last())
    }

    @Test
    fun `isSubmittable should be false for a duration over the maximum`() {
        assertFalse(FORM.copy(durationMinutes = "481", maxDurationMinutes = 480L).isSubmittable)
        assertTrue(FORM.copy(durationMinutes = "480", maxDurationMinutes = 480L).isSubmittable)
    }

    @Test
    fun `isSubmittable should be false for a window that ends before it starts`() {
        val form = HUMAN_FORM.copy(endMinutes = 11 * 60)

        assertTrue(form.isWindowEndBeforeStart)
        assertFalse(form.isSubmittable)
    }

    @Test
    fun `isSubmittable should be false for a window over the maximum`() {
        val form = HUMAN_FORM.copy(endDate = LocalDate.of(2026, 1, 2))

        assertTrue(form.isWindowOverMaximum)
        assertFalse(form.isSubmittable)
    }

    @Test
    fun `isSubmittable should be true for a valid window with a reason`() {
        assertEquals(60L, HUMAN_FORM.windowMinutesOrNull)
        assertTrue(HUMAN_FORM.isSubmittable)
    }

    @Test
    fun `isSubmittable should be false for a window with a blank reason`() {
        assertFalse(HUMAN_FORM.copy(reason = " ").isSubmittable)
    }

    @Test
    fun `isWindowOverMaximum should be false when there is no maximum`() {
        val form = HUMAN_FORM.copy(endDate = LocalDate.of(2026, 3, 1), maxDurationMinutes = 0L)

        assertFalse(form.isWindowOverMaximum)
        assertTrue(form.isSubmittable)
    }
}

private val FORM = VaultItemPamRequestForm(
    isHumanApproval = false,
    durationMinutes = "60",
    maxDurationMinutes = 480L,
    startDate = LocalDate.of(2026, 1, 1),
    startMinutes = 12 * 60,
    endDate = LocalDate.of(2026, 1, 1),
    endMinutes = 13 * 60,
    reason = "",
)

private val HUMAN_FORM = FORM.copy(isHumanApproval = true, reason = "Fix the outage")
