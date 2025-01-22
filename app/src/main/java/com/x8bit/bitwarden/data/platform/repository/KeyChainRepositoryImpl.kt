package com.x8bit.bitwarden.data.platform.repository

import android.content.Context
import android.security.KeyChain
import android.security.KeyChainException
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSource
import com.x8bit.bitwarden.data.platform.repository.util.toEnvironmentUrlsOrDefault
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.inject.Inject

/**
 * Default implementation of [KeyChainRepository].
 */
class KeyChainRepositoryImpl @Inject constructor(
    environmentDiskSource: EnvironmentDiskSource,
    val context: Context,
) : KeyChainRepository {
    private var alias: String? = null
    private var key: PrivateKey? = null
    private var chain: Array<X509Certificate>? = null

    init {
        alias = environmentDiskSource
            .preAuthEnvironmentUrlData
            .toEnvironmentUrlsOrDefault()
            .environmentUrlData
            .keyAlias
    }

    override fun getPrivateKey(): PrivateKey? {
        if (key == null && !alias.isNullOrEmpty()) {
            key = try {
                KeyChain.getPrivateKey(context, alias!!)
            } catch (_: KeyChainException) {
                null
            }
        }

        return key
    }

    override fun getCertificateChain(): Array<X509Certificate>? {
        if (chain == null && !alias.isNullOrEmpty()) {
            chain = try {
                KeyChain.getCertificateChain(context, alias!!)
            } catch (_: KeyChainException) {
                null
            }
        }

        return chain
    }
}
