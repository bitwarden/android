package com.x8bit.bitwarden.data.platform.util

import androidx.credentials.provider.CallingAppInfo
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult
import com.x8bit.bitwarden.ui.platform.base.util.toHostOrPathOrNull
import java.security.MessageDigest

/**
 * Returns the name of the RP. If this [CallingAppInfo] is a privileged app the RP host name will be
 * returned. If this [CallingAppInfo] is a native RP application the package name will be returned.
 * Otherwise, `null` is returned.
 */
fun CallingAppInfo.getFido2RpIdOrNull(): String? =
    if (isOriginPopulated()) {
        origin?.toHostOrPathOrNull()
    } else {
        packageName
    }

/**
 * Returns the signing certificate hash formatted as a hex string.
 */
@OptIn(ExperimentalStdlibApi::class)
fun CallingAppInfo.getCallingAppApkFingerprint(): String {
    val cert = signingInfo.apkContentsSigners[0].toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val certHash = md.digest(cert)
    return certHash
        .joinToString(":") { b ->
            b.toHexString(HexFormat.UpperCase)
        }
}

/**
 * Returns true if this [CallingAppInfo] is present in the privileged app [allowList]. Otherwise,
 * returns false.
 */
fun CallingAppInfo.validatePrivilegedApp(allowList: String): Fido2ValidateOriginResult {

    if (!allowList.contains("\"package_name\": \"$packageName\"")) {
        return Fido2ValidateOriginResult.Error.PrivilegedAppNotAllowed
    }

    return try {
        if (getOrigin(allowList) != null) {
            Fido2ValidateOriginResult.Success
        } else {
            Fido2ValidateOriginResult.Error.PasskeyNotSupportedForApp
        }
    } catch (e: IllegalStateException) {
        // We know the package name is in the allow list so we can infer that this exception is
        // thrown because no matching signature is found.
        Fido2ValidateOriginResult.Error.PrivilegedAppSignatureNotFound
    } catch (e: IllegalArgumentException) {
        // The allow list is not formatted correctly so we notify the user passkeys are not
        // supported for this application
        Fido2ValidateOriginResult.Error.PasskeyNotSupportedForApp
    }
}
