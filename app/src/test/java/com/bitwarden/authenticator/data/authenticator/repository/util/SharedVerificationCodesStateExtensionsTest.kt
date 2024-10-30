package com.bitwarden.authenticator.data.authenticator.repository.util

import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SharedVerificationCodesStateExtensionsTest {

    @Test
    @Suppress("MaxLineLength")
    fun `isSyncWithBitwardenEnabled should return true only when SharedVerificationCodesState is Success `() {
        assertFalse(SharedVerificationCodesState.AppNotInstalled.isSyncWithBitwardenEnabled)
        assertFalse(SharedVerificationCodesState.Error.isSyncWithBitwardenEnabled)
        assertFalse(SharedVerificationCodesState.FeatureNotEnabled.isSyncWithBitwardenEnabled)
        assertFalse(SharedVerificationCodesState.Loading.isSyncWithBitwardenEnabled)
        assertFalse(SharedVerificationCodesState.OsVersionNotSupported.isSyncWithBitwardenEnabled)
        assertFalse(SharedVerificationCodesState.SyncNotEnabled.isSyncWithBitwardenEnabled)
        assertTrue(SharedVerificationCodesState.Success(emptyList()).isSyncWithBitwardenEnabled)
    }
}
