package com.x8bit.bitwarden.data.auth.repository.util

import com.bitwarden.network.model.MasterPasswordPolicyOptionsJson
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MasterPasswordPolicyOptionsJsonExtensionsTest {
    @Test
    fun `toPolicyInformation should map every field to its policy counterpart`() {
        val json = MasterPasswordPolicyOptionsJson(
            minimumComplexity = 3,
            minimumLength = 12,
            shouldRequireUppercase = true,
            shouldRequireLowercase = false,
            shouldRequireNumbers = true,
            shouldRequireSpecialCharacters = false,
            shouldEnforceOnLogin = true,
        )

        assertEquals(
            PolicyInformation.MasterPassword(
                minLength = 12,
                minComplexity = 3,
                requireUpper = true,
                requireLower = false,
                requireNumbers = true,
                requireSpecial = false,
                enforceOnLogin = true,
            ),
            json.toPolicyInformation(),
        )
    }

    @Test
    fun `toPolicyInformation should preserve null fields`() {
        val json = MasterPasswordPolicyOptionsJson(
            minimumComplexity = null,
            minimumLength = null,
            shouldRequireUppercase = null,
            shouldRequireLowercase = null,
            shouldRequireNumbers = null,
            shouldRequireSpecialCharacters = null,
            shouldEnforceOnLogin = null,
        )

        assertEquals(
            PolicyInformation.MasterPassword(
                minLength = null,
                minComplexity = null,
                requireUpper = null,
                requireLower = null,
                requireNumbers = null,
                requireSpecial = null,
                enforceOnLogin = null,
            ),
            json.toPolicyInformation(),
        )
    }
}
