package com.x8bit.bitwarden.data.autofill.manager.chrome

import com.x8bit.bitwarden.data.autofill.model.chrome.ChromeThirdPartyAutoFillData
import com.x8bit.bitwarden.data.autofill.model.chrome.ChromeThirdPartyAutofillStatus
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

/**
 * Default implementation of [ChromeThirdPartyAutofillEnabledManager].
 */
class ChromeThirdPartyAutofillEnabledManagerImpl(
    private val featureFlagManager: FeatureFlagManager,
) : ChromeThirdPartyAutofillEnabledManager {
    override var chromeThirdPartyAutofillStatus: ChromeThirdPartyAutofillStatus = DEFAULT_STATUS
        set(value) {
            field = value
            mutableChromeThirdPartyAutofillStatusStateFlow.update {
                value
            }
        }

    private val mutableChromeThirdPartyAutofillStatusStateFlow = MutableStateFlow(
        chromeThirdPartyAutofillStatus,
    )

    override val chromeThirdPartyAutofillStatusFlow: Flow<ChromeThirdPartyAutofillStatus>
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

private val DEFAULT_STATUS = ChromeThirdPartyAutofillStatus(
    ChromeThirdPartyAutoFillData(
        isAvailable = false,
        isThirdPartyEnabled = false,
    ),
    ChromeThirdPartyAutoFillData(
        isAvailable = false,
        isThirdPartyEnabled = false,
    ),
)
