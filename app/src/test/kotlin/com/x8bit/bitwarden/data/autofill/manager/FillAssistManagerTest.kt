package com.x8bit.bitwarden.data.autofill.manager

import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.data.datasource.disk.model.ServerConfig
import com.bitwarden.data.repository.ServerConfigRepository
import com.bitwarden.network.model.ConfigResponseJson
import com.bitwarden.network.model.FillAssistFormsJson
import com.bitwarden.network.model.FillAssistManifestJson
import com.bitwarden.network.service.FillAssistService
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.util.userSwitchingChangesFlow
import com.x8bit.bitwarden.data.autofill.datasource.disk.FillAssistDiskSource
import com.x8bit.bitwarden.data.autofill.model.FillAssistRules
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

private const val BASE_URL = "https://fill-assist.example.com"
private const val MANIFEST_URL = "$BASE_URL/manifest.json"
private const val FORMS_URL = "$BASE_URL/forms.v0.json"
private const val CID = "sha256:abc123"

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2026-01-01T12:00:00Z"),
    ZoneOffset.UTC,
)

/** A timestamp far in the past, ensuring the timestamp check never skips network calls. */
private const val STALE_TIMESTAMP = 0L

class FillAssistManagerTest {

    private val featureFlagManager: FeatureFlagManager = mockk {
        every { getFeatureFlag(FlagKey.FillAssistTargetingRules) } returns true
    }

    private val serverConfigRepository: ServerConfigRepository = mockk {
        every { serverConfigStateFlow } returns MutableStateFlow(SERVER_CONFIG)
    }

    private val authDiskSource: AuthDiskSource = mockk {
        every { userSwitchingChangesFlow } returns bufferedMutableSharedFlow()
    }

    private val fillAssistService: FillAssistService = mockk {
        coEvery { getManifest(url = MANIFEST_URL) } returns Result.success(MANIFEST)
        coEvery { getForms(formsUrl = FORMS_URL) } returns Result.success(FORMS_V1)
    }

    private val fillAssistDiskSource: FillAssistDiskSource = mockk {
        every { getLastFetchTimestamp(BASE_URL) } returns STALE_TIMESTAMP
        every { getLastKnownCid(BASE_URL) } returns null
        every { getFillAssistRules(BASE_URL) } returns null
        every { storeFillAssistRules(any(), any()) } just runs
        every { storeLastKnownCid(any(), any()) } just runs
        every { storeLastFetchTimestamp(any(), any()) } just runs
    }

    private val manager = FillAssistManagerImpl(
        fillAssistService = fillAssistService,
        fillAssistDiskSource = fillAssistDiskSource,
        featureFlagManager = featureFlagManager,
        serverConfigRepository = serverConfigRepository,
        authDiskSource = authDiskSource,
        clock = FIXED_CLOCK,
        dispatcherManager = FakeDispatcherManager(),
    )

    @BeforeEach
    fun setUp() {
        // FillAssistManagerImpl.init {} calls sync() immediately via ioScope.launch, and the
        // serverConfigStateFlow subscription also fires on construction (StateFlow replays its
        // current value). Clear call counts so test verifications see a clean slate.
        clearMocks(fillAssistService, fillAssistDiskSource, answers = false)
    }

    @Test
    fun `sync returns success and does nothing when feature flag is disabled`() = runTest {
        every {
            featureFlagManager.getFeatureFlag(FlagKey.FillAssistTargetingRules)
        } returns false

        val result = manager.sync()

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { fillAssistService.getManifest(any()) }
        verify(exactly = 0) { fillAssistDiskSource.storeFillAssistRules(any(), any()) }
    }

    @Test
    fun `sync returns success and does nothing when fillAssistRulesUrl is null`() = runTest {
        every { serverConfigRepository.serverConfigStateFlow } returns MutableStateFlow(null)

        val result = manager.sync()

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { fillAssistService.getManifest(any()) }
    }

    @Test
    fun `sync skips all network calls when timestamp is fresh`() = runTest {
        every {
            fillAssistDiskSource.getLastFetchTimestamp(BASE_URL)
        } returns FIXED_CLOCK.millis() - (6 * 60 * 60 * 1000L - 1)

        val result = manager.sync()

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { fillAssistService.getManifest(any()) }
        coVerify(exactly = 0) { fillAssistService.getForms(any()) }
    }

