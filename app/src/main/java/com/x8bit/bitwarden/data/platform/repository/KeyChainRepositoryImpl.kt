package com.x8bit.bitwarden.data.platform.repository

import android.app.Activity
import android.security.KeyChain
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSource
import com.x8bit.bitwarden.data.platform.repository.util.toEnvironmentUrlsOrDefault
import timber.log.Timber
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.inject.Inject

class KeyChainRepositoryImpl @Inject constructor(
    environmentDiskSource: EnvironmentDiskSource,
) : KeyChainRepository {
    private var activity: Activity? = null
    private var alias: String? = null
    private var key: PrivateKey? = null
    private var chain: Array<X509Certificate>? = null

    init {
        alias =
            environmentDiskSource.preAuthEnvironmentUrlData.toEnvironmentUrlsOrDefault().environmentUrlData.keyAlias
    }

    override fun configure(activity: Activity) {
        this.activity = activity
    }

    override fun choosePrivateKeyAlias(callback: ChoosePrivateKeyAliasCallback) {
        if (activity == null) {
            Timber.tag(this.javaClass.name)
                .d("activity is not set yet -- trying to choose private key alias")
            return
        }

        KeyChain.choosePrivateKeyAlias(activity!!, { a ->
            callback.getCallback().alias(a)
            alias = a
        }, null, null, null, alias)
    }

    override fun getPrivateKey(): PrivateKey? {
        if (key == null && activity != null && !alias.isNullOrEmpty()) {
            key = try {
                KeyChain.getPrivateKey(activity!!, alias!!)
            } catch (e: Exception) {
                null
            }
        }

        return key
    }

    override fun getCertificateChain(): Array<X509Certificate>? {
        if (chain == null && activity != null && !alias.isNullOrEmpty()) {
            chain = try {
                KeyChain.getCertificateChain(activity!!, alias!!)
            } catch (e: Exception) {
                null
            }
        }

        return chain
    }
}