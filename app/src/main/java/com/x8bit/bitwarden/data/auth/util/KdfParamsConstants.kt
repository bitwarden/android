package com.x8bit.bitwarden.data.auth.util

import com.bitwarden.crypto.Kdf
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * Constants relating to [Kdf] initialization defaults.
 */
@OmitFromCoverage
object KdfParamsConstants {

    /**
     * The default number of iterations when calculating a user's password for [Kdf.Pbkdf2].
     */
    const val DEFAULT_PBKDF2_ITERATIONS: Int = 600000

    /**
     * The default number of iterations when calculating a user's password for [Kdf.Argon2id].
     */
    const val DEFAULT_ARGON2_ITERATIONS: Int = 3

    /**
     * The default amount of memory to use when calculating a password hash (MB) for [Kdf.Argon2id].
     */
    const val DEFAULT_ARGON2_MEMORY: Int = 64

    /**
     * The default number of threads to use when calculating a password hash for [Kdf.Argon2id].
     */
    const val DEFAULT_ARGON2_PARALLELISM: Int = 4
}
