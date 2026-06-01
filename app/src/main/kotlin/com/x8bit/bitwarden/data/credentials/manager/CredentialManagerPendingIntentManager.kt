package com.x8bit.bitwarden.data.credentials.manager

import android.app.PendingIntent

/**
 * Key for the user id included in Credential provider "create entries".
 *
 * @see CredentialManagerPendingIntentManager.createFido2CreationPendingIntent
 */
const val EXTRA_KEY_USER_ID: String = "user_id"

/**
 * Key for the credential id included in FIDO 2 provider "get entries".
 *
 * @see CredentialManagerPendingIntentManager.createFido2GetCredentialPendingIntent
 */
const val EXTRA_KEY_CREDENTIAL_ID: String = "credential_id"

/**
 * Key for the cipher id included in FIDO 2 provider "get entries".
 *
 * @see CredentialManagerPendingIntentManager.createFido2GetCredentialPendingIntent
 */
const val EXTRA_KEY_CIPHER_ID: String = "cipher_id"

/**
 * Key for the user verification performed during vault unlock while
 * processing a Credential request.
 */
const val EXTRA_KEY_UV_PERFORMED_DURING_UNLOCK: String = "uv_performed_during_unlock"

/**
 * A manager class for creating pending intents used in credential management operations.
 */
interface CredentialManagerPendingIntentManager {

    /**
     * Creates a pending intent to use when providing options for FIDO 2 credential creation.
     */
    fun createFido2CreationPendingIntent(
        userId: String,
    ): PendingIntent

    /**
     * Creates a pending intent to use when providing options for FIDO 2 credential filling.
     */
    fun createFido2GetCredentialPendingIntent(
        userId: String,
        credentialId: String,
        cipherId: String,
        isUserVerified: Boolean,
    ): PendingIntent

    /**
     * Creates a pending intent to use when providing unlock options for FIDO 2 credential filling.
     */
    fun createFido2UnlockPendingIntent(
        userId: String,
    ): PendingIntent

    /**
     * Creates a pending intent to use when providing options for Password credential creation.
     */
    fun createPasswordCreationPendingIntent(
        userId: String,
    ): PendingIntent

    /**
     * Creates a pending intent to use when providing options for Password credential filling.
     */
    fun createPasswordGetCredentialPendingIntent(
        userId: String,
        cipherId: String?,
        isUserVerified: Boolean,
    ): PendingIntent
}
