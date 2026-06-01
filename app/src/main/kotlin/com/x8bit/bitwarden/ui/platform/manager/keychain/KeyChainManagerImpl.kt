package com.x8bit.bitwarden.ui.platform.manager.keychain

import android.app.Activity
import android.security.KeyChain
import androidx.core.net.toUri
import com.x8bit.bitwarden.ui.platform.manager.keychain.model.PrivateKeyAliasSelectionResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first

/**
 * Default implementation of [KeyChainManager].
 */
class KeyChainManagerImpl(
    private val activity: Activity,
) : KeyChainManager {

    override suspend fun choosePrivateKeyAlias(
        currentServerUrl: String?,
    ): PrivateKeyAliasSelectionResult =
        callbackFlow {
            try {
                KeyChain.choosePrivateKeyAlias(
                    activity,
                    { alias ->
                        trySend(PrivateKeyAliasSelectionResult.Success(alias))
                        close()
                    },
                    null,
                    null,
                    currentServerUrl?.toUri(),
                    null,
                )
            } catch (_: IllegalArgumentException) {
                trySend(PrivateKeyAliasSelectionResult.Error)
                close()
            }

            awaitClose()
        }
            .first()
}
