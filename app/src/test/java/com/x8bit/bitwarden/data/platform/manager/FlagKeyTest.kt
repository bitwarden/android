package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FlagKeyTest {

    @Test
    fun `AuthenticatorSync default value should be true`() {
        assertTrue(FlagKey.AuthenticatorSync.defaultValue)
    }

    @Test
    fun `EmailVerification default value should be false`() {
        assertFalse(FlagKey.EmailVerification.defaultValue)
    }

    @Test
    fun `OnboardingCarousel default value should be false`() {
        assertFalse(FlagKey.OnboardingCarousel.defaultValue)
    }

    @Test
    fun `OnboardingFlow default value should be false`() {
        assertFalse(FlagKey.OnboardingFlow.defaultValue)
    }

    @Test
    fun `ImportLoginsFlow default value should be false`() {
        assertFalse(FlagKey.ImportLoginsFlow.defaultValue)
    }

    @Test
    fun `SshKeyCipherItems default value should be false`() {
        assertFalse(FlagKey.SshKeyCipherItems.defaultValue)
    }
}
