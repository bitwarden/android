package com.x8bit.bitwarden.data.platform.repository.util

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StateFlowExtensionsTest {

    @Suppress("MaxLineLength")
    @Test
    fun `observeWhenSubscribedAndLoggedIn should observe the given flow depending on the state of the source and user flow`() =
        runTest {
            val userStateFlow = MutableStateFlow<UserStateJson?>(null)
            val observerStateFlow = MutableStateFlow("")
            val sourceMutableStateFlow = MutableStateFlow(Unit)

            assertEquals(0, observerStateFlow.subscriptionCount.value)
            sourceMutableStateFlow
                .observeWhenSubscribedAndLoggedIn(
                    userStateFlow = userStateFlow,
                    observer = { observerStateFlow },
                )
                .launchIn(backgroundScope)

            observerStateFlow.subscriptionCount.test {
                // No subscriber to start
                assertEquals(0, awaitItem())

                userStateFlow.value = mockk<UserStateJson> {
                    every { activeUserId } returns "user_id_1234"
                }
                // Still none, since no one has subscribed to the testMutableStateFlow
                expectNoEvents()

                val job = sourceMutableStateFlow.launchIn(backgroundScope)
                // Now we subscribe to the observer flow since have a active user and a listener
                assertEquals(1, awaitItem())

                userStateFlow.value = mockk<UserStateJson> {
                    every { activeUserId } returns "user_id_4321"
                }
                // The user changed, so we clear the previous observer but then resubscribe
                // with the new user ID
                assertEquals(0, awaitItem())
                assertEquals(1, awaitItem())

                job.cancel()
                // Job is canceled, we should have no more subscribers
                assertEquals(0, awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `observeWhenSubscribedAndUnlocked should observe the given flow depending on the state of the source user and vault unlock flow`() =
        runTest {
            val userStateFlow = MutableStateFlow<UserStateJson?>(null)
            val vaultUnlockFlow = MutableStateFlow<List<VaultUnlockData>>(emptyList())
            val observerStateFlow = MutableStateFlow("")
            val sourceMutableStateFlow = MutableStateFlow(Unit)

            assertEquals(0, observerStateFlow.subscriptionCount.value)
            sourceMutableStateFlow
                .observeWhenSubscribedAndUnlocked(
                    userStateFlow = userStateFlow,
                    vaultUnlockFlow = vaultUnlockFlow,
                    observer = { observerStateFlow },
                )
                .launchIn(backgroundScope)

            observerStateFlow.subscriptionCount.test {
                // No subscriber to start
                assertEquals(0, awaitItem())

                userStateFlow.value = mockk<UserStateJson> {
                    every { activeUserId } returns "user_id_1234"
                }
                // Still none, since no one has subscribed to the testMutableStateFlow
                expectNoEvents()

                vaultUnlockFlow.value = listOf(
                    VaultUnlockData(
                        userId = "user_id_1234",
                        status = VaultUnlockData.Status.UNLOCKED,
                    ),
                )

                // Still none, since no one has subscribed to the testMutableStateFlow
                expectNoEvents()

                val job = sourceMutableStateFlow.launchIn(backgroundScope)
                // Now we subscribe to the observer flow since have a active user and a listener
                assertEquals(1, awaitItem())

                userStateFlow.value = mockk<UserStateJson> {
                    every { activeUserId } returns "user_id_4321"
                }
                // The user changed, so we clear the previous observer but then resubscribe
                // with the new user ID
                assertEquals(0, awaitItem())
                assertEquals(1, awaitItem())

                vaultUnlockFlow.value = listOf(
                    VaultUnlockData(
                        userId = "user_id_4321",
                        status = VaultUnlockData.Status.UNLOCKED,
                    ),
                )

                // The VaultUnlockData changed, so we clear the previous observer but then resubscribe
                // with the new data
                assertEquals(0, awaitItem())
                assertEquals(1, awaitItem())

                job.cancel()
                // Job is canceled, we should have no more subscribers
                assertEquals(0, awaitItem())
            }
        }
}
