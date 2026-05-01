package com.bitwarden.network.model

import androidx.annotation.Keep
import com.bitwarden.core.data.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the type of registered device as returned by the server.
 */
@Serializable(DeviceTypeSerializer::class)
enum class DeviceType {
    @SerialName("0")
    ANDROID,

    @SerialName("1")
    IOS,

    @SerialName("2")
    CHROME_EXTENSION,

    @SerialName("3")
    FIREFOX_EXTENSION,

    @SerialName("4")
    OPERA_EXTENSION,

    @SerialName("5")
    EDGE_EXTENSION,

    @SerialName("6")
    WINDOWS_DESKTOP,

    @SerialName("7")
    MAC_OS_DESKTOP,

    @SerialName("8")
    LINUX_DESKTOP,

    @SerialName("9")
    CHROME_BROWSER,

    @SerialName("10")
    FIREFOX_BROWSER,

    @SerialName("11")
    OPERA_BROWSER,

    @SerialName("12")
    EDGE_BROWSER,

    @SerialName("13")
    IE_BROWSER,

    @SerialName("14")
    UNKNOWN_BROWSER,

    @SerialName("15")
    ANDROID_AMAZON,

    @SerialName("16")
    UWP,

    @SerialName("17")
    SAFARI_BROWSER,

    @SerialName("18")
    VIVALDI_BROWSER,

    @SerialName("19")
    VIVALDI_EXTENSION,

    @SerialName("20")
    SAFARI_EXTENSION,

    @SerialName("21")
    SDK,

    @SerialName("22")
    SERVER,

    @SerialName("23")
    WINDOWS_CLI,

    @SerialName("24")
    MAC_OS_CLI,

    @SerialName("25")
    LINUX_CLI,

    @SerialName("26")
    DUCK_DUCK_GO_BROWSER,

    /**
     * Represents an unknown device type.
     *
     * This is used for forward compatibility to handle new device types that the client doesn't
     * yet understand.
     */
    @SerialName("-1")
    UNKNOWN,
}

@Keep
private class DeviceTypeSerializer : BaseEnumeratedIntSerializer<DeviceType>(
    className = "DeviceType",
    values = DeviceType.entries.toTypedArray(),
    default = DeviceType.UNKNOWN,
)
