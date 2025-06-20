package com.x8bit.bitwarden.data.auth.repository.model

import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LoginResultExtensionsTest {

    @Test
    fun `VaultUnlockResult with error message maps to LoginResult Error with correct message`() {
        val errorMessage = "foo"
        val error = Throwable("Fail")
        val result = VaultUnlockResult
            .AuthenticationError(
                message = errorMessage,
                error = error,
            )
            .toLoginErrorResult()
        assertEquals(LoginResult.Error(errorMessage = errorMessage, error = error), result)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `VaultUnlockResult with null error message as default maps to LoginResult Error with null message`() {
        val error = Throwable("Fail")
        val result = VaultUnlockResult.AuthenticationError(error = error).toLoginErrorResult()
        assertEquals(LoginResult.Error(errorMessage = null, error = error), result)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `VaultUnlockResult with no message value are mapped to LoginResult with null error message`() {
        val error = Throwable("Fail")
        val invalidStateResult =
            VaultUnlockResult.InvalidStateError(error = error).toLoginErrorResult()
        val genericErrorResult = VaultUnlockResult.GenericError(error = error).toLoginErrorResult()
        val biometricErrorResult =
            VaultUnlockResult.BiometricDecodingError(error = error).toLoginErrorResult()
        val expectedResult = LoginResult.Error(errorMessage = null, error = error)

        assertEquals(expectedResult, invalidStateResult)
        assertEquals(expectedResult, genericErrorResult)
        assertEquals(expectedResult, biometricErrorResult)
    }
}
