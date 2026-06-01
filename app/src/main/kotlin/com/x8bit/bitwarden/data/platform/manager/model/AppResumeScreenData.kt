package com.x8bit.bitwarden.data.platform.manager.model

import kotlinx.serialization.Serializable

/**
 * Data class representing the Screen Data for app resume.
 */
@Serializable
sealed class AppResumeScreenData {

    /**
     * Data object representing the Generator screen for app resume.
     */
    @Serializable
    data object GeneratorScreen : AppResumeScreenData()

    /**
     * Data object representing the Send screen for app resume.
     */
    @Serializable
    data object SendScreen : AppResumeScreenData()

    /**
     * Data class representing the Search screen for app resume.
     */
    @Serializable
    data class SearchScreen(val searchTerm: String) : AppResumeScreenData()

    /**
     * Data object representing the Verification Code screen for app resume.
     */
    @Serializable
    data object VerificationCodeScreen : AppResumeScreenData()
}
