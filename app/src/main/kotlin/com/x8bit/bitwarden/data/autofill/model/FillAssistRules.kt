package com.x8bit.bitwarden.data.autofill.model

import kotlinx.serialization.Serializable

/**
 * Parsed, storage-ready representation of fill-assist targeting rules for all known hosts.
 *
 * @property hostRules Map of hostname (with optional port) to a list of [HostRule] entries.
 * Multiple [HostRule] entries per host are possible when different pages define different forms.
 */
@Serializable
data class FillAssistRules(
    val hostRules: Map<String, List<HostRule>>,
) {
    /**
     * Describes one parsed form for a host. Combines host-level and pathname-level forms into a
     * single pooled representation so the consumer does not need to know the current URL path.
     *
     * @property category The form's purpose category (e.g. "account-login", "payment-card").
     * @property fields Map of field key (e.g. "username", "password") to a list of
     * [SelectorClause] alternatives. The first clause that matches a view node is used.
     */
    @Serializable
    data class HostRule(
        val category: String,
        val fields: Map<String, List<SelectorClause>>,
    )

    /**
     * A single parsed CSS selector expressing HTML attribute constraints for matching a view node
     * via [android.view.ViewStructure.HtmlInfo]. All non-null fields are AND constraints.
     *
     * @property tag The HTML tag name (e.g. "input", "select").
     * @property id The value of the element's [id] attribute.
     * @property name The value of the element's [name] attribute.
     * @property type The value of the element's [type] attribute (e.g. "password", "text").
     * @property role The value of the element's [role] attribute.
     */
    @Serializable
    data class SelectorClause(
        val tag: String?,
        val id: String?,
        val name: String?,
        val type: String?,
        val role: String?,
    )
}
