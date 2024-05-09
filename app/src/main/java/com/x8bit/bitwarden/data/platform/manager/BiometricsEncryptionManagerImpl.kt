package com.x8bit.bitwarden.data.platform.manager

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import com.x8bit.bitwarden.BuildConfig
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Default implementation of [BiometricsEncryptionManager] for managing Android keystore encryption
 * and decryption.
 */
@OmitFromCoverage
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

    override fun createCipher(userId: String): Cipher {
        val secretKey: SecretKey = generateKey()
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        // This should never fail to initialize / return false because the cipher is newly generated
        initializeCipher(
            userId = userId,
            cipher = cipher,
            secretKey = secretKey,
        )
        return cipher
    }

    override fun getOrCreateCipher(userId: String): Cipher? {
        val secretKey = try {
            getSecretKey() ?: generateKey()
        } catch (e: InvalidAlgorithmParameterException) {
            // user removed all biometrics from the device
            settingsDiskSource.systemBiometricIntegritySource = null
            return null
        }
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val isCipherInitialized = initializeCipher(
            userId = userId,
            cipher = cipher,
            secretKey = secretKey,
        )
        return cipher?.takeIf { isCipherInitialized }
    }

    override fun setupBiometrics(userId: String) {
        createIntegrityValues(userId)
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

    /**
     * Generates a [SecretKey] from which the [Cipher] will be generated.
     */
    private fun generateKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ENCRYPTION_KEYSTORE_NAME,
        )
        keyGen.init(keyGenParameterSpec)
        keyGen.generateKey()
        return requireNotNull(getSecretKey())
    }

    /**
     * Returns the [SecretKey] stored in the keystore, or null if there isn't one.
     */
    private fun getSecretKey(): SecretKey? {
        keystore.load(null)
        return keystore.getKey(ENCRYPTION_KEY_NAME, null) as? SecretKey
    }

    /**
     * Initialize a [Cipher] and return a boolean indicating whether it is valid.
     */
    private fun initializeCipher(
        userId: String,
        cipher: Cipher,
        secretKey: SecretKey,
    ): Boolean =
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
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
            // Fallback for old Bitwarden users without a key
            createIntegrityValues(userId)
            true
        }

    /**
     * Validates the keystore key and decrypts it using the user-provided [cipher].
     */
    private fun isSystemBiometricIntegrityValid(userId: String, cipher: Cipher?): Boolean {
        val secretKey = getSecretKey()
        return if (cipher != null && secretKey != null) {
            initializeCipher(
                userId = userId,
                cipher = cipher,
                secretKey = secretKey,
            )
        } else {
            false
        }
    }

    /**
     * Creates the initial values to be used for biometrics, including the key from which the
     * master [Cipher] will be generated.
     */
    @Suppress("TooGenericExceptionCaught")
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

        try {
            createCipher(userId)
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
