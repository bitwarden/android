@file:OmitFromCoverage

package com.x8bit.bitwarden.data.credentials.util

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.credentials.provider.BiometricPromptData
import com.bitwarden.annotation.OmitFromCoverage
import javax.crypto.Cipher

/**
 * Builds a [BiometricPromptData] instance with the provided [Cipher].
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
fun buildPromptDataWithCipher(
    cipher: Cipher,
): BiometricPromptData = BiometricPromptData.Builder()
    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    .setCryptoObject(BiometricPrompt.CryptoObject(cipher))
    .build()
