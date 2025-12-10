package com.bitwarden.authenticator.data.platform.manager

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.authenticator.BuildConfig
import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import com.bitwarden.authenticator.data.platform.datasource.disk.SettingsDiskSource
import timber.log.Timber
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

    override fun createCipherOrNull(): Cipher? {
        val secretKey: SecretKey = generateKeyOrNull() ?: run {
            // user removed all biometrics from the device
            destroyBiometrics()
            return null
        }
        val cipher = try {
            Cipher.getInstance(CIPHER_TRANSFORMATION)
        } catch (nsae: NoSuchAlgorithmException) {
            Timber.w(nsae, "createCipherOrNull failed to get cipher instance")
            return null
        } catch (nspe: NoSuchPaddingException) {
            Timber.w(nspe, "createCipherOrNull failed to get cipher instance")
            return null
        }
        // Instantiate integrity values.
        createIntegrityValues()
        // This should never fail to initialize / return false because the cipher is newly generated
        cipher.initializeCipher(secretKey = secretKey)
        return cipher
    }

    override fun clearBiometrics() {
        settingsDiskSource.systemBiometricIntegritySource?.let { systemBioIntegrityState ->
            settingsDiskSource.storeAccountBiometricIntegrityValidity(
                systemBioIntegrityState = systemBioIntegrityState,
                value = null,
            )
        }
        authDiskSource.storeUserBiometricUnlockKey(biometricsKey = null)
        authDiskSource.userBiometricKeyInitVector = null
        keystore.deleteEntry(ENCRYPTION_KEY_NAME)
    }

    override fun getOrCreateCipher(): Cipher? {
        // Attempt to get the key. If that fails, then we need to generate a new one.
        val secretKey: SecretKey = getSecretKeyOrNull()
            ?: generateKeyOrNull()
            ?: run {
                // user removed all biometrics from the device
                destroyBiometrics()
                return null
            }

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val isCipherInitialized = cipher.initializeCipher(secretKey = secretKey)
        return cipher?.takeIf { isCipherInitialized }
    }

    override fun isBiometricIntegrityValid(): Boolean =
        isSystemBiometricIntegrityValid() && isAccountBiometricIntegrityValid()

    override fun isAccountBiometricIntegrityValid(): Boolean {
        val systemBioIntegrityState = settingsDiskSource
            .systemBiometricIntegritySource
            ?: return false
        return settingsDiskSource
            .getAccountBiometricIntegrityValidity(
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
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ENCRYPTION_KEYSTORE_NAME)
        } catch (nsae: NoSuchAlgorithmException) {
            Timber.w(nsae, "generateKeyOrNull failed to get key generator instance")
            return null
        } catch (nspe: NoSuchProviderException) {
            Timber.w(nspe, "generateKeyOrNull failed to get key generator instance")
            return null
        } catch (iae: IllegalArgumentException) {
            Timber.w(iae, "generateKeyOrNull failed to get key generator instance")
            return null
        }

        return try {
            keyGen.init(keyGenParameterSpec)
            keyGen.generateKey()
        } catch (iape: InvalidAlgorithmParameterException) {
            Timber.w(iape, "generateKeyOrNull failed to initialize and generate key")
            null
        } catch (pe: ProviderException) {
            Timber.w(pe, "generateKeyOrNull failed to initialize and generate key")
            null
        }
    }

    /**
     * Returns the [SecretKey] stored in the keystore, or null if there isn't one.
     */
    private fun getSecretKeyOrNull(): SecretKey? =
        try {
            keystore.getKey(ENCRYPTION_KEY_NAME, null)?.let { it as SecretKey }
        } catch (kse: KeyStoreException) {
            // keystore was not loaded
            Timber.w(kse, "getSecretKeyOrNull failed to retrieve secret key")
            null
        } catch (nsae: NoSuchAlgorithmException) {
            // keystore algorithm cannot be found
            Timber.w(nsae, "getSecretKeyOrNull failed to retrieve secret key")
            null
        } catch (uke: UnrecoverableKeyException) {
            // key could not be recovered
            Timber.w(uke, "getSecretKeyOrNull failed to retrieve secret key")
            null
        }

    /**
     * Initialize a [Cipher] and return a boolean indicating whether it is valid.
     */
    private fun Cipher.initializeCipher(secretKey: SecretKey): Boolean =
        try {
            authDiskSource
                .userBiometricKeyInitVector
                ?.let { init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(it)) }
                ?: init(Cipher.ENCRYPT_MODE, secretKey)
            true
        } catch (kpie: KeyPermanentlyInvalidatedException) {
            // Biometric has changed
            Timber.w(kpie, "initializeCipher failed to initialize cipher")
            destroyBiometrics()
            false
        } catch (uke: UnrecoverableKeyException) {
            // Biometric was disabled and re-enabled
            Timber.w(uke, "initializeCipher failed to initialize cipher")
            destroyBiometrics()
            false
        } catch (ike: InvalidKeyException) {
            // User has no key
            Timber.w(ike, "initializeCipher failed to initialize cipher")
            destroyBiometrics()
            true
        }

    /**
     * Validates the keystore key and decrypts it, if decryption is successful `true` is returned,
     * `false` otherwise.
     */
    private fun isSystemBiometricIntegrityValid(): Boolean {
        // Attempt to get the user scoped key. If that fails, we check to see if a legacy key
        // is available.
        val cipher = getOrCreateCipher()
        val secretKey = getSecretKeyOrNull()
        return if (cipher != null && secretKey != null) {
            cipher.initializeCipher(secretKey = secretKey)
        } else {
            false
        }
    }

    private fun createIntegrityValues() {
        val systemBiometricIntegritySource = settingsDiskSource
            .systemBiometricIntegritySource
            ?: UUID.randomUUID().toString()
        settingsDiskSource.systemBiometricIntegritySource = systemBiometricIntegritySource
        settingsDiskSource.storeAccountBiometricIntegrityValidity(
            systemBioIntegrityState = systemBiometricIntegritySource,
            value = true,
        )
    }

    private fun destroyBiometrics() {
        clearBiometrics()
        settingsDiskSource.systemBiometricIntegritySource = null
    }
}

private const val ENCRYPTION_KEYSTORE_NAME: String = "AndroidKeyStore"
private const val ENCRYPTION_KEY_NAME: String = "${BuildConfig.APPLICATION_ID}.biometric_integrity"
private const val CIPHER_TRANSFORMATION =
    KeyProperties.KEY_ALGORITHM_AES + "/" +
        KeyProperties.BLOCK_MODE_CBC + "/" +
        KeyProperties.ENCRYPTION_PADDING_PKCS7
