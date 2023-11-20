package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AccountSummaryExtensionsTest {

    @Suppress("MaxLineLength")
    @Test
    fun `initials should return the starting letters of the first two words in the name if present`() {
        assertEquals(
            "FS",
            mockk<AccountSummary>() {
                every { name } returns "First Second Third"
            }
                .initials,
        )
    }

    @Test
    fun `initials should return a default value if the name is not present`() {
        assertEquals(
            "..",
            mockk<AccountSummary>() {
                every { name } returns null
            }
                .initials,
        )
    }

    @Test
    fun `iconRes returns a checkmark for active accounts`() {
        assertEquals(
            R.drawable.ic_check_mark,
            mockk<AccountSummary>() {
                every { status } returns AccountSummary.Status.ACTIVE
            }
                .iconRes,
        )
    }

    @Test
    fun `iconRes returns a locked lock for locked accounts`() {
        assertEquals(
            R.drawable.ic_locked,
            mockk<AccountSummary>() {
                every { status } returns AccountSummary.Status.LOCKED
            }
                .iconRes,
        )
    }

    @Test
    fun `iconRes returns an unlocked lock for unlocked accounts`() {
        assertEquals(
            R.drawable.ic_unlocked,
            mockk<AccountSummary>() {
                every { status } returns AccountSummary.Status.UNLOCKED
            }
                .iconRes,
        )
    }

    @Test
    fun `supportingTextResOrNull returns a null for active accounts`() {
        assertNull(
            mockk<AccountSummary>() {
                every { status } returns AccountSummary.Status.ACTIVE
            }
                .supportingTextResOrNull,
        )
    }

    @Test
    fun `supportingTextResOrNull returns Locked locked accounts`() {
        assertEquals(
            R.string.account_locked,
            mockk<AccountSummary>() {
                every { status } returns AccountSummary.Status.LOCKED
            }
                .supportingTextResOrNull,
        )
    }

    @Test
    fun `supportingTextResOrNull returns Unlocked for unlocked accounts`() {
        assertEquals(
            R.string.account_unlocked,
            mockk<AccountSummary>() {
                every { status } returns AccountSummary.Status.UNLOCKED
            }
                .supportingTextResOrNull,
        )
    }
}
