package com.bitwarden.authenticator.data.platform.repository

import app.cash.turbine.test
import com.bitwarden.authenticator.data.platform.datasource.disk.FeatureFlagOverrideDiskSource
import com.bitwarden.authenticator.data.platform.datasource.disk.model.ServerConfig
import com.bitwarden.authenticator.data.platform.datasource.network.model.ConfigResponseJson
import com.bitwarden.authenticator.data.platform.manager.model.FlagKey
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DebugMenuRepositoryTest {
    private val mockFeatureFlagOverrideDiskSource =
        mockk<FeatureFlagOverrideDiskSource> {
            every { getFeatureFlag(FlagKey.DummyBoolean) } returns true
            every { getFeatureFlag(FlagKey.DummyString) } returns TEST_STRING_VALUE
            every { getFeatureFlag(FlagKey.DummyInt()) } returns TEST_INT_VALUE
            every { saveFeatureFlag(any(), any()) } just io.mockk.runs
        }
    private val mutableServerConfigStateFlow =
        MutableStateFlow<ServerConfig?>(
            null,
        )
    private val mockServerConfigRepository =
        mockk<ServerConfigRepository> {
            every { serverConfigStateFlow } returns mutableServerConfigStateFlow
        }

    private val debugMenuRepository =
        DebugMenuRepositoryImpl(
            featureFlagOverrideDiskSource = mockFeatureFlagOverrideDiskSource,
            serverConfigRepository = mockServerConfigRepository,
        )

    @Test
    fun `updateFeatureFlag should save the feature flag to disk`() {
        debugMenuRepository.updateFeatureFlag(
            FlagKey.DummyBoolean,
            true,
        )
        verify(exactly = 1) {
            mockFeatureFlagOverrideDiskSource.saveFeatureFlag(
                FlagKey.DummyBoolean,
                true,
            )
        }
    }

    @Test
    fun `updateFeatureFlag should cause the feature flag overrides updated flow to emit`() =
        runTest {
            debugMenuRepository.updateFeatureFlag(
                FlagKey.DummyBoolean,
                true,
            )
            debugMenuRepository.featureFlagOverridesUpdatedFlow.test {
                awaitItem() // initial value on subscription
                awaitItem()
                cancel()
            }
        }

    @Test
    fun `getFeatureFlag should return the feature flag boolean value from disk`() {
        Assertions.assertTrue(debugMenuRepository.getFeatureFlag(FlagKey.DummyBoolean)!!)
    }

    @Test
    fun `getFeatureFlag should return the feature flag string value from disk`() {
        Assertions.assertEquals(
            TEST_STRING_VALUE,
            debugMenuRepository.getFeatureFlag(FlagKey.DummyString)!!,
        )
    }

    @Test
    fun `getFeatureFlag should return the feature flag int value from disk`() {
        Assertions.assertEquals(
            TEST_INT_VALUE,
            debugMenuRepository.getFeatureFlag(FlagKey.DummyInt())!!,
        )
    }

    @Test
    fun `getFeatureFlag should return null if the feature flag does not exist in disk`() {
        every { mockFeatureFlagOverrideDiskSource.getFeatureFlag<Boolean>(any()) } returns null
        Assertions.assertNull(debugMenuRepository.getFeatureFlag(FlagKey.DummyBoolean))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `resetFeatureFlagOverrides should reset flags to default values if they don't exist in server config`() =
        runTest {
            debugMenuRepository.resetFeatureFlagOverrides()
            verify(exactly = 1) {
                mockFeatureFlagOverrideDiskSource.saveFeatureFlag(
                    FlagKey.PasswordManagerSync,
                    FlagKey.PasswordManagerSync.defaultValue,
                )
                mockFeatureFlagOverrideDiskSource.saveFeatureFlag(
                    FlagKey.BitwardenAuthenticationEnabled,
                    FlagKey.BitwardenAuthenticationEnabled.defaultValue,
                )
            }
            debugMenuRepository.featureFlagOverridesUpdatedFlow.test {
                awaitItem() // initial value on subscription
                awaitItem()
                expectNoEvents()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `resetFeatureFlagOverrides should save all feature flags to values from the server config if remote configured is on`() =
        runTest {
            val mockServerData =
                mockk<ConfigResponseJson>(
                    relaxed = true,
                ) {
                    every { featureStates } returns mapOf(
                        FlagKey.PasswordManagerSync.keyName to JsonPrimitive(
                            true,
                        ),
                        FlagKey.BitwardenAuthenticationEnabled.keyName to JsonPrimitive(
                            false,
                        ),
                    )
                }
            val mockServerConfig =
                mockk<ServerConfig>(
                    relaxed = true,
                ) {
                    every { serverData } returns mockServerData
                }
            mutableServerConfigStateFlow.value = mockServerConfig

            debugMenuRepository.resetFeatureFlagOverrides()

            Assertions.assertTrue(FlagKey.PasswordManagerSync.isRemotelyConfigured)
            Assertions.assertFalse(FlagKey.BitwardenAuthenticationEnabled.isRemotelyConfigured)
            verify(exactly = 1) {
                mockFeatureFlagOverrideDiskSource.saveFeatureFlag(
                    FlagKey.PasswordManagerSync,
                    true,
                )
                mockFeatureFlagOverrideDiskSource.saveFeatureFlag(
                    FlagKey.BitwardenAuthenticationEnabled,
                    false,
                )
            }

            debugMenuRepository.featureFlagOverridesUpdatedFlow.test {
                awaitItem() // initial value on subscription
                awaitItem()
                cancel()
            }
        }
}

private const val TEST_STRING_VALUE = "test"
private const val TEST_INT_VALUE = 100
