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
            every { action } returns Intent.ACTION_VIEW
        }
        val result = intent.getDuoCallbackTokenResult()
        assertNull(result)
    }

    @Test
    fun `getDuoCallbackTokenResult should return MissingToken code is null`() {
        val intent = mockk<Intent> {
            every { data?.host } returns "duo-callback"
            every { data?.getQueryParameter("code") } returns null
            every { data?.getQueryParameter("state") } returns "state"
            every { action } returns Intent.ACTION_VIEW
        }
        val result = intent.getDuoCallbackTokenResult()
        assertEquals(DuoCallbackTokenResult.MissingToken, result)
    }

    @Test
    fun `getDuoCallbackTokenResult should return MissingToken state is null`() {
        val intent = mockk<Intent> {
            every { data?.host } returns "duo-callback"
            every { data?.getQueryParameter("code") } returns "code"
            every { data?.getQueryParameter("state") } returns null
            every { action } returns Intent.ACTION_VIEW
        }
        val result = intent.getDuoCallbackTokenResult()
        assertEquals(DuoCallbackTokenResult.MissingToken, result)
    }

    @Test
    fun `getDuoCallbackTokenResult should return Success when all data is present`() {
        val intent = mockk<Intent> {
            every { data?.host } returns "duo-callback"
            every { data?.getQueryParameter("code") } returns "code"
            every { data?.getQueryParameter("state") } returns "state"
            every { action } returns Intent.ACTION_VIEW
        }
        val result = intent.getDuoCallbackTokenResult()
        assertEquals(DuoCallbackTokenResult.Success(token = "code|state"), result)
    }
}
