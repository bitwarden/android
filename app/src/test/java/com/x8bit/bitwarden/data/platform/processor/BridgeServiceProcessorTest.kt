package com.x8bit.bitwarden.data.platform.processor

import android.os.Build
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BridgeServiceProcessorTest {

    private val featureFlagManager: FeatureFlagManager = mockk()

    private val bridgeServiceManager = BridgeServiceProcessorImpl(
        featureFlagManager = featureFlagManager,
    )

    @BeforeEach
    fun setup() {
        mockkStatic(::isBuildVersionBelow)
    }

    @Test
    fun `when AuthenticatorSync feature flag is off, should return null binder`() {
        every { isBuildVersionBelow(Build.VERSION_CODES.S) } returns false
        every { featureFlagManager.getFeatureFlag(FlagKey.AuthenticatorSync) } returns false
        assertNull(bridgeServiceManager.binder)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `when AuthenticatorSync feature flag is on and running Android level greater than S, should return non-null binder`() {
        every { isBuildVersionBelow(Build.VERSION_CODES.S) } returns false
        every { featureFlagManager.getFeatureFlag(FlagKey.AuthenticatorSync) } returns true
        assertNotNull(bridgeServiceManager.binder)
    }

    @Test
    fun `when below Android level S, should never return a binder regardless of feature flag`() {
        every { isBuildVersionBelow(Build.VERSION_CODES.S) } returns true
        every { featureFlagManager.getFeatureFlag(FlagKey.AuthenticatorSync) } returns false
        assertNull(bridgeServiceManager.binder)

        every { featureFlagManager.getFeatureFlag(FlagKey.AuthenticatorSync) } returns true
        assertNull(bridgeServiceManager.binder)
    }
}
