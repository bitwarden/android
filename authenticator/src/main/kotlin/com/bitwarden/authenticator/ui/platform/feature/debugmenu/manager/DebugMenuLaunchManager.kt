package com.bitwarden.authenticator.ui.platform.feature.debugmenu.manager

import android.view.InputEvent

/**
 * Manager for abstracting the logic of launching debug menu.
 */
interface DebugMenuLaunchManager {

    /**
     * Defines an interface to action on specific input events.
     * @param event the input event to evaluate
     * @param action the action to perform if the event matches
     *
     * @return true if the action was performed, false otherwise.
     */
    fun actionOnInputEvent(event: InputEvent, action: () -> Unit): Boolean
}
