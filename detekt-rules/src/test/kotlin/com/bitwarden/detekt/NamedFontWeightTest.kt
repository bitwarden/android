package com.bitwarden.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NamedFontWeightTest {

    private val rule = NamedFontWeight(Config.empty)

    @Test
    fun `numeric constant should be reported with the named replacement`() {
        val findings = rule.lint("val weight = FontWeight.W600")

        assertEquals(1, findings.size)
        assertEquals(
            "Use FontWeight.SemiBold instead of FontWeight.W600.",
            findings.single().message,
        )
    }

    @Test
    fun `fully qualified numeric constant should be reported`() {
        val findings = rule.lint("val weight = androidx.compose.ui.text.font.FontWeight.W400")

        assertEquals(1, findings.size)
        assertEquals(
            "Use FontWeight.Normal instead of FontWeight.W400.",
            findings.single().message,
        )
    }

    @Test
    fun `numeric constructor should be reported with the named replacement`() {
        val findings = rule.lint("val weight = FontWeight(700)")

        assertEquals(1, findings.size)
        assertEquals("Use FontWeight.Bold instead of FontWeight(700).", findings.single().message)
    }

    @Test
    fun `named constant should not be reported`() {
        assertEquals(0, rule.lint("val weight = FontWeight.SemiBold").size)
    }

    @Test
    fun `weight without a named constant should not be reported`() {
        assertEquals(0, rule.lint("val weight = FontWeight(650)").size)
    }

    @Test
    fun `weight property should not be reported`() {
        assertEquals(0, rule.lint("val value = FontWeight.Bold.weight").size)
    }

    @Test
    fun `numeric constants in a text style should each be reported`() {
        val code = """
            val style = TextStyle(fontWeight = FontWeight.W500)
            val family = FontFamily(Font(1, weight = FontWeight.W900))
        """.trimIndent()

        assertEquals(2, rule.lint(code).size)
    }
}
