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
    fun `initials should return the starting letters of the first two words for a multi-word name and ignore any extra spaces`() {
        assertEquals(
            "FS",
            mockk<AccountSummary> {
                every { name } returns "First   Second Third"
            }
                .initials,
        )
    }

    @Test
    fun `initials should return the first two letters of the name for a single word name`() {
        assertEquals(
            "FI",
            mockk<AccountSummary> {
                every { name } returns "First"
            }
                .initials,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `initials should return the first two letters of the user's email if the name is not present`() {
        assertEquals(
            "TE",
            mockk<AccountSummary> {
                every { name } returns null
                every { email } returns "test@bitwarden.com"
            }
                .initials,
        )
    }

    @Test
    fun `iconRes returns a checkmark for active accounts`() {
        assertEquals(
            R.drawable.ic_check_mark,
            mockk<AccountSummary> {
                every { status } returns AccountSummary.Status.ACTIVE
            }
                .iconRes,
        )
    }

    @Test
    fun `iconRes returns a locked lock for locked accounts`() {
        assertEquals(
            R.drawable.ic_locked,
            mockk<AccountSummary> {
                every { status } returns AccountSummary.Status.LOCKED
            }
                .iconRes,
        )
    }

    @Test
    fun `iconRes returns a locked lock for logged out accounts`() {
        assertEquals(
            R.drawable.ic_locked,
            mockk<AccountSummary> {
                every { status } returns AccountSummary.Status.LOGGED_OUT
            }
                .iconRes,
        )
    }

    @Test
    fun `iconRes returns an unlocked lock for unlocked accounts`() {
        assertEquals(
            R.drawable.ic_unlocked,
            mockk<AccountSummary> {
                every { status } returns AccountSummary.Status.UNLOCKED
            }
                .iconRes,
        )
    }

    @Test
    fun `iconTestTag returns ActiveVaultIcon for active account`() {
        val accountSummary = mockk<AccountSummary> {
            every { status } returns AccountSummary.Status.ACTIVE
        }
        assertEquals("ActiveVaultIcon", accountSummary.iconTestTag)
    }

    @Test
    fun `iconTestTag returns InactiveVaultIcon for locked account`() {
        val accountSummary = mockk<AccountSummary> {
            every { status } returns AccountSummary.Status.LOCKED
        }
        assertEquals("InactiveVaultIcon", accountSummary.iconTestTag)
    }

    @Test
    fun `iconTestTag returns InactiveVaultIcon for logged out account`() {
        val accountSummary = mockk<AccountSummary> {
            every { status } returns AccountSummary.Status.LOGGED_OUT
        }
        assertEquals("InactiveVaultIcon", accountSummary.iconTestTag)
    }

    @Test
    fun `iconTestTag returns InactiveVaultIcon for unlocked account`() {
        val accountSummary = mockk<AccountSummary> {
            every { status } returns AccountSummary.Status.UNLOCKED
        }
        assertEquals("InactiveVaultIcon", accountSummary.iconTestTag)
    }

    @Test
    fun `supportingTextResOrNull returns a null for active accounts`() {
        assertNull(
            mockk<AccountSummary> {
                every { status } returns AccountSummary.Status.ACTIVE
            }
                .supportingTextResOrNull,
        )
    }

    @Test
    fun `supportingTextResOrNull returns Locked locked accounts`() {
        assertEquals(
            R.string.account_locked,
            mockk<AccountSummary> {
                every { status } returns AccountSummary.Status.LOCKED
            }
                .supportingTextResOrNull,
        )
    }

    @Test
    fun `supportingTextResOrNull returns Logged Out for logged out accounts`() {
        assertEquals(
            R.string.account_logged_out,
            mockk<AccountSummary> {
                every { status } returns AccountSummary.Status.LOGGED_OUT
            }
                .supportingTextResOrNull,
        )
    }

    @Test
    fun `supportingTextResOrNull returns Unlocked for unlocked accounts`() {
        assertEquals(
            R.string.account_unlocked,
            mockk<AccountSummary> {
                every { status } returns AccountSummary.Status.UNLOCKED
            }
                .supportingTextResOrNull,
        )
    }
}
