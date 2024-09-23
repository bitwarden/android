package com.x8bit.bitwarden.data.autofill.accessibility.model

/**
 * Represents the known username fields for a given [uriAuthority].
 */
data class KnownUsernameField(
    val uriAuthority: String,
    val accessOptions: List<AccessOptions>,
) {
    constructor(
        uriAuthority: String,
        accessOption: AccessOptions,
    ) : this(uriAuthority = uriAuthority, accessOptions = listOf(accessOption))
}

/**
 * Represents the view IDs for a given uri path.
 */
data class AccessOptions(
    val matchValue: String,
    val matchingStrategy: MatchingStrategy = MatchingStrategy.ENDS_WITH_CASE_SENSITIVE,
    val usernameViewIds: List<String>,
) {
    constructor(
        matchValue: String,
        matchingStrategy: MatchingStrategy = MatchingStrategy.ENDS_WITH_CASE_SENSITIVE,
        usernameViewId: String,
    ) : this(
        matchValue = matchValue,
        matchingStrategy = matchingStrategy,
        usernameViewIds = listOf(usernameViewId),
    )

    /**
     * Indicates the matching strategy needed for the particular [AccessOptions].
     */
    enum class MatchingStrategy(
        val matches: (uriPath: String, matchValue: String) -> Boolean,
    ) {
        CONTAINS_CASE_INSENSITIVE(
            matches = { uriPath, matchValue ->
                uriPath.contains(other = matchValue, ignoreCase = true)
            },
        ),
        CONTAINS_CASE_SENSITIVE(
            matches = { uriPath, matchValue ->
                uriPath.contains(other = matchValue, ignoreCase = false)
            },
        ),
        ENDS_WITH_CASE_INSENSITIVE(
            matches = { uriPath, matchValue ->
                uriPath.endsWith(suffix = matchValue, ignoreCase = true)
            },
        ),
        ENDS_WITH_CASE_SENSITIVE(
            matches = { uriPath, matchValue ->
                uriPath.endsWith(suffix = matchValue, ignoreCase = false)
            },
        ),
        STARTS_WITH_CASE_INSENSITIVE(
            matches = { uriPath, matchValue ->
                uriPath.startsWith(prefix = matchValue, ignoreCase = true)
            },
        ),
        STARTS_WITH_CASE_SENSITIVE(
            matches = { uriPath, matchValue ->
                uriPath.startsWith(prefix = matchValue, ignoreCase = false)
            },
        ),
    }
}
