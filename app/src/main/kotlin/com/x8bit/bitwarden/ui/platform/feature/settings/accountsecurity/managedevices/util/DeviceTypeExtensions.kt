package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices.util

import com.bitwarden.network.model.DeviceType
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText

/**
 * Converts a [DeviceType] to a human-readable display name.
 * Returns e.g. "Mobile - Android", "Extension - Chrome", "Desktop - Windows".
 */
@Suppress("CyclomaticComplexMethod")
val DeviceType.readableDeviceTypeName: Text
    get() = when (this) {
        DeviceType.ANDROID -> BitwardenString.mobile_platform.asText("Android")
        DeviceType.IOS -> BitwardenString.mobile_platform.asText("iOS")
        DeviceType.CHROME_EXTENSION -> BitwardenString.extension_platform.asText("Chrome")
        DeviceType.FIREFOX_EXTENSION -> BitwardenString.extension_platform.asText("Firefox")
        DeviceType.OPERA_EXTENSION -> BitwardenString.extension_platform.asText("Opera")
        DeviceType.EDGE_EXTENSION -> BitwardenString.extension_platform.asText("Edge")
        DeviceType.WINDOWS_DESKTOP -> BitwardenString.desktop_platform.asText("Windows")
        DeviceType.MAC_OS_DESKTOP -> BitwardenString.desktop_platform.asText("MacOS")
        DeviceType.LINUX_DESKTOP -> BitwardenString.desktop_platform.asText("Linux")
        DeviceType.CHROME_BROWSER -> BitwardenString.web_platform.asText("Chrome")
        DeviceType.FIREFOX_BROWSER -> BitwardenString.web_platform.asText("Firefox")
        DeviceType.OPERA_BROWSER -> BitwardenString.web_platform.asText("Opera")
        DeviceType.EDGE_BROWSER -> BitwardenString.web_platform.asText("Edge")
        DeviceType.IE_BROWSER -> BitwardenString.web_platform.asText("IE")
        DeviceType.UNKNOWN_BROWSER -> BitwardenString.web_platform.asText("Unknown")
        DeviceType.ANDROID_AMAZON -> BitwardenString.mobile_platform.asText("Amazon")
        DeviceType.UWP -> BitwardenString.desktop_platform.asText("Windows UWP")
        DeviceType.SAFARI_BROWSER -> BitwardenString.web_platform.asText("Safari")
        DeviceType.VIVALDI_BROWSER -> BitwardenString.web_platform.asText("Vivaldi")
        DeviceType.VIVALDI_EXTENSION -> BitwardenString.extension_platform.asText("Vivaldi")
        DeviceType.SAFARI_EXTENSION -> BitwardenString.extension_platform.asText("Safari")
        DeviceType.SDK -> BitwardenString.sdk.asText()
        DeviceType.SERVER -> BitwardenString.server.asText()
        DeviceType.WINDOWS_CLI -> BitwardenString.cli_platform.asText("Windows")
        DeviceType.MAC_OS_CLI -> BitwardenString.cli_platform.asText("MacOS")
        DeviceType.LINUX_CLI -> BitwardenString.cli_platform.asText("Linux")
        DeviceType.DUCK_DUCK_GO_BROWSER -> BitwardenString.extension_platform.asText("DuckDuckGo")
        DeviceType.UNKNOWN -> BitwardenString.unknown_device.asText()
    }
