package com.x8bit.bitwarden.data.platform.manager

/**
 * Responsible for managing whether or not the app review prompt should be shown.
 */
interface ReviewPromptManager {
    /**
     * Register an add cipher item action.
     */
    fun registerAddCipherAction()

    /**
     * Register a generated result action.
     */
    fun registerGeneratedResultAction()

    /**
     * Register a create send action.
     */
    fun registerCreateSendAction()

    /**
     * Returns a boolean value indicating whether or not the user should be prompted to
     * review the app.
     */
    fun shouldPromptForAppReview(): Boolean
}
