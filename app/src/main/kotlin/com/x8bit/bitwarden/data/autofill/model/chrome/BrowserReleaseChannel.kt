package com.x8bit.bitwarden.data.autofill.model.chrome

private const val CHROME_BETA_CHANNEL_PACKAGE = "com.chrome.beta"
private const val CHROME_RELEASE_CHANNEL_PACKAGE = "com.android.chrome"

/**
 * Enumerated values of each browser that supports third party autofill checks.
 *
 * @property packageName the package name of the release channel for the browser version.
 */
enum class BrowserReleaseChannel(val packageName: String) {
    CHROME_STABLE(CHROME_RELEASE_CHANNEL_PACKAGE),
    CHROME_BETA(CHROME_BETA_CHANNEL_PACKAGE),
}