    @Test
    fun `sync skips forms download and updates timestamp when CID is unchanged`() = runTest {
        every { fillAssistDiskSource.getLastKnownCid(BASE_URL) } returns CID

        val result = manager.sync()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { fillAssistService.getManifest(url = MANIFEST_URL) }
        coVerify(exactly = 0) { fillAssistService.getForms(any()) }
        verify(exactly = 0) { fillAssistDiskSource.storeFillAssistRules(any(), any()) }
        verify {
            fillAssistDiskSource.storeLastFetchTimestamp(
                BASE_URL,
                FIXED_CLOCK.millis(),
            )
        }
    }

    @Test
    fun `sync re-fetches forms when CID changes`() = runTest {
        every { fillAssistDiskSource.getLastKnownCid(BASE_URL) } returns "sha256:old"

        val result = manager.sync()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { fillAssistService.getForms(formsUrl = FORMS_URL) }
        verify { fillAssistDiskSource.storeFillAssistRules(BASE_URL, any()) }
        verify { fillAssistDiskSource.storeLastKnownCid(BASE_URL, CID) }
        verify {
            fillAssistDiskSource.storeLastFetchTimestamp(
                BASE_URL,
                FIXED_CLOCK.millis(),
            )
        }
    }

    @Test
    fun `sync returns failure when manifest fetch fails`() = runTest {
        coEvery {
            fillAssistService.getManifest(any())
        } returns Result.failure(RuntimeException("network error"))

        val result = manager.sync()

        assertTrue(result.isFailure)
        verify(exactly = 0) { fillAssistDiskSource.storeFillAssistRules(any(), any()) }
    }

    @Test
    fun `sync does not store anything when schemaVersion major is unsupported`() = runTest {
        coEvery { fillAssistService.getForms(any()) } returns Result.success(
            FORMS_V1.copy(schemaVersion = "1.0.0"),
        )

        val result = manager.sync()

        assertTrue(result.isSuccess)
        verify(exactly = 0) { fillAssistDiskSource.storeFillAssistRules(any(), any()) }
        verify(exactly = 0) { fillAssistDiskSource.storeLastKnownCid(any(), any()) }
        verify(exactly = 0) { fillAssistDiskSource.storeLastFetchTimestamp(any(), any()) }
    }

    @Test
    fun `sync happy path stores rules, cid, and timestamp`() = runTest {
        val result = manager.sync()

        assertTrue(result.isSuccess)
        verify { fillAssistDiskSource.storeFillAssistRules(BASE_URL, any()) }
        verify { fillAssistDiskSource.storeLastKnownCid(BASE_URL, CID) }
        verify {
            fillAssistDiskSource.storeLastFetchTimestamp(
                BASE_URL,
                FIXED_CLOCK.millis(),
            )
        }
    }

    @Test
    fun `sync pools forms from multiple pathnames under the same host`() = runTest {
        coEvery {
            fillAssistService.getForms(any())
        } returns Result.success(FORMS_V1_MULTI_PATHNAME)

        val rulesSlot = slot<FillAssistRules>()
        every {
            fillAssistDiskSource.storeFillAssistRules(any(), capture(rulesSlot))
        } just runs

        manager.sync()

        assertEquals(EXPECTED_RULES_MULTI_PATHNAME, rulesSlot.captured)
    }

    @Test
    fun `sync pools host-level forms and pathname forms under the same host`() = runTest {
        coEvery {
            fillAssistService.getForms(any())
        } returns Result.success(FORMS_V1_HOST_AND_PATHNAME)

        val rulesSlot = slot<FillAssistRules>()
        every {
            fillAssistDiskSource.storeFillAssistRules(any(), capture(rulesSlot))
        } just runs

        manager.sync()

        assertEquals(EXPECTED_RULES_HOST_AND_PATHNAME, rulesSlot.captured)
    }

