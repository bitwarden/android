package com.x8bit.bitwarden.data.autofill.model.browser

private const val BRAVE_CHANNEL_PACKAGE = "com.brave.browser"
private const val CHROME_BETA_CHANNEL_PACKAGE = "com.chrome.beta"
private const val CHROME_RELEASE_CHANNEL_PACKAGE = "com.android.chrome"

/**
 * Enumerated values of each browser that supports third party autofill checks.
 *
 * @property packageName the package name of the release channel for the browser version.
 */
enum class BrowserPackage(val packageName: String) {
    BRAVE_RELEASE(BRAVE_CHANNEL_PACKAGE),
    CHROME_STABLE(CHROME_RELEASE_CHANNEL_PACKAGE),
    CHROME_BETA(CHROME_BETA_CHANNEL_PACKAGE),
}
