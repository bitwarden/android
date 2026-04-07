@file:OmitFromCoverage

package com.bitwarden.cxf.validator.dsl

import android.app.Activity
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.cxf.validator.CredentialExchangeRequestValidator
import com.bitwarden.cxf.validator.CredentialExchangeRequestValidatorImpl

/**
 * A builder class for constructing an instance of [CredentialExchangeRequestValidator].
 *
 * This class follows the builder pattern and is designed to be used with the
 * [credentialExchangeRequestValidator] DSL function. It allows for the configuration of necessary
 * dependencies required by [CredentialExchangeRequestValidator].
 *
 * Example usage:
 * ```
 * val requestValidator = credentialExchangeRequestValidator(activity = activity)
 * ```
 *
 * @see credentialExchangeRequestValidator
 */
@OmitFromCoverage
class CredentialExchangeRequestValidatorBuilder internal constructor() {
    internal fun build(activity: Activity): CredentialExchangeRequestValidator =
        CredentialExchangeRequestValidatorImpl(activity = activity)
}

/**
 * Creates an instance of [CredentialExchangeRequestValidator] using the
 * [CredentialExchangeRequestValidatorBuilder] DSL.
 *
 * This function provides a convenient way to configure and build a
 * [CredentialExchangeRequestValidator].
 *
 * @param activity The [Activity] that is handling the request.
 * @param config A lambda with a receiver of type [CredentialExchangeRequestValidatorBuilder] to
 * configure the validator.
 *
 * @return A new instance of [CredentialExchangeRequestValidator].
 * @see CredentialExchangeRequestValidatorBuilder
 */
fun credentialExchangeRequestValidator(
    activity: Activity,
    config: CredentialExchangeRequestValidatorBuilder.() -> Unit = { },
): CredentialExchangeRequestValidator = CredentialExchangeRequestValidatorBuilder()
    .apply(config)
    .build(activity)
