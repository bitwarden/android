package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices.util

import android.content.res.Resources
import com.bitwarden.ui.platform.resource.BitwardenString
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeviceTypeExtensionsTest {

    private val resources = mockk<Resources> {
        every { getText(BitwardenString.mobile) } returns "Mobile"
        every { getText(BitwardenString.extension) } returns "Extension"
        every { getText(BitwardenString.desktop) } returns "Desktop"
        every { getText(BitwardenString.web) } returns "Web"
        every { getText(BitwardenString.sdk) } returns "SDK"
        every { getText(BitwardenString.server) } returns "Server"
        every { getText(BitwardenString.cli) } returns "CLI"
        every { getText(BitwardenString.unknown_device) } returns "Unknown device"
    }

    @Test
    fun `type 0 should return Mobile - Android`() {
        assertEquals("Mobile - Android", 0.readableDeviceTypeName.toString(resources))
    }

    @Test
    fun `type 1 should return Mobile - iOS`() {
        assertEquals("Mobile - iOS", 1.readableDeviceTypeName.toString(resources))
    }

    @Test
    fun `type 2 should return Extension - Chrome`() {
        assertEquals("Extension - Chrome", 2.readableDeviceTypeName.toString(resources))
    }

    @Test
    fun `type 3 should return Extension - Firefox`() {
        assertEquals("Extension - Firefox", 3.readableDeviceTypeName.toString(resources))
    }

    @Test
    fun `type 5 should return Extension - Edge`() {
        assertEquals("Extension - Edge", 5.readableDeviceTypeName.toString(resources))
    }

    @Test
    fun `type 6 should return Desktop - Windows`() {
        assertEquals("Desktop - Windows", 6.readableDeviceTypeName.toString(resources))
    }

    @Test
    fun `type 7 should return Desktop - MacOS`() {
        assertEquals("Desktop - MacOS", 7.readableDeviceTypeName.toString(resources))
    }

    @Test
    fun `type 9 should return Web - Chrome`() {
        assertEquals("Web - Chrome", 9.readableDeviceTypeName.toString(resources))
    }

    @Test
    fun `type 15 should return Mobile - Amazon`() {
        assertEquals("Mobile - Amazon", 15.readableDeviceTypeName.toString(resources))
    }

    @Test
    fun `type 16 should return Desktop - Windows UWP`() {
        assertEquals("Desktop - Windows UWP", 16.readableDeviceTypeName.toString(resources))
    }

    @Test
    fun `type 20 should return Extension - Safari`() {
        assertEquals("Extension - Safari", 20.readableDeviceTypeName.toString(resources))
    }

    @Test
    fun `type 21 should return SDK category only with no platform suffix`() {
        assertEquals("SDK", 21.readableDeviceTypeName.toString(resources))
    }

    @Test
    fun `type 22 should return Server category only with no platform suffix`() {
        assertEquals("Server", 22.readableDeviceTypeName.toString(resources))
    }

    @Test
    fun `type 23 should return CLI - Windows`() {
        assertEquals("CLI - Windows", 23.readableDeviceTypeName.toString(resources))
    }

    @Test
    fun `type 24 should return CLI - MacOs`() {
        assertEquals("CLI - MacOs", 24.readableDeviceTypeName.toString(resources))
    }

    @Test
    fun `type 25 should return CLI - Linux`() {
        assertEquals("CLI - Linux", 25.readableDeviceTypeName.toString(resources))
    }

    @Test
    fun `type 26 should return Extension - DuckDuckGo`() {
        assertEquals("Extension - DuckDuckGo", 26.readableDeviceTypeName.toString(resources))
    }

    @Test
    fun `unknown type should return unknown device string`() {
        assertEquals("Unknown device", 999.readableDeviceTypeName.toString(resources))
    }

    @Test
    fun `negative type should return unknown device string`() {
        assertEquals("Unknown device", (-1).readableDeviceTypeName.toString(resources))
    }
}
