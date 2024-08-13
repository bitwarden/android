package com.x8bit.bitwarden.data.platform.datasource.disk

import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import kotlinx.coroutines.flow.Flow

/**
 * Primary access point for general environment-related disk information.
 */
interface EnvironmentDiskSource {
    /**
     * The currently persisted [EnvironmentUrlDataJson] (or `null` if not set).
     */
    var preAuthEnvironmentUrlData: EnvironmentUrlDataJson?

    /**
     * Emits updates that track [preAuthEnvironmentUrlData]. This will replay the last known value,
     * if any.
     */
    val preAuthEnvironmentUrlDataFlow: Flow<EnvironmentUrlDataJson?>

    /**
     * Gets the pre authentication urls for the given [userEmail].
     */
    fun getPreAuthEnvironmentUrlDataForEmail(userEmail: String): EnvironmentUrlDataJson?

    /**
     * Stores the [urls] for the given [userEmail].
     */
    fun storePreAuthEnvironmentUrlDataForEmail(userEmail: String, urls: EnvironmentUrlDataJson)
}
