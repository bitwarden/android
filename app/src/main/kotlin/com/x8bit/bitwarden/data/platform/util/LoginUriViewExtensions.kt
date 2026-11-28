package com.x8bit.bitwarden.data.platform.util

import com.bitwarden.vault.LoginUriView

/**
 * The prefix of every login URI that pairs the login with a desktop app.
 */
private const val DESKTOP_APP_URI_PREFIX: String = "desktopapp://"

/**
 * Returns `true` if this login URI pairs the login with a desktop app, whether it is a
 * valid pairing.
 */
val LoginUriView.isDesktopAppUri: Boolean
    get() = this.uri?.startsWith(prefix = DESKTOP_APP_URI_PREFIX) == true
