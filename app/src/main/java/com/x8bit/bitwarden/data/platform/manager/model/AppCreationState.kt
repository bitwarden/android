package com.x8bit.bitwarden.data.platform.manager.model

/**
 * Represents the creation state of the app.
 */
sealed class AppCreationState {
    /**
     * Denotes that the app is currently created.
     *
     * @param isAutoFill Whether the app was created for autofill.
     */
    data class Created(val isAutoFill: Boolean) : AppCreationState()

    /**
     * Denotes that the app is currently destroyed.
     */
    data object Destroyed : AppCreationState()
}
