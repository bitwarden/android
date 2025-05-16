package com.x8bit.bitwarden.data.platform.manager

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import com.bitwarden.annotation.OmitFromCoverage
import com.x8bit.bitwarden.BuildConfig
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.ProviderException
import java.security.UnrecoverableKeyException
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * Default implementation of [BiometricsEncryptionManager] for managing Android keystore encryption
 * and decryption.
 */
@Suppress("TooManyFunctions")
@OmitFromCoverage
class BiometricsEncryptionManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val settingsDiskSource: SettingsDiskSource,
) : BiometricsEncryptionManager {
    private val keystore = KeyStore
        .getInstance(ENCRYPTION_KEYSTORE_NAME)
        .also { it.load(null) }

    override fun createCipherOrNull(userId: String): Cipher? {
        val secretKey: SecretKey = generateKeyOrNull(userId = userId)
            ?: run {
                // user removed all biometrics from the device
                destroyBiometrics(userId = userId)
                return null
            }
        val cipher = try {
            Cipher.getInstance(CIPHER_TRANSFORMATION)
        } catch (_: NoSuchAlgorithmException) {
            return null
        } catch (_: NoSuchPaddingException) {
            return null
        }
        // Instantiate integrity values.
        createIntegrityValues(userId = userId)
        // This should never fail to initialize / return false because the cipher is newly generated
        cipher.initializeCipher(userId = userId, secretKey = secretKey)
        return cipher
    }

    override fun clearBiometrics(userId: String) {
        settingsDiskSource.systemBiometricIntegritySource?.let { systemBioIntegrityState ->
            settingsDiskSource.storeAccountBiometricIntegrityValidity(
                userId = userId,
                systemBioIntegrityState = systemBioIntegrityState,
                value = null,
            )
        }
        authDiskSource.storeUserBiometricUnlockKey(userId = userId, biometricsKey = null)
        authDiskSource.storeUserBiometricInitVector(userId = userId, iv = null)
        keystore.deleteEntry(encryptionKeyName(userId = userId))
    }

    override fun getOrCreateCipher(userId: String): Cipher? {
        // Attempt to get the user scoped key. If that fails, we check to see if a legacy key
        // is available. If neither succeeds, then we need to generate a new one.
        val secretKey: SecretKey = getSecretKeyOrNull(userId = userId)
            ?: getSecretKeyOrNull(userId = null)
            ?: generateKeyOrNull(userId = userId)
            ?: run {
                // user removed all biometrics from the device
                destroyBiometrics(userId = userId)
                return null
            }

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val isCipherInitialized = cipher.initializeCipher(userId = userId, secretKey = secretKey)
        return cipher?.takeIf { isCipherInitialized }
    }

    override fun isBiometricIntegrityValid(userId: String, cipher: Cipher?): Boolean =
        isSystemBiometricIntegrityValid(userId, cipher) && isAccountBiometricIntegrityValid(userId)

    override fun isAccountBiometricIntegrityValid(userId: String): Boolean {
        val systemBioIntegrityState = settingsDiskSource
            .systemBiometricIntegritySource
            ?: return false
        return settingsDiskSource
            .getAccountBiometricIntegrityValidity(
                userId = userId,
                systemBioIntegrityState = systemBioIntegrityState,
            )
            ?: false
    }

    private fun getKeyGenParameterSpec(userId: String): KeyGenParameterSpec =
        KeyGenParameterSpec
            .Builder(
                encryptionKeyName(userId = userId),
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .build()

    private fun encryptionKeyName(userId: String?): String =
        "${BuildConfig.APPLICATION_ID}.biometric_integrity${userId?.let { ".$it" }.orEmpty()}"

    /**
     * Generates a [SecretKey] from which the [Cipher] will be generated, or `null` if a key cannot
     * be generated.
     */
    private fun generateKeyOrNull(userId: String): SecretKey? {
        val keyGen = try {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ENCRYPTION_KEYSTORE_NAME)
        } catch (_: NoSuchAlgorithmException) {
            return null
        } catch (_: NoSuchProviderException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }

        return try {
            keyGen.init(getKeyGenParameterSpec(userId = userId))
            keyGen.generateKey()
        } catch (_: InvalidAlgorithmParameterException) {
            null
        } catch (_: ProviderException) {
            null
        }
    }

    /**
     * Returns the [SecretKey] stored in the keystore, or null if there isn't one.
     */
    private fun getSecretKeyOrNull(userId: String?): SecretKey? =
        try {
            keystore
                .getKey(encryptionKeyName(userId = userId), null)
                ?.let { it as SecretKey }
        } catch (_: KeyStoreException) {
            // keystore was not loaded
            null
        } catch (_: NoSuchAlgorithmException) {
            // keystore algorithm cannot be found
            null
        } catch (_: UnrecoverableKeyException) {
            // key could not be recovered
            null
        }

    /**
     * Initialize a [Cipher] and return a boolean indicating whether it is valid.
     */
    private fun Cipher.initializeCipher(
        userId: String,
        secretKey: SecretKey,
    ): Boolean =
        try {
            authDiskSource
                .getUserBiometricInitVector(userId = userId)
                ?.let { init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(it)) }
                ?: init(Cipher.ENCRYPT_MODE, secretKey)
            true
        } catch (_: KeyPermanentlyInvalidatedException) {
            // Biometric has changed
            destroyBiometrics(userId = userId)
            false
        } catch (_: UnrecoverableKeyException) {
            // Biometric was disabled and re-enabled
            destroyBiometrics(userId = userId)
            false
        } catch (_: InvalidKeyException) {
            // User has no key
            destroyBiometrics(userId = userId)
            true
        }

    /**
     * Validates the keystore key and decrypts it using the user-provided [cipher].
     */
    private fun isSystemBiometricIntegrityValid(userId: String, cipher: Cipher?): Boolean {
        // Attempt to get the user scoped key. If that fails, we check to see if a legacy key
        // is available.
        val secretKey = getSecretKeyOrNull(userId = userId) ?: getSecretKeyOrNull(userId = null)
        return if (cipher != null && secretKey != null) {
            cipher.initializeCipher(userId = userId, secretKey = secretKey)
        } else {
            false
        }
    }

    /**
     * Creates the initial values to be used for biometrics, including the key from which the
     * master [Cipher] will be generated.
     */
    private fun createIntegrityValues(userId: String) {
        val systemBiometricIntegritySource = settingsDiskSource
            .systemBiometricIntegritySource
            ?: UUID.randomUUID().toString()
        settingsDiskSource.systemBiometricIntegritySource = systemBiometricIntegritySource
        settingsDiskSource.storeAccountBiometricIntegrityValidity(
            userId = userId,
            systemBioIntegrityState = systemBiometricIntegritySource,
            value = true,
        )
    }

    private fun destroyBiometrics(userId: String) {
        clearBiometrics(userId = userId)
        settingsDiskSource.systemBiometricIntegritySource = null
    }
}

private const val ENCRYPTION_KEYSTORE_NAME: String = "AndroidKeyStore"
private const val CIPHER_TRANSFORMATION =
    KeyProperties.KEY_ALGORITHM_AES + "/" +
        KeyProperties.BLOCK_MODE_CBC + "/" +
        KeyProperties.ENCRYPTION_PADDING_PKCS7
