package com.x8bit.bitwarden.data.autofill.manager

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AutofillEnabledManagerTest {
    private val autofillEnabledManager: AutofillEnabledManager = AutofillEnabledManagerImpl()

    @Suppress("MaxLineLength")
    @Test
    fun `isAutofillEnabledStateFlow should emit whenever isAutofillEnabled is set to a unique value`() =
        runTest {
            autofillEnabledManager.isAutofillEnabledStateFlow.test {
                assertFalse(awaitItem())

                autofillEnabledManager.isAutofillEnabled = true
                assertTrue(awaitItem())

                autofillEnabledManager.isAutofillEnabled = true
                expectNoEvents()

                autofillEnabledManager.isAutofillEnabled = false
                assertFalse(awaitItem())
            }
        }
}
