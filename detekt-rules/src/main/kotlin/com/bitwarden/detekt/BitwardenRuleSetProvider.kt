package com.bitwarden.detekt

import com.bitwarden.detekt.rules.NoSharingInPropertyGetter
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

/**
 * Registers Bitwarden's custom detekt rules.
 *
 * To add a rule, create it in the `rules` package, add it to the list below, and add an entry for
 * it under the `bitwarden` rule set in `detekt-config.yml`.
 */
class BitwardenRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = RULE_SET_ID

    override fun instance(config: Config): RuleSet = RuleSet(
        id = ruleSetId,
        rules = listOf(
            NoSharingInPropertyGetter(config),
        ),
    )

    private companion object {
        const val RULE_SET_ID = "bitwarden"
    }
}
