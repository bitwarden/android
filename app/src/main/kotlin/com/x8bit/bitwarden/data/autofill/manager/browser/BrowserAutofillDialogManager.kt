package com.x8bit.bitwarden.data.autofill.manager.browser

/**
 * Manager to handle whether the Browser Autofill Dialog should be displayed.
 */
interface BrowserAutofillDialogManager {
    /**
     * Number of browsers installed that may need autofill enabled.
     */
    val browserCount: Int

    /**
     * Indicates whether the dialog should be displayed to the user.
     */
    val shouldShowDialog: Boolean

    /**
     * The dialog has been dismissed and we should delay displaying it again.
     */
    fun delayDialog()
}
