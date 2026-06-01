package com.x8bit.bitwarden.data.auth.util

import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class YubiKeyResultUtilsTest {

    @Test
    fun `getYubiKeyResultOrNull should return null when dataString is null`() {
        val mockIntent = mockk<Intent> {
            every { dataString } returns null
        }
        assertNull(mockIntent.getYubiKeyResultOrNull())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getYubiKeyResultOrNull should return null when pattern does not match (contains numbers)`() {
        val data = "cbdefghijklnrtuvcbdefghijklnrtuvcbdefghijkl3"
        val mockIntent = mockk<Intent> {
            every { dataString } returns data
        }
        assertNull(mockIntent.getYubiKeyResultOrNull())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getYubiKeyResultOrNull should return null when pattern does not match (contains space)`() {
        val data = "cbdefghijklnrtuvcbdefghijklnrtuvcbdefghijkl "
        val mockIntent = mockk<Intent> {
            every { dataString } returns data
        }
        assertNull(mockIntent.getYubiKeyResultOrNull())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getYubiKeyResultOrNull should return null when pattern matches but token is shorter that 44 characters`() {
        // 43 characters
        val data = "cbdefghijklnrtuvcbdefghijklnrtuvcbdefghijkl"
        val mockIntent = mockk<Intent> {
            every { dataString } returns data
        }
        assertNull(mockIntent.getYubiKeyResultOrNull())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getYubiKeyResultOrNull should return null when pattern matches but token is longer that 44 characters`() {
        // 45 characters
        val data = "cbdefghijklnrtuvcbdefghijklnrtuvcbdefghijklnr"
        val mockIntent = mockk<Intent> {
            every { dataString } returns data
        }
        assertNull(mockIntent.getYubiKeyResultOrNull())
    }

    @Test
    fun `getYubiKeyResultOrNull should return YubiKeyResult when pattern matches`() {
        val data = "cbdefghijklnrtuvcbdefghijklnrtuvcbdefghijkln"
        val mockIntent = mockk<Intent> {
            every { dataString } returns data
        }
        assertEquals(YubiKeyResult(data), mockIntent.getYubiKeyResultOrNull())
    }
}
