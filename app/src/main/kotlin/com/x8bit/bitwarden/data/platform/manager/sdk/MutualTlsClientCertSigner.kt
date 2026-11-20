package com.x8bit.bitwarden.data.platform.manager.sdk

import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import com.bitwarden.api.base.ClientCertSigner
import com.bitwarden.api.base.ClientCertSignerException
import com.bitwarden.api.base.TlsSigScheme
import timber.log.Timber
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.ProviderException
import java.security.PublicKey
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PSSParameterSpec

/**
 * Signs the SDK's mTLS handshakes with [privateKey]. The SDK calls [sign] synchronously, so the
 * key must already be resolved when this signer is created.
 *
 * @param privateKey The client certificate's private key.
 * @param publicKey The leaf certificate's public key, used to determine the key type and curve.
 * @param keyInfo The KeyStore authorizations of [privateKey], or `null` when it is not a KeyStore
 * key.
 */
class MutualTlsClientCertSigner(
    private val privateKey: PrivateKey,
    publicKey: PublicKey,
    keyInfo: KeyInfo? = privateKey.androidKeyInfoOrNull(),
) : ClientCertSigner {

    private val supportedSchemes: List<TlsSigScheme> = supportedSchemes(publicKey, keyInfo)
    private val isKeyStoreKey: Boolean = keyInfo != null

    override fun schemes(): List<TlsSigScheme> = supportedSchemes

    override fun sign(scheme: TlsSigScheme, message: ByteArray): ByteArray =
        try {
            initSignature(scheme)
                .apply { update(message) }
                .sign()
        } catch (e: GeneralSecurityException) {
            throw signingFailed(scheme, e)
        } catch (e: ProviderException) {
            // AndroidKeyStore reports KeyStore failures as ProviderException.
            throw signingFailed(scheme, e)
        } catch (e: UnsupportedOperationException) {
            // Thrown by providers that do not accept explicit PSS parameters.
            throw signingFailed(scheme, e)
        }

    private fun initSignature(scheme: TlsSigScheme): Signature {
        val signature = Signature.getInstance(scheme.algorithm)
        // Initializing first lets the JCA pick the provider that owns the key.
        signature.initSign(privateKey)
        // KeyStore rejects explicit PSS parameters, and its fixed ones are the ones TLS requires.
        val pssParameterSpec = scheme.pssParameterSpec
        if (!isKeyStoreKey && pssParameterSpec != null) {
            signature.setParameter(pssParameterSpec)
        }
        return signature
    }

    private fun signingFailed(
        scheme: TlsSigScheme,
        cause: Throwable,
    ): ClientCertSignerException {
        Timber.e(cause, "Failed to sign the mTLS handshake with $scheme")
        return ClientCertSignerException.SigningFailed(reason = "${scheme.name}: $cause")
    }
}

private val RSA_SCHEMES: List<TlsSigScheme> = listOf(
    TlsSigScheme.RSA_PSS_SHA256,
    TlsSigScheme.RSA_PSS_SHA384,
    TlsSigScheme.RSA_PSS_SHA512,
    TlsSigScheme.RSA_PKCS1_SHA256,
    TlsSigScheme.RSA_PKCS1_SHA384,
    TlsSigScheme.RSA_PKCS1_SHA512,
)

private const val P256_FIELD_SIZE: Int = 256
private const val P384_FIELD_SIZE: Int = 384

/**
 * Returns the schemes the key can sign with: those matching its type and curve, narrowed to the
 * digests and paddings in [keyInfo] when the key has KeyStore authorizations.
 */
private fun supportedSchemes(publicKey: PublicKey, keyInfo: KeyInfo?): List<TlsSigScheme> {
    val candidates = when (publicKey) {
        // The SDK drops the RSA schemes the key's modulus is too short for.
        is RSAPublicKey -> RSA_SCHEMES
        is ECPublicKey -> when (publicKey.params.curve.field.fieldSize) {
            P256_FIELD_SIZE -> listOf(TlsSigScheme.ECDSA_P256_SHA256)
            P384_FIELD_SIZE -> listOf(TlsSigScheme.ECDSA_P384_SHA384)
            else -> emptyList()
        }

        else -> emptyList()
    }
    keyInfo ?: return candidates
    if (keyInfo.purposes and KeyProperties.PURPOSE_SIGN == 0) return emptyList()
    return candidates.filter { scheme ->
        scheme.digest in keyInfo.digests &&
            (scheme.padding == null || scheme.padding in keyInfo.signaturePaddings)
    }
}

