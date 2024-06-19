package com.x8bit.bitwarden.data.platform.manager.ciphermatching

import android.content.Context
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.LoginUriView
import com.bitwarden.vault.LoginView
import com.bitwarden.vault.UriMatchType
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.util.getDomainOrNull
import com.x8bit.bitwarden.data.platform.util.getHostWithPortOrNull
import com.x8bit.bitwarden.data.platform.util.getWebHostFromAndroidUriOrNull
import com.x8bit.bitwarden.data.platform.util.isAndroidApp
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DomainsData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CipherMatchingManagerTest {
    private lateinit var cipherMatchingManager: CipherMatchingManager

    // Setup dependencies
    private val context: Context = mockk()
    private val settingsRepository: SettingsRepository = mockk {
        every { defaultUriMatchType } returns DEFAULT_URI_MATCH_TYPE
    }
    private val vaultRepository: VaultRepository = mockk {
        every { domainsStateFlow } returns MutableStateFlow(DataState.Loaded(DOMAINS_DATA))
    }

    // Setup test ciphers
    private val defaultMatchLoginUriViewOne: LoginUriView = mockk {
        every { match } returns null
        every { uri } returns DEFAULT_LOGIN_VIEW_URI_ONE
    }
    private val defaultMatchLoginUriViewTwo: LoginUriView = mockk {
        every { match } returns null
        every { uri } returns DEFAULT_LOGIN_VIEW_URI_TWO
    }
    private val defaultMatchLoginUriViewThree: LoginUriView = mockk {
        every { match } returns null
        every { uri } returns DEFAULT_LOGIN_VIEW_URI_THREE
    }
    private val defaultMatchLoginUriViewFour: LoginUriView = mockk {
        every { match } returns null
        every { uri } returns DEFAULT_LOGIN_VIEW_URI_FOUR
    }
    private val defaultMatchLoginUriViewFive: LoginUriView = mockk {
        every { match } returns null
        every { uri } returns DEFAULT_LOGIN_VIEW_URI_FIVE
    }
    private val defaultMatchLoginView: LoginView = mockk {
        every { uris } returns listOf(
            defaultMatchLoginUriViewOne,
            defaultMatchLoginUriViewTwo,
            defaultMatchLoginUriViewThree,
            defaultMatchLoginUriViewFour,
            defaultMatchLoginUriViewFive,
        )
    }
    private val defaultMatchCipher: CipherView = mockk {
        every { login } returns defaultMatchLoginView
    }
    private val exactMatchLoginUriViewOne: LoginUriView = mockk {
        every { match } returns UriMatchType.EXACT
        every { uri } returns "google.com"
    }
    private val exactMatchLoginUriViewTwo: LoginUriView = mockk {
        every { match } returns UriMatchType.EXACT
        every { uri } returns "notExactMatch.com"
    }
    private val exactMatchLoginView: LoginView = mockk {
        every { uris } returns listOf(
            exactMatchLoginUriViewOne,
            exactMatchLoginUriViewTwo,
        )
    }
    private val exactMatchCipher: CipherView = mockk {
        every { login } returns exactMatchLoginView
    }
    private val hostMatchLoginUriViewMatching: LoginUriView = mockk {
        every { match } returns UriMatchType.HOST
        every { uri } returns HOST_LOGIN_VIEW_URI_MATCHING
    }
    private val hostMatchLoginUriViewNotMatching: LoginUriView = mockk {
        every { match } returns UriMatchType.HOST
        every { uri } returns HOST_LOGIN_VIEW_URI_NOT_MATCHING
    }
    private val hostMatchLoginView: LoginView = mockk {
        every { uris } returns listOf(
            hostMatchLoginUriViewMatching,
            hostMatchLoginUriViewNotMatching,
        )
    }
    private val hostMatchCipher: CipherView = mockk {
        every { login } returns hostMatchLoginView
    }
    private val neverMatchLoginUriView: LoginUriView = mockk {
        every { match } returns UriMatchType.NEVER
        every { uri } returns "google.com"
    }
    private val neverMatchLoginView: LoginView = mockk {
        every { uris } returns listOf(neverMatchLoginUriView)
    }
    private val neverMatchCipher: CipherView = mockk {
        every { login } returns neverMatchLoginView
    }
    private val regexMatchLoginUriViewMatching: LoginUriView = mockk {
        every { match } returns UriMatchType.REGULAR_EXPRESSION
        every { uri } returns ".*"
    }
    private val regexMatchLoginUriViewNotMatching: LoginUriView = mockk {
        every { match } returns UriMatchType.REGULAR_EXPRESSION
        every { uri } returns "$^"
    }
    private val regexMatchLoginView: LoginView = mockk {
        every { uris } returns listOf(
            regexMatchLoginUriViewMatching,
            regexMatchLoginUriViewNotMatching,
        )
    }
    private val regexMatchCipher: CipherView = mockk {
        every { login } returns regexMatchLoginView
    }
    private val startsWithMatchLoginUriViewMatching: LoginUriView = mockk {
        every { match } returns UriMatchType.STARTS_WITH
        every { uri } returns "g"
    }
    private val startsWithMatchLoginUriViewNotMatching: LoginUriView = mockk {
        every { match } returns UriMatchType.REGULAR_EXPRESSION
        every { uri } returns "!!!!!!"
    }
    private val startsWithMatchLoginView: LoginView = mockk {
        every { uris } returns listOf(
            startsWithMatchLoginUriViewMatching,
            startsWithMatchLoginUriViewNotMatching,
        )
    }
    private val startsWithMatchCipher: CipherView = mockk {
        every { login } returns startsWithMatchLoginView
    }
    private val ciphers: List<CipherView> = listOf(
        defaultMatchCipher,
        exactMatchCipher,
        hostMatchCipher,
        neverMatchCipher,
        regexMatchCipher,
        startsWithMatchCipher,
    )

    @BeforeEach
    fun setup() {
        mockkStatic(
            String::isAndroidApp,
            String::getDomainOrNull,
            String::getWebHostFromAndroidUriOrNull,
        )
        cipherMatchingManager = CipherMatchingManagerImpl(
            context = context,
            settingsRepository = settingsRepository,
            vaultRepository = vaultRepository,
        )
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(
            String::isAndroidApp,
            String::getDomainOrNull,
            String::getWebHostFromAndroidUriOrNull,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `filterCiphersForMatches should perform cipher matching when is android app and matching URI`() =
        runTest {
            // Setup
            val uri = "google.com"
            val expected = listOf(
                defaultMatchCipher,
                exactMatchCipher,
                hostMatchCipher,
                regexMatchCipher,
                startsWithMatchCipher,
            )
            setupMocksForMatchingCiphers(
                isAndroidApp = true,
                uri = uri,
            )

            // Test
            val actual = cipherMatchingManager.filterCiphersForMatches(
                ciphers = ciphers,
                matchUri = uri,
            )

            // Verify
            assertEquals(expected, actual)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `filterCiphersForMatches should perform cipher matching when is android app and difficult to match URI`() =
        runTest {
            // Setup
            val uri = "difficultToMatch.com"
            // The default cipher only has a fuzzy match
            // and therefore is at the end of the list.
            val expected = listOf(
                hostMatchCipher,
                regexMatchCipher,
                defaultMatchCipher,
            )
            setupMocksForMatchingCiphers(
                isAndroidApp = true,
                uri = uri,
            )

            // Test
            val actual = cipherMatchingManager.filterCiphersForMatches(
                ciphers = ciphers,
                matchUri = uri,
            )

            // Verify
            assertEquals(expected, actual)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `filterCiphersForMatches should perform cipher matching when not android app and matching URI`() =
        runTest {
            // Setup
            val uri = "google.com"
            val expected = listOf(
                defaultMatchCipher,
                exactMatchCipher,
                hostMatchCipher,
                regexMatchCipher,
                startsWithMatchCipher,
            )
            setupMocksForMatchingCiphers(
                isAndroidApp = false,
                uri = uri,
            )

            // Test
            val actual = cipherMatchingManager.filterCiphersForMatches(
                ciphers = ciphers,
                matchUri = uri,
            )

            // Verify
            assertEquals(expected, actual)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `filterCiphersForMatches should perform cipher matching when not android app and difficult to match URI`() =
        runTest {
            // Setup
            val uri = "difficultToMatch.com"
            val expected = listOf(
                hostMatchCipher,
                regexMatchCipher,
            )
            setupMocksForMatchingCiphers(
                isAndroidApp = false,
                uri = uri,
            )

            // Test
            val actual = cipherMatchingManager.filterCiphersForMatches(
                ciphers = ciphers,
                matchUri = uri,
            )

            // Verify
            assertEquals(expected, actual)
        }

    @Test
    fun `filterCiphersForMatches should skip ciphers without login details`() =
        runTest {
            // Setup
            val uri = "noMatches.com"
            val ciphers = listOf<CipherView>(
                mockk {
                    every { login } returns null
                },
            )
            with(uri) {
                every { isAndroidApp() } returns false
                every { getDomainOrNull(context = context) } returns this
                every { getWebHostFromAndroidUriOrNull() } returns null
            }

            // Test
            val actual = cipherMatchingManager.filterCiphersForMatches(
                ciphers = ciphers,
                matchUri = uri,
            )

            // Verify
            assertEquals(emptyList<CipherView>(), actual)
        }

    /**
     * Setup mocks for matching the massive list of [ciphers].
     */
    private fun setupMocksForMatchingCiphers(
        isAndroidApp: Boolean,
        uri: String,
    ) {
        with(uri) {
            every { isAndroidApp() } returns isAndroidApp
            every { getDomainOrNull(context = context) } returns this.takeIf { isAndroidApp }
            every { getHostWithPortOrNull() } returns HOST_WITH_PORT
            every {
                getWebHostFromAndroidUriOrNull()
            } returns ANDROID_APP_WEB_URL.takeIf { isAndroidApp }
        }
        every {
            DEFAULT_LOGIN_VIEW_URI_ONE.getDomainOrNull(context = context)
        } returns DEFAULT_LOGIN_VIEW_URI_ONE
        every {
            DEFAULT_LOGIN_VIEW_URI_TWO.getDomainOrNull(context = context)
        } returns null
        every {
            DEFAULT_LOGIN_VIEW_URI_THREE.getDomainOrNull(context = context)
        } returns uri
        every {
            DEFAULT_LOGIN_VIEW_URI_FOUR.getDomainOrNull(context = context)
        } returns "bitwarden.com"
        every {
            DEFAULT_LOGIN_VIEW_URI_FIVE.getDomainOrNull(context = context)
        } returns null

        every { HOST_LOGIN_VIEW_URI_MATCHING.getHostWithPortOrNull() } returns HOST_WITH_PORT
        every { HOST_LOGIN_VIEW_URI_NOT_MATCHING.getHostWithPortOrNull() } returns null
    }
}

private const val ANDROID_APP_WEB_URL = "ANDROID_APP_WEB_URL"
private val DEFAULT_URI_MATCH_TYPE =
    com.x8bit.bitwarden.data.platform.repository.model.UriMatchType.DOMAIN
private val EQUIVALENT_DOMAINS = listOf(
    "google.com",
    "google.co.uk",
)
private val GLOBAL_EQUIVALENT_DOMAINS_DATA = listOf(
    "bitwarden.com",
    "bitwarden.co.uk",
    ANDROID_APP_WEB_URL,
)
private val GLOBAL_EQUIVALENT_DOMAINS = DomainsData.GlobalEquivalentDomain(
    isExcluded = false,
    domains = GLOBAL_EQUIVALENT_DOMAINS_DATA,
    type = 0,
)
private val DOMAINS_DATA = DomainsData(
    equivalentDomains = listOf(EQUIVALENT_DOMAINS),
    globalEquivalentDomains = listOf(GLOBAL_EQUIVALENT_DOMAINS),
)

// Setup state for default ciphers
private const val DEFAULT_LOGIN_VIEW_URI_ONE: String = "google.com"
private const val DEFAULT_LOGIN_VIEW_URI_TWO: String = ANDROID_APP_WEB_URL
private const val DEFAULT_LOGIN_VIEW_URI_THREE: String = "DEFAULT_LOGIN_VIEW_URI_THREE"
private const val DEFAULT_LOGIN_VIEW_URI_FOUR: String = "DEFAULT_LOGIN_VIEW_URI_FOUR"
private const val DEFAULT_LOGIN_VIEW_URI_FIVE: String = "DEFAULT_LOGIN_VIEW_URI_FIVE"

// Setup state for host ciphers
private const val HOST_LOGIN_VIEW_URI_MATCHING: String = "DEFAULT_LOGIN_VIEW_URI_MATCHING"
private const val HOST_LOGIN_VIEW_URI_NOT_MATCHING: String = "DEFAULT_LOGIN_VIEW_URI_NOT_MATCHING"
private const val HOST_WITH_PORT: String = "HOST_WITH_PORT"
