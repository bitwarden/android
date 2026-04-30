package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class DeviceTypeExtensionsTest {
    @TestFactory
    fun `readableDeviceTypeName returns correct value for each known device type`() =
        listOf(
            0 to BitwardenString.mobile_platform.asText("Android"),
            1 to BitwardenString.mobile_platform.asText("iOS"),
            2 to BitwardenString.extension_platform.asText("Chrome"),
            3 to BitwardenString.extension_platform.asText("Firefox"),
            4 to BitwardenString.extension_platform.asText("Opera"),
            5 to BitwardenString.extension_platform.asText("Edge"),
            6 to BitwardenString.desktop_platform.asText("Windows"),
            7 to BitwardenString.desktop_platform.asText("MacOS"),
            8 to BitwardenString.desktop_platform.asText("Linux"),
            9 to BitwardenString.web_platform.asText("Chrome"),
            10 to BitwardenString.web_platform.asText("Firefox"),
            11 to BitwardenString.web_platform.asText("Opera"),
            12 to BitwardenString.web_platform.asText("Edge"),
            13 to BitwardenString.web_platform.asText("IE"),
            14 to BitwardenString.web_platform.asText("Unknown"),
            15 to BitwardenString.mobile_platform.asText("Amazon"),
            16 to BitwardenString.desktop_platform.asText("Windows UWP"),
            17 to BitwardenString.web_platform.asText("Safari"),
            18 to BitwardenString.web_platform.asText("Vivaldi"),
            19 to BitwardenString.extension_platform.asText("Vivaldi"),
            20 to BitwardenString.extension_platform.asText("Safari"),
            21 to BitwardenString.sdk.asText(),
            22 to BitwardenString.server.asText(),
            23 to BitwardenString.cli_platform.asText("Windows"),
            24 to BitwardenString.cli_platform.asText("MacOS"),
            25 to BitwardenString.cli_platform.asText("Linux"),
            26 to BitwardenString.extension_platform.asText("DuckDuckGo"),
        ).map { (type, expected) ->
            dynamicTest("type $type should return $expected") {
                assertEquals(expected, type.readableDeviceTypeName)
            }
        }

    @Test
    fun `unknown type should return unknown device string`() {
        assertEquals(BitwardenString.unknown_device.asText(), 999.readableDeviceTypeName)
    }

    @Test
    fun `negative type should return unknown device string`() {
        assertEquals(BitwardenString.unknown_device.asText(), (-1).readableDeviceTypeName)
    }
}
