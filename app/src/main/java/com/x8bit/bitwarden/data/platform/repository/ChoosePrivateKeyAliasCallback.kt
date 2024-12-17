package com.x8bit.bitwarden.data.platform.repository

import android.security.KeyChainAliasCallback

interface ChoosePrivateKeyAliasCallback {
    fun getCallback(): KeyChainAliasCallback
}