package com.x8bit.bitwarden.data.auth.util

import android.content.Intent
import android.net.Uri
import com.x8bit.bitwarden.data.platform.manager.model.CompleteRegistrationData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CompleteRegistrationDataUtilsTest {

    @BeforeEach
    fun setup() {
        mockkStatic(Uri::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `getCompleteRegistrationDataIntentOrNull valid URI returns CompleteRegistrationData`() {
        val mockIntent = mockk<Intent> {
            every { data } returns mockk()
        }
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns uriMock
        every { uriMock.host } returns "www.bitwarden.com"
        every { uriMock.path } returns "/finish-signup"
        every { uriMock.getQueryParameter("email") } returns "example@email.com"
        every { uriMock.getQueryParameter("token") } returns "verificationtoken"
        every { uriMock.getBooleanQueryParameter("fromEmail", true) } returns true

        assertEquals(
            CompleteRegistrationData(
                email = "example@email.com",
                verificationToken = "verificationtoken",
                fromEmail = true,
            ),
            mockIntent.getCompleteRegistrationDataIntentOrNull(),
        )
    }

    @Test
    fun `getCompleteRegistrationDataIntentOrNull null data returns null`() {
        val mockIntent = mockk<Intent> {
            every { data } returns mockk()
        }
        every { Uri.parse(any()) } returns null

        assertNull(
            mockIntent.getCompleteRegistrationDataIntentOrNull(),
        )
    }

    @Test
    fun `getCompleteRegistrationDataIntentOrNull Uri with no host`() {
        val mockIntent = mockk<Intent> {
            every { data } returns mockk()
        }
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns null
        every { uriMock.host } returns null

        assertNull(
            mockIntent.getCompleteRegistrationDataIntentOrNull(),
        )
    }

    @Test
    fun `getCompleteRegistrationDataIntentOrNull URI does not contain finish-signup path`() {
        val mockIntent = mockk<Intent> {
            every { data } returns mockk()
        }
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns null
        every { uriMock.host } returns null
        every { uriMock.path } returns "/finish"
        assertNull(
            mockIntent.getCompleteRegistrationDataIntentOrNull(),
        )
    }

    @Test
    fun `getCompleteRegistrationDataIntentOrNull URI does not contain parameter email`() {
        val mockIntent = mockk<Intent> {
            every { data } returns mockk()
        }
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns uriMock
        every { uriMock.host } returns "www.bitwarden.com"
        every { uriMock.path } returns "/finish-signup"
        every { uriMock.getQueryParameter("email") } returns null
        assertNull(
            mockIntent.getCompleteRegistrationDataIntentOrNull(),
        )
    }

    @Test
    fun `getCompleteRegistrationDataIntentOrNull URI does not contain parameter token`() {
        val mockIntent = mockk<Intent> {
            every { data } returns mockk()
        }
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns uriMock
        every { uriMock.host } returns "www.bitwarden.com"
        every { uriMock.path } returns "/finish-signup"
        every { uriMock.getQueryParameter("email") } returns "example@email.com"
        every { uriMock.getQueryParameter("token") } returns null
        assertNull(
            mockIntent.getCompleteRegistrationDataIntentOrNull(),
        )
    }
}
