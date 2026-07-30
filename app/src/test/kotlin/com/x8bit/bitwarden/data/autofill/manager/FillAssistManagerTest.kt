package com.x8bit.bitwarden.data.autofill.manager

import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.data.datasource.disk.model.ServerConfig
import com.bitwarden.data.repository.ServerConfigRepository
import com.bitwarden.network.model.ConfigResponseJson
import com.bitwarden.network.model.FillAssistFormsJson
import com.bitwarden.network.model.FillAssistManifestJson
import com.bitwarden.network.service.FillAssistService
import com.x8bit.bitwarden.data.autofill.datasource.disk.FillAssistDiskSource
import com.x8bit.bitwarden.data.autofill.model.FillAssistRules
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSource
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

private const val BASE_URL = "https://fill-assist.example.com"
private const val FORMS_FILENAME = "forms.v1.json"
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

    private val settingsRepository: SettingsRepository = mockk {
        every { isFillAssistEnabled } returns true
    }

    private val serverConfigFlow = MutableStateFlow<ServerConfig?>(SERVER_CONFIG)

    private val serverConfigRepository: ServerConfigRepository = mockk {
        every { serverConfigStateFlow } returns serverConfigFlow
    }

    private val fillAssistService: FillAssistService = mockk {
        coEvery { getManifest() } returns Result.success(MANIFEST)
        coEvery { getForms(any()) } returns Result.success(FORMS_V1)
    }

    private val fillAssistDiskSource: FillAssistDiskSource = mockk {
        every { getLastFetchTimestamp(BASE_URL) } returns STALE_TIMESTAMP
        every { getLastKnownCid(BASE_URL) } returns null
        every { getFillAssistRules(BASE_URL) } returns null
        every { storeFillAssistRules(any(), any()) } just runs
        every { storeLastKnownCid(any(), any()) } just runs
        every { storeLastFetchTimestamp(any(), any()) } just runs
    }

    private val environmentDiskSource: EnvironmentDiskSource = mockk {
        every { fillAssistRulesUrl = any() } just runs
    }

    private val manager = FillAssistManagerImpl(
        fillAssistService = fillAssistService,
        fillAssistDiskSource = fillAssistDiskSource,
        featureFlagManager = featureFlagManager,
        serverConfigRepository = serverConfigRepository,
        settingsRepository = settingsRepository,
        environmentDiskSource = environmentDiskSource,
        clock = FIXED_CLOCK,
        dispatcherManager = FakeDispatcherManager(),
    )

    @BeforeEach
    fun setUp() {
        // serverConfigStateFlow replays its current value on subscription, triggering
        // syncIfNecessary() and the URL write during construction. Clear call counts for a clean
        // test slate.
        clearMocks(fillAssistService, fillAssistDiskSource, environmentDiskSource, answers = false)
    }

    @Test
    fun `server config change writes fillAssistRulesUrl to environment disk source`() = runTest {
        serverConfigFlow.value = null
        verify { environmentDiskSource.fillAssistRulesUrl = null }

        serverConfigFlow.value = SERVER_CONFIG
        verify { environmentDiskSource.fillAssistRulesUrl = BASE_URL }
    }

    @Test
    fun `sync returns success and does nothing when feature flag is disabled`() = runTest {
        every {
            featureFlagManager.getFeatureFlag(FlagKey.FillAssistTargetingRules)
        } returns false

        manager.syncIfNecessary()

        coVerify(exactly = 0) { fillAssistService.getManifest() }
        verify(exactly = 0) { fillAssistDiskSource.storeFillAssistRules(any(), any()) }
    }

    @Test
    fun `sync returns success and does nothing when fill assist is disabled in settings`() =
        runTest {
            every { settingsRepository.isFillAssistEnabled } returns false

            manager.syncIfNecessary()

            coVerify(exactly = 0) { fillAssistService.getManifest() }
            verify(exactly = 0) { fillAssistDiskSource.storeFillAssistRules(any(), any()) }
        }

    @Test
    fun `sync returns success and does nothing when fillAssistRulesUrl is null`() = runTest {
        serverConfigFlow.value = null

        manager.syncIfNecessary()

        coVerify(exactly = 0) { fillAssistService.getManifest() }
    }

    @Test
    fun `sync skips all network calls when timestamp is fresh`() = runTest {
        every {
            fillAssistDiskSource.getLastFetchTimestamp(BASE_URL)
        } returns FIXED_CLOCK.millis() - (6 * 60 * 60 * 1000L - 1)

        manager.syncIfNecessary()

        coVerify(exactly = 0) { fillAssistService.getManifest() }
        coVerify(exactly = 0) { fillAssistService.getForms(any()) }
    }

    @Test
    fun `sync skips forms download and updates timestamp when CID is unchanged`() = runTest {
        every { fillAssistDiskSource.getLastKnownCid(BASE_URL) } returns CID

        manager.syncIfNecessary()

        coVerify(exactly = 1) { fillAssistService.getManifest() }
        coVerify(exactly = 0) { fillAssistService.getForms(any()) }
        verify(exactly = 0) { fillAssistDiskSource.storeFillAssistRules(any(), any()) }
        verify { fillAssistDiskSource.storeLastFetchTimestamp(BASE_URL, FIXED_CLOCK.millis()) }
    }

    @Test
    fun `sync re-fetches forms when CID changes`() = runTest {
        every { fillAssistDiskSource.getLastKnownCid(BASE_URL) } returns "sha256:old"

        manager.syncIfNecessary()

        coVerify(exactly = 1) { fillAssistService.getForms(filename = FORMS_FILENAME) }
        verify { fillAssistDiskSource.storeFillAssistRules(BASE_URL, any()) }
        verify { fillAssistDiskSource.storeLastKnownCid(BASE_URL, CID) }
        verify { fillAssistDiskSource.storeLastFetchTimestamp(BASE_URL, FIXED_CLOCK.millis()) }
    }

    @Test
    fun `sync does not store data when manifest fetch fails`() = runTest {
        coEvery {
            fillAssistService.getManifest()
        } returns Result.failure(RuntimeException("network error"))

        manager.syncIfNecessary()

        verify(exactly = 0) { fillAssistDiskSource.storeFillAssistRules(any(), any()) }
        verify(exactly = 0) { fillAssistDiskSource.storeLastKnownCid(any(), any()) }
        verify(exactly = 0) { fillAssistDiskSource.storeLastFetchTimestamp(any(), any()) }
    }

    @Test
    fun `sync does not store rules or cid when schemaVersion major is unsupported`() = runTest {
        coEvery { fillAssistService.getForms(any()) } returns Result.success(
            FORMS_V1.copy(schemaVersion = "2.0.0"),
        )

        manager.syncIfNecessary()

        verify(exactly = 0) { fillAssistDiskSource.storeFillAssistRules(any(), any()) }
        verify(exactly = 0) { fillAssistDiskSource.storeLastKnownCid(any(), any()) }
        verify { fillAssistDiskSource.storeLastFetchTimestamp(BASE_URL, FIXED_CLOCK.millis()) }
    }

    @Test
    fun `sync happy path stores rules, cid, and timestamp`() = runTest {
        manager.syncIfNecessary()

        verify { fillAssistDiskSource.storeFillAssistRules(BASE_URL, any()) }
        verify { fillAssistDiskSource.storeLastKnownCid(BASE_URL, CID) }
        verify { fillAssistDiskSource.storeLastFetchTimestamp(BASE_URL, FIXED_CLOCK.millis()) }
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

        manager.syncIfNecessary()

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

        manager.syncIfNecessary()

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

            manager.syncIfNecessary()

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

        manager.syncIfNecessary()

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
        serverConfigFlow.value = null
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
    fun `parseSingleSelector extracts last segment of shadow DOM selector`() {
        assertEquals(
            FillAssistRules.SelectorClause(
                tag = "input",
                id = "field",
                name = null,
                type = null,
                role = null,
            ),
            parseSingleSelector("div#container >>> input#field"),
        )
    }

    @Test
    fun `parseSingleSelector extracts last segment of multi-level shadow DOM selector`() {
        assertEquals(
            FillAssistRules.SelectorClause(
                tag = "input",
                name = "password",
                id = null,
                type = null,
                role = null,
            ),
            parseSingleSelector("div#form-container >>> form > div >>> input[name='password']"),
        )
    }

    @Test
    fun `parseSingleSelector returns null for pure class selector`() {
        assertNull(parseSingleSelector(".loginForm"))
    }

    @Test
    fun `parseSingleSelector splits on descendant whitespace remaining after a shadow boundary`() {
        assertEquals(
            FillAssistRules.SelectorClause(
                tag = "input",
                id = null,
                name = "email",
                type = null,
                role = null,
            ),
            parseSingleSelector("custom-element >>> div.wrapper input[name='email']"),
        )
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

    @Test
    fun `parseSingleSelector returns null when only constraint is an unsupported attribute`() {
        assertNull(parseSingleSelector("input[autocomplete='current-password']"))
    }

    @Test
    fun `parseSingleSelector ignores an unsupported attribute when a supported one is present`() {
        assertEquals(
            FillAssistRules.SelectorClause(
                tag = "input",
                id = null,
                name = "password",
                type = null,
                role = null,
            ),
            parseSingleSelector("input[name='password'][autocomplete='current-password']"),
        )
    }

    @Test
    fun `parseSingleSelector returns null when only constraint is a class qualifier`() {
        assertNull(parseSingleSelector("input.hidden"))
    }

    @Test
    fun `parseSingleSelector ignores a class qualifier when a supported attribute is present`() {
        assertEquals(
            FillAssistRules.SelectorClause(
                tag = "input",
                id = null,
                name = "password",
                type = null,
                role = null,
            ),
            parseSingleSelector("input.hidden[name='password']"),
        )
    }

    // endregion
}

private val MANIFEST = FillAssistManifestJson(
    buildId = "local-build",
    timestamp = "2026-01-01T12:00:00Z",
    gitSha = "abc123",
    maps = FillAssistManifestJson.MapsJson(
        forms = mapOf(
            "v1" to FillAssistManifestJson.FileEntryJson(
                filename = FORMS_FILENAME,
                cid = CID,
                schema = "forms.v1.schema.json",
                deprecated = null,
            ),
        ),
    ),
)

private val FORMS_V1 = FillAssistFormsJson(
    schemaVersion = "1.0.0",
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
    schemaVersion = "1.0.0",
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
    schemaVersion = "1.0.0",
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
    schemaVersion = "1.0.0",
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
    schemaVersion = "1.0.0",
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
        settings = null,
    ),
)
