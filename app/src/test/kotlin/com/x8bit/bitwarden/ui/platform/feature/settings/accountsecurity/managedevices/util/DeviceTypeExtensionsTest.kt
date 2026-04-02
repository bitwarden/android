package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeviceTypeExtensionsTest {

    private val resourceManager = mockk<ResourceManager> {
        every { getString(BitwardenString.mobile) } returns "Mobile"
        every { getString(BitwardenString.extension) } returns "Extension"
        every { getString(BitwardenString.desktop) } returns "Desktop"
        every { getString(BitwardenString.web) } returns "Web"
        every { getString(BitwardenString.sdk) } returns "SDK"
        every { getString(BitwardenString.server) } returns "Server"
        every { getString(BitwardenString.cli) } returns "CLI"
        every { getString(BitwardenString.unknown_device) } returns "Unknown device"
    }

    @Test
    fun `type 0 should return Mobile - Android`() {
        assertEquals("Mobile - Android".asText(), 0.toReadableDeviceTypeName(resourceManager))
    }

    @Test
    fun `type 1 should return Mobile - iOS`() {
        assertEquals("Mobile - iOS".asText(), 1.toReadableDeviceTypeName(resourceManager))
    }

    @Test
    fun `type 2 should return Extension - Chrome`() {
        assertEquals("Extension - Chrome".asText(), 2.toReadableDeviceTypeName(resourceManager))
    }

    @Test
    fun `type 3 should return Extension - Firefox`() {
        assertEquals("Extension - Firefox".asText(), 3.toReadableDeviceTypeName(resourceManager))
    }

    @Test
    fun `type 5 should return Extension - Edge`() {
        assertEquals("Extension - Edge".asText(), 5.toReadableDeviceTypeName(resourceManager))
    }

    @Test
    fun `type 6 should return Desktop - Windows`() {
        assertEquals("Desktop - Windows".asText(), 6.toReadableDeviceTypeName(resourceManager))
    }

    @Test
    fun `type 7 should return Desktop - MacOS`() {
        assertEquals("Desktop - MacOS".asText(), 7.toReadableDeviceTypeName(resourceManager))
    }

    @Test
    fun `type 9 should return Web - Chrome`() {
        assertEquals("Web - Chrome".asText(), 9.toReadableDeviceTypeName(resourceManager))
    }

    @Test
    fun `type 15 should return Mobile - Amazon`() {
        assertEquals("Mobile - Amazon".asText(), 15.toReadableDeviceTypeName(resourceManager))
    }

    @Test
    fun `type 16 should return Desktop - Windows UWP`() {
        assertEquals("Desktop - Windows UWP".asText(), 16.toReadableDeviceTypeName(resourceManager))
    }

    @Test
    fun `type 20 should return Extension - Safari`() {
        assertEquals("Extension - Safari".asText(), 20.toReadableDeviceTypeName(resourceManager))
    }

    @Test
    fun `type 21 should return SDK category only with no platform suffix`() {
        assertEquals("SDK".asText(), 21.toReadableDeviceTypeName(resourceManager))
    }

    @Test
    fun `type 22 should return Server category only with no platform suffix`() {
        assertEquals("Server".asText(), 22.toReadableDeviceTypeName(resourceManager))
    }

    @Test
    fun `type 23 should return CLI - Windows`() {
        assertEquals("CLI - Windows".asText(), 23.toReadableDeviceTypeName(resourceManager))
    }

    @Test
    fun `type 24 should return CLI - MacOs`() {
        assertEquals("CLI - MacOs".asText(), 24.toReadableDeviceTypeName(resourceManager))
    }

    @Test
    fun `type 25 should return CLI - Linux`() {
        assertEquals("CLI - Linux".asText(), 25.toReadableDeviceTypeName(resourceManager))
    }

    @Test
    fun `type 26 should return Extension - DuckDuckGo`() {
        assertEquals(
            "Extension - DuckDuckGo".asText(),
            26.toReadableDeviceTypeName(resourceManager),
        )
    }

    @Test
    fun `unknown type should return unknown device string`() {
        assertEquals("Unknown device".asText(), 999.toReadableDeviceTypeName(resourceManager))
    }

    @Test
    fun `negative type should return unknown device string`() {
        assertEquals("Unknown device".asText(), (-1).toReadableDeviceTypeName(resourceManager))
    }
}
