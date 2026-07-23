package com.x8bit.bitwarden.data.platform.util

import com.bitwarden.vault.CipherView
import com.bitwarden.vault.IdentityView
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CipherViewExtensionsTest {

    private val emptyIdentityView = IdentityView(
        title = null,
        firstName = null,
        middleName = null,
        lastName = null,
        address1 = null,
        address2 = null,
        address3 = null,
        city = null,
        state = null,
        postalCode = null,
        country = null,
        company = null,
        email = null,
        phone = null,
        ssn = null,
        username = null,
        passportNumber = null,
        licenseNumber = null,
    )

    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun `isActive should return true when item is not archived and not deleted`() {
        val cipherListView = mockk<CipherView> {
            every { archivedDate } returns null
            every { deletedDate } returns null
        }

        assertTrue(cipherListView.isActive)
    }

    @Test
    fun `isActive should return false when item is archived and not deleted`() {
        val cipherListView = mockk<CipherView> {
            every { archivedDate } returns clock.instant()
            every { deletedDate } returns null
        }

        assertFalse(cipherListView.isActive)
    }

    @Test
    fun `isActive should return false when item is not archived and is deleted`() {
        val cipherListView = mockk<CipherView> {
            every { archivedDate } returns null
            every { deletedDate } returns clock.instant()
        }

        assertFalse(cipherListView.isActive)
    }

    @Test
    fun `isActive should return false when item is archived and is deleted`() {
        val cipherListView = mockk<CipherView> {
            every { archivedDate } returns clock.instant()
            every { deletedDate } returns clock.instant()
        }

        assertFalse(cipherListView.isActive)
    }

    @Test
    fun `identityName should return null when all name parts are null`() {
        assertNull(emptyIdentityView.identityName)
    }

    @Test
    fun `identityName should join all name parts with title case applied to title`() {
        val identityView = emptyIdentityView.copy(
            title = "MX",
            firstName = null,
            middleName = "middleName",
            lastName = null,
        )

        assertEquals("Mx middleName", identityView.identityName)
    }

    @Test
    fun `identityName should join all name parts when fully populated`() {
        val identityView = emptyIdentityView.copy(
            title = "mr",
            firstName = "firstName",
            middleName = "middleName",
            lastName = "lastName",
        )

        assertEquals(
            "Mr firstName middleName lastName",
            identityView.identityName,
        )
    }

    @Test
    fun `identityAddress should return null when all address parts are null`() {
        assertNull(emptyIdentityView.identityAddress)
    }

    @Test
    fun `identityAddress should collapse the city, state, and postal code when all are null`() {
        val identityView = emptyIdentityView.copy(
            address1 = null,
            address2 = null,
            address3 = "address3",
            city = null,
            state = "state",
            postalCode = null,
            country = null,
        )

        assertEquals("address3\n-, state, -", identityView.identityAddress)
    }

    @Test
    fun `identityAddress should join all address parts when fully populated`() {
        val identityView = emptyIdentityView.copy(
            address1 = "address1",
            address2 = "address2",
            address3 = "address3",
            city = "city",
            state = "state",
            postalCode = "postalCode",
            country = "country",
        )

        assertEquals(
            "address1\naddress2\naddress3\ncity, state, postalCode\ncountry",
            identityView.identityAddress,
        )
    }
}
