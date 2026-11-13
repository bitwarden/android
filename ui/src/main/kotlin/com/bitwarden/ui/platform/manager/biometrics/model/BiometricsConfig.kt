package com.bitwarden.ui.platform.manager.biometrics.model

import androidx.annotation.StringRes

/**
 * App-specific configuration for the
 * [com.bitwarden.ui.platform.manager.biometrics.BiometricsManager].
 *
 * @property promptTitleRes The string resource used as the title of all prompts.
 * @property biometricsPromptDescriptionRes The string resource used as the description of the
 * biometrics prompt.
 * @property isDeviceCredentialAllowedForBiometrics Whether device credentials (PIN, pattern, or
 * password) may be used in place of biometrics when prompting for biometrics. This also
 * determines what is considered when checking whether biometrics are supported.
 */
data class BiometricsConfig(
    @StringRes val promptTitleRes: Int,
    @StringRes val biometricsPromptDescriptionRes: Int,
    val isDeviceCredentialAllowedForBiometrics: Boolean,
)
