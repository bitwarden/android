package com.x8bit.bitwarden.data.platform.repository

import android.security.KeyChainAliasCallback

/**
 * Callback for [KeyChainRepositoryImpl.choosePrivateKeyAlias].
 */
class ChoosePrivateKeyAliasCallbackImpl constructor(val callback: (String?) -> Unit) :
    ChoosePrivateKeyAliasCallback {
    /**
     * Returns the callback.
     */
    override fun getCallback(): KeyChainAliasCallback {
        return KeyChainAliasCallback {
            callback(it)
        }
    }
}
