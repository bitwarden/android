package com.x8bit.bitwarden.data.platform.repository

import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSource
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.toEnvironmentUrls
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Primary implementation of [EnvironmentRepository].
 */
class EnvironmentRepositoryImpl(
    private val environmentDiskSource: EnvironmentDiskSource,
    private val dispatcher: CoroutineDispatcher,
) : EnvironmentRepository {

    private val scope = CoroutineScope(dispatcher)

    override var environment: Environment
        get() = environmentDiskSource
            .preAuthEnvironmentUrlData
            .toEnvironmentUrlsOrDefault()
        set(value) {
            environmentDiskSource.preAuthEnvironmentUrlData = value.environmentUrlData
        }

    override val environmentStateFlow: StateFlow<Environment>
        get() = environmentDiskSource
            .preAuthEnvironmentUrlDataFlow
            .map { it.toEnvironmentUrlsOrDefault() }
            .stateIn(
                scope = scope,
                started = SharingStarted.Lazily,
                initialValue = Environment.Us,
            )
}

/**
 * Converts a nullable [EnvironmentUrlDataJson] to an [Environment], where `null` values default to
 * the US environment.
 */
private fun EnvironmentUrlDataJson?.toEnvironmentUrlsOrDefault(): Environment =
    this?.toEnvironmentUrls() ?: Environment.Us
