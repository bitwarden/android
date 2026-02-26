@file:OmitFromCoverage

package com.bitwarden.cxf.importer.dsl

import android.app.Activity
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.cxf.importer.CredentialExchangeImporter
import com.bitwarden.cxf.importer.CredentialExchangeImporterImpl

/**
 * A builder class for constructing an instance of [CredentialExchangeImporter].
 *
 * This builder is invoked within the [credentialExchangeImporter] function to configure and create
 * the importer. It is not intended to be instantiated directly.
 */
@OmitFromCoverage
class CredentialExchangeImporterBuilder internal constructor() {
    internal fun build(activity: Activity): CredentialExchangeImporter =
        CredentialExchangeImporterImpl(activity = activity)
}

/**
 * Creates an instance of [CredentialExchangeImporter] using the provided [activity]
 * and an optional [config] lambda to configure the importer.
 *
 * This function acts as a DSL entry point for building a [CredentialExchangeImporter].
 *
 * Example usage:
 * ```kotlin
 * val importer = credentialExchangeImporter(activity) {
 *     // Configuration options for the builder can be set here if any are added in the future.
 *     logsManager = myLogsManager // Optional: pass your LogsManager instance
 * }
 * importer.importCredentials()
 * ```
 *
 * @param activity The Android [Activity] triggering the import process.
 * @param config A lambda with [CredentialExchangeImporterBuilder] as its receiver,
 * allowing for declarative configuration of the importer.
 * @return A fully configured instance of [CredentialExchangeImporter].
 */
fun credentialExchangeImporter(
    activity: Activity,
    config: CredentialExchangeImporterBuilder.() -> Unit = {},
): CredentialExchangeImporter = CredentialExchangeImporterBuilder()
    .apply(config)
    .build(activity = activity)
