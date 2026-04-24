package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText

/**
 * Converts a device type integer to a human-readable display name.
 * Returns e.g. "Mobile - Android", "Extension - Chrome", "Desktop - Windows".
 */
@Suppress("CyclomaticComplexMethod", "MagicNumber")
val Int.readableDeviceTypeName: Text
    get() = when (this) {
        0 -> BitwardenString.mobile_platform.asText("Android")
        1 -> BitwardenString.mobile_platform.asText("iOS")
        2 -> BitwardenString.extension_platform.asText("Chrome")
        3 -> BitwardenString.extension_platform.asText("Firefox")
        4 -> BitwardenString.extension_platform.asText("Opera")
        5 -> BitwardenString.extension_platform.asText("Edge")
        6 -> BitwardenString.desktop_platform.asText("Windows")
        7 -> BitwardenString.desktop_platform.asText("MacOS")
        8 -> BitwardenString.desktop_platform.asText("Linux")
        9 -> BitwardenString.web_platform.asText("Chrome")
        10 -> BitwardenString.web_platform.asText("Firefox")
        11 -> BitwardenString.web_platform.asText("Opera")
        12 -> BitwardenString.web_platform.asText("Edge")
        13 -> BitwardenString.web_platform.asText("IE")
        14 -> BitwardenString.web_platform.asText("Unknown")
        15 -> BitwardenString.mobile_platform.asText("Amazon")
        16 -> BitwardenString.desktop_platform.asText("Windows UWP")
        17 -> BitwardenString.web_platform.asText("Safari")
        18 -> BitwardenString.web_platform.asText("Vivaldi")
        19 -> BitwardenString.extension_platform.asText("Vivaldi")
        20 -> BitwardenString.extension_platform.asText("Safari")
        21 -> BitwardenString.sdk.asText()
        22 -> BitwardenString.server.asText()
        23 -> BitwardenString.cli_platform.asText("Windows")
        24 -> BitwardenString.cli_platform.asText("MacOS")
        25 -> BitwardenString.cli_platform.asText("Linux")
        26 -> BitwardenString.extension_platform.asText("DuckDuckGo")
        else -> BitwardenString.unknown_device.asText()
    }
