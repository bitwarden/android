package com.x8bit.bitwarden.ui.auth.feature.resetPassword.util

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.model.createMockMasterPasswordPolicy
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.util.toDisplayLabels
import com.x8bit.bitwarden.ui.platform.base.util.asText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PolicyInformationMasterPasswordExtensionsTest {
    @Test
    fun `toDisplayLabels with multiple minLength values should choose highest value`() {
        val policyList = listOf(
            createMockMasterPasswordPolicy(minLength = null),
            createMockMasterPasswordPolicy(minLength = 10),
            createMockMasterPasswordPolicy(minLength = 2),
        )
        assertEquals(
            listOf(R.string.policy_in_effect_min_length.asText(10)),
            policyList.toDisplayLabels(),
        )
    }

    @Test
    fun `toDisplayLabels with multiple minComplexity values should choose highest value`() {
        val policyList = listOf(
            createMockMasterPasswordPolicy(minComplexity = null),
            createMockMasterPasswordPolicy(minComplexity = 1),
            createMockMasterPasswordPolicy(minComplexity = 2),
        )
        assertEquals(
            listOf(R.string.policy_in_effect_min_complexity.asText(2)),
            policyList.toDisplayLabels(),
        )
    }

    @Test
    fun `toDisplayLabels lists any nonNull requirements`() {
        val policyList = listOf(
            createMockMasterPasswordPolicy(requireUpper = true),
            createMockMasterPasswordPolicy(requireLower = true),
            createMockMasterPasswordPolicy(requireNumbers = true),
            createMockMasterPasswordPolicy(requireSpecial = true),
        )
        assertEquals(
            listOf(
                R.string.policy_in_effect_uppercase.asText(),
                R.string.policy_in_effect_lowercase.asText(),
                R.string.policy_in_effect_numbers.asText(),
                R.string.policy_in_effect_special.asText(),
            ),
            policyList.toDisplayLabels(),
        )
    }
}
