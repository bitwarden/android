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
    fun `identityAutofillName should return null when all name parts are null`() {
        assertNull(emptyIdentityView.identityAutofillName)
    }

    @Test
    fun `identityAutofillName should exclude the title and join the remaining name parts`() {
        val identityView = emptyIdentityView.copy(
            title = "mr",
            firstName = "firstName",
            middleName = "middleName",
            lastName = "lastName",
        )

        assertEquals(
            "firstName middleName lastName",
            identityView.identityAutofillName,
        )
    }

    @Test
    fun `identityAutofillAddress should return null when all address parts are null`() {
        assertNull(emptyIdentityView.identityAutofillAddress)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `identityAutofillAddress should omit missing parts entirely rather than using a placeholder`() {
        val identityView = emptyIdentityView.copy(
            address1 = null,
            address2 = null,
            address3 = "address3",
            city = null,
            state = "state",
            postalCode = null,
            country = null,
        )

        assertEquals("address3 state", identityView.identityAutofillAddress)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `identityAutofillAddress should join all address parts with a single space when fully populated`() {
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
            "address1 address2 address3 city state postalCode country",
            identityView.identityAutofillAddress,
        )
    }
}
