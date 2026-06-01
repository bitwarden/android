package com.x8bit.bitwarden.ui.tools.feature.generator.util

import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PolicyInformationPasswordGeneratorExtensionsTest {
    @Test
    fun `toStrictestPolicy should select the strictest version of each rule`() {
        assertEquals(
            PolicyInformation.PasswordGenerator(
                overridePasswordType = "password",
                minLength = 4,
                capitalize = true,
                includeNumber = true,
                minNumberWords = 2,
                minNumbers = 9,
                minSpecial = 3,
                useLower = true,
                useNumbers = false,
                useSpecial = true,
                useUpper = true,
            ),
            listOf(POLICY_1, POLICY_2, POLICY_3).toStrictestPolicy(),
        )
    }

    @Test
    fun `toStrictestPolicy should select the strictest version of each rule and passphrase`() {
        assertEquals(
            PolicyInformation.PasswordGenerator(
                overridePasswordType = "passphrase",
                minLength = 4,
                capitalize = true,
                includeNumber = false,
                minNumberWords = 2,
                minNumbers = 9,
                minSpecial = 3,
                useLower = true,
                useNumbers = false,
                useSpecial = true,
                useUpper = true,
            ),
            listOf(POLICY_2, POLICY_3).toStrictestPolicy(),
        )
    }

    @Test
    fun `toStrictestPolicy returns default settings when no policies are present`() {
        assertEquals(
            PolicyInformation.PasswordGenerator(
                overridePasswordType = null,
                minLength = null,
                capitalize = false,
                includeNumber = false,
                minNumberWords = null,
                minNumbers = null,
                minSpecial = null,
                useLower = false,
                useNumbers = false,
                useSpecial = false,
                useUpper = false,
            ),
            listOf<PolicyInformation.PasswordGenerator>().toStrictestPolicy(),
        )
    }
}

private val POLICY_1 = PolicyInformation.PasswordGenerator(
    overridePasswordType = "password",
    minLength = 0,
    capitalize = false,
    includeNumber = true,
    minNumberWords = 2,
    minNumbers = 3,
    minSpecial = null,
    useLower = false,
    useNumbers = false,
    useSpecial = true,
    useUpper = false,
)

private val POLICY_2 = PolicyInformation.PasswordGenerator(
    overridePasswordType = "passphrase",
    minLength = 0,
    capitalize = false,
    includeNumber = false,
    minNumberWords = 0,
    minNumbers = 0,
    minSpecial = 0,
    useLower = false,
    useNumbers = false,
    useSpecial = false,
    useUpper = false,
)

private val POLICY_3 = PolicyInformation.PasswordGenerator(
    overridePasswordType = null,
    minLength = 4,
    capitalize = true,
    includeNumber = false,
    minNumberWords = 2,
    minNumbers = 9,
    minSpecial = 3,
    useLower = true,
    useNumbers = false,
    useSpecial = true,
    useUpper = true,
)
