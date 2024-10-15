package com.x8bit.bitwarden.data.platform.manager.ciphermatching

import com.bitwarden.vault.CipherView
import com.bitwarden.vault.LoginUriView
import com.bitwarden.vault.LoginView
import com.bitwarden.vault.UriMatchType
import com.x8bit.bitwarden.data.platform.manager.ResourceCacheManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.util.getDomainOrNull
import com.x8bit.bitwarden.data.platform.util.getHostOrNull
import com.x8bit.bitwarden.data.platform.util.getHostWithPortOrNull
import com.x8bit.bitwarden.data.platform.util.getWebHostFromAndroidUriOrNull
import com.x8bit.bitwarden.data.platform.util.hasPort
import com.x8bit.bitwarden.data.platform.util.isAndroidApp
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DomainsData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CipherMatchingManagerTest {
    private lateinit var cipherMatchingManager: CipherMatchingManager

    // Setup dependencies
    private val resourceCacheManager: ResourceCacheManager = mockk()
    private val settingsRepository: SettingsRepository = mockk {
        every { defaultUriMatchType } returns DEFAULT_URI_MATCH_TYPE
    }
    private val mutableDomainsStateFlow = MutableStateFlow<DataState<DomainsData>>(
        value = DataState.Loaded(DOMAINS_DATA),
    )
    private val vaultRepository: VaultRepository = mockk {
        every { domainsStateFlow } returns mutableDomainsStateFlow
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
    private val hostNoPortMatchLoginUriViewMatching: LoginUriView = mockk {
        every { match } returns UriMatchType.HOST
        every { uri } returns HOST_NO_PORT_LOGIN_VIEW_URI_MATCHING
    }
    private val hostNoPortMatchLoginUriViewNotMatching: LoginUriView = mockk {
        every { match } returns UriMatchType.HOST
        every { uri } returns HOST_NO_PORT_LOGIN_VIEW_URI_NOT_MATCHING
    }
    private val hostNoPortMatchLoginView: LoginView = mockk {
        every { uris } returns listOf(
            hostNoPortMatchLoginUriViewMatching,
            hostNoPortMatchLoginUriViewNotMatching,
        )
    }
    private val hostNoPortMatchCipher: CipherView = mockk {
        every { login } returns hostNoPortMatchLoginView
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
        hostNoPortMatchCipher,
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
            resourceCacheManager = resourceCacheManager,
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

    @Test
    fun `filterCiphersForMatches should return an empty list when retrieving domains times out`() =
        runTest {
            // Setup
            val uri = "google.com"
            mutableDomainsStateFlow.value = DataState.Loading

            // Test
            val actual = async {
                cipherMatchingManager.filterCiphersForMatches(
                    ciphers = ciphers,
                    matchUri = uri,
                )
            }

            testScheduler.runCurrent()
            assertFalse(actual.isCompleted)
            testScheduler.advanceTimeBy(delayTimeMillis = 1_000L)
            testScheduler.runCurrent()

            // Verify
            assertTrue(actual.isCompleted)
            assertEquals(emptyList<CipherView>(), actual.await())
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
                hostNoPortMatchCipher,
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
                hostNoPortMatchCipher,
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
                hostNoPortMatchCipher,
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
                hostNoPortMatchCipher,
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
                every { getDomainOrNull(resourceCacheManager = resourceCacheManager) } returns this
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
        hasPort: Boolean = true,
    ) {
        with(uri) {
            every { isAndroidApp() } returns isAndroidApp
            every {
                getDomainOrNull(resourceCacheManager = resourceCacheManager)
            } returns this.takeIf { isAndroidApp }
            every { hasPort() } returns hasPort
            every { getHostOrNull() } returns HOST
            every { getHostWithPortOrNull() } returns HOST_WITH_PORT
            every {
                getWebHostFromAndroidUriOrNull()
            } returns ANDROID_APP_WEB_URL.takeIf { isAndroidApp }
        }
        every {
            DEFAULT_LOGIN_VIEW_URI_ONE.getDomainOrNull(resourceCacheManager = resourceCacheManager)
        } returns DEFAULT_LOGIN_VIEW_URI_ONE
        every {
            DEFAULT_LOGIN_VIEW_URI_TWO.getDomainOrNull(resourceCacheManager = resourceCacheManager)
        } returns null
        every {
            DEFAULT_LOGIN_VIEW_URI_THREE.getDomainOrNull(
                resourceCacheManager = resourceCacheManager,
            )
        } returns uri
        every {
            DEFAULT_LOGIN_VIEW_URI_FOUR.getDomainOrNull(resourceCacheManager = resourceCacheManager)
        } returns "bitwarden.com"
        every {
            DEFAULT_LOGIN_VIEW_URI_FIVE.getDomainOrNull(resourceCacheManager = resourceCacheManager)
        } returns null

        every { HOST_LOGIN_VIEW_URI_MATCHING.hasPort() } returns true
        every { HOST_LOGIN_VIEW_URI_MATCHING.getHostWithPortOrNull() } returns HOST_WITH_PORT
        every { HOST_LOGIN_VIEW_URI_NOT_MATCHING.hasPort() } returns true
        every { HOST_LOGIN_VIEW_URI_NOT_MATCHING.getHostWithPortOrNull() } returns null

        every { HOST_NO_PORT_LOGIN_VIEW_URI_MATCHING.hasPort() } returns false
        every { HOST_NO_PORT_LOGIN_VIEW_URI_MATCHING.getHostOrNull() } returns HOST
        every { HOST_NO_PORT_LOGIN_VIEW_URI_NOT_MATCHING.hasPort() } returns false
        every { HOST_NO_PORT_LOGIN_VIEW_URI_NOT_MATCHING.getHostOrNull() } returns null
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
private const val HOST_NO_PORT_LOGIN_VIEW_URI_MATCHING: String =
    "HOST_NO_PORT_LOGIN_VIEW_URI_MATCHING"
private const val HOST_NO_PORT_LOGIN_VIEW_URI_NOT_MATCHING: String =
    "HOST_NO_PORT_LOGIN_VIEW_URI_NOT_MATCHING"
private const val HOST_WITH_PORT: String = "HOST_WITH_PORT"
private const val HOST: String = "HOST"
