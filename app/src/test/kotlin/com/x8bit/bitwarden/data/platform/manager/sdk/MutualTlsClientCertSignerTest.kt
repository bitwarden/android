package com.x8bit.bitwarden.data.platform.manager.sdk

import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import com.bitwarden.api.base.TlsSigScheme
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec

class MutualTlsClientCertSignerTest {

    @Test
    fun `schemes should return every RSA scheme for an RSA key without KeyStore authorizations`() {
        val signer = MutualTlsClientCertSigner(
            privateKey = RSA_KEY_PAIR.private,
            publicKey = RSA_KEY_PAIR.public,
            keyInfo = null,
        )

        assertEquals(ALL_RSA_SCHEMES, signer.schemes())
    }

    @Test
    fun `schemes should return ECDSA P-256 for a P-256 key without KeyStore authorizations`() {
        val signer = MutualTlsClientCertSigner(
            privateKey = P256_KEY_PAIR.private,
            publicKey = P256_KEY_PAIR.public,
            keyInfo = null,
        )

        assertEquals(listOf(TlsSigScheme.ECDSA_P256_SHA256), signer.schemes())
    }

    @Test
    fun `schemes should return ECDSA P-384 for a P-384 key without KeyStore authorizations`() {
        val signer = MutualTlsClientCertSigner(
            privateKey = P384_KEY_PAIR.private,
            publicKey = P384_KEY_PAIR.public,
            keyInfo = null,
        )

        assertEquals(listOf(TlsSigScheme.ECDSA_P384_SHA384), signer.schemes())
    }

    @Test
    fun `schemes should return nothing for a P-521 key`() {
        val keyPair = generateEcKeyPair(curve = "secp521r1")
        val signer = MutualTlsClientCertSigner(
            privateKey = keyPair.private,
            publicKey = keyPair.public,
            keyInfo = null,
        )

        assertEquals(emptyList<TlsSigScheme>(), signer.schemes())
    }

    @Test
    fun `schemes should return only the RSA schemes the KeyStore key is authorized for`() {
        val signer = MutualTlsClientCertSigner(
            privateKey = RSA_KEY_PAIR.private,
            publicKey = RSA_KEY_PAIR.public,
            keyInfo = keyInfo(
                digests = arrayOf(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512),
                paddings = arrayOf(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1),
            ),
        )

        assertEquals(
            listOf(TlsSigScheme.RSA_PKCS1_SHA256, TlsSigScheme.RSA_PKCS1_SHA512),
            signer.schemes(),
        )
    }

    @Test
    fun `schemes should return ECDSA for a KeyStore EC key authorized for its digest`() {
        val p256Signer = MutualTlsClientCertSigner(
            privateKey = P256_KEY_PAIR.private,
            publicKey = P256_KEY_PAIR.public,
            keyInfo = keyInfo(),
        )
        val p384Signer = MutualTlsClientCertSigner(
            privateKey = P384_KEY_PAIR.private,
            publicKey = P384_KEY_PAIR.public,
            keyInfo = keyInfo(),
        )

        assertEquals(listOf(TlsSigScheme.ECDSA_P256_SHA256), p256Signer.schemes())
        assertEquals(listOf(TlsSigScheme.ECDSA_P384_SHA384), p384Signer.schemes())
    }

    @Test
    fun `schemes should return nothing for a KeyStore EC key not authorized for its digest`() {
        val signer = MutualTlsClientCertSigner(
            privateKey = P384_KEY_PAIR.private,
            publicKey = P384_KEY_PAIR.public,
            keyInfo = keyInfo(digests = arrayOf(KeyProperties.DIGEST_SHA256)),
        )

        assertEquals(emptyList<TlsSigScheme>(), signer.schemes())
    }

    @Test
    fun `schemes should return nothing for a KeyStore key not authorized to sign`() {
        val signer = MutualTlsClientCertSigner(
            privateKey = RSA_KEY_PAIR.private,
            publicKey = RSA_KEY_PAIR.public,
            keyInfo = keyInfo(purposes = KeyProperties.PURPOSE_DECRYPT),
        )

        assertEquals(emptyList<TlsSigScheme>(), signer.schemes())
    }

    private fun keyInfo(
        purposes: Int = KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
        digests: Array<String> = arrayOf(
            KeyProperties.DIGEST_SHA256,
            KeyProperties.DIGEST_SHA384,
            KeyProperties.DIGEST_SHA512,
        ),
        paddings: Array<String> = arrayOf(
            KeyProperties.SIGNATURE_PADDING_RSA_PKCS1,
            KeyProperties.SIGNATURE_PADDING_RSA_PSS,
        ),
    ): KeyInfo = mockk {
        every { this@mockk.purposes } returns purposes
        every { this@mockk.digests } returns digests
        every { signaturePaddings } returns paddings
    }
}

private val ALL_RSA_SCHEMES = listOf(
    TlsSigScheme.RSA_PSS_SHA256,
    TlsSigScheme.RSA_PSS_SHA384,
    TlsSigScheme.RSA_PSS_SHA512,
    TlsSigScheme.RSA_PKCS1_SHA256,
    TlsSigScheme.RSA_PKCS1_SHA384,
    TlsSigScheme.RSA_PKCS1_SHA512,
)

private val RSA_KEY_PAIR: KeyPair = KeyPairGenerator
    .getInstance("RSA")
    .apply { initialize(2048) }
    .generateKeyPair()
private val P256_KEY_PAIR: KeyPair = generateEcKeyPair(curve = "secp256r1")
private val P384_KEY_PAIR: KeyPair = generateEcKeyPair(curve = "secp384r1")

private fun generateEcKeyPair(curve: String): KeyPair = KeyPairGenerator
    .getInstance("EC")
    .apply { initialize(ECGenParameterSpec(curve)) }
    .generateKeyPair()
