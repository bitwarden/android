package com.x8bit.bitwarden.data.auth.repository.util

import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull

class SsoUtilsTest {

    @Test
    fun `getSsoCallbackResult should return null when data is null`() {
        val intent = mockk<Intent> {
            every { data } returns null
            every { action } returns Intent.ACTION_VIEW
        }
        val result = intent.getSsoCallbackResult()
        assertNull(result)
    }

    @Test
    fun `getSsoCallbackResult should return null when action is not Intent ACTION_VIEW`() {
        val intent = mockk<Intent> {
            every { data } returns null
            every { action } returns Intent.ACTION_ANSWER
        }
        val result = intent.getSsoCallbackResult()
        assertNull(result)
    }

    @Test
    fun `getSsoCallbackResult should return MissingCode with missing state code`() {
        val intent = mockk<Intent> {
            every { data?.getQueryParameter("state") } returns "myState"
            every { data?.getQueryParameter("code") } returns null
            every { action } returns Intent.ACTION_VIEW
            every { data?.host } returns "sso-callback"
        }
        val result = intent.getSsoCallbackResult()
        assertEquals(SsoCallbackResult.MissingCode, result)
    }

    @Test
    fun `getSsoCallbackResult should return Success when code query parameter is present`() {
        val intent = mockk<Intent> {
            every { data?.getQueryParameter("code") } returns "myCode"
            every { data?.getQueryParameter("state") } returns "myState"
            every { action } returns Intent.ACTION_VIEW
            every { data?.host } returns "sso-callback"
        }
        val result = intent.getSsoCallbackResult()
        assertEquals(
            SsoCallbackResult.Success(state = "myState", code = "myCode"),
            result,
        )
    }
}
