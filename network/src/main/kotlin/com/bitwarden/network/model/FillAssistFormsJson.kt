package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Represents the fill-assist forms rules file.
 *
 * @property schemaVersion The semantic version string for this file (e.g. "1.0.0").
 * @property hosts Map of hostname (optionally with port) to [HostEntryJson], or null if the host
 * is explicitly excluded from fill-assist.
 */
@Serializable
data class FillAssistFormsJson(
    @SerialName("schemaVersion")
    val schemaVersion: String,

    @SerialName("hosts")
    val hosts: Map<String, HostEntryJson?>,
) {
    /**
     * Form descriptions and pathname-specific overrides for a single host.
     *
     * @property forms Site-wide fallback form descriptions.
     * @property pathnames Pathname-specific overrides; a null value means that path is excluded.
     */
    @Serializable
    data class HostEntryJson(
        @SerialName("forms")
        val forms: List<FormJson>?,

        @SerialName("pathnames")
        val pathnames: Map<String, PathnameEntryJson?>?,
    )

    /**
     * Form descriptions for a specific pathname.
     *
     * @property forms The form descriptions for this path.
     */
    @Serializable
    data class PathnameEntryJson(
        @SerialName("forms")
        val forms: List<FormJson>,
    )

    /**
     * Describes one logical form on a page.
     *
     * @property category The categorical purpose of this form (e.g. "account-login").
     * @property container Optional CSS selectors identifying the form's container element.
     * @property fields Map of field key to [JsonElement] representing a compositeSelectorArray.
     * Each array element is either a CSS selector string or an array of strings for composite
     * multi-input fields. Unknown fields are gracefully ignored via [ignoreUnknownKeys].
     */
    @Serializable
    data class FormJson(
        @SerialName("category")
        val category: String,

        @SerialName("container")
        val container: List<String>?,

        @SerialName("fields")
        val fields: Map<String, JsonElement>,
    )
}
