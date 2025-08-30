package com.x8bit.bitwarden.data.auth.datasource.disk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Describes the current status of a user in the account onboarding steps.
 */
@Serializable
enum class OnboardingStatus {

    /**
     * Onboarding has not yet started.
     */
    @SerialName("notStarted")
    NOT_STARTED,

    /**
     * The user is completing the account lock setup.
     */
    @SerialName("accountLockSetup")
    ACCOUNT_LOCK_SETUP,

    /**
     * The user is completing the auto fill service setup.
     */
    @SerialName("autofillSetup")
    AUTOFILL_SETUP,

    /**
     * The user is completing the final step of the onboarding process.
     */
    @SerialName("finalStep")
    FINAL_STEP,

    /**
     * The user has completed all onboarding steps.
     */
    @SerialName("complete")
    COMPLETE,
}
