package com.bitwarden.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

/**
 * Provides Bitwarden's custom Detekt rules.
 */
class BitwardenRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "bitwarden"

    override fun instance(config: Config): RuleSet =
        RuleSet(
            id = ruleSetId,
            rules = listOf(
                NamedFontWeight(config),
            ),
        )
}
