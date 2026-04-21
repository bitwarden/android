package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices.util

import android.content.res.Resources
import com.bitwarden.ui.platform.resource.BitwardenString
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeviceTypeExtensionsTest {

    private val resources = mockk<Resources> {
        // Categories with platform: getString(resId, platform)
        every { getString(BitwardenString.mobile_platform, "Android") } returns "Mobile - Android"
        every { getString(BitwardenString.mobile_platform, "iOS") } returns "Mobile - iOS"
        every { getString(BitwardenString.mobile_platform, "Amazon") } returns "Mobile - Amazon"
        every {
            getString(
                BitwardenString.extension_platform,
                "Chrome",
            )
        } returns "Extension - Chrome"
        every {
            getString(
                BitwardenString.extension_platform,
                "Firefox",
            )
        } returns "Extension - Firefox"
        every { getString(BitwardenString.extension_platform, "Opera") } returns "Extension - Opera"
        every { getString(BitwardenString.extension_platform, "Edge") } returns "Extension - Edge"
        every {
            getString(
                BitwardenString.extension_platform,
                "Vivaldi",
            )
        } returns "Extension - Vivaldi"
        every {
            getString(
                BitwardenString.extension_platform,
                "Safari",
            )
        } returns "Extension - Safari"
        every {
            getString(
                BitwardenString.extension_platform,
                "DuckDuckGo",
            )
        } returns "Extension - DuckDuckGo"
        every { getString(BitwardenString.desktop_platform, "Windows") } returns "Desktop - Windows"
        every { getString(BitwardenString.desktop_platform, "MacOS") } returns "Desktop - MacOS"
        every { getString(BitwardenString.desktop_platform, "Linux") } returns "Desktop - Linux"
        every {
            getString(
                BitwardenString.desktop_platform,
                "Windows UWP",
            )
        } returns "Desktop - Windows UWP"
        every { getString(BitwardenString.web_platform, "Chrome") } returns "Web - Chrome"
        every { getString(BitwardenString.web_platform, "Firefox") } returns "Web - Firefox"
        every { getString(BitwardenString.web_platform, "Opera") } returns "Web - Opera"
        every { getString(BitwardenString.web_platform, "Edge") } returns "Web - Edge"
        every { getString(BitwardenString.web_platform, "IE") } returns "Web - IE"
        every { getString(BitwardenString.web_platform, "Unknown") } returns "Web - Unknown"
        every { getString(BitwardenString.web_platform, "Safari") } returns "Web - Safari"
        every { getString(BitwardenString.web_platform, "Vivaldi") } returns "Web - Vivaldi"
        every { getString(BitwardenString.cli_platform, "Windows") } returns "CLI - Windows"
        every { getString(BitwardenString.cli_platform, "MacOS") } returns "CLI - MacOS"
        every { getString(BitwardenString.cli_platform, "Linux") } returns "CLI - Linux"
        // Categories without platform: getText(resId)
        every { getText(BitwardenString.sdk) } returns "SDK"
        every { getText(BitwardenString.server) } returns "Server"
        every { getText(BitwardenString.unknown_device) } returns "Unknown device"
    }

    @Test
    fun `type 0 should return Mobile - Android`() {
        assertEquals(
            "Mobile - Android",
            0.readableDeviceTypeName.toString(resources),
        )
    }

    @Test
    fun `type 1 should return Mobile - iOS`() {
        assertEquals(
            "Mobile - iOS",
            1.readableDeviceTypeName.toString(resources),
        )
    }

    @Test
    fun `type 2 should return Extension - Chrome`() {
        assertEquals(
            "Extension - Chrome",
            2.readableDeviceTypeName.toString(resources),
        )
    }

    @Test
    fun `type 3 should return Extension - Firefox`() {
        assertEquals(
            "Extension - Firefox",
            3.readableDeviceTypeName.toString(resources),
        )
    }

    @Test
    fun `type 5 should return Extension - Edge`() {
        assertEquals(
            "Extension - Edge",
            5.readableDeviceTypeName.toString(resources),
        )
    }

    @Test
    fun `type 6 should return Desktop - Windows`() {
        assertEquals(
            "Desktop - Windows",
            6.readableDeviceTypeName.toString(resources),
        )
    }

    @Test
    fun `type 7 should return Desktop - MacOS`() {
        assertEquals(
            "Desktop - MacOS",
            7.readableDeviceTypeName.toString(resources),
        )
    }

    @Test
    fun `type 9 should return Web - Chrome`() {
        assertEquals(
            "Web - Chrome",
            9.readableDeviceTypeName.toString(resources),
        )
    }

    @Test
    fun `type 15 should return Mobile - Amazon`() {
        assertEquals(
            "Mobile - Amazon",
            15.readableDeviceTypeName.toString(resources),
        )
    }

    @Test
    fun `type 16 should return Desktop - Windows UWP`() {
        assertEquals(
            "Desktop - Windows UWP",
            16.readableDeviceTypeName.toString(resources),
        )
    }

    @Test
    fun `type 20 should return Extension - Safari`() {
        assertEquals(
            "Extension - Safari",
            20.readableDeviceTypeName.toString(resources),
        )
    }

    @Test
    fun `type 21 should return SDK category only with no platform suffix`() {
        assertEquals(
            "SDK",
            21.readableDeviceTypeName.toString(resources),
        )
    }

    @Test
    fun `type 22 should return Server category only with no platform suffix`() {
        assertEquals(
            "Server",
            22.readableDeviceTypeName.toString(resources),
        )
    }

    @Test
    fun `type 23 should return CLI - Windows`() {
        assertEquals(
            "CLI - Windows",
            23.readableDeviceTypeName.toString(resources),
        )
    }

    @Test
    fun `type 24 should return CLI - MacOS`() {
        assertEquals(
            "CLI - MacOS",
            24.readableDeviceTypeName.toString(resources),
        )
    }

    @Test
    fun `type 25 should return CLI - Linux`() {
        assertEquals(
            "CLI - Linux",
            25.readableDeviceTypeName.toString(resources),
        )
    }

    @Test
    fun `type 26 should return Extension - DuckDuckGo`() {
        assertEquals(
            "Extension - DuckDuckGo",
            26.readableDeviceTypeName.toString(resources),
        )
    }

    @Test
    fun `unknown type should return unknown device string`() {
        assertEquals(
            "Unknown device",
            999.readableDeviceTypeName.toString(resources),
        )
    }

    @Test
    fun `negative type should return unknown device string`() {
        assertEquals(
            "Unknown device",
            (-1).readableDeviceTypeName.toString(resources),
        )
    }
}