    @Test
    fun `sync merges forms with the same category from different pathnames into one HostRule`() =
        runTest {
            coEvery {
                fillAssistService.getForms(any())
            } returns Result.success(FORMS_V1_SAME_CATEGORY_PATHNAMES)

            val rulesSlot = slot<FillAssistRules>()
            every {
                fillAssistDiskSource.storeFillAssistRules(any(), capture(rulesSlot))
            } just runs

            manager.sync()

            assertEquals(EXPECTED_RULES_MERGED_CATEGORY, rulesSlot.captured)
        }

    @Test
    fun `sync deduplicates selector clauses within a merged category`() = runTest {
        coEvery {
            fillAssistService.getForms(any())
        } returns Result.success(FORMS_V1_DUPLICATE_SELECTORS)

        val rulesSlot = slot<FillAssistRules>()
        every {
            fillAssistDiskSource.storeFillAssistRules(any(), capture(rulesSlot))
        } just runs

        manager.sync()

        assertEquals(EXPECTED_RULES_DEDUPLICATED_SELECTORS, rulesSlot.captured)
    }

    @Test
    fun `getFillAssistRules delegates to disk source`() {
        val expected = FillAssistRules(hostRules = emptyMap())
        every { fillAssistDiskSource.getFillAssistRules(BASE_URL) } returns expected
        assertEquals(expected, manager.getFillAssistRules())
    }

    @Test
    fun `getFillAssistRules returns null when disk source has no data`() {
        every { fillAssistDiskSource.getFillAssistRules(BASE_URL) } returns null
        assertNull(manager.getFillAssistRules())
    }

    @Test
    fun `getFillAssistRules returns null when server URL is not configured`() {
        every { serverConfigRepository.serverConfigStateFlow } returns MutableStateFlow(null)
        assertNull(manager.getFillAssistRules())
    }

    // region CSS parser

    @Test
    fun `parseSingleSelector extracts tag and id shorthand`() {
        assertEquals(
            FillAssistRules.SelectorClause(
                tag = "input",
                id = "oid",
                name = null,
                type = null,
                role = null,
            ),
            parseSingleSelector("input#oid"),
        )
    }

    @Test
    fun `parseSingleSelector extracts name attribute`() {
        assertEquals(
            FillAssistRules.SelectorClause(
                tag = "input",
                id = null,
                name = "p",
                type = null,
                role = null,
            ),
            parseSingleSelector("input[name='p']"),
        )
    }

    @Test
    fun `parseSingleSelector extracts compound selector with id shorthand and name`() {
        assertEquals(
            FillAssistRules.SelectorClause(
                tag = "input",
                id = "password",
                name = "password",
                type = null,
                role = null,
            ),
            parseSingleSelector("input#password[name='password']"),
        )
    }

    @Test
    fun `parseSingleSelector extracts role attribute`() {
        assertEquals(
            FillAssistRules.SelectorClause(
                tag = "form",
                id = null,
                name = null,
                type = null,
                role = "search",
            ),
            parseSingleSelector("form[role='search']"),
        )
    }

    @Test
    fun `parseSingleSelector returns null for shadow DOM selector`() {
        assertNull(parseSingleSelector("div#container >>> input#field"))
    }

    @Test
    fun `parseSingleSelector returns null for pure class selector`() {
        assertNull(parseSingleSelector(".loginForm"))
    }

    @Test
    fun `parseSingleSelector handles select element`() {
        assertEquals(
            FillAssistRules.SelectorClause(
                tag = "select",
                id = "state",
                name = null,
                type = null,
                role = null,
            ),
            parseSingleSelector("select#state"),
        )
    }

    // endregion
}

private val MANIFEST = FillAssistManifestJson(
    buildId = "local-build",
    timestamp = null,
    gitSha = null,
    maps = FillAssistManifestJson.MapsJson(
        forms = mapOf(
            "v0" to FillAssistManifestJson.FileEntryJson(
                filename = "forms.v0.json",
                cid = CID,
                schema = null,
            ),
        ),
    ),
)

