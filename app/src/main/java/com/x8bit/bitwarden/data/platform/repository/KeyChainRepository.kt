package com.x8bit.bitwarden.data.platform.repository

import android.app.Activity
import java.security.PrivateKey
import java.security.cert.X509Certificate

interface KeyChainRepository {
    fun choosePrivateKeyAlias(activity: Activity, callback: ChoosePrivateKeyAliasCallback)

    fun getPrivateKey(): PrivateKey?

    fun getCertificateChain(): Array<X509Certificate>?
}