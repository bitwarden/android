package com.x8bit.bitwarden.data.platform.datasource.disk

import com.bitwarden.core.di.CoreModule
import com.bitwarden.data.datasource.disk.base.FakeSharedPreferences
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CustomHeadersDiskSourceTest {
    private val fakeEncryptedSharedPreferences = FakeSharedPreferences()
    private val fakeSharedPreferences = FakeSharedPreferences()
    private val json = CoreModule.providesJson(buildInfoManager = mockk(relaxed = true))

    private val customHeadersDiskSource: CustomHeadersDiskSource = CustomHeadersDiskSourceImpl(
        sharedPreferences = fakeSharedPreferences,
        encryptedSharedPreferences = fakeEncryptedSharedPreferences,
        json = json,
    )

    @Test
    fun `getCustomHeaders should return null when no headers exist`() {
        assertNull(customHeadersDiskSource.getCustomHeaders(id = "unknownId"))
    }

    @Test
    fun `storeCustomHeaders should persist headers and getCustomHeaders should retrieve them`() {
        val id = "headersId"
        val headers = mapOf(
            "CF-Access-Client-Id" to "clientId",
            "CF-Access-Client-Secret" to "clientSecret",
        )

        customHeadersDiskSource.storeCustomHeaders(id = id, headers = headers)

        assertEquals(headers, customHeadersDiskSource.getCustomHeaders(id = id))
    }

    @Test
    fun `storeCustomHeaders should update existing headers`() {
        val id = "headersId"
        val initialHeaders = mapOf("X-Custom-Header" to "initialValue")
        val updatedHeaders = mapOf("X-Custom-Header" to "updatedValue")

        customHeadersDiskSource.storeCustomHeaders(id = id, headers = initialHeaders)
        customHeadersDiskSource.storeCustomHeaders(id = id, headers = updatedHeaders)

        assertEquals(updatedHeaders, customHeadersDiskSource.getCustomHeaders(id = id))
    }

    @Test
    fun `storeCustomHeaders with null should remove stored headers`() {
        val id = "headersId"
        val headers = mapOf("X-Custom-Header" to "value")

        customHeadersDiskSource.storeCustomHeaders(id = id, headers = headers)
        customHeadersDiskSource.storeCustomHeaders(id = id, headers = null)

        assertNull(customHeadersDiskSource.getCustomHeaders(id = id))
    }

    @Test
    fun `storeCustomHeaders with null should not affect other ids`() {
        val id1 = "headersId1"
        val id2 = "headersId2"
        val headers1 = mapOf("X-Custom-Header-A" to "1")
        val headers2 = mapOf("X-Custom-Header-B" to "2")

        customHeadersDiskSource.storeCustomHeaders(id = id1, headers = headers1)
        customHeadersDiskSource.storeCustomHeaders(id = id2, headers = headers2)
        customHeadersDiskSource.storeCustomHeaders(id = id1, headers = null)

        assertNull(customHeadersDiskSource.getCustomHeaders(id = id1))
        assertEquals(headers2, customHeadersDiskSource.getCustomHeaders(id = id2))
    }
}