/**
 * Returns the KeyStore authorizations of this key, or `null` when it is not a KeyStore key or its
 * authorizations cannot be read.
 */
private fun PrivateKey.androidKeyInfoOrNull(): KeyInfo? = try {
    KeyFactory
        .getInstance(algorithm, ANDROID_KEY_STORE_PROVIDER)
        .getKeySpec(this, KeyInfo::class.java)
} catch (_: GeneralSecurityException) {
    null
} catch (_: ProviderException) {
    // The legacy KeyStore on API 29-30 throws this when the key has been deleted.
    null
}

private const val ANDROID_KEY_STORE_PROVIDER: String = "AndroidKeyStore"

/**
 * The [Signature] algorithm for this scheme, which includes the digest because the SDK passes the
 * unhashed message.
 */
private val TlsSigScheme.algorithm: String
    get() = when (this) {
        TlsSigScheme.RSA_PKCS1_SHA256 -> "SHA256withRSA"
        TlsSigScheme.RSA_PKCS1_SHA384 -> "SHA384withRSA"
        TlsSigScheme.RSA_PKCS1_SHA512 -> "SHA512withRSA"
        TlsSigScheme.RSA_PSS_SHA256 -> "SHA256withRSA/PSS"
        TlsSigScheme.RSA_PSS_SHA384 -> "SHA384withRSA/PSS"
        TlsSigScheme.RSA_PSS_SHA512 -> "SHA512withRSA/PSS"
        TlsSigScheme.ECDSA_P256_SHA256 -> "SHA256withECDSA"
        TlsSigScheme.ECDSA_P384_SHA384 -> "SHA384withECDSA"
    }

/**
 * The scheme's digest, named as in [KeyProperties].
 */
private val TlsSigScheme.digest: String
    get() = when (this) {
        TlsSigScheme.RSA_PKCS1_SHA256,
        TlsSigScheme.RSA_PSS_SHA256,
        TlsSigScheme.ECDSA_P256_SHA256,
            -> KeyProperties.DIGEST_SHA256

        TlsSigScheme.RSA_PKCS1_SHA384,
        TlsSigScheme.RSA_PSS_SHA384,
        TlsSigScheme.ECDSA_P384_SHA384,
            -> KeyProperties.DIGEST_SHA384

        TlsSigScheme.RSA_PKCS1_SHA512,
        TlsSigScheme.RSA_PSS_SHA512,
            -> KeyProperties.DIGEST_SHA512
    }

/**
 * The scheme's RSA signature padding, named as in [KeyProperties], or `null` for ECDSA.
 */
private val TlsSigScheme.padding: String?
    get() = when (this) {
        TlsSigScheme.RSA_PKCS1_SHA256,
        TlsSigScheme.RSA_PKCS1_SHA384,
        TlsSigScheme.RSA_PKCS1_SHA512,
            -> KeyProperties.SIGNATURE_PADDING_RSA_PKCS1

        TlsSigScheme.RSA_PSS_SHA256,
        TlsSigScheme.RSA_PSS_SHA384,
        TlsSigScheme.RSA_PSS_SHA512,
            -> KeyProperties.SIGNATURE_PADDING_RSA_PSS

        TlsSigScheme.ECDSA_P256_SHA256,
        TlsSigScheme.ECDSA_P384_SHA384,
            -> null
    }

/**
 * TLS's RSA-PSS parameters: MGF1 over the message digest and a salt as long as the digest.
 */
private val TlsSigScheme.pssParameterSpec: PSSParameterSpec?
    get() = if (padding == KeyProperties.SIGNATURE_PADDING_RSA_PSS) {
        PSSParameterSpec(
            digest,
            "MGF1",
            MGF1ParameterSpec(digest),
            MessageDigest.getInstance(digest).digestLength,
            // PSSParameterSpec.TRAILER_FIELD_BC needs a newer API than minSdk.
            1,
        )
    } else {
        null
    }
