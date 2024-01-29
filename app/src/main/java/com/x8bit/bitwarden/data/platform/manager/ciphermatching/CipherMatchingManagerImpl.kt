package com.x8bit.bitwarden.data.platform.manager.ciphermatching

import android.content.Context
import com.bitwarden.core.CipherView
import com.bitwarden.core.LoginUriView
import com.bitwarden.core.UriMatchType
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.util.getDomainOrNull
import com.x8bit.bitwarden.data.platform.util.getHostWithPortOrNull
import com.x8bit.bitwarden.data.platform.util.getWebHostFromAndroidUriOrNull
import com.x8bit.bitwarden.data.platform.util.isAndroidApp
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DomainsData
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.util.toSdkUriMatchType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlin.text.Regex
import kotlin.text.RegexOption
import kotlin.text.isNullOrBlank
import kotlin.text.lowercase
import kotlin.text.matches
import kotlin.text.startsWith

/**
 * The default [CipherMatchingManager] implementation. This class is responsible for matching
 * ciphers based on special criteria.
 */
class CipherMatchingManagerImpl(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val vaultRepository: VaultRepository,
) : CipherMatchingManager {
    override suspend fun filterCiphersForMatches(
        ciphers: List<CipherView>,
        matchUri: String,
    ): List<CipherView> {
        val equivalentDomainsData = vaultRepository
            .domainsStateFlow
            .mapNotNull { it.data }
            .first()

        val isAndroidApp = matchUri.isAndroidApp()
        val defaultUriMatchType = settingsRepository.defaultUriMatchType.toSdkUriMatchType()
        val domain = matchUri
            .getDomainOrNull(context = context)
            ?.lowercase()

        // Retrieve domains that are considered equivalent to the specified matchUri for cipher
        // comparison. If a cipher doesn't have a URI matching the matchUri, but matches a domain in
        // matchingDomains, it's considered a match.
        val matchingDomains = getMatchingDomains(
            domainsData = equivalentDomainsData,
            isAndroidApp = isAndroidApp,
            matchDomain = domain,
            matchUri = matchUri,
        )

        val exactMatchingCiphers = mutableListOf<CipherView>()
        val fuzzyMatchingCiphers = mutableListOf<CipherView>()

        ciphers
            .forEach { cipherView ->
                val matchResult = checkForCipherMatch(
                    cipherView = cipherView,
                    context = context,
                    defaultUriMatchType = defaultUriMatchType,
                    isAndroidApp = isAndroidApp,
                    matchUri = matchUri,
                    matchingDomains = matchingDomains,
                )

                when (matchResult) {
                    MatchResult.EXACT -> exactMatchingCiphers.add(cipherView)
                    MatchResult.FUZZY -> fuzzyMatchingCiphers.add(cipherView)
                    MatchResult.NONE -> Unit
                }
            }

        return exactMatchingCiphers + fuzzyMatchingCiphers
    }
}

/**
 * Returns a list of domains that match the specified domain. If the domain is contained within
 * the [DomainsData], this will return all matching domains. Otherwise, it will return
 * [matchDomain] or [matchUri] depending on [isAndroidApp].
 */
private fun getMatchingDomains(
    domainsData: DomainsData,
    isAndroidApp: Boolean,
    matchDomain: String?,
    matchUri: String,
): MatchingDomains {
    val androidAppWebHost = matchUri.getWebHostFromAndroidUriOrNull()
    val equivalentDomainsList = domainsData
        .equivalentDomains
        .plus(
            elements = domainsData
                .globalEquivalentDomains
                .map { it.domains },
        )

    val exactMatchDomains = mutableListOf<String>()
    val fuzzyMatchDomains = mutableListOf<String>()
    equivalentDomainsList
        .forEach { equivalentDomains ->
            when {
                isAndroidApp && equivalentDomains.contains(matchUri) -> {
                    exactMatchDomains.addAll(equivalentDomains)
                }

                isAndroidApp && equivalentDomains.contains(androidAppWebHost) -> {
                    fuzzyMatchDomains.addAll(equivalentDomains)
                }

                !isAndroidApp && equivalentDomains.contains(matchDomain) -> {
                    exactMatchDomains.addAll(equivalentDomains)
                }
            }
        }

    // If there are no equivalent domains, add a version of the original URI to the list.
    when {
        exactMatchDomains.isEmpty() && isAndroidApp -> exactMatchDomains.add(matchUri)
        exactMatchDomains.isEmpty() && matchDomain != null -> exactMatchDomains.add(matchDomain)
    }

    return MatchingDomains(
        exactMatches = exactMatchDomains,
        fuzzyMatches = fuzzyMatchDomains,
    )
}

