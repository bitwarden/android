package com.x8bit.bitwarden.data.auth.util

import com.bitwarden.crypto.Kdf
import com.x8bit.bitwarden.data.auth.datasource.network.model.PreLoginResponseJson

/**
 * Convert [PreLoginResponseJson.KdfParams] to [Kdf] params for use with Bitwarden SDK.
 */
fun PreLoginResponseJson.KdfParams.toSdkParams(): Kdf = when (this) {
    is PreLoginResponseJson.KdfParams.Argon2ID -> {
        Kdf.Argon2id(
            iterations = this.iterations,
            memory = this.memory,
            parallelism = this.parallelism,
        )
    }

    is PreLoginResponseJson.KdfParams.Pbkdf2 -> {
        Kdf.Pbkdf2(
            iterations = this.iterations,
        )
    }
}
