package com.bitwarden.authenticator.data.platform.manager

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import com.bitwarden.authenticator.BuildConfig
import com.bitwarden.authenticator.data.platform.datasource.disk.SettingsDiskSource
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

/**
 * Default implementation of [BiometricsEncryptionManager] for managing Android keystore encryption
 * and decryption.
 */
class BiometricsEncryptionManagerImpl(
    private val settingsDiskSource: SettingsDiskSource,
) : BiometricsEncryptionManager {
    private val keystore = KeyStore
        .getInstance(ENCRYPTION_KEYSTORE_NAME)
        .also { it.load(null) }

    private val keyGenParameterSpec: KeyGenParameterSpec
        get() = KeyGenParameterSpec
            .Builder(
                ENCRYPTION_KEY_NAME,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .build()

    override fun setupBiometrics() {
        createIntegrityValues()
    }

    override fun isBiometricIntegrityValid(): Boolean =
        isSystemBiometricIntegrityValid() && isAccountBiometricIntegrityValid()

    private fun isAccountBiometricIntegrityValid(): Boolean {
        val systemBioIntegrityState = settingsDiskSource
            .systemBiometricIntegritySource
            ?: return false
        return settingsDiskSource
            .getAccountBiometricIntegrityValidity(
                systemBioIntegrityState = systemBioIntegrityState,
            )
            ?: false
    }

    private fun isSystemBiometricIntegrityValid(): Boolean =
        try {
            keystore.load(null)
            keystore
                .getKey(ENCRYPTION_KEY_NAME, null)
                ?.let { Cipher.getInstance(CIPHER_TRANSFORMATION).init(Cipher.ENCRYPT_MODE, it) }
            true
        } catch (e: KeyPermanentlyInvalidatedException) {
            // Biometric has changed
            settingsDiskSource.systemBiometricIntegritySource = null
            false
        } catch (e: UnrecoverableKeyException) {
            // Biometric was disabled and re-enabled
            settingsDiskSource.systemBiometricIntegritySource = null
            false
        } catch (e: InvalidKeyException) {
            // Fallback for old bitwarden users without a key
            createIntegrityValues()
            true
        }

    @Suppress("TooGenericExceptionCaught")
    private fun createIntegrityValues() {
        val systemBiometricIntegritySource = settingsDiskSource
            .systemBiometricIntegritySource
            ?: UUID.randomUUID().toString()
        settingsDiskSource.systemBiometricIntegritySource = systemBiometricIntegritySource
        settingsDiskSource.storeAccountBiometricIntegrityValidity(
            systemBioIntegrityState = systemBiometricIntegritySource,
            value = true,
        )

        try {
            val keyGen = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ENCRYPTION_KEYSTORE_NAME,
            )
            keyGen.init(keyGenParameterSpec)
            keyGen.generateKey()
        } catch (e: Exception) {
            // Catch silently to allow biometrics to function on devices that are in
            // a state where key generation is not functioning
        }
    }
}

private const val ENCRYPTION_KEYSTORE_NAME: String = "AndroidKeyStore"
private const val ENCRYPTION_KEY_NAME: String = "${BuildConfig.APPLICATION_ID}.biometric_integrity"
private const val CIPHER_TRANSFORMATION =
    KeyProperties.KEY_ALGORITHM_AES + "/" +
        KeyProperties.BLOCK_MODE_CBC + "/" +
        KeyProperties.ENCRYPTION_PADDING_PKCS7
