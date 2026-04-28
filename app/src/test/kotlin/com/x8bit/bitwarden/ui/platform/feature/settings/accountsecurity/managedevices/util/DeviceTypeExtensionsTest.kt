package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeviceTypeExtensionsTest {

    @Test
    fun `type 0 should return Mobile - Android`() {
        assertEquals(
            BitwardenString.mobile_platform.asText("Android"),
            0.readableDeviceTypeName,
        )
    }

    @Test
    fun `type 1 should return Mobile - iOS`() {
        assertEquals(
            BitwardenString.mobile_platform.asText("iOS"),
            1.readableDeviceTypeName,
        )
    }

    @Test
    fun `type 2 should return Extension - Chrome`() {
        assertEquals(
            BitwardenString.extension_platform.asText("Chrome"),
            2.readableDeviceTypeName,
        )
    }

    @Test
    fun `type 3 should return Extension - Firefox`() {
        assertEquals(
            BitwardenString.extension_platform.asText("Firefox"),
            3.readableDeviceTypeName,
        )
    }

    @Test
    fun `type 5 should return Extension - Edge`() {
        assertEquals(
            BitwardenString.extension_platform.asText("Edge"),
            5.readableDeviceTypeName,
        )
    }

    @Test
    fun `type 6 should return Desktop - Windows`() {
        assertEquals(
            BitwardenString.desktop_platform.asText("Windows"),
            6.readableDeviceTypeName,
        )
    }

    @Test
    fun `type 7 should return Desktop - MacOS`() {
        assertEquals(
            BitwardenString.desktop_platform.asText("MacOS"),
            7.readableDeviceTypeName,
        )
    }

    @Test
    fun `type 9 should return Web - Chrome`() {
        assertEquals(
            BitwardenString.web_platform.asText("Chrome"),
            9.readableDeviceTypeName,
        )
    }

    @Test
    fun `type 15 should return Mobile - Amazon`() {
        assertEquals(
            BitwardenString.mobile_platform.asText("Amazon"),
            15.readableDeviceTypeName,
        )
    }

    @Test
    fun `type 16 should return Desktop - Windows UWP`() {
        assertEquals(
            BitwardenString.desktop_platform.asText("Windows UWP"),
            16.readableDeviceTypeName,
        )
    }

    @Test
    fun `type 20 should return Extension - Safari`() {
        assertEquals(
            BitwardenString.extension_platform.asText("Safari"),
            20.readableDeviceTypeName,
        )
    }

    @Test
    fun `type 21 should return SDK category only with no platform suffix`() {
        assertEquals(
            BitwardenString.sdk.asText(),
            21.readableDeviceTypeName,
        )
    }

    @Test
    fun `type 22 should return Server category only with no platform suffix`() {
        assertEquals(
            BitwardenString.server.asText(),
            22.readableDeviceTypeName,
        )
    }

    @Test
    fun `type 23 should return CLI - Windows`() {
        assertEquals(
            BitwardenString.cli_platform.asText("Windows"),
            23.readableDeviceTypeName,
        )
    }

    @Test
    fun `type 24 should return CLI - MacOS`() {
        assertEquals(
            BitwardenString.cli_platform.asText("MacOS"),
            24.readableDeviceTypeName,
        )
    }

    @Test
    fun `type 25 should return CLI - Linux`() {
        assertEquals(
            BitwardenString.cli_platform.asText("Linux"),
            25.readableDeviceTypeName,
        )
    }

    @Test
    fun `type 26 should return Extension - DuckDuckGo`() {
        assertEquals(
            BitwardenString.extension_platform.asText("DuckDuckGo"),
            26.readableDeviceTypeName,
        )
    }

    @Test
    fun `unknown type should return unknown device string`() {
        assertEquals(
            BitwardenString.unknown_device.asText(),
            999.readableDeviceTypeName,
        )
    }

    @Test
    fun `negative type should return unknown device string`() {
        assertEquals(
            BitwardenString.unknown_device.asText(),
            (-1).readableDeviceTypeName,
        )
    }
}
