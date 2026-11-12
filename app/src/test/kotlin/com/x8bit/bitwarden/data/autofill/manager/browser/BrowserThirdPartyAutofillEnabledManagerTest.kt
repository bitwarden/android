package com.x8bit.bitwarden.data.autofill.manager.browser

import app.cash.turbine.test
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutoFillData
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BrowserThirdPartyAutofillEnabledManagerTest {
    private val browserThirdPartyAutofillEnabledManager: BrowserThirdPartyAutofillEnabledManager =
        BrowserThirdPartyAutofillEnabledManagerImpl()

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

    @Test
    fun `isBrowserThirdPartyAutofillStatusCheckedFlow should emit true once status is set`() =
        runTest {
            browserThirdPartyAutofillEnabledManager
                .isBrowserThirdPartyAutofillStatusCheckedFlow
                .test {
                    assertFalse(awaitItem())

                    browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus =
                        DEFAULT_EXPECTED_AUTOFILL_STATUS
                    assertTrue(awaitItem())

                    browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus =
                        DEFAULT_EXPECTED_AUTOFILL_STATUS.copy(
                            chromeStableStatusData = DEFAULT_BROWSER_AUTOFILL_DATA.copy(
                                isAvailable = true,
                            ),
                        )
                    expectNoEvents()
                }
        }
}

private val DEFAULT_BROWSER_AUTOFILL_DATA = BrowserThirdPartyAutoFillData(
    isAvailable = false,
    isThirdPartyEnabled = false,
)

private val DEFAULT_EXPECTED_AUTOFILL_STATUS = BrowserThirdPartyAutofillStatus(
    braveStableStatusData = DEFAULT_BROWSER_AUTOFILL_DATA,
    chromeStableStatusData = DEFAULT_BROWSER_AUTOFILL_DATA,
    chromeBetaChannelStatusData = DEFAULT_BROWSER_AUTOFILL_DATA,
    vivaldiStableChannelStatusData = DEFAULT_BROWSER_AUTOFILL_DATA,
    defaultBrowserPackageName = null,
)
