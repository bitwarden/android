package com.x8bit.bitwarden.data.platform.datasource.disk.model

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.security.cert.X509Certificate

class MutualTlsCertificateTest {

    @Test
    fun `leafCertificate should return the first certificate of the chain`() {
        val leaf = mockk<X509Certificate>()
        val intermediate = mockk<X509Certificate>()
        val certificate = MutualTlsCertificate(
            alias = "mockAlias",
            privateKey = mockk(),
            certificateChain = listOf(leaf, intermediate),
        )

        assertSame(leaf, certificate.leafCertificate)
    }

    @Test
    fun `leafCertificate should return null when the chain is empty`() {
        val certificate = MutualTlsCertificate(
            alias = "mockAlias",
            privateKey = mockk(),
            certificateChain = emptyList(),
        )

        assertNull(certificate.leafCertificate)
    }
}
