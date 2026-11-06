package com.bitwarden.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression

/**
 * Reports numeric `FontWeight` values, such as `FontWeight.W600` or `FontWeight(600)`, and
 * suggests the named constant used in the Figma designs, such as `FontWeight.SemiBold`.
 */
class NamedFontWeight(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.Style,
        description = "Use named FontWeight constants to match the Figma designs.",
        debt = Debt.FIVE_MINS,
    )

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        if (!expression.receiverExpression.text.isFontWeight()) return
        val selector = expression.selectorExpression?.text ?: return
        if (!selector.startsWith(prefix = "W")) return
        val weight = selector.removePrefix(prefix = "W").toIntOrNull() ?: return
        reportIfNamed(expression = expression, current = "FontWeight.$selector", weight = weight)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (expression.calleeExpression?.text?.isFontWeight() != true) return
        val argument = expression.valueArguments.singleOrNull()?.getArgumentExpression()
        val weight = (argument as? KtConstantExpression)?.text?.toIntOrNull() ?: return
        reportIfNamed(expression = expression, current = "FontWeight($weight)", weight = weight)
    }

    private fun reportIfNamed(expression: KtExpression, current: String, weight: Int) {
        val name = NAMED_WEIGHTS[weight] ?: return
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = "Use FontWeight.$name instead of $current.",
            ),
        )
    }

    private fun String.isFontWeight(): Boolean =
        this == FONT_WEIGHT || endsWith(suffix = ".$FONT_WEIGHT")

    private companion object {
        const val FONT_WEIGHT: String = "FontWeight"

        @Suppress("MagicNumber")
        val NAMED_WEIGHTS: Map<Int, String> = mapOf(
            100 to "Thin",
            200 to "ExtraLight",
            300 to "Light",
            400 to "Normal",
            500 to "Medium",
            600 to "SemiBold",
            700 to "Bold",
            800 to "ExtraBold",
            900 to "Black",
        )
    }
}
