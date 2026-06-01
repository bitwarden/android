@file:OmitFromCoverage

package com.bitwarden.cxf.manager.dsl

import android.app.Activity
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.cxf.manager.CredentialExchangeCompletionManager
import com.bitwarden.cxf.manager.CredentialExchangeCompletionManagerImpl
import java.time.Clock

/**
 * A DSL for building a [CredentialExchangeCompletionManager].
 *
 * This class provides a structured way to configure and create an instance of
 * [CredentialExchangeCompletionManager], which is used to finalize the credential
 * exchange process by returning a result to the calling application. It is primarily
 * used within the [credentialExchangeCompletionManager] builder function.
 *
 */
@OmitFromCoverage
class CredentialExchangeCompletionManagerBuilder internal constructor() {
    /**
     * The relying party ID of the credential exporter.
     */
    lateinit var exporterRpId: String

    /**
     * The display name of the credential exporter.
     */
    lateinit var exporterDisplayName: String

    /**
     * Constructs a [CredentialExchangeCompletionManager] instance with the configured properties.
     *
     * This function is internal and called by the [credentialExchangeCompletionManager] builder
     * after the [CredentialExchangeCompletionManagerBuilder] has been configured.
     *
     * @param activity The [Activity] that initiated the credential exchange operation.
     * @return An initialized [CredentialExchangeCompletionManager] ready to complete the flow.
     */
    internal fun build(activity: Activity, clock: Clock): CredentialExchangeCompletionManager =
        CredentialExchangeCompletionManagerImpl(
            activity = activity,
            clock = clock,
        )
}

/**
 * Creates an instance of [CredentialExchangeCompletionManager] using a DSL-style builder.
 *
 * This function is the entry point for handling the completion of a credential exchange flow,
 * such as after a user has successfully selected items to export.
 *
 * Example usage:
 * ```
 * val completionManager = credentialExchangeCompletionManager(activity) {
 *     exporterRpId = "example.com"
 *     exporterDisplayName = "Example"
 * }
 *
 * // Use the completionManager to finish the credential exchange.
 * completionManager.completeCredentialExport(...)
 * ```
 *
 * @param activity The [Activity] that initiated the credential exchange operation. This is
 * used to send back the result to the calling application (e.g., the browser).
 * @param config A lambda with [CredentialExchangeCompletionManagerBuilder] as its receiver,
 * allowing for declarative configuration of the manager.
 *
 * @return A configured [CredentialExchangeCompletionManager] instance.
 */
fun credentialExchangeCompletionManager(
    activity: Activity,
    clock: Clock,
    config: CredentialExchangeCompletionManagerBuilder.() -> Unit,
): CredentialExchangeCompletionManager =
    CredentialExchangeCompletionManagerBuilder()
        .apply(config)
        .build(
            activity = activity,
            clock = clock,
        )