/**
 * Check to see if [cipherView] matches [matchUri] in some way. The returned [MatchResult] will
 * provide details on the match quality.
 *
 * @param cipherView The cipher to be judged for a match.
 * @param context A context for getting string resources.
 * @param defaultUriMatchType The global default [UriMatchType].
 * @param isAndroidApp Whether or not the [matchUri] belongs to an Android app.
 * @param matchingDomains The set of domains that match the domain of [matchUri].
 * @param matchUri The uri that this cipher is being matched to.
 */
@Suppress("LongParameterList")
private fun checkForCipherMatch(
    cipherView: CipherView,
    context: Context,
    defaultUriMatchType: UriMatchType,
    isAndroidApp: Boolean,
    matchingDomains: MatchingDomains,
    matchUri: String,
): MatchResult {
    val matchResults = cipherView
        .login
        ?.uris
        ?.map { loginUriView ->
            loginUriView.checkForMatch(
                context = context,
                defaultUriMatchType = defaultUriMatchType,
                isAndroidApp = isAndroidApp,
                matchingDomains = matchingDomains,
                matchUri = matchUri,
            )
        }

    return matchResults
        ?.firstOrNull { it == MatchResult.EXACT }
        ?: matchResults
            ?.firstOrNull { it == MatchResult.FUZZY }
        ?: MatchResult.NONE
}

/**
 * Check to see how well this [LoginUriView] matches [matchUri].
 *
 * @param context A context for getting app information.
 * @param defaultUriMatchType The global default [UriMatchType].
 * @param isAndroidApp Whether or not the [matchUri] belongs to an Android app.
 * @param matchingDomains The set of domains that match the domain of [matchUri].
 * @param matchUri The uri that this [LoginUriView] is being matched to.
 */
private fun LoginUriView.checkForMatch(
    context: Context,
    defaultUriMatchType: UriMatchType,
    isAndroidApp: Boolean,
    matchingDomains: MatchingDomains,
    matchUri: String,
): MatchResult {
    val matchType = this.match ?: defaultUriMatchType
    val loginViewUri = this.uri

    return if (!loginViewUri.isNullOrBlank()) {
        when (matchType) {
            UriMatchType.DOMAIN -> {
                checkUriForDomainMatch(
                    context = context,
                    isAndroidApp = isAndroidApp,
                    matchingDomains = matchingDomains,
                    uri = loginViewUri,
                )
            }

            UriMatchType.EXACT -> exactIfTrue(loginViewUri == matchUri)

            UriMatchType.HOST -> {
                val loginUriHost = loginViewUri.getHostWithPortOrNull()
                val matchUriHost = matchUri.getHostWithPortOrNull()
                exactIfTrue(matchUriHost != null && loginUriHost == matchUriHost)
            }

            UriMatchType.NEVER -> MatchResult.NONE

            UriMatchType.REGULAR_EXPRESSION -> {
                val pattern = Regex(loginViewUri, RegexOption.IGNORE_CASE)
                exactIfTrue(matchUri.matches(pattern))
            }

            UriMatchType.STARTS_WITH -> exactIfTrue(matchUri.startsWith(loginViewUri))
        }
    } else {
        MatchResult.NONE
    }
}

/**
 * Check to see if [uri] matches [matchingDomains] in some way.
 */
private fun checkUriForDomainMatch(
    isAndroidApp: Boolean,
    context: Context,
    matchingDomains: MatchingDomains,
    uri: String,
): MatchResult = when {
    matchingDomains.exactMatches.contains(uri) -> MatchResult.EXACT
    isAndroidApp && matchingDomains.fuzzyMatches.contains(uri) -> MatchResult.FUZZY
    else -> {
        val domain = uri
            .getDomainOrNull(context = context)
            ?.lowercase()

        // We only care about fuzzy matches if we are isAndroidApp is true because the fuzzu
        // matches are generated using a app URI derived host.
        when {
            matchingDomains.exactMatches.contains(domain) -> MatchResult.EXACT
            isAndroidApp && matchingDomains.fuzzyMatches.contains(domain) -> MatchResult.FUZZY
            else -> MatchResult.NONE
        }
    }
}

/**
 * Simple function to return [MatchResult.EXACT] if [condition] is true, and
 * [MatchResult.NONE] otherwise.
 */
private fun exactIfTrue(condition: Boolean): MatchResult =
    if (condition) MatchResult.EXACT else MatchResult.NONE

/**
 * A convenience data class for holding domain matches.
 */
private data class MatchingDomains(
    val exactMatches: List<String>,
    val fuzzyMatches: List<String>,
)

/**
 * A enum to represent the quality of a match.
 */
private enum class MatchResult {
    EXACT,
    FUZZY,
    NONE,
}
