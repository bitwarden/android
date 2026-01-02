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
 * Note: Xiaomi HyperOS is known to not support biometric prompt.
 */
fun PublicKeyCredentialEntry.Builder.setBiometricPromptDataIfSupported(
    cipher: Cipher?,
): PublicKeyCredentialEntry.Builder =
    if (isBuildVersionAtLeast(Build.VERSION_CODES.VANILLA_ICE_CREAM) && !isHyperOS() && cipher != null) {
        setBiometricPromptData(
            biometricPromptData = buildPromptDataWithCipher(cipher),
        )
    } else {
        this
    }

/**
 * Sets the biometric prompt data on the [PasswordCredentialEntry.Builder] if supported.
 * Note: Xiaomi HyperOS is known to not support biometric prompt.
 */
fun PasswordCredentialEntry.Builder.setBiometricPromptDataIfSupported(
    cipher: Cipher?,
): PasswordCredentialEntry.Builder =
    if (isBuildVersionAtLeast(Build.VERSION_CODES.VANILLA_ICE_CREAM) && !isHyperOS() && cipher != null) {
        setBiometricPromptData(
            biometricPromptData = buildPromptDataWithCipher(cipher),
        )
    } else {
        this
    }
