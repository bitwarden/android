package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices.util

import androidx.annotation.StringRes
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText

private data class DeviceTypeEntry(@StringRes val categoryResId: Int, val platform: String)

/**
 * Converts a device type integer to a human-readable display name.
 * Returns e.g. "Mobile - Android", "Extension - Chrome", "Desktop - Windows".
 */
@Suppress("CyclomaticComplexMethod", "MagicNumber")
val Int.readableDeviceTypeName: Text
    get() {
        val entry: DeviceTypeEntry = when (this) {
            0 -> DeviceTypeEntry(BitwardenString.mobile_platform, "Android")
            1 -> DeviceTypeEntry(BitwardenString.mobile_platform, "iOS")
            2 -> DeviceTypeEntry(BitwardenString.extension_platform, "Chrome")
            3 -> DeviceTypeEntry(BitwardenString.extension_platform, "Firefox")
            4 -> DeviceTypeEntry(BitwardenString.extension_platform, "Opera")
            5 -> DeviceTypeEntry(BitwardenString.extension_platform, "Edge")
            6 -> DeviceTypeEntry(BitwardenString.desktop_platform, "Windows")
            7 -> DeviceTypeEntry(BitwardenString.desktop_platform, "MacOS")
            8 -> DeviceTypeEntry(BitwardenString.desktop_platform, "Linux")
            9 -> DeviceTypeEntry(BitwardenString.web_platform, "Chrome")
            10 -> DeviceTypeEntry(BitwardenString.web_platform, "Firefox")
            11 -> DeviceTypeEntry(BitwardenString.web_platform, "Opera")
            12 -> DeviceTypeEntry(BitwardenString.web_platform, "Edge")
            13 -> DeviceTypeEntry(BitwardenString.web_platform, "IE")
            14 -> DeviceTypeEntry(BitwardenString.web_platform, "Unknown")
            15 -> DeviceTypeEntry(BitwardenString.mobile_platform, "Amazon")
            16 -> DeviceTypeEntry(BitwardenString.desktop_platform, "Windows UWP")
            17 -> DeviceTypeEntry(BitwardenString.web_platform, "Safari")
            18 -> DeviceTypeEntry(BitwardenString.web_platform, "Vivaldi")
            19 -> DeviceTypeEntry(BitwardenString.extension_platform, "Vivaldi")
            20 -> DeviceTypeEntry(BitwardenString.extension_platform, "Safari")
            21 -> DeviceTypeEntry(BitwardenString.sdk, "")
            22 -> DeviceTypeEntry(BitwardenString.server, "")
            23 -> DeviceTypeEntry(BitwardenString.cli_platform, "Windows")
            24 -> DeviceTypeEntry(BitwardenString.cli_platform, "MacOS")
            25 -> DeviceTypeEntry(BitwardenString.cli_platform, "Linux")
            26 -> DeviceTypeEntry(BitwardenString.extension_platform, "DuckDuckGo")
            else -> return BitwardenString.unknown_device.asText()
        }

        return if (entry.platform.isNotEmpty()) {
            entry.categoryResId.asText(entry.platform)
        } else {
            entry.categoryResId.asText()
        }
    }
