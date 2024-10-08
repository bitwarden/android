package com.x8bit.bitwarden.data.platform.manager

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import com.x8bit.bitwarden.BuildConfig
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import java.io.IOException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.ProviderException
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
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

    override fun createCipherOrNull(userId: String): Cipher? {
        val secretKey: SecretKey = generateKeyOrNull()
            ?: run {
                // user removed all biometrics from the device
                settingsDiskSource.systemBiometricIntegritySource = null
                return null
            }
        val cipher = try {
            Cipher.getInstance(CIPHER_TRANSFORMATION)
        } catch (_: NoSuchAlgorithmException) {
            return null
        } catch (_: NoSuchPaddingException) {
            return null
        }
        // This should never fail to initialize / return false because the cipher is newly generated
        initializeCipher(
            userId = userId,
            cipher = cipher,
            secretKey = secretKey,
        )
        return cipher
    }

    override fun getOrCreateCipher(userId: String): Cipher? {
        val secretKey = getSecretKeyOrNull()
            ?: generateKeyOrNull()
            ?: run {
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
     * Generates a [SecretKey] from which the [Cipher] will be generated, or `null` if a key cannot
     * be generated.
     */
    private fun generateKeyOrNull(): SecretKey? {
        val keyGen = try {
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ENCRYPTION_KEYSTORE_NAME,
            )
        } catch (_: NoSuchAlgorithmException) {
            return null
        } catch (_: NoSuchProviderException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }

        try {
            keyGen.init(keyGenParameterSpec)
            keyGen.generateKey()
        } catch (_: InvalidAlgorithmParameterException) {
            return null
        } catch (_: ProviderException) {
            return null
        }

        return getSecretKeyOrNull()
    }

    /**
     * Returns the [SecretKey] stored in the keystore, or null if there isn't one.
     */
    private fun getSecretKeyOrNull(): SecretKey? {
        try {
            keystore.load(null)
        } catch (_: IllegalArgumentException) {
            // keystore could not be loaded because [param] is unrecognized.
            return null
        } catch (_: IOException) {
            // keystore data format is invalid or the password is incorrect.
            return null
        } catch (_: NoSuchAlgorithmException) {
            // keystore integrity could not be checked due to missing algorithm.
            return null
        } catch (_: CertificateException) {
            // keystore certificates could not be loaded
            return null
        }

        return try {
            keystore.getKey(ENCRYPTION_KEY_NAME, null) as? SecretKey
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
        } catch (_: KeyPermanentlyInvalidatedException) {
            // Biometric has changed
            settingsDiskSource.systemBiometricIntegritySource = null
            false
        } catch (_: UnrecoverableKeyException) {
            // Biometric was disabled and re-enabled
            settingsDiskSource.systemBiometricIntegritySource = null
            false
        } catch (_: InvalidKeyException) {
            // Fallback for old Bitwarden users without a key
            createIntegrityValues(userId)
            true
        }

    /**
     * Validates the keystore key and decrypts it using the user-provided [cipher].
     */
    private fun isSystemBiometricIntegrityValid(userId: String, cipher: Cipher?): Boolean {
        val secretKey = getSecretKeyOrNull()
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

        // Ignore result so biometrics function on devices that are in a state where key generation
        // is not functioning
        createCipherOrNull(userId)
    }
}

private const val ENCRYPTION_KEYSTORE_NAME: String = "AndroidKeyStore"
private const val ENCRYPTION_KEY_NAME: String = "${BuildConfig.APPLICATION_ID}.biometric_integrity"
private const val CIPHER_TRANSFORMATION =
    KeyProperties.KEY_ALGORITHM_AES + "/" +
        KeyProperties.BLOCK_MODE_CBC + "/" +
        KeyProperties.ENCRYPTION_PADDING_PKCS7
