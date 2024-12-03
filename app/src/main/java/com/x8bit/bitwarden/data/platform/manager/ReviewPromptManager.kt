package com.x8bit.bitwarden.data.platform.manager

/**
 * Responsible for managing whether or not the app review prompt should be shown.
 */
interface ReviewPromptManager {
    /**
     * Increments the add cipher item action count for the active user.
     */
    fun incrementAddCipherActionCount()

    /**
     * Increments the copied generated result action count for the active user.
     */
    fun incrementCopyGeneratedResultActionCount()

    /**
     * Increments the created send action count for the active user.
     */
    fun incrementCreateSendActionCount()

    /**
     * Returns a boolean value indicating whether or not the user should be prompted to
     * review the app.
     */
    fun shouldPromptForAppReview(): Boolean
}
