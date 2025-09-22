package com.x8bit.bitwarden.data.autofill.manager.browser

import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManager
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import java.time.Clock

/**
 * We only show the dialog once per 24 hour period.
 */
private const val SHOW_DIALOG_DELAY_MS: Long = 24L * 60L * 60L * 1000L

/**
 * The default implementation of the [BrowserAutofillDialogManager].
 */
internal class BrowserAutofillDialogManagerImpl(
    private val autofillEnabledManager: AutofillEnabledManager,
    private val browserThirdPartyAutofillEnabledManager: BrowserThirdPartyAutofillEnabledManager,
    private val clock: Clock,
    private val settingsDiskSource: SettingsDiskSource,
) : BrowserAutofillDialogManager {
    override val shouldShowDialog: Boolean
        get() = autofillEnabledManager.isAutofillEnabled &&
            browserThirdPartyAutofillEnabledManager
                .browserThirdPartyAutofillStatus
                .isAnyIsAvailableAndDisabled &&
            settingsDiskSource.browserAutofillDialogReshowTime?.isBefore(clock.instant()) != false

    override fun delayDialog() {
        settingsDiskSource.browserAutofillDialogReshowTime =
            clock.instant().plusMillis(SHOW_DIALOG_DELAY_MS)
    }
}
