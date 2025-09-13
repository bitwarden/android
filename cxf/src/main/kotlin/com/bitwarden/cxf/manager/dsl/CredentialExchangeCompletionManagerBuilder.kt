@file:OmitFromCoverage

package com.bitwarden.cxf.manager.dsl

import android.app.Activity
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.cxf.manager.CredentialExchangeCompletionManager
import com.bitwarden.cxf.manager.CredentialExchangeCompletionManagerImpl

/**
 * A DSL for building a [CredentialExchangeCompletionManager].
 *
 * This class provides a structured way to configure and create an instance of
 * [CredentialExchangeCompletionManager], which is used to finalize the credential
 * exchange process by returning a result to the calling application. It is primarily
 * used within the [credentialExchangeCompletionManager] builder function.
 *
 * @property activity The activity that will handle the completion of the credential exchange.
 */
@OmitFromCoverage
class CredentialExchangeCompletionManagerBuilder
internal constructor(private val activity: Activity) {
    internal fun build(): CredentialExchangeCompletionManager =
        CredentialExchangeCompletionManagerImpl(activity = activity)
}

/**
 * Creates an instance of [CredentialExchangeCompletionManager] using a DSL-style builder.
 *
 * This function is the entry point for handling the completion of a credential exchange flow,
 * such as after a user has successfully created or selected a passkey.
 *
 * Example usage:
 * ```
 * val completionManager = credentialExchangeCompletionManager(activity) {
 *     // Configuration options can be added here if the DSL is extended in the future.
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
    config: CredentialExchangeCompletionManagerBuilder.() -> Unit = {},
): CredentialExchangeCompletionManager =
    CredentialExchangeCompletionManagerBuilder(activity = activity)
        .apply(config)
        .build()
