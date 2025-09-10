@file:OmitFromCoverage

package com.bitwarden.cxf.registry.dsl

import android.content.Context
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.cxf.registry.CredentialExchangeRegistry
import com.bitwarden.cxf.registry.CredentialExchangeRegistryImpl

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
    context: Context,
    builder: CredentialExchangeRegistryBuilder.() -> Unit = { },
): CredentialExchangeRegistry = CredentialExchangeRegistryBuilder(applicationContext = context)
    .apply(builder)
    .build()

/**
 * A builder class for constructing an instance of [CredentialExchangeRegistry].
 *
 * This class follows the builder pattern and is designed to be used with the
 * [credentialExchangeRegistry] DSL function. It allows for the configuration of necessary
 * dependencies required by [CredentialExchangeRegistry].
 *
 * Example usage:
 * ```
 * val credentialExchangeRegistry = credentialExchangeRegistry(applicationContext = context)
 * ```
 *
 *  @property applicationContext The application context required for building the manager.
 *
 * @see credentialExchangeRegistry
 */
@OmitFromCoverage
class CredentialExchangeRegistryBuilder
internal constructor(private val applicationContext: Context) {

    internal fun build(): CredentialExchangeRegistry =
        CredentialExchangeRegistryImpl(applicationContext)
}
