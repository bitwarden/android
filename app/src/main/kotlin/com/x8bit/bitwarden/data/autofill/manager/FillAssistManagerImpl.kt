package com.x8bit.bitwarden.data.autofill.manager

import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.data.repository.ServerConfigRepository
import com.bitwarden.network.model.FillAssistFormsJson
import com.bitwarden.network.service.FillAssistService
import com.x8bit.bitwarden.data.autofill.datasource.disk.FillAssistDiskSource
import com.x8bit.bitwarden.data.autofill.model.FillAssistRules
import com.x8bit.bitwarden.data.autofill.model.FillAssistRules.SelectorClause
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSource
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import timber.log.Timber
import java.time.Clock

private const val CURRENT_FORMS_VERSION = "v1"
private const val EXPECTED_SCHEMA_MAJOR = "1"

/** Re-fetch interval in milliseconds (6 hours, matching the browser implementation). */
private const val UPDATE_INTERVAL_MS = 6 * 60 * 60 * 1000L

// Matches [attr='value'] and [attr="value"] attribute selectors.
private val ATTRIBUTE_REGEX = Regex("""\[([a-zA-Z\-]+)=['"](.*?)['"]]""")

// Matches the CSS #id shorthand (e.g. "input#oid", "select#state").
// Used as a fallback when [id='value'] is absent.
private val ID_SHORTHAND_REGEX = Regex("""#([^.\[#\s]+)""")

// Extracts the leading tag name from a selector (e.g. "input", "select", "form").
private val TAG_REGEX = Regex("""^([a-zA-Z][a-zA-Z0-9]*)""")

/**
 * Primary implementation of [FillAssistManager].
 */
@Suppress("LongParameterList")
class FillAssistManagerImpl(
    private val fillAssistService: FillAssistService,
    private val fillAssistDiskSource: FillAssistDiskSource,
    private val featureFlagManager: FeatureFlagManager,
    private val serverConfigRepository: ServerConfigRepository,
    private val environmentDiskSource: EnvironmentDiskSource,
    private val clock: Clock,
    dispatcherManager: DispatcherManager,
) : FillAssistManager {

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)
    private val ioScope = CoroutineScope(dispatcherManager.io)
    private var syncJob: Job = Job().apply { complete() }

    init {
        serverConfigRepository.serverConfigStateFlow
            .onEach { config ->
                environmentDiskSource.fillAssistRulesUrl =
                    config?.serverData?.environment?.fillAssistRulesUrl
            }
            .filterNotNull()
            .onEach { syncIfNecessary() }
            .launchIn(unconfinedScope)
    }

    override fun syncIfNecessary() {
        if (!featureFlagManager.getFeatureFlag(FlagKey.FillAssistTargetingRules)) return
        val serverUrl = serverConfigRepository
            .serverConfigStateFlow
            .value
            ?.serverData
            ?.environment
            ?.fillAssistRulesUrl
            ?: return
        val lastFetch = fillAssistDiskSource.getLastFetchTimestamp(serverUrl) ?: 0L
        if (clock.millis() - lastFetch < UPDATE_INTERVAL_MS) return
        if (!syncJob.isCompleted) return
        syncJob = ioScope.launch { sync(serverUrl) }
    }

    private suspend fun sync(serverUrl: String) = runCatching {
        val manifest = fillAssistService.getManifest().getOrThrow()

        val versionEntry = manifest.maps.forms[CURRENT_FORMS_VERSION]
            ?: error("Version $CURRENT_FORMS_VERSION not found in manifest")

        if (versionEntry.deprecated == true) {
            Timber.w("Fill-assist forms $CURRENT_FORMS_VERSION is deprecated")
        }

        if (versionEntry.cid == fillAssistDiskSource.getLastKnownCid(serverUrl)) {
            fillAssistDiskSource.storeLastFetchTimestamp(
                serverUrl = serverUrl,
                timestamp = clock.millis(),
            )
            return@runCatching
        }

        val forms = fillAssistService
            .getForms(filename = versionEntry.filename)
            .getOrThrow()

        val schemaMajor = forms.schemaVersion.substringBefore('.')
        if (schemaMajor != EXPECTED_SCHEMA_MAJOR) {
            Timber.w("Unsupported fill-assist schema version: ${forms.schemaVersion}")
            fillAssistDiskSource.storeLastFetchTimestamp(
                serverUrl = serverUrl,
                timestamp = clock.millis(),
            )
            return@runCatching
        }

        val rules = parseForms(forms)
        fillAssistDiskSource.storeFillAssistRules(serverUrl = serverUrl, rules = rules)
        fillAssistDiskSource.storeLastKnownCid(serverUrl = serverUrl, cid = versionEntry.cid)
        fillAssistDiskSource.storeLastFetchTimestamp(
            serverUrl = serverUrl,
            timestamp = clock.millis(),
        )
    }.also { result ->
        result.onFailure { Timber.w(it, "Fill-assist sync failed") }
    }

    override fun getFillAssistRules(): FillAssistRules? {
        val serverUrl = serverConfigRepository
            .serverConfigStateFlow
            .value
            ?.serverData
            ?.environment
            ?.fillAssistRulesUrl
            ?: return null
        return fillAssistDiskSource.getFillAssistRules(serverUrl = serverUrl)
    }
}

