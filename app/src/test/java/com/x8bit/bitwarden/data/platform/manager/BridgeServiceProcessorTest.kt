package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BridgeServiceProcessorTest {

    private val featureFlagManager: FeatureFlagManager = mockk()

    private val bridgeServiceManager = BridgeServiceProcessorImpl(
        featureFlagManager = featureFlagManager,
    )

    @Test
    fun `when AuthenticatorSync feature flag is off, should return null binder`() {
        every { featureFlagManager.getFeatureFlag(FlagKey.AuthenticatorSync) } returns false
        assertNull(bridgeServiceManager.binder)
    }

    @Test
    fun `when AuthenticatorSync feature flag is on, should return non-null binder`() {
        every { featureFlagManager.getFeatureFlag(FlagKey.AuthenticatorSync) } returns true
        assertNotNull(bridgeServiceManager.binder)
    }
}
