package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of getting the user fingerprint.
 */
sealed class UserFingerprintResult {
    /**
     * Contains the user fingerprint.
     */
    data class Success(
        val fingerprint: String,
    ) : UserFingerprintResult()

    /**
     * There was an error getting the user fingerprint.
     */
    data object Error : UserFingerprintResult()
}
