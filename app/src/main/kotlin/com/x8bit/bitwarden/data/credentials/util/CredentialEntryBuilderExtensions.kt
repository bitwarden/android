@file:OmitFromCoverage

package com.x8bit.bitwarden.data.credentials.util

import android.os.Build
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.core.util.isHyperOS
import javax.crypto.Cipher

/**
 * Sets the biometric prompt data on the [PublicKeyCredentialEntry.Builder] if supported.
 */
fun PublicKeyCredentialEntry.Builder.setBiometricPromptDataIfSupported(
    cipher: Cipher?,
): PublicKeyCredentialEntry.Builder =
    if (isBiometricPromptDataSupported() && cipher != null) {
        setBiometricPromptData(
            biometricPromptData = buildPromptDataWithCipher(cipher),
        )
    } else {
        this
    }

/**
 * Sets the biometric prompt data on the [PasswordCredentialEntry.Builder] if supported.
 */
fun PasswordCredentialEntry.Builder.setBiometricPromptDataIfSupported(
    cipher: Cipher?,
): PasswordCredentialEntry.Builder =
    if (isBiometricPromptDataSupported() && cipher != null) {
        setBiometricPromptData(
            biometricPromptData = buildPromptDataWithCipher(cipher),
        )
    } else {
        this
    }

/**
 * Returns whether biometric prompt data is supported on this device.
 * Note: Xiaomi HyperOS is known to be incompatible.
 */
private fun isBiometricPromptDataSupported(): Boolean {
    return isBuildVersionAtLeast(Build.VERSION_CODES.VANILLA_ICE_CREAM) &&
        !isHyperOS()
}
