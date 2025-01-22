package com.x8bit.bitwarden.data.platform.repository

import android.security.KeyChainAliasCallback

/**
 * Callback for [KeyChainRepositoryImpl.choosePrivateKeyAlias].
 */
interface ChoosePrivateKeyAliasCallback {
    /**
     * Returns the callback.
     */
    fun getCallback(): KeyChainAliasCallback
}
