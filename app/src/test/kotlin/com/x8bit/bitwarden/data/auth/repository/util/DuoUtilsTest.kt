package com.x8bit.bitwarden.data.auth.repository.util

import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

class DuoUtilsTest {

    @Test
    fun `getDuoCallbackTokenResult should return null when action is not VIEW`() {
        val intent = mockk<Intent> {
            every { data } returns mockk()
            every { action } returns Intent.ACTION_SEND
        }
        val result = intent.getDuoCallbackTokenResult()
        assertNull(result)
    }

    @Test
    fun `getDuoCallbackTokenResult should return null when data is null`() {
        val intent = mockk<Intent> {
            every { data } returns null
            every { action } returns Intent.ACTION_VIEW
        }
        val result = intent.getDuoCallbackTokenResult()
        assertNull(result)
    }

    @Test
    fun `getDuoCallbackTokenResult should return null when host is not the duo callback`() {
        val intent = mockk<Intent> {
            every { data?.host } returns "wrongHost"
            every { data?.scheme } returns "bitwarden"
            every { action } returns Intent.ACTION_VIEW
        }
        val result = intent.getDuoCallbackTokenResult()
        assertNull(result)
    }

    @Test
    fun `getDuoCallbackTokenResult should return MissingToken code is null`() {
        val intent = mockk<Intent> {
            every { data?.host } returns "duo-callback"
            every { data?.scheme } returns "bitwarden"
            every { data?.getQueryParameter("code") } returns null
            every { data?.getQueryParameter("state") } returns "state"
            every { action } returns Intent.ACTION_VIEW
        }
        val result = intent.getDuoCallbackTokenResult()
        assertEquals(DuoCallbackTokenResult.MissingToken, result)
    }

    @Test
    fun `getDuoCallbackTokenResult for deeplink should return MissingToken when state is null`() {
        val intent = mockk<Intent> {
            every { data?.host } returns "duo-callback"
            every { data?.scheme } returns "bitwarden"
            every { data?.getQueryParameter("code") } returns "code"
            every { data?.getQueryParameter("state") } returns null
            every { action } returns Intent.ACTION_VIEW
        }
        val result = intent.getDuoCallbackTokenResult()
        assertEquals(DuoCallbackTokenResult.MissingToken, result)
    }

    @Test
    fun `getDuoCallbackTokenResult for deeplink should return Success when all data is present`() {
        val intent = mockk<Intent> {
            every { data?.host } returns "duo-callback"
            every { data?.scheme } returns "bitwarden"
            every { data?.getQueryParameter("code") } returns "code"
            every { data?.getQueryParameter("state") } returns "state"
            every { action } returns Intent.ACTION_VIEW
        }
        val result = intent.getDuoCallbackTokenResult()
        assertEquals(DuoCallbackTokenResult.Success(token = "code|state"), result)
    }

    @Test
    fun `getDuoCallbackTokenResult for app link should return MissingToken when state is null`() {
        val intent = mockk<Intent> {
            every { data?.host } returns "bitwarden.com"
            every { data?.scheme } returns "https"
            every { data?.path } returns "/duo-callback"
            every { data?.getQueryParameter("code") } returns "code"
            every { data?.getQueryParameter("state") } returns null
            every { action } returns Intent.ACTION_VIEW
        }
        val result = intent.getDuoCallbackTokenResult()
        assertEquals(DuoCallbackTokenResult.MissingToken, result)
    }

    @Test
    fun `getDuoCallbackTokenResult for app link should return Success when all data is present`() {
        val intent = mockk<Intent> {
            every { data?.host } returns "bitwarden.eu"
            every { data?.scheme } returns "https"
            every { data?.path } returns "/duo-callback"
            every { data?.getQueryParameter("code") } returns "code"
            every { data?.getQueryParameter("state") } returns "state"
            every { action } returns Intent.ACTION_VIEW
        }
        val result = intent.getDuoCallbackTokenResult()
        assertEquals(DuoCallbackTokenResult.Success(token = "code|state"), result)
    }
}
