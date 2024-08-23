package com.x8bit.bitwarden.data.platform.manager

import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SpecialCircumstanceManagerTest {
    private val specialCircumstanceManager: SpecialCircumstanceManager =
        SpecialCircumstanceManagerImpl()

    @Test
    fun `specialCircumstanceStateFlow should emit whenever the SpecialCircumstance is updated`() =
        runTest {
            specialCircumstanceManager.specialCircumstanceStateFlow.test {
                assertNull(awaitItem())

                val specialCircumstance1 = mockk<SpecialCircumstance>()
                specialCircumstanceManager.specialCircumstance = specialCircumstance1

                assertEquals(specialCircumstance1, awaitItem())

                val specialCircumstance2 = mockk<SpecialCircumstance>()
                specialCircumstanceManager.specialCircumstance = specialCircumstance2

                assertEquals(specialCircumstance2, awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `clearSpecialCircumstanceAfterLogin should clear the SpecialCircumstance if it is a PreLogin`() =
        runTest {
            specialCircumstanceManager.specialCircumstanceStateFlow.test {
                assertNull(awaitItem())

                val preLoginSpecialCircumstance =
                    mockk<SpecialCircumstance.PreLogin.CompleteRegistration>()

                specialCircumstanceManager.specialCircumstance = preLoginSpecialCircumstance
                assertEquals(preLoginSpecialCircumstance, awaitItem())

                specialCircumstanceManager.clearSpecialCircumstanceAfterLogin()
                assertNull(awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `clearSpecialCircumstanceAfterLogin should not clear the SpecialCircumstance if it is not a PreLogin`() =
        runTest {
            specialCircumstanceManager.specialCircumstanceStateFlow.test {
                assertNull(awaitItem())

                specialCircumstanceManager.specialCircumstance = SpecialCircumstance.VaultShortcut
                assertEquals(SpecialCircumstance.VaultShortcut, awaitItem())

                specialCircumstanceManager.clearSpecialCircumstanceAfterLogin()
                expectNoEvents()
            }
        }
}
