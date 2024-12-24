package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FlagKeyTest {

    @Test
    fun `AuthenticatorSync default value should be false`() {
        assertFalse(FlagKey.AuthenticatorSync.defaultValue)
    }

    @Test
    fun `AuthenticatorSync is remotely configured value should be true`() {
        assertTrue(FlagKey.AuthenticatorSync.isRemotelyConfigured)
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

    @Test
    fun `AppReviewPrompt default value should be false`() {
        assertFalse(FlagKey.AppReviewPrompt.defaultValue)
    }

    @Test
    fun `NewDevicePermanentDismiss default value should be false`() {
        assertFalse(FlagKey.NewDevicePermanentDismiss.defaultValue)
    }

    @Test
    fun `NewDevicePermanentDismiss is remotely configured value should be true`() {
        assertTrue(FlagKey.NewDevicePermanentDismiss.isRemotelyConfigured)
    }

    @Test
    fun `NewDeviceTemporaryDismiss default value should be false`() {
        assertFalse(FlagKey.NewDeviceTemporaryDismiss.defaultValue)
    }

    @Test
    fun `NewDeviceTemporaryDismiss is remotely configured value should be true`() {
        assertTrue(FlagKey.NewDeviceTemporaryDismiss.isRemotelyConfigured)
    }
}
