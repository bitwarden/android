package com.x8bit.bitwarden.data.auth.repository.util

import com.bitwarden.crypto.Kdf
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.util.KdfParamsConstants

/**
 * Convert [AccountJson.Profile] to [Kdf] params for use with Bitwarden SDK.
 */
fun AccountJson.Profile.toSdkParams(): Kdf {
    return when (this.kdfType) {
        KdfTypeJson.ARGON2_ID -> Kdf.Argon2id(
            iterations = (kdfIterations ?: KdfParamsConstants.DEFAULT_ARGON2_ITERATIONS).toUInt(),
            memory = (kdfMemory ?: KdfParamsConstants.DEFAULT_ARGON2_MEMORY).toUInt(),
            parallelism =
            (kdfParallelism ?: KdfParamsConstants.DEFAULT_ARGON2_PARALLELISM).toUInt(),
        )

        KdfTypeJson.PBKDF2_SHA256 -> Kdf.Pbkdf2(
            iterations = (kdfIterations ?: KdfParamsConstants.DEFAULT_PBKDF2_ITERATIONS).toUInt(),
        )

        else -> Kdf.Pbkdf2(iterations = KdfParamsConstants.DEFAULT_PBKDF2_ITERATIONS.toUInt())
    }
}
