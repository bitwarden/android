package com.x8bit.bitwarden.ui.tools.feature.generator.util

import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation

/**
 * Creates the strictest set of rules based on contents of list of
 * [PolicyInformation.MasterPassword].
 */
fun List<PolicyInformation.MasterPassword>.toStrictestPolicy() = PolicyInformation.MasterPassword(
    minLength = mapNotNull { it.minLength }.maxOrNull(),
    minComplexity = mapNotNull { it.minComplexity }.maxOrNull(),
    requireUpper = mapNotNull { it.requireUpper }.any { it },
    requireLower = mapNotNull { it.requireLower }.any { it },
    requireNumbers = mapNotNull { it.requireNumbers }.any { it },
    requireSpecial = mapNotNull { it.requireSpecial }.any { it },
    enforceOnLogin = mapNotNull { it.enforceOnLogin }.any { it },
)
