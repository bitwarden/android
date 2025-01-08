package com.x8bit.bitwarden.data.platform.manager.model

import kotlinx.serialization.Serializable

/**
 * Data class representing the Screen Data for app resume.
 */
@Serializable
sealed class AppResumeScreenData {

    /**
     * Data class representing the Generator screen for app resume.
     */
    @Serializable
    data object GeneratorScreen : AppResumeScreenData()

    /**
     * Data class representing the Send screen for app resume.
     */
    @Serializable
    data object SendScreen : AppResumeScreenData()
}
