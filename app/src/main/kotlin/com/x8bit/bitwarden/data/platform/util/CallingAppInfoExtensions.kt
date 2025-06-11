package com.x8bit.bitwarden.data.platform.util

import android.util.Base64
import androidx.credentials.provider.CallingAppInfo
import com.x8bit.bitwarden.data.credentials.model.ValidateOriginResult
import java.security.MessageDigest

/**
 * Returns the application's signing certificate hash formatted as a hex string if it has a single
 * signing certificate. Otherwise `null` is returned.
 */
@OptIn(ExperimentalStdlibApi::class)
fun CallingAppInfo.getSignatureFingerprintAsHexString(): String? {
    return getAppSigningSignatureFingerprint()
        ?.joinToString(":") { b ->
            b.toHexString(HexFormat.UpperCase)
        }
}

/**
 * Returns true if this [CallingAppInfo] is present in the privileged app [allowList]. Otherwise,
 * returns false.
 */
fun CallingAppInfo.validatePrivilegedApp(allowList: String): ValidateOriginResult {

    if (!allowList.contains("\"$packageName\"")) {
        return ValidateOriginResult.Error.PrivilegedAppNotAllowed
    }

    return try {
        val origin = getOrigin(allowList)
        if (origin.isNullOrEmpty()) {
            ValidateOriginResult.Error.PasskeyNotSupportedForApp
        } else {
            ValidateOriginResult.Success(origin)
        }
    } catch (_: IllegalStateException) {
        // We know the package name is in the allow list so we can infer that this exception is
        // thrown because no matching signature is found.
        ValidateOriginResult.Error.PrivilegedAppSignatureNotFound
    } catch (_: IllegalArgumentException) {
        // The allow list is not formatted correctly so we notify the user passkeys are not
        // supported for this application
        ValidateOriginResult.Error.PasskeyNotSupportedForApp
    }
}

/**
 * Returns the signing key hash of the calling application formatted as an origin URI for an
 * unprivileged application.
 */
fun CallingAppInfo.getAppOrigin(): String {
    val certHash = getAppSigningSignatureFingerprint()
    return "android:apk-key-hash:${Base64.encodeToString(certHash, ENCODING_FLAGS)}"
}

/**
 * Returns a [ByteArray] containing the application's signing certificate signature hash. If
 * multiple signers are identified `null` is returned.
 */
fun CallingAppInfo.getAppSigningSignatureFingerprint(): ByteArray? {
    if (signingInfo.hasMultipleSigners()) return null

    val signature = signingInfo.apkContentsSigners.first()
    val md = MessageDigest.getInstance(SHA_ALGORITHM)
    return md.digest(signature.toByteArray())
}

private const val SHA_ALGORITHM = "SHA-256"
private const val ENCODING_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
