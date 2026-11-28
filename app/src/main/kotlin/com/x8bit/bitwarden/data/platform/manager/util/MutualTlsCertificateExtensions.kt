package com.x8bit.bitwarden.data.platform.manager.util

import com.bitwarden.api.base.ClientCertificate
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsCertificate
import com.x8bit.bitwarden.data.platform.manager.sdk.MutualTlsClientCertSigner
import timber.log.Timber
import java.security.cert.CertificateEncodingException

/**
 * Converts a [MutualTlsCertificate] to the SDK's [ClientCertificate], or returns `null` when the
 * chain is empty or cannot be encoded.
 */
fun MutualTlsCertificate.toClientCertificateOrNull(): ClientCertificate? {
    val leaf = leafCertificate ?: return null
    val chain = try {
        certificateChain.map { it.encoded }
    } catch (e: CertificateEncodingException) {
        Timber.e(e, "Unable to encode the mTLS certificate chain")
        return null
    }
    return ClientCertificate(
        chain = chain,
        signer = MutualTlsClientCertSigner(
            privateKey = privateKey,
            publicKey = leaf.publicKey,
        ),
    )
}
