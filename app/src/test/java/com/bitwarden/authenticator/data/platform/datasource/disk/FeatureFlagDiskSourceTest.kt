package com.bitwarden.authenticator.data.platform.datasource.disk

import androidx.core.content.edit
import app.cash.turbine.test
import com.bitwarden.authenticator.data.platform.base.FakeSharedPreferences
import com.bitwarden.authenticator.data.platform.datasource.disk.model.FeatureFlagsConfiguration
import com.bitwarden.authenticator.data.platform.datasource.network.di.PlatformNetworkModule
import com.bitwarden.authenticator.data.platform.manager.model.LocalFeatureFlag
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FeatureFlagDiskSourceTest {
    private val json = PlatformNetworkModule.providesJson()

    private val fakeSharedPreferences = FakeSharedPreferences()

    private val featureFlagDiskSource = FeatureFlagDiskSourceImpl(
        sharedPreferences = fakeSharedPreferences,
        json = json,
    )

    @Test
    fun `featureFlagsConfiguration should pull from and update SharedPreferences`() {
        val featureFlagsConfigKey = "bwPreferencesStorage:featureFlags"

        assertNull(featureFlagDiskSource.featureFlagsConfiguration)
        assertNull(fakeSharedPreferences.getString(featureFlagsConfigKey, null))

        featureFlagDiskSource.featureFlagsConfiguration = FEATURE_FLAGS_CONFIGURATION
        assertEquals(
            json.parseToJsonElement(
                FEATURE_FLAGS_CONFIGURATION_JSON
            ),
            json.parseToJsonElement(
                fakeSharedPreferences.getString(featureFlagsConfigKey, null)!!
            ),
        )

        fakeSharedPreferences.edit { putString(featureFlagsConfigKey, null) }
        assertNull(featureFlagDiskSource.featureFlagsConfiguration)
    }

    @Test
    fun `featureFlagsConfigFlow should react to changes in featureFlagsConfig`() = runTest {
        featureFlagDiskSource.featureFlagsConfigurationFlow.test {
            assertNull(featureFlagDiskSource.featureFlagsConfiguration)
            assertNull(awaitItem())

            featureFlagDiskSource.featureFlagsConfiguration = FEATURE_FLAGS_CONFIGURATION
            assertEquals(FEATURE_FLAGS_CONFIGURATION, awaitItem())
        }
    }

}

private const val FEATURE_FLAGS_CONFIGURATION_JSON = """
{
    "featureFlags" : {
        "bitwarden-authentication-enabled" : true    
    }
}

"""

private val FEATURE_FLAGS_CONFIGURATION = FeatureFlagsConfiguration(
    featureFlags = mapOf(
        LocalFeatureFlag.BitwardenAuthenticationEnabled.name to JsonPrimitive(true)
    )
)
