package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices.util

import com.bitwarden.network.model.DeviceType
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class DeviceTypeExtensionsTest {
    @TestFactory
    fun `readableDeviceTypeName returns correct value for each known device type`() =
        listOf(
            DeviceType.ANDROID to BitwardenString.mobile_platform.asText("Android"),
            DeviceType.IOS to BitwardenString.mobile_platform.asText("iOS"),
            DeviceType.CHROME_EXTENSION to BitwardenString.extension_platform.asText("Chrome"),
            DeviceType.FIREFOX_EXTENSION to BitwardenString.extension_platform.asText("Firefox"),
            DeviceType.OPERA_EXTENSION to BitwardenString.extension_platform.asText("Opera"),
            DeviceType.EDGE_EXTENSION to BitwardenString.extension_platform.asText("Edge"),
            DeviceType.WINDOWS_DESKTOP to BitwardenString.desktop_platform.asText("Windows"),
            DeviceType.MAC_OS_DESKTOP to BitwardenString.desktop_platform.asText("MacOS"),
            DeviceType.LINUX_DESKTOP to BitwardenString.desktop_platform.asText("Linux"),
            DeviceType.CHROME_BROWSER to BitwardenString.web_platform.asText("Chrome"),
            DeviceType.FIREFOX_BROWSER to BitwardenString.web_platform.asText("Firefox"),
            DeviceType.OPERA_BROWSER to BitwardenString.web_platform.asText("Opera"),
            DeviceType.EDGE_BROWSER to BitwardenString.web_platform.asText("Edge"),
            DeviceType.IE_BROWSER to BitwardenString.web_platform.asText("IE"),
            DeviceType.UNKNOWN_BROWSER to BitwardenString.web_platform.asText("Unknown"),
            DeviceType.ANDROID_AMAZON to BitwardenString.mobile_platform.asText("Amazon"),
            DeviceType.UWP to BitwardenString.desktop_platform.asText("Windows UWP"),
            DeviceType.SAFARI_BROWSER to BitwardenString.web_platform.asText("Safari"),
            DeviceType.VIVALDI_BROWSER to BitwardenString.web_platform.asText("Vivaldi"),
            DeviceType.VIVALDI_EXTENSION to BitwardenString.extension_platform.asText("Vivaldi"),
            DeviceType.SAFARI_EXTENSION to BitwardenString.extension_platform.asText("Safari"),
            DeviceType.SDK to BitwardenString.sdk.asText(),
            DeviceType.SERVER to BitwardenString.server.asText(),
            DeviceType.WINDOWS_CLI to BitwardenString.cli_platform.asText("Windows"),
            DeviceType.MAC_OS_CLI to BitwardenString.cli_platform.asText("MacOS"),
            DeviceType.LINUX_CLI to BitwardenString.cli_platform.asText("Linux"),
            DeviceType.DUCK_DUCK_GO_BROWSER to
                BitwardenString.extension_platform.asText("DuckDuckGo"),
            DeviceType.UNKNOWN to BitwardenString.unknown_device.asText(),
        ).map { (type, expected) ->
            dynamicTest("$type should return $expected") {
                assertEquals(expected, type.readableDeviceTypeName)
            }
        }
}
