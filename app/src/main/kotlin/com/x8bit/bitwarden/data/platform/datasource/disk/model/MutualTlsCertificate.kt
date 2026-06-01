package com.x8bit.bitwarden.data.platform.datasource.disk.model

import java.security.PrivateKey
import java.security.cert.X509Certificate

/**
 * Represents a mutual TLS certificate.
 */
data class MutualTlsCertificate(
    val alias: String,
    val privateKey: PrivateKey,
    val certificateChain: List<X509Certificate>,
) {
    /**
     * Leaf certificate of the chain.
     */
    val leafCertificate: X509Certificate?
        get() = certificateChain.lastOrNull()

    override fun toString(): String = leafCertificate
        ?.let {
            buildString {
                appendLine("Subject: ${it.subjectDN}")
                appendLine("Issuer: ${it.issuerDN}")
                appendLine("Valid From: ${it.notBefore}")
                appendLine("Valid Until: ${it.notAfter}")
            }
        }
        .orEmpty()
}
