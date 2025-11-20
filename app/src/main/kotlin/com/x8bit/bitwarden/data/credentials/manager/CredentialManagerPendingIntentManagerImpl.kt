package com.x8bit.bitwarden.data.credentials.manager

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.util.toPendingIntentMutabilityFlag
import kotlin.random.Random

/**
 * Primary implementation of [CredentialManagerPendingIntentManager].
 */
@OmitFromCoverage
class CredentialManagerPendingIntentManagerImpl(
    private val context: Context,
) : CredentialManagerPendingIntentManager {

    /**
     * Creates a pending intent to use when providing options for FIDO 2 credential creation.
     */
    override fun createFido2CreationPendingIntent(
        userId: String,
    ): PendingIntent {
        val intent = Intent(CREATE_PASSKEY_ACTION)
            .setPackage(context.packageName)
            .putExtra(EXTRA_KEY_USER_ID, userId)

        return PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ Random.nextInt(),
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT.toPendingIntentMutabilityFlag(),
        )
    }

    /**
     * Creates a pending intent to use when providing options for FIDO 2 credential filling.
     */
    override fun createFido2GetCredentialPendingIntent(
        userId: String,
        credentialId: String,
        cipherId: String,
        isUserVerified: Boolean,
    ): PendingIntent {
        val intent = Intent(GET_PASSKEY_ACTION)
            .setPackage(context.packageName)
            .putExtra(EXTRA_KEY_USER_ID, userId)
            .putExtra(EXTRA_KEY_CREDENTIAL_ID, credentialId)
            .putExtra(EXTRA_KEY_CIPHER_ID, cipherId)
            .putExtra(EXTRA_KEY_UV_PERFORMED_DURING_UNLOCK, isUserVerified)

        return PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ Random.nextInt(),
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT.toPendingIntentMutabilityFlag(),
        )
    }

    /**
     * Creates a pending intent to use when providing unlock options for FIDO 2 credential filling.
     */
    override fun createFido2UnlockPendingIntent(
        userId: String,
    ): PendingIntent {
        val intent = Intent(UNLOCK_ACCOUNT_ACTION)
            .setPackage(context.packageName)
            .putExtra(EXTRA_KEY_USER_ID, userId)

        return PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ Random.nextInt(),
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT.toPendingIntentMutabilityFlag(),
        )
    }

    /**
     * Creates a pending intent to use when providing options for FIDO 2 credential creation.
     */
    override fun createPasswordCreationPendingIntent(
        userId: String,
    ): PendingIntent {
        val intent = Intent(CREATE_PASSWORD_ACTION)
            .setPackage(context.packageName)
            .putExtra(EXTRA_KEY_USER_ID, userId)

        return PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ Random.nextInt(),
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT.toPendingIntentMutabilityFlag(),
        )
    }

    /**
     * Creates a pending intent to use when providing options for Password credential filling.
     */
    override fun createPasswordGetCredentialPendingIntent(
        userId: String,
        cipherId: String?,
        isUserVerified: Boolean,
    ): PendingIntent {
        val intent = Intent(GET_PASSWORD_ACTION)
            .setPackage(context.packageName)
            .putExtra(EXTRA_KEY_USER_ID, userId)
            .putExtra(EXTRA_KEY_CIPHER_ID, cipherId)
            .putExtra(EXTRA_KEY_UV_PERFORMED_DURING_UNLOCK, isUserVerified)

        return PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ Random.nextInt(),
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT.toPendingIntentMutabilityFlag(),
        )
    }
}

private const val CREATE_PASSKEY_ACTION = "com.x8bit.bitwarden.credentials.ACTION_CREATE_PASSKEY"
private const val UNLOCK_ACCOUNT_ACTION = "com.x8bit.bitwarden.credentials.ACTION_UNLOCK_ACCOUNT"
private const val GET_PASSKEY_ACTION = "com.x8bit.bitwarden.credentials.ACTION_GET_PASSKEY"
private const val CREATE_PASSWORD_ACTION = "com.x8bit.bitwarden.credentials.ACTION_CREATE_PASSWORD"
private const val GET_PASSWORD_ACTION = "com.x8bit.bitwarden.credentials.ACTION_GET_PASSWORD"
