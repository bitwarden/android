package com.x8bit.bitwarden.data.autofill.manager

import app.cash.turbine.test
import com.bitwarden.vault.CipherView
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AutofillSelectionManagerTest {
    private val autofillSelectionManager: AutofillSelectionManager = AutofillSelectionManagerImpl()

    @Test
    fun `autofillSelectionFlow should emit whenever emitAutofillSelection is called`() =
        runTest {
            autofillSelectionManager.autofillSelectionFlow.test {
                expectNoEvents()

                val cipherView1 = mockk<CipherView>()
                autofillSelectionManager.emitAutofillSelection(cipherView = cipherView1)

                assertEquals(cipherView1, awaitItem())

                val cipherView2 = mockk<CipherView>()
                autofillSelectionManager.emitAutofillSelection(cipherView = cipherView2)

                assertEquals(cipherView2, awaitItem())
            }
        }
}
