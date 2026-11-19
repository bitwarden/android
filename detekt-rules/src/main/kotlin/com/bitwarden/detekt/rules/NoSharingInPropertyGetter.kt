package com.bitwarden.detekt.rules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Reports property getters that call `stateIn` or `shareIn`.
 *
 * A getter runs on every access, so each read builds a new shared flow and launches a new
 * collector on its scope. When that scope is long lived the collectors are never cancelled and
 * pile up. Create the flow once as an initialized property (or `by lazy`) instead.
 *
 * Bad:
 * ```kotlin
 * val flow: StateFlow<Int>
 *     get() = source.stateIn(scope, SharingStarted.Eagerly, 0)
 * ```
 *
 * Good:
 * ```kotlin
 * val flow: StateFlow<Int> = source.stateIn(scope, SharingStarted.Eagerly, 0)
 * ```
 */
class NoSharingInPropertyGetter(config: Config = Config.empty) : Rule(config) {

    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.Defect,
        description = "A property getter must not call stateIn or shareIn. Every access creates " +
            "a new shared flow and collector. Create the flow once as an initialized property.",
        debt = Debt.TWENTY_MINS,
    )

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        val getter = property.getter ?: return
        val callsSharingOperator = getter
            .collectDescendantsOfType<KtCallExpression>()
            .any { it.calleeExpression?.text in SHARING_OPERATORS }
        if (callsSharingOperator) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(getter),
                    message = "The getter of '${property.name}' calls stateIn or shareIn, so " +
                        "every access creates a new shared flow. Initialize the property once " +
                        "instead.",
                ),
            )
        }
    }

    private companion object {
        val SHARING_OPERATORS = setOf("stateIn", "shareIn")
    }
}
