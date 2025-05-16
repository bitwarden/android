@file:OmitFromCoverage

package com.x8bit.bitwarden.data.credentials.util

import android.os.Build
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.bitwarden.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import javax.crypto.Cipher

/**
 * Sets the biometric prompt data on the [PublicKeyCredentialEntry.Builder] if supported.
 */
fun PublicKeyCredentialEntry.Builder.setBiometricPromptDataIfSupported(
    cipher: Cipher?,
    isSingleTapAuthEnabled: Boolean,
): PublicKeyCredentialEntry.Builder =
    if (!isBuildVersionBelow(Build.VERSION_CODES.VANILLA_ICE_CREAM) &&
        cipher != null &&
        isSingleTapAuthEnabled
    ) {
        setBiometricPromptData(
            biometricPromptData = buildPromptDataWithCipher(cipher),
        )
    } else {
        this
    }
