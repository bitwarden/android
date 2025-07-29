package com.x8bit.bitwarden.ui.platform.manager.intent

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.credentials.CredentialManager
import com.x8bit.bitwarden.data.autofill.util.toPendingIntentMutabilityFlag

/**
 * Key for the user id included in Credential provider "create entries".
 *
 * @see IntentManager.createFido2CreationPendingIntent
 */
const val EXTRA_KEY_USER_ID: String = "user_id"

/**
 * Key for the credential id included in FIDO 2 provider "get entries".
 *
 * @see IntentManager.createFido2GetCredentialPendingIntent
 */
const val EXTRA_KEY_CREDENTIAL_ID: String = "credential_id"

/**
 * Key for the cipher id included in FIDO 2 provider "get entries".
 *
 * @see IntentManager.createFido2GetCredentialPendingIntent
 */
const val EXTRA_KEY_CIPHER_ID: String = "cipher_id"

/**
 * Key for the user verification performed during vault unlock while
 * processing a Credential request.
 */
const val EXTRA_KEY_UV_PERFORMED_DURING_UNLOCK: String = "uv_performed_during_unlock"

/**
 * Primary implementation of [CredentialManagerIntentManager] that provides methods to create
 * various [PendingIntent]s for credential management actions.
 */
@Suppress("CommentWrapping")
class CredentialManagerIntentManagerImpl(
    private val context: Context,
) : CredentialManagerIntentManager {
    /**
     * Creates a [PendingIntent] for FIDO2 credential creation.
     *
     * @param action The action to be performed by the intent.
     * @param userId The ID of the user for whom the credential is being created.
     * @param requestCode The request code to be used with the pending intent.
     * @return The created [PendingIntent].
     */
    override fun createFido2CreationPendingIntent(
        action: String,
        userId: String,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(action)
            .setPackage(context.packageName)
            .putExtra(EXTRA_KEY_USER_ID, userId)

        return PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ requestCode,
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT.toPendingIntentMutabilityFlag(),
        )
    }

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
    override fun createFido2GetCredentialPendingIntent(
        action: String,
        userId: String,
        credentialId: String,
        cipherId: String,
        isUserVerified: Boolean,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(action)
            .setPackage(context.packageName)
            .putExtra(EXTRA_KEY_USER_ID, userId)
            .putExtra(EXTRA_KEY_CREDENTIAL_ID, credentialId)
            .putExtra(EXTRA_KEY_CIPHER_ID, cipherId)
            .putExtra(EXTRA_KEY_UV_PERFORMED_DURING_UNLOCK, isUserVerified)

        return PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ requestCode,
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT.toPendingIntentMutabilityFlag(),
        )
    }

    /**
     * Creates a [PendingIntent] for unlocking a FIDO2 credential.
     *
     * @param action The action to be performed by the intent.
     * @param userId The ID of the user whose credential is being unlocked.
     * @param requestCode The request code to be used with the pending intent.
     * @return The created [PendingIntent].
     */
    override fun createFido2UnlockPendingIntent(
        action: String,
        userId: String,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(action)
            .setPackage(context.packageName)
            .putExtra(EXTRA_KEY_USER_ID, userId)

        return PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ requestCode,
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT.toPendingIntentMutabilityFlag(),
        )
    }

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
    override fun createPasswordGetCredentialPendingIntent(
        action: String,
        userId: String,
        cipherId: String?,
        isUserVerified: Boolean,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(action)
            .setPackage(context.packageName)
            .putExtra(EXTRA_KEY_USER_ID, userId)
            .putExtra(EXTRA_KEY_CIPHER_ID, cipherId)
            .putExtra(EXTRA_KEY_UV_PERFORMED_DURING_UNLOCK, isUserVerified)

        return PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ requestCode,
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT.toPendingIntentMutabilityFlag(),
        )
    }

    /**
     * Starts the credential manager settings.
     */
    override fun startCredentialManagerSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            CredentialManager.create(context).createSettingsPendingIntent().send()
        }
    }
}
