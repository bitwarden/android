package com.x8bit.bitwarden.ui.platform.manager.intent

import android.app.PendingIntent
import android.content.Context

/**
 * A manager interface for handling intents related to credential management.
 */
interface CredentialManagerIntentManager {
    /**
     * Creates a [PendingIntent] for FIDO2 credential creation.
     *
     * @param action The action to be performed by the intent.
     * @param userId The ID of the user for whom the credential is being created.
     * @param requestCode The request code to be used with the pending intent.
     * @return The created [PendingIntent].
     */
    fun createFido2CreationPendingIntent(
        action: String,
        userId: String,
        requestCode: Int,
    ): PendingIntent

    /**
     * Creates a [PendingIntent] for retrieving a FIDO2 credential.
     *
     * @param action The action to be performed by the intent.
     * @param userId The ID of the user whose credential is being retrieved.
     * @param credentialId The ID of the credential to be retrieved.
     * @param cipherId The ID of the cipher associated with the credential.
     * @param isUserVerified Indicates whether the user has been verified.
     * @param requestCode The request code to be used with the pending intent.
     * @return The created [PendingIntent].
     */
    @Suppress("LongParameterList")
    fun createFido2GetCredentialPendingIntent(
        action: String,
        userId: String,
        credentialId: String,
        cipherId: String,
        isUserVerified: Boolean,
        requestCode: Int,
    ): PendingIntent

    /**
     * Creates a [PendingIntent] for unlocking a FIDO2 credential.
     *
     * @param action The action to be performed by the intent.
     * @param userId The ID of the user whose credential is being unlocked.
     * @param requestCode The request code to be used with the pending intent.
     * @return The created [PendingIntent].
     */
    fun createFido2UnlockPendingIntent(
        action: String,
        userId: String,
        requestCode: Int,
    ): PendingIntent

    /**
     * Creates a [PendingIntent] for retrieving a password credential.
     * @param action The action to be performed by the intent.
     * @param userId The ID of the user whose credential is being retrieved.
     * @param cipherId The ID of the cipher associated with the credential, or `null` if not
     * applicable.
     * @param isUserVerified Indicates whether the user has been verified.
     * @param requestCode The request code to be used with the pending intent.
     * @return The created [PendingIntent].
     */
    fun createPasswordGetCredentialPendingIntent(
        action: String,
        userId: String,
        cipherId: String?,
        isUserVerified: Boolean,
        requestCode: Int,
    ): PendingIntent

    /**
     * Starts the credential manager settings.
     */
    fun startCredentialManagerSettings(context: Context)
}
