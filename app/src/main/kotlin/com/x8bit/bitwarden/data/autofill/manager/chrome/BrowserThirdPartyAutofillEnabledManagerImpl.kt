package com.x8bit.bitwarden.data.autofill.manager.chrome

import com.x8bit.bitwarden.data.autofill.model.chrome.BrowserThirdPartyAutoFillData
import com.x8bit.bitwarden.data.autofill.model.chrome.BrowserThirdPartyAutofillStatus
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
            mutableChromeThirdPartyAutofillStatusStateFlow.update {
                value
            }
        }

    private val mutableChromeThirdPartyAutofillStatusStateFlow = MutableStateFlow(
        value = browserThirdPartyAutofillStatus,
    )

    override val chromeThirdPartyAutofillStatusFlow: Flow<BrowserThirdPartyAutofillStatus>
        get() = mutableChromeThirdPartyAutofillStatusStateFlow
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
    chromeStableStatusData = BrowserThirdPartyAutoFillData(
        isAvailable = false,
        isThirdPartyEnabled = false,
    ),
    chromeBetaChannelStatusData = BrowserThirdPartyAutoFillData(
        isAvailable = false,
        isThirdPartyEnabled = false,
    ),
)
