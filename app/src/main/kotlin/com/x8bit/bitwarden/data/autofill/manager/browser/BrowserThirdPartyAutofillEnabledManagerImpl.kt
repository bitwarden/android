package com.x8bit.bitwarden.data.autofill.manager.browser

import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutoFillData
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

/**
 * Default implementation of [BrowserThirdPartyAutofillEnabledManager].
 */
class BrowserThirdPartyAutofillEnabledManagerImpl(
    private val featureFlagManager: FeatureFlagManager,
) : BrowserThirdPartyAutofillEnabledManager {
    override var browserThirdPartyAutofillStatus: BrowserThirdPartyAutofillStatus = DEFAULT_STATUS
        set(value) {
            field = value
            mutableBrowserThirdPartyAutofillStatusStateFlow.update {
                value
            }
        }

    private val mutableBrowserThirdPartyAutofillStatusStateFlow = MutableStateFlow(
        value = browserThirdPartyAutofillStatus,
    )

    override val browserThirdPartyAutofillStatusFlow: Flow<BrowserThirdPartyAutofillStatus>
        get() = mutableBrowserThirdPartyAutofillStatusStateFlow
            .combine(
                featureFlagManager.getFeatureFlagFlow(FlagKey.ChromeAutofill),
            ) { data, enabled ->
                if (enabled) {
                    data
                } else {
                    DEFAULT_STATUS
                }
            }
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
)
