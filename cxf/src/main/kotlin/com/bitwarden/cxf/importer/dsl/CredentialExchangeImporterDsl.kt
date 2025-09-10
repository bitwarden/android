@file:OmitFromCoverage

package com.bitwarden.cxf.importer.dsl

import android.content.Context
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.cxf.importer.CredentialExchangeImporter
import com.bitwarden.cxf.importer.CredentialExchangeImporterImpl

/**
 * Creates an instance of [CredentialExchangeImporter] using the provided [activityContext]
 * and an optional [config] lambda to configure the importer.
 *
 * This function acts as a DSL entry point for building a [CredentialExchangeImporter].
 *
 * Example usage:
 * ```kotlin
 * val importer = credentialExchangeImporter(context) {
 *     // Configuration options for the builder can be set here if any are added in the future.
 * }
 * importer.importCredentials()
 * ```
 *
 * @param activityContext The Android [Context] required for the import process,
 * typically from an Activity.
 * @param config A lambda with [CredentialExchangeImporterBuilder] as its receiver,
 * allowing for declarative configuration of the importer.
 * @return A fully configured instance of [CredentialExchangeImporter].
 */
fun credentialExchangeImporter(
    activityContext: Context,
    config: CredentialExchangeImporterBuilder.() -> Unit = {},
): CredentialExchangeImporter = CredentialExchangeImporterBuilder(activityContext)
    .apply(config)
    .build()

/**
 * A builder class for constructing an instance of [CredentialExchangeImporter].
 *
 * This builder is invoked within the [credentialExchangeImporter] function to configure and create
 * the importer. It is not intended to be instantiated directly.
 *
 * @param activityContext The Android [Context] required for the import process.
 */
@OmitFromCoverage
class CredentialExchangeImporterBuilder
internal constructor(private val activityContext: Context) {
    internal fun build(): CredentialExchangeImporter =
        CredentialExchangeImporterImpl(
            activityContext = activityContext,
        )
}
