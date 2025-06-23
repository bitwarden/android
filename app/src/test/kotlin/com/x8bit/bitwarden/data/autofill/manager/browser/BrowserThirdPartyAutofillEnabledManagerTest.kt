package com.x8bit.bitwarden.data.autofill.manager.browser

import app.cash.turbine.test
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutoFillData
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BrowserThirdPartyAutofillEnabledManagerTest {
    private val mutableChromeAutofillFeatureFlow = MutableStateFlow(true)
    private val featureFlagManager = mockk<FeatureFlagManager> {
        every {
            getFeatureFlagFlow(FlagKey.ChromeAutofill)
        } returns mutableChromeAutofillFeatureFlow
    }
    private val browserThirdPartyAutofillEnabledManager =
        BrowserThirdPartyAutofillEnabledManagerImpl(featureFlagManager = featureFlagManager)

    @Suppress("MaxLineLength")
    @Test
    fun `browserThirdPartyAutofillStatusFlow should emit whenever isAutofillEnabled is set to a unique value`() =
        runTest {
            browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatusFlow.test {
                assertEquals(
                    DEFAULT_EXPECTED_AUTOFILL_STATUS,
                    awaitItem(),
                )
                val firstExpectedStatusChange = DEFAULT_EXPECTED_AUTOFILL_STATUS.copy(
                    chromeStableStatusData = DEFAULT_BROWSER_AUTOFILL_DATA.copy(isAvailable = true),
                )
                browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus =
                    firstExpectedStatusChange

                assertEquals(
                    firstExpectedStatusChange,
                    awaitItem(),
                )
                browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus =
                    firstExpectedStatusChange.copy()
                expectNoEvents()

                val secondExpectedStatusChange = firstExpectedStatusChange
                    .copy(
                        chromeBetaChannelStatusData = DEFAULT_BROWSER_AUTOFILL_DATA.copy(
                            isThirdPartyEnabled = true,
                        ),
                    )
                browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus =
                    secondExpectedStatusChange

                assertEquals(
                    secondExpectedStatusChange,
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `browserThirdPartyAutofillStatusFlow should not emit whenever isAutofillEnabled is set to a unique value if the feature is off`() =
        runTest {
            mutableChromeAutofillFeatureFlow.update { false }
            browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatusFlow.test {
                assertEquals(
                    DEFAULT_EXPECTED_AUTOFILL_STATUS,
                    awaitItem(),
                )
                val firstExpectedStatusChange = DEFAULT_EXPECTED_AUTOFILL_STATUS.copy(
                    chromeStableStatusData = DEFAULT_BROWSER_AUTOFILL_DATA.copy(isAvailable = true),
                )
                browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus =
                    firstExpectedStatusChange

                assertEquals(
                    DEFAULT_EXPECTED_AUTOFILL_STATUS,
                    awaitItem(),
                )
                browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus =
                    firstExpectedStatusChange.copy()
                expectNoEvents()

                val secondExpectedStatusChange = firstExpectedStatusChange
                    .copy(
                        chromeBetaChannelStatusData = DEFAULT_BROWSER_AUTOFILL_DATA.copy(
                            isThirdPartyEnabled = true,
                        ),
                    )
                browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus =
                    secondExpectedStatusChange

                assertEquals(
                    DEFAULT_EXPECTED_AUTOFILL_STATUS,
                    awaitItem(),
                )
            }
        }
}

private val DEFAULT_BROWSER_AUTOFILL_DATA = BrowserThirdPartyAutoFillData(
    isAvailable = false,
    isThirdPartyEnabled = false,
)

private val DEFAULT_EXPECTED_AUTOFILL_STATUS = BrowserThirdPartyAutofillStatus(
    chromeStableStatusData = DEFAULT_BROWSER_AUTOFILL_DATA,
    chromeBetaChannelStatusData = DEFAULT_BROWSER_AUTOFILL_DATA,
)
