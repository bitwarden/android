package com.bitwarden.cxf.validator

import android.app.Activity
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CredentialExchangeRequestValidatorTest {

    private val mockActivity: Activity = mockk {
        every { callingPackage } returns "mockCallingPackage"
    }
    private val credentialExchangeRequestValidator =
        CredentialExchangeRequestValidatorImpl(mockActivity)

    @Test
    fun `validateRequest should return false when callingPackage is not GMS`() {
        every { mockActivity.callingPackage } returns "otherPackage"
        assertFalse(credentialExchangeRequestValidator.validate(mockk()))
    }

    @Test
    fun `validateRequest should return true when callingPackage is GMS`() {
        every { mockActivity.callingPackage } returns "com.google.android.gms"
        assertTrue(credentialExchangeRequestValidator.validate(mockk()))
    }
}
