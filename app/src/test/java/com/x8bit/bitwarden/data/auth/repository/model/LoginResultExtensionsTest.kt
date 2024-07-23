package com.x8bit.bitwarden.data.auth.repository.model

import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LoginResultExtensionsTest {

    @Test
    fun `VaultUnlockResult with error message maps to LoginResult Error with correct message`() {
        val errorMessage = "foo"
        val result = VaultUnlockResult.AuthenticationError(errorMessage).toLoginErrorResult()
        assertEquals(LoginResult.Error(errorMessage), result)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `VaultUnlockResult with null error message as default maps to LoginResult Error with null message`() {
        val result = VaultUnlockResult.AuthenticationError().toLoginErrorResult()
        assertEquals(LoginResult.Error(errorMessage = null), result)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `VaultUnlockResult with no message value are mapped to LoginResult with null error message`() {
        val invalidStateResult = VaultUnlockResult.InvalidStateError.toLoginErrorResult()
        val genericErrorResult = VaultUnlockResult.GenericError.toLoginErrorResult()
        val expectedResult = LoginResult.Error(errorMessage = null)

        assertEquals(expectedResult, invalidStateResult)
        assertEquals(expectedResult, genericErrorResult)
    }
}
