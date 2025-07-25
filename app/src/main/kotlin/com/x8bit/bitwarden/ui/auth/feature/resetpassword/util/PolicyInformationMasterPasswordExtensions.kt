package com.x8bit.bitwarden.ui.auth.feature.resetpassword.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation

/**
 * Convert a list of master password policies into a list of text instructions
 * for the user about what requirements the password must meet.
 */
fun List<PolicyInformation.MasterPassword>.toDisplayLabels(): List<Text> {
    val list = mutableListOf<Text>()

    mapNotNull { it.minLength }.maxOrNull()?.let {
        list.add(BitwardenString.policy_in_effect_min_length.asText(it))
    }

    mapNotNull { it.minComplexity }.maxOrNull()?.let {
        list.add(BitwardenString.policy_in_effect_min_complexity.asText(it))
    }

    if (mapNotNull { it.requireUpper }.any { it }) {
        list.add(BitwardenString.policy_in_effect_uppercase.asText())
    }

    if (mapNotNull { it.requireLower }.any { it }) {
        list.add(BitwardenString.policy_in_effect_lowercase.asText())
    }

    if (mapNotNull { it.requireNumbers }.any { it }) {
        list.add(BitwardenString.policy_in_effect_numbers.asText())
    }

    if (mapNotNull { it.requireSpecial }.any { it }) {
        list.add(BitwardenString.policy_in_effect_special.asText())
    }

    return list
}