// region CSS parser

private fun parseForms(forms: FillAssistFormsJson): FillAssistRules {
    val hostRules = forms.hosts
        .mapNotNull { (hostname, hostEntry) -> hostEntry?.let { hostname to parseHostEntry(it) } }
        .filter { (_, rules) -> rules.isNotEmpty() }
        .toMap()
    return FillAssistRules(hostRules = hostRules)
}

private fun parseHostEntry(
    hostEntry: FillAssistFormsJson.HostEntryJson,
): List<FillAssistRules.HostRule> {
    val allForms = buildList {
        addAll(hostEntry.forms.orEmpty())
        hostEntry.pathnames?.values?.filterNotNull()?.forEach { addAll(it.forms) }
    }.distinct()

    return buildFieldsByCategory(allForms).map { (category, fields) ->
        FillAssistRules.HostRule(
            category = category,
            fields = fields.mapValues { (_, selectors) -> selectors.distinct() },
        )
    }
}

private fun buildFieldsByCategory(
    forms: List<FillAssistFormsJson.FormJson>,
): Map<String, MutableMap<String, MutableList<SelectorClause>>> {
    val result = mutableMapOf<String, MutableMap<String, MutableList<SelectorClause>>>()
    forms.forEach { form ->
        val parsedFields = form.fields
            .mapValues { (_, elem) -> parseCompositeSelectorArray(elem) }
            .filterValues { it.isNotEmpty() }
            .takeIf { it.isNotEmpty() } ?: return@forEach
        val categoryFields = result.getOrPut(form.category) { mutableMapOf() }
        parsedFields.forEach { (fieldKey, selectors) ->
            categoryFields.getOrPut(fieldKey) { mutableListOf() }.addAll(selectors)
        }
    }
    return result
}

private fun parseCompositeSelectorArray(element: JsonElement): List<SelectorClause> {
    if (element !is JsonArray) return emptyList()
    val result = mutableListOf<SelectorClause>()
    for (item in element) {
        when (item) {
            is JsonPrimitive -> parseSingleSelector(item.content)?.let { result.add(it) }
            is JsonArray -> item
                .filterIsInstance<JsonPrimitive>()
                .mapNotNull { parseSingleSelector(it.content) }
                .forEach { result.add(it) }

            else -> Unit
        }
    }
    return result
}

internal fun parseSingleSelector(selector: String): SelectorClause? {
    // For shadow DOM / iframe selectors (>>>), extract the last segment — the actual target
    // element. Android's autofill framework may expose these elements via htmlInfo when they
    // are reachable (e.g. open shadow roots), so we parse their attributes for matching.
    val effective = if (selector.contains(">>>")) {
        selector.substringAfterLast(">>>").trim()
    } else {
        selector
    }
    if (effective.trimStart().startsWith(".")) return null

    val tag = TAG_REGEX.find(effective)?.groupValues?.get(1)

    var id: String? = null
    var name: String? = null
    var type: String? = null
    var role: String? = null

    ATTRIBUTE_REGEX.findAll(effective).forEach { match ->
        val attrName = match.groupValues[1]
        val attrValue = match.groupValues[2]
        when (attrName) {
            "id" -> id = attrValue
            "name" -> name = attrValue
            "type" -> type = attrValue
            "role" -> role = attrValue
        }
    }

    // Fallback: extract id from #shorthand (e.g. input#oid) when not present as [id='...'].
    if (id == null) {
        id = ID_SHORTHAND_REGEX.find(effective)?.groupValues?.get(1)
    }

    return SelectorClause(tag = tag, id = id, name = name, type = type, role = role)
}

// endregion
