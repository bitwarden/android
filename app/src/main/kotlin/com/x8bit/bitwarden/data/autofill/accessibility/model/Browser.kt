package com.x8bit.bitwarden.data.autofill.accessibility.model

/**
 * A model representing a supported browser.
 */
data class Browser(
    val packageName: String,
    val possibleUrlFieldIds: List<String>,
    val urlExtractor: (String) -> String? = { it },
) {
    constructor(
        packageName: String,
        urlFieldId: String,
        urlExtractor: (String) -> String? = { it },
    ) : this(
        packageName = packageName,
        possibleUrlFieldIds = listOf(urlFieldId),
        urlExtractor = urlExtractor,
    )
}
