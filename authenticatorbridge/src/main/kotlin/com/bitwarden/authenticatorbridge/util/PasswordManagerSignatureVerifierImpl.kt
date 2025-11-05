package com.bitwarden.authenticatorbridge.util

import android.content.Context
import android.content.pm.PackageManager
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.authenticatorbridge.R
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Default implementation of [PasswordManagerSignatureVerifier].
 *
 * Validates Password Manager applications by verifying their cryptographic signing certificate
 * against a whitelist of known-good certificate hashes.
 *
 * This addresses VULN-314: Insufficient package name validation allows TOTP secret harvesting
 * when malicious applications spoof legitimate package names without proper cryptographic
 * identity verification.
 *
 * Security Considerations:
 * - Rejects apps with multiple signers to prevent signature rotation attacks
 * - Uses GET_SIGNING_CERTIFICATES (API 28+) for secure certificate retrieval
 * - Validates SHA-256 certificate fingerprint against hardcoded whitelist
 * - Fails closed on any validation error or exception
 *
 * References:
 * [1] Android AOSP. "APK Signature Scheme v3."
 *     https://source.android.com/docs/security/features/apksigning/v3
 *     "Multiple signers are not supported and Google Play does not publish apps signed
 *     with multiple certificates"
 *
 * [2] Android Developers. "SigningInfo API Reference."
 *     https://developer.android.com/reference/android/content/pm/SigningInfo
 *     Deprecation of GET_SIGNATURES in favor of GET_SIGNING_CERTIFICATES for improved security.
 *
 * [3] OWASP MASTG. "MASTG-TEST-0038: Making Sure that the App is Properly Signed."
 *     https://mas.owasp.org/MASTG/tests/android/MASVS-RESILIENCE/MASTG-TEST-0038/
 *     Best practices for signature validation and integrity verification.
 *
 * [4] GuardSquare. "Janus Vulnerability (CVE-2017-13156)."
 *     https://www.guardsquare.com/blog/janus-vulnerability
 *     Historical context: signature bypass attacks mitigated by v2/v3 schemes.
 *
 * @param context Android context for accessing PackageManager and resources
 */
@OmitFromCoverage
internal class PasswordManagerSignatureVerifierImpl(
    private val context: Context,
) : PasswordManagerSignatureVerifier {

    private val knownPasswordManagerCertificates: List<String> by lazy {
        context.resources
            .getStringArray(R.array.known_bitwarden_certs)
            .toList()
    }

    override fun isValidPasswordManagerApp(packageName: String): Boolean {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            )

            val signingInfo = packageInfo.signingInfo ?: return false

            // Reject multiple signers to prevent signature rotation attacks.
            // Bitwarden uses stable, long-lived signing certificates and never performs rotation.
            // Any multi-signer scenario indicates:
            // - Malicious rotation attempt
            // - Compromised/tampered APK
            // - Non-genuine Bitwarden application
            // See: https://source.android.com/docs/security/features/apksigning/v3
            if (signingInfo.hasMultipleSigners()) return false

            val signature = signingInfo.apkContentsSigners.first()
            val sha256 = MessageDigest.getInstance("SHA-256")
            val certHash = sha256.digest(signature.toByteArray())
            val certHashHex = certHash.joinToString("") { "%02x".format(it) }

            knownPasswordManagerCertificates.contains(certHashHex)
        } catch (_: PackageManager.NameNotFoundException) {
            // Package not installed
            false
        } catch (_: SecurityException) {
            // Permission denied or security policy violation
            false
        } catch (_: NoSuchAlgorithmException) {
            // SHA-256 algorithm not available (should never happen on modern Android)
            false
        } catch (_: NoSuchElementException) {
            // No signing certificates found
            false
        }
    }
}
