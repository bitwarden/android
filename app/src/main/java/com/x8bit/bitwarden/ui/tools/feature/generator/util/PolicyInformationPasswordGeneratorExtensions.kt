package com.x8bit.bitwarden.ui.tools.feature.generator.util

import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation

/**
 * Creates the strictest set of rules based on the contents of the list of
 * [PolicyInformation.PasswordGenerator].
 */
fun List<PolicyInformation.PasswordGenerator>.toStrictestPolicy():
    PolicyInformation.PasswordGenerator {
    return PolicyInformation.PasswordGenerator(
        capitalize = mapNotNull { it.capitalize }.any { it },
        defaultType = firstNotNullOfOrNull { it.defaultType },
        includeNumber = mapNotNull { it.includeNumber }.any { it },
        minLength = mapNotNull { it.minLength }.maxOrNull(),
        minNumberWords = mapNotNull { it.minNumberWords }.maxOrNull(),
        minNumbers = mapNotNull { it.minNumbers }.maxOrNull(),
        minSpecial = mapNotNull { it.minSpecial }.maxOrNull(),
        useLower = mapNotNull { it.useLower }.any { it },
        useNumbers = mapNotNull { it.useNumbers }.any { it },
        useSpecial = mapNotNull { it.useSpecial }.any { it },
        useUpper = mapNotNull { it.useUpper }.any { it },
    )
}
