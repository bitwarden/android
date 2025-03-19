package com.x8bit.bitwarden.data.autofill.model.chrome

private const val BETA_CHANNEL_PACKAGE = "com.chrome.beta"
private const val CHROME_CHANNEL_PACKAGE = "com.android.chrome"

/**
 * Enumerated values of each version of Chrome supported for third party autofill checks.
 *
 * @property packageName the package name of the release channel for the Chrome version.
 */
enum class ChromeReleaseChannel(val packageName: String) {
    STABLE(CHROME_CHANNEL_PACKAGE),
    BETA(BETA_CHANNEL_PACKAGE),
}
