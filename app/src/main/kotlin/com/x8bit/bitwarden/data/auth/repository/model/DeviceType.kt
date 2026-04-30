package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Represents the type of a registered device, mirroring the server's DeviceType enum.
 *
 * @property value The integer value sent by the server.
 */
@Suppress("MagicNumber")
enum class DeviceType(val value: Int) {
    ANDROID(0),
    IOS(1),
    CHROME_EXTENSION(2),
    FIREFOX_EXTENSION(3),
    OPERA_EXTENSION(4),
    EDGE_EXTENSION(5),
    WINDOWS_DESKTOP(6),
    MAC_OS_DESKTOP(7),
    LINUX_DESKTOP(8),
    CHROME_BROWSER(9),
    FIREFOX_BROWSER(10),
    OPERA_BROWSER(11),
    EDGE_BROWSER(12),
    IE_BROWSER(13),
    UNKNOWN_BROWSER(14),
    ANDROID_AMAZON(15),
    UWP(16),
    SAFARI_BROWSER(17),
    VIVALDI_BROWSER(18),
    VIVALDI_EXTENSION(19),
    SAFARI_EXTENSION(20),
    SDK(21),
    SERVER(22),
    WINDOWS_CLI(23),
    MAC_OS_CLI(24),
    LINUX_CLI(25),
    DUCK_DUCK_GO_BROWSER(26),
    UNKNOWN(-1),
}
