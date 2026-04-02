package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices.util

import androidx.annotation.StringRes
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager

/**
 * Converts a device type integer to a human-readable display name.
 * Returns e.g. "Mobile - Android", "Extension - Chrome", "Desktop - Windows".
 */
@Suppress("CyclomaticComplexMethod", "MagicNumber")
fun Int.toReadableDeviceTypeName(resourceManager: ResourceManager): Text {
    data class DeviceTypeEntry(@StringRes val categoryResId: Int, val platform: String)

    val entry: DeviceTypeEntry = when (this) {
        0 -> DeviceTypeEntry(BitwardenString.mobile, "Android")
        1 -> DeviceTypeEntry(BitwardenString.mobile, "iOS")
        2 -> DeviceTypeEntry(BitwardenString.extension, "Chrome")
        3 -> DeviceTypeEntry(BitwardenString.extension, "Firefox")
        4 -> DeviceTypeEntry(BitwardenString.extension, "Opera")
        5 -> DeviceTypeEntry(BitwardenString.extension, "Edge")
        6 -> DeviceTypeEntry(BitwardenString.desktop, "Windows")
        7 -> DeviceTypeEntry(BitwardenString.desktop, "MacOS")
        8 -> DeviceTypeEntry(BitwardenString.desktop, "Linux")
        9 -> DeviceTypeEntry(BitwardenString.web, "Chrome")
        10 -> DeviceTypeEntry(BitwardenString.web, "Firefox")
        11 -> DeviceTypeEntry(BitwardenString.web, "Opera")
        12 -> DeviceTypeEntry(BitwardenString.web, "Edge")
        13 -> DeviceTypeEntry(BitwardenString.web, "IE")
        14 -> DeviceTypeEntry(BitwardenString.web, "Unknown")
        15 -> DeviceTypeEntry(BitwardenString.mobile, "Amazon")
        16 -> DeviceTypeEntry(BitwardenString.desktop, "Windows UWP")
        17 -> DeviceTypeEntry(BitwardenString.web, "Safari")
        18 -> DeviceTypeEntry(BitwardenString.web, "Vivaldi")
        19 -> DeviceTypeEntry(BitwardenString.extension, "Vivaldi")
        20 -> DeviceTypeEntry(BitwardenString.extension, "Safari")
        21 -> DeviceTypeEntry(BitwardenString.sdk, "")
        22 -> DeviceTypeEntry(BitwardenString.server, "")
        23 -> DeviceTypeEntry(BitwardenString.cli, "Windows")
        24 -> DeviceTypeEntry(BitwardenString.cli, "MacOs")
        25 -> DeviceTypeEntry(BitwardenString.cli, "Linux")
        26 -> DeviceTypeEntry(BitwardenString.extension, "DuckDuckGo")
        else -> return resourceManager.getString(BitwardenString.unknown_device).asText()
    }

    val category = resourceManager.getString(entry.categoryResId)
    return if (entry.platform.isNotEmpty()) {
        "$category - ${entry.platform}".asText()
    } else {
        category.asText()
    }
}