private val FORMS_V1 = FillAssistFormsJson(
    schemaVersion = "0.1.0",
    hosts = mapOf(
        "example.com" to FillAssistFormsJson.HostEntryJson(
            forms = listOf(
                FillAssistFormsJson.FormJson(
                    category = "account-login",
                    container = null,
                    fields = mapOf(
                        "username" to JsonArray(
                            listOf(JsonPrimitive("input#user")),
                        ),
                    ),
                ),
            ),
            pathnames = null,
        ),
    ),
)

// Host with two pathnames — both forms must appear in the stored rules.
private val FORMS_V1_MULTI_PATHNAME = FillAssistFormsJson(
    schemaVersion = "0.1.0",
    hosts = mapOf(
        "example.com" to FillAssistFormsJson.HostEntryJson(
            forms = null,
            pathnames = mapOf(
                "/login" to FillAssistFormsJson.PathnameEntryJson(
                    forms = listOf(
                        FillAssistFormsJson.FormJson(
                            category = "account-login",
                            container = null,
                            fields = mapOf(
                                "username" to JsonArray(listOf(JsonPrimitive("input#user"))),
                                "password" to JsonArray(listOf(JsonPrimitive("input#pass"))),
                            ),
                        ),
                    ),
                ),
                "/register" to FillAssistFormsJson.PathnameEntryJson(
                    forms = listOf(
                        FillAssistFormsJson.FormJson(
                            category = "account-creation",
                            container = null,
                            fields = mapOf(
                                "username" to JsonArray(listOf(JsonPrimitive("input#email"))),
                                "newPassword" to JsonArray(listOf(JsonPrimitive("input#new-pass"))),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    ),
)

private val EXPECTED_RULES_MULTI_PATHNAME = FillAssistRules(
    hostRules = mapOf(
        "example.com" to listOf(
            FillAssistRules.HostRule(
                category = "account-login",
                fields = mapOf(
                    "username" to listOf(
                        FillAssistRules.SelectorClause(
                            tag = "input",
                            id = "user",
                            name = null,
                            type = null,
                            role = null,
                        ),
                    ),
                    "password" to listOf(
                        FillAssistRules.SelectorClause(
                            tag = "input",
                            id = "pass",
                            name = null,
                            type = null,
                            role = null,
                        ),
                    ),
                ),
            ),
            FillAssistRules.HostRule(
                category = "account-creation",
                fields = mapOf(
                    "username" to listOf(
                        FillAssistRules.SelectorClause(
                            tag = "input",
                            id = "email",
                            name = null,
                            type = null,
                            role = null,
                        ),
                    ),
                    "newPassword" to listOf(
                        FillAssistRules.SelectorClause(
                            tag = "input",
                            id = "new-pass",
                            name = null,
                            type = null,
                            role = null,
                        ),
                    ),
                ),
            ),
        ),
    ),
)

// Host with both top-level forms and pathname forms — both must appear in the stored rules.
private val FORMS_V1_HOST_AND_PATHNAME = FillAssistFormsJson(
    schemaVersion = "0.1.0",
    hosts = mapOf(
        "example.com" to FillAssistFormsJson.HostEntryJson(
            forms = listOf(
                FillAssistFormsJson.FormJson(
                    category = "account-login",
                    container = null,
                    fields = mapOf(
                        "username" to JsonArray(listOf(JsonPrimitive("input#user"))),
                    ),
                ),
            ),
            pathnames = mapOf(
                "/checkout" to FillAssistFormsJson.PathnameEntryJson(
                    forms = listOf(
                        FillAssistFormsJson.FormJson(
                            category = "payment-card",
                            container = null,
                            fields = mapOf(
                                "cardNumber" to JsonArray(listOf(JsonPrimitive("input#card-num"))),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    ),
)

private val EXPECTED_RULES_HOST_AND_PATHNAME = FillAssistRules(
    hostRules = mapOf(
        "example.com" to listOf(
            FillAssistRules.HostRule(
                category = "account-login",
                fields = mapOf(
                    "username" to listOf(
                        FillAssistRules.SelectorClause(
                            tag = "input",
                            id = "user",
                            name = null,
                            type = null,
                            role = null,
                        ),
                    ),
                ),
            ),
            FillAssistRules.HostRule(
                category = "payment-card",
                fields = mapOf(
                    "cardNumber" to listOf(
                        FillAssistRules.SelectorClause(
                            tag = "input",
                            id = "card-num",
                            name = null,
                            type = null,
                            role = null,
                        ),
                    ),
                ),
            ),
        ),
    ),
)

// Two pathnames both define account-login — must be merged into one HostRule.
private val FORMS_V1_SAME_CATEGORY_PATHNAMES = FillAssistFormsJson(
    schemaVersion = "0.1.0",
    hosts = mapOf(
        "example.com" to FillAssistFormsJson.HostEntryJson(
            forms = null,
            pathnames = mapOf(
                "/login" to FillAssistFormsJson.PathnameEntryJson(
                    forms = listOf(
                        FillAssistFormsJson.FormJson(
                            category = "account-login",
                            container = null,
                            fields = mapOf(
                                "username" to JsonArray(listOf(JsonPrimitive("input#user"))),
                            ),
                        ),
                    ),
                ),
                "/signin" to FillAssistFormsJson.PathnameEntryJson(
                    forms = listOf(
                        FillAssistFormsJson.FormJson(
                            category = "account-login",
                            container = null,
                            fields = mapOf(
                                "username" to JsonArray(listOf(JsonPrimitive("input#email"))),
                                "password" to JsonArray(listOf(JsonPrimitive("input#pass"))),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    ),
)

private val EXPECTED_RULES_MERGED_CATEGORY = FillAssistRules(
    hostRules = mapOf(
        "example.com" to listOf(
            FillAssistRules.HostRule(
                category = "account-login",
                fields = mapOf(
                    "username" to listOf(
                        FillAssistRules.SelectorClause(
                            tag = "input",
                            id = "user",
                            name = null,
                            type = null,
                            role = null,
                        ),
                        FillAssistRules.SelectorClause(
                            tag = "input",
                            id = "email",
                            name = null,
                            type = null,
                            role = null,
                        ),
                    ),
                    "password" to listOf(
                        FillAssistRules.SelectorClause(
                            tag = "input",
                            id = "pass",
                            name = null,
                            type = null,
                            role = null,
                        ),
                    ),
                ),
            ),
        ),
    ),
)

// Two pathnames define the same selector — the duplicate must be removed.
private val FORMS_V1_DUPLICATE_SELECTORS = FillAssistFormsJson(
    schemaVersion = "0.1.0",
    hosts = mapOf(
        "example.com" to FillAssistFormsJson.HostEntryJson(
            forms = null,
            pathnames = mapOf(
                "/login" to FillAssistFormsJson.PathnameEntryJson(
                    forms = listOf(
                        FillAssistFormsJson.FormJson(
                            category = "account-login",
                            container = null,
                            fields = mapOf(
                                "username" to JsonArray(listOf(JsonPrimitive("input#user"))),
                            ),
                        ),
                    ),
                ),
                "/other-login" to FillAssistFormsJson.PathnameEntryJson(
                    forms = listOf(
                        FillAssistFormsJson.FormJson(
                            category = "account-login",
                            container = null,
                            fields = mapOf(
                                "username" to JsonArray(listOf(JsonPrimitive("input#user"))),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    ),
)

private val EXPECTED_RULES_DEDUPLICATED_SELECTORS = FillAssistRules(
    hostRules = mapOf(
        "example.com" to listOf(
            FillAssistRules.HostRule(
                category = "account-login",
                fields = mapOf(
                    "username" to listOf(
                        FillAssistRules.SelectorClause(
                            tag = "input",
                            id = "user",
                            name = null,
                            type = null,
                            role = null,
                        ),
                    ),
                ),
            ),
        ),
    ),
)

private val SERVER_CONFIG = ServerConfig(
    lastSync = 0L,
    serverData = ConfigResponseJson(
        type = null,
        version = null,
        gitHash = null,
        server = null,
        environment = ConfigResponseJson.EnvironmentJson(
            cloudRegion = null,
            vaultUrl = null,
            apiUrl = null,
            identityUrl = null,
            notificationsUrl = null,
            ssoUrl = null,
            fillAssistRulesUrl = BASE_URL,
        ),
        featureStates = null,
        communication = null,
    ),
)
