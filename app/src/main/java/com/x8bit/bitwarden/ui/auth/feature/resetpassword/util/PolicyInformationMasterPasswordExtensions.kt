package com.x8bit.bitwarden.ui.auth.feature.resetpassword.util

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText

/**
 * Convert a list of master password policies into a list of text instructions
 * for the user about what requirements the password must meet.
 */
fun List<PolicyInformation.MasterPassword>.toDisplayLabels(): List<Text> {
    val list = mutableListOf<Text>()

    mapNotNull { it.minLength }.maxOrNull()?.let {
        list.add(R.string.policy_in_effect_min_length.asText(it))
    }

    mapNotNull { it.minComplexity }.maxOrNull()?.let {
        list.add(R.string.policy_in_effect_min_complexity.asText(it))
    }

    if (mapNotNull { it.requireUpper }.any { it }) {
        list.add(R.string.policy_in_effect_uppercase.asText())
    }

    if (mapNotNull { it.requireLower }.any { it }) {
        list.add(R.string.policy_in_effect_lowercase.asText())
    }

    if (mapNotNull { it.requireNumbers }.any { it }) {
        list.add(R.string.policy_in_effect_numbers.asText())
    }

    if (mapNotNull { it.requireSpecial }.any { it }) {
        list.add(R.string.policy_in_effect_special.asText())
    }

    return list
}
