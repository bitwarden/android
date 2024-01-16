package com.x8bit.bitwarden.data.auth.datasource.sdk.util

import com.bitwarden.crypto.Kdf
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson.ARGON2_ID
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson.PBKDF2_SHA256

/**
 * Convert a [Kdf] to a [KdfTypeJson].
 */
fun Kdf.toKdfTypeJson(): KdfTypeJson =
    when (this) {
        is Kdf.Argon2id -> ARGON2_ID
        is Kdf.Pbkdf2 -> PBKDF2_SHA256
    }
