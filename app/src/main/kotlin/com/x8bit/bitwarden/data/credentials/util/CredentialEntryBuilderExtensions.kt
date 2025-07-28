@file:OmitFromCoverage

package com.x8bit.bitwarden.data.credentials.util

import android.os.Build
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.util.isBuildVersionAtLeast
import javax.crypto.Cipher

/**
 * Sets the biometric prompt data on the [PublicKeyCredentialEntry.Builder] if supported.
 */
fun PublicKeyCredentialEntry.Builder.setBiometricPromptDataIfSupported(
    cipher: Cipher?,
): PublicKeyCredentialEntry.Builder =
    if (isBuildVersionAtLeast(Build.VERSION_CODES.VANILLA_ICE_CREAM) && cipher != null) {
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
    if (isBuildVersionAtLeast(Build.VERSION_CODES.VANILLA_ICE_CREAM) && cipher != null) {
        setBiometricPromptData(
            biometricPromptData = buildPromptDataWithCipher(cipher),
        )
    } else {
        this
    }
