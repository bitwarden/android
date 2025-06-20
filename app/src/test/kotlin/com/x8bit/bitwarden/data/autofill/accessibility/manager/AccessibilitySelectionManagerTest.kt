package com.x8bit.bitwarden.data.autofill.accessibility.manager

import app.cash.turbine.test
import com.bitwarden.vault.CipherView
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AccessibilitySelectionManagerTest {

    private val accessibilitySelectionManager: AccessibilitySelectionManager =
        AccessibilitySelectionManagerImpl()

    @Test
    fun `autofillSelectionFlow should emit whenever emitAutofillSelection is called`() =
        runTest {
            accessibilitySelectionManager.accessibilitySelectionFlow.test {
                expectNoEvents()

                val cipherView1 = mockk<CipherView>()
                accessibilitySelectionManager.emitAccessibilitySelection(cipherView = cipherView1)

                assertEquals(cipherView1, awaitItem())

                val cipherView2 = mockk<CipherView>()
                accessibilitySelectionManager.emitAccessibilitySelection(cipherView = cipherView2)

                assertEquals(cipherView2, awaitItem())
            }
        }
}
