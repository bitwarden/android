package com.bitwarden.data.manager.appstate.model

import android.content.Intent

/**
 * Represents the creation state of the app.
 */
sealed class AppCreationState {
    /**
     * Denotes that the app is currently created.
     *
     * @param intent The intent data that started the app.
     */
    data class Created(val intent: Intent) : AppCreationState()

    /**
     * Denotes that the app is currently destroyed.
     */
    data object Destroyed : AppCreationState()
}
