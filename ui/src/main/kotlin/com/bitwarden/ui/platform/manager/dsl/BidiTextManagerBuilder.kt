@file:OmitFromCoverage

package com.bitwarden.ui.platform.manager.dsl

import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.manager.BidiTextManager
import com.bitwarden.ui.platform.manager.BidiTextManagerImpl

/**
 * A builder class for constructing an instance of [BidiTextManager].
 *
 * This class follows the builder pattern and is designed to be used with the
 * [bidiTextManager] DSL function. It allows for the configuration of necessary
 * dependencies required by [BidiTextManager].
 *
 * Example usage:
 * ```
 * val bidiTextManager = bidiTextManager()
 * ```
 *
 * @see bidiTextManager
 */
@OmitFromCoverage
class BidiTextManagerBuilder internal constructor() {
    internal fun build(): BidiTextManager = BidiTextManagerImpl()
}

/**
 * Creates an instance of [BidiTextManager] using the [BidiTextManagerBuilder] DSL.
 *
 * This function provides a convenient way to configure and build a [BidiTextManager].
 *
 * @param builder A lambda with a receiver of type [BidiTextManagerBuilder] to configure
 * the manager.
 *
 * @return A new instance of [BidiTextManager].
 * @see BidiTextManagerBuilder
 */
fun bidiTextManager(
    builder: BidiTextManagerBuilder.() -> Unit = { },
): BidiTextManager = BidiTextManagerBuilder()
    .apply(builder)
    .build()
