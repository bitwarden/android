package com.x8bit.bitwarden.ui.tools.feature.generator.util

import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PolicyInformationMasterPasswordExtensionsTest {

    @Test
    fun `toStrictestPolicy enforces the strictest MasterPassword policy`() {
        assertEquals(
            PolicyInformation.MasterPassword(
                minLength = 24,
                minComplexity = 24,
                requireUpper = true,
                requireLower = true,
                requireNumbers = true,
                requireSpecial = true,
                enforceOnLogin = true,
            ),
            listOf(
                POLICY_1,
                POLICY_2,
                POLICY_3,
            )
                .toStrictestPolicy(),
        )
    }
}

private val POLICY_1 = PolicyInformation.MasterPassword(
    minLength = 6,
    minComplexity = 24,
    requireUpper = false,
    requireLower = true,
    requireNumbers = false,
    requireSpecial = false,
    enforceOnLogin = true,
)

private val POLICY_2 = PolicyInformation.MasterPassword(
    minLength = 24,
    minComplexity = 12,
    requireUpper = true,
    requireLower = false,
    requireNumbers = false,
    requireSpecial = true,
    enforceOnLogin = false,
)

private val POLICY_3 = PolicyInformation.MasterPassword(
    minLength = 12,
    minComplexity = 24,
    requireUpper = false,
    requireLower = false,
    requireNumbers = true,
    requireSpecial = false,
    enforceOnLogin = false,
)
