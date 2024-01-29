package com.x8bit.bitwarden.data.platform.util

import android.content.Context
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.model.DomainName
import java.net.URI

/**
 * A regular expression that matches IP addresses.
 */
private const val IP_REGEX: String =
    "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
        "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
        "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
        "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"

/**
 * Parses the base domain from the URL. Returns null if unavailable.
 */
fun URI.parseDomainOrNull(context: Context): String? {
    val host = this?.host ?: return null
    val isIpAddress = host.matches(IP_REGEX.toRegex())

    return if (host == "localhost" || isIpAddress) {
        host
    } else {
        parseDomainNameOrNullInternal(
            context = context,
            host = host,
        )
            ?.domain
    }
}

/**
 *  Parses a URL to get the breakdown of a URL's domain. Returns null if invalid.
 */
fun URI.parseDomainNameOrNull(context: Context): DomainName? =
    this
        // URI is a platform type and host can be null.
        ?.host
        ?.let { nonNullHost ->
            parseDomainNameOrNullInternal(
                context = context,
                host = nonNullHost,
            )
        }

/**
 * The internal implementation of [parseDomainNameOrNull]. This doesn't extend URI and has a
 * non-null [host] parameter. Technically, URI.host could be null and we want to avoid issues with
 * that.
 */
@Suppress("LongMethod")
private fun parseDomainNameOrNullInternal(
    context: Context,
    host: String,
): DomainName? {
    val exceptionSuffixes = context
        .resources
        .getStringArray(R.array.exception_suffixes)
        .toList()
    val normalSuffixes = context
        .resources
        .getStringArray(R.array.normal_suffixes)
        .toList()
    val wildCardSuffixes = context
        .resources
        .getStringArray(R.array.wild_card_suffixes)
        .toList()

    // Split the host into parts separated by a period. Start with the last part and incrementally
    // add back the earlier parts to build a list of any matching domains in the data set.
    val hostParts = host
        .split(".")
        .reversed()
    var partialDomain = ""
    val ruleMatches: MutableList<SuffixMatchType> = mutableListOf()

    // Check to see if this part of the host belongs to any of the suffix lists.
    hostParts
        .forEach { hostPart ->
            partialDomain = if (partialDomain.isBlank()) {
                hostPart
            } else {
                "$hostPart.$partialDomain"
            }

            when {
                // Normal suffixes first.
                normalSuffixes.contains(partialDomain) -> {
                    ruleMatches.add(
                        SuffixMatchType.Normal(
                            partialDomain = partialDomain,
                        ),
                    )
                }

                // Then wild cards.
                wildCardSuffixes.contains(partialDomain) -> {
                    ruleMatches.add(
                        SuffixMatchType.WildCard(
                            partialDomain = partialDomain,
                        ),
                    )
                }

                // And finally, exceptions.
                exceptionSuffixes.contains(partialDomain) -> {
                    ruleMatches.add(
                        SuffixMatchType.Exception(
                            partialDomain = partialDomain,
                        ),
                    )
                }
            }
        }

    // Take only the largest public suffix match that occurs within our URI's host. We want the
    // largest because if the URI was "airbnb.co.uk" we our list would contain "uk" and "co.uk",
    // which are both valid top level domains. In this case, "uk" is just simply not the top level
    // domain.
    val largestMatch = ruleMatches.maxByOrNull {
        it
            .partialDomain
            .split('.')
            .size
    }

    // Determine the position of the top level domain within the host.
    val tldRange: IntRange? = when (largestMatch) {
        is SuffixMatchType.Exception,
        is SuffixMatchType.Normal,
        -> {
            host.findLastSubstringIndicesOrNull(largestMatch.partialDomain)
        }

        is SuffixMatchType.WildCard -> {
            // This gets the last portion of the top level domain.
            val nonWildcardTldIndex = host.lastIndexOf(".${largestMatch.partialDomain}")

            if (nonWildcardTldIndex != -1) {
                val nonWildcardTld = host.substring(0, nonWildcardTldIndex)

                // But we need to also match the wildcard portion.
                val dotIndex = nonWildcardTld.lastIndexOf(".")

                if (dotIndex != -1) {
                    IntRange(dotIndex + 1, nonWildcardTldIndex - 1)
                } else {
                    null
                }
            } else {
                null
            }
        }

        null -> null
    }

    return tldRange
        ?.first
        ?.let { firstIndex ->
            val topLevelDomain = host.substring(firstIndex)

            // Parse the remaining parts prior to the TLD.
            // - If there's 0 parts left, there is just a TLD and no domain or subdomain.
            // - If there's 1 part, it's the domain, and there is no subdomain.
            // - If there's 2+ parts, the last part is the domain, the other parts (combined) are
            //   the subdomain.
            val possibleSubDomainAndDomain = if (firstIndex > 0) {
                host.substring(0, firstIndex - 1)
            } else {
                null
            }
            val subDomainAndDomainParts = possibleSubDomainAndDomain?.split(".")
            val secondLevelDomain = subDomainAndDomainParts?.lastOrNull()
            val subDomain = subDomainAndDomainParts
                ?.dropLast(1)
                ?.joinToString(separator = ".")
                // joinToString leaves white space if called on an empty list.
                // So only take the string if it wasn't empty after the dropLast(1).
                .takeIf { (subDomainAndDomainParts?.size ?: 0) > 1 }

            DomainName(
                secondLevelDomain = secondLevelDomain,
                topLevelDomain = topLevelDomain,
                subDomain = subDomain,
            )
        }
}

/**
 * A type of domain suffix match.
 */
private sealed class SuffixMatchType {

    /**
     * The partial domain that was actually matched.
     */
    abstract val partialDomain: String

    /**
     * The match occurred with an exception suffix that starts with '!'.
     */
    data class Exception(
        override val partialDomain: String,
    ) : SuffixMatchType()

    /**
     * The match occurred with a normal suffix.
     */
    data class Normal(
        override val partialDomain: String,
    ) : SuffixMatchType()

    /**
     * The match occurred with a wildcard suffix that starts with '*'.
     */
    data class WildCard(
        override val partialDomain: String,
    ) : SuffixMatchType()
}
