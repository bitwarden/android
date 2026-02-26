@file:OmitFromCoverage

package com.bitwarden.cxf.registry.dsl

import android.app.Application
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.cxf.registry.CredentialExchangeRegistry
import com.bitwarden.cxf.registry.CredentialExchangeRegistryImpl

/**
 * A builder class for constructing an instance of [CredentialExchangeRegistry].
 *
 * This class follows the builder pattern and is designed to be used with the
 * [credentialExchangeRegistry] DSL function. It allows for the configuration of necessary
 * dependencies required by [CredentialExchangeRegistry].
 *
 * Example usage:
 * ```
 * val credentialExchangeRegistry = credentialExchangeRegistry(application = application)
 * ```
 *
 * @see credentialExchangeRegistry
 */
@OmitFromCoverage
class CredentialExchangeRegistryBuilder internal constructor() {
    internal fun build(application: Application): CredentialExchangeRegistry =
        CredentialExchangeRegistryImpl(application = application)
}

/**
 * Creates an instance of [CredentialExchangeRegistry] using the [CredentialExchangeRegistryBuilder]
 * DSL.
 *
 * This function provides a convenient way to configure and build a [CredentialExchangeRegistry].
 *
 * @param builder A lambda with a receiver of type [CredentialExchangeRegistryBuilder] to configure
 * the manager.
 *
 * @return A new instance of [CredentialExchangeRegistry].
 * @see CredentialExchangeRegistryBuilder
 */
fun credentialExchangeRegistry(
    application: Application,
    builder: CredentialExchangeRegistryBuilder.() -> Unit = { },
): CredentialExchangeRegistry = CredentialExchangeRegistryBuilder()
    .apply(builder)
    .build(application)
