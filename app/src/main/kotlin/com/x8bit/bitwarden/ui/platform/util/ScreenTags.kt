package com.x8bit.bitwarden.ui.platform.util

/**
 * Test tags for the root composable of each screen. UI tests use them to check which screen is
 * shown and to limit lookups to that screen, so a tag shared with a dialog or another screen
 * can't match by mistake.
 */
object ScreenTags {
    const val ACCESSIBILITY_DISCLOSURE: String = "AccessibilityDisclosureScreen"
    const val ENVIRONMENT: String = "EnvironmentScreen"
    const val LANDING: String = "LandingScreen"
    const val LOGIN: String = "LoginScreen"
    const val VAULT: String = "VaultScreen"
    const val VAULT_ITEM: String = "VaultItemScreen"
    const val VAULT_UNLOCKED_NAV_BAR: String = "VaultUnlockedNavBarScreen"
    const val WELCOME: String = "WelcomeScreen"
}
