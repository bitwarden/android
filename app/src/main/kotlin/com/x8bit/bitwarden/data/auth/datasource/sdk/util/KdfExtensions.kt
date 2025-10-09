package com.x8bit.bitwarden.data.auth.datasource.sdk.util

import com.bitwarden.crypto.Kdf
import com.bitwarden.network.model.KdfJson
import com.bitwarden.network.model.KdfTypeJson
import com.bitwarden.network.model.KdfTypeJson.ARGON2_ID
import com.bitwarden.network.model.KdfTypeJson.PBKDF2_SHA256

/**
 * Convert a [Kdf] to a [KdfTypeJson].
 */
fun Kdf.toKdfTypeJson(): KdfTypeJson =
    when (this) {
        is Kdf.Argon2id -> ARGON2_ID
        is Kdf.Pbkdf2 -> PBKDF2_SHA256
    }

/**
 * Convert a [Kdf] to [KdfJson]
 */
fun Kdf.toKdfRequestModel(): KdfJson =
    when (this) {
        is Kdf.Argon2id -> KdfJson(
            kdfType = toKdfTypeJson(),
            iterations = iterations.toInt(),
            memory = memory.toInt(),
            parallelism = parallelism.toInt(),
        )

        is Kdf.Pbkdf2 -> KdfJson(
            kdfType = toKdfTypeJson(),
            iterations = iterations.toInt(),
            memory = null,
            parallelism = null,
        )
    }
