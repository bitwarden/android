package com.x8bit.bitwarden.data.autofill.manager.chrome

import app.cash.turbine.test
import com.x8bit.bitwarden.data.autofill.model.chrome.ChromeThirdPartyAutoFillData
import com.x8bit.bitwarden.data.autofill.model.chrome.ChromeThirdPartyAutofillStatus
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ChromeThirdPartyAutofillEnabledManagerTest {
    private val mutableChromeAutofillFeatureFlow = MutableStateFlow(true)
    private val featureFlagManager = mockk<FeatureFlagManager> {
        every {
            getFeatureFlagFlow(FlagKey.ChromeAutofill)
        } returns mutableChromeAutofillFeatureFlow
    }
    private val chromeThirdPartyAutofillEnabledManager =
        ChromeThirdPartyAutofillEnabledManagerImpl(featureFlagManager = featureFlagManager)

    @Suppress("MaxLineLength")
    @Test
    fun `chromeThirdPartyAutofillStatusStateFlow should emit whenever isAutofillEnabled is set to a unique value`() =
        runTest {
            chromeThirdPartyAutofillEnabledManager.chromeThirdPartyAutofillStatusFlow.test {
                assertEquals(
                    DEFAULT_EXPECTED_AUTOFILL_STATUS,
                    awaitItem(),
                )
                val firstExpectedStatusChange = DEFAULT_EXPECTED_AUTOFILL_STATUS.copy(
                    stableStatusData = DEFAULT_CHROME_AUTOFILL_DATA.copy(isAvailable = true),
                )
                chromeThirdPartyAutofillEnabledManager.chromeThirdPartyAutofillStatus =
                    firstExpectedStatusChange

                assertEquals(
                    firstExpectedStatusChange,
                    awaitItem(),
                )
                chromeThirdPartyAutofillEnabledManager.chromeThirdPartyAutofillStatus =
                    firstExpectedStatusChange.copy()
                expectNoEvents()

                val secondExpectedStatusChange = firstExpectedStatusChange
                    .copy(
                        betaChannelStatusData = DEFAULT_CHROME_AUTOFILL_DATA.copy(
                            isThirdPartyEnabled = true,
                        ),
                    )
                chromeThirdPartyAutofillEnabledManager.chromeThirdPartyAutofillStatus =
                    secondExpectedStatusChange

                assertEquals(
                    secondExpectedStatusChange,
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `chromeThirdPartyAutofillStatusStateFlow should not emit whenever isAutofillEnabled is set to a unique value if the feature is off`() =
        runTest {
            mutableChromeAutofillFeatureFlow.update { false }
            chromeThirdPartyAutofillEnabledManager.chromeThirdPartyAutofillStatusFlow.test {
                assertEquals(
                    DEFAULT_EXPECTED_AUTOFILL_STATUS,
                    awaitItem(),
                )
                val firstExpectedStatusChange = DEFAULT_EXPECTED_AUTOFILL_STATUS.copy(
                    stableStatusData = DEFAULT_CHROME_AUTOFILL_DATA.copy(isAvailable = true),
                )
                chromeThirdPartyAutofillEnabledManager.chromeThirdPartyAutofillStatus =
                    firstExpectedStatusChange

                assertEquals(
                    DEFAULT_EXPECTED_AUTOFILL_STATUS,
                    awaitItem(),
                )
                chromeThirdPartyAutofillEnabledManager.chromeThirdPartyAutofillStatus =
                    firstExpectedStatusChange.copy()
                expectNoEvents()

                val secondExpectedStatusChange = firstExpectedStatusChange
                    .copy(
                        betaChannelStatusData = DEFAULT_CHROME_AUTOFILL_DATA.copy(
                            isThirdPartyEnabled = true,
                        ),
                    )
                chromeThirdPartyAutofillEnabledManager.chromeThirdPartyAutofillStatus =
                    secondExpectedStatusChange

                assertEquals(
                    DEFAULT_EXPECTED_AUTOFILL_STATUS,
                    awaitItem(),
                )
            }
        }
}

private val DEFAULT_CHROME_AUTOFILL_DATA = ChromeThirdPartyAutoFillData(
    isAvailable = false,
    isThirdPartyEnabled = false,
)

private val DEFAULT_EXPECTED_AUTOFILL_STATUS = ChromeThirdPartyAutofillStatus(
    stableStatusData = DEFAULT_CHROME_AUTOFILL_DATA,
    betaChannelStatusData = DEFAULT_CHROME_AUTOFILL_DATA,
)
