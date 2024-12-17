package com.x8bit.bitwarden.data.platform.repository

import android.security.KeyChainAliasCallback

class ChoosePrivateKeyAliasCallbackImpl constructor(val callback: (String?) -> Unit) :
    ChoosePrivateKeyAliasCallback {
    override fun getCallback(): KeyChainAliasCallback {
        return KeyChainAliasCallback {
            callback(it)
        }
    }
}