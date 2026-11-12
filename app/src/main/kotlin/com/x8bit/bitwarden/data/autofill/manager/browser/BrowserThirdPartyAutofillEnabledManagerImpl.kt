package com.x8bit.bitwarden.data.autofill.manager.browser

import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutoFillData
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Default implementation of [BrowserThirdPartyAutofillEnabledManager].
 */
class BrowserThirdPartyAutofillEnabledManagerImpl : BrowserThirdPartyAutofillEnabledManager {
    override var browserThirdPartyAutofillStatus: BrowserThirdPartyAutofillStatus = DEFAULT_STATUS
        set(value) {
            field = value
            mutableBrowserThirdPartyAutofillStatusStateFlow.update {
                value
            }
            mutableIsBrowserThirdPartyAutofillStatusCheckedStateFlow.value = true
        }

    private val mutableBrowserThirdPartyAutofillStatusStateFlow = MutableStateFlow(
        value = browserThirdPartyAutofillStatus,
    )

    // Flipped to true by the first set of browserThirdPartyAutofillStatus and never reset, so
    // callers can tell DEFAULT_STATUS apart from a real device check.
    private val mutableIsBrowserThirdPartyAutofillStatusCheckedStateFlow = MutableStateFlow(
        value = false,
    )

    override val browserThirdPartyAutofillStatusFlow: Flow<BrowserThirdPartyAutofillStatus>
        get() = mutableBrowserThirdPartyAutofillStatusStateFlow

    override val isBrowserThirdPartyAutofillStatusCheckedFlow: StateFlow<Boolean>
        get() = mutableIsBrowserThirdPartyAutofillStatusCheckedStateFlow.asStateFlow()
}

private val DEFAULT_STATUS = BrowserThirdPartyAutofillStatus(
    braveStableStatusData = BrowserThirdPartyAutoFillData(
        isAvailable = false,
        isThirdPartyEnabled = false,
    ),
    chromeStableStatusData = BrowserThirdPartyAutoFillData(
        isAvailable = false,
        isThirdPartyEnabled = false,
    ),
    chromeBetaChannelStatusData = BrowserThirdPartyAutoFillData(
        isAvailable = false,
        isThirdPartyEnabled = false,
    ),
    vivaldiStableChannelStatusData = BrowserThirdPartyAutoFillData(
        isAvailable = false,
        isThirdPartyEnabled = false,
    ),
    defaultBrowserPackageName = null,
)
