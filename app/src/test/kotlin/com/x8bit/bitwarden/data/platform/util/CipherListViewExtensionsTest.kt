package com.x8bit.bitwarden.data.platform.util

import com.bitwarden.vault.CipherListView
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CipherListViewExtensionsTest {

    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun `isActive should return true when item is not archived and not deleted`() {
        val cipherListView = mockk<CipherListView> {
            every { archivedDate } returns null
            every { deletedDate } returns null
        }

        assertTrue(cipherListView.isActive)
    }

    @Test
    fun `isActive should return false when item is archived and not deleted`() {
        val cipherListView = mockk<CipherListView> {
            every { archivedDate } returns clock.instant()
            every { deletedDate } returns null
        }

        assertFalse(cipherListView.isActive)
    }

    @Test
    fun `isActive should return false when item is not archived and is deleted`() {
        val cipherListView = mockk<CipherListView> {
            every { archivedDate } returns null
            every { deletedDate } returns clock.instant()
        }

        assertFalse(cipherListView.isActive)
    }

    @Test
    fun `isActive should return false when item is archived and is deleted`() {
        val cipherListView = mockk<CipherListView> {
            every { archivedDate } returns clock.instant()
            every { deletedDate } returns clock.instant()
        }

        assertFalse(cipherListView.isActive)
    }
}
