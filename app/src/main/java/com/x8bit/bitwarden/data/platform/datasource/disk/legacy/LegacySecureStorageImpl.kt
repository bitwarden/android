@file:Suppress("DEPRECATION")

package com.x8bit.bitwarden.data.platform.datasource.disk.legacy

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.Key
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.security.auth.x500.X500Principal

/**
 * Primary implementation of [LegacySecureStorage].
 *
 * Apart from removing the ability to store new data (rather than just retrieve it) this is adapted
 * with minimal modifications from the [Xamarin.Essentials.SecureStorage source code](https://github.com/xamarin/Essentials/blob/main/Xamarin.Essentials/SecureStorage/SecureStorage.android.cs)
 */
@Suppress(
    "NestedBlockDepth",
    "TooGenericExceptionCaught",
    "MagicNumber",
)
@OmitFromCoverage
class LegacySecureStorageImpl(
    private val context: Context,
) : LegacySecureStorage {
    private val alias = "${context.packageName}.xamarinessentials"
    private val sharedPreferences = context.getSharedPreferences(alias, Context.MODE_PRIVATE)
    private val locker = Any()

    private val legacyKeyHashFallback: Boolean = true

    override fun get(key: String): String? {
        if (key.isBlank()) return null
        return platformGetAsync(key)
    }

    override fun getRawKeys(): Set<String> =
        sharedPreferences.all.keys

    override fun remove(key: String) {
        platformRemove(key)
    }

    override fun removeAll() {
        platformRemoveAll()
    }

    private fun platformGetAsync(key: String): String? {

        var encStr: String? = null
        var foundLegacyValue = false

        if (legacyKeyHashFallback) {
            if (!sharedPreferences.all.containsKey(key)) {
                val md5Key = md5Hash(key)
                if (sharedPreferences.all.containsKey(md5Key)) {
                    encStr = sharedPreferences.getString(md5Key, null)
                    sharedPreferences.edit { putString(key, encStr) }
                    foundLegacyValue = true

                    try {
                        sharedPreferences.edit { remove(md5Key) }
                    } catch (e: Exception) {
                        // no-op
                    }
                }
            }
        }

        if (!foundLegacyValue) {
            encStr = sharedPreferences.getString(key, null)
        }

        var decryptedData: String? = null
        if (!encStr.isNullOrBlank()) {
            try {
                val encData = Base64.decode(encStr, Base64.DEFAULT)
                synchronized(locker) {
                    val ks = AndroidKeyStore(
                        legacySecureStorage = this,
                        sharedPreferences = sharedPreferences,
                        context = context,
                        keystoreAlias = alias,
                        alwaysUseAsymmetricKeyStorage = false,
                    )
                    decryptedData = ks.decrypt(encData)
                }
            } catch (e: AEADBadTagException) {
                remove(key)
            }
        }

        return decryptedData
    }

    private fun platformRemove(key: String): Boolean {
        sharedPreferences.edit { remove(key) }
        checkForAndRemoveLegacyKey(key)
        return true
    }

    private fun checkForAndRemoveLegacyKey(key: String) {
        if (legacyKeyHashFallback) {
            val md5Key = md5Hash(key)
            if (sharedPreferences.all.containsKey(md5Key)) {
                try {
                    sharedPreferences.edit { remove(md5Key) }
                } catch (e: Exception) {
                    // no-op
                }
            }
        }
    }

    private fun platformRemoveAll() {
        sharedPreferences.edit { clear() }
    }

    private fun md5Hash(input: String): String {
        val hash = StringBuilder()
        val md5provider = MessageDigest.getInstance("MD5")
        val bytes = md5provider.digest(input.toByteArray())

        for (i in bytes.indices) {
            hash.append(String.format("%02x", bytes[i]))
        }

        return hash.toString()
    }
}

@Suppress("MagicNumber")
@OmitFromCoverage
private class AndroidKeyStore(
    private val legacySecureStorage: LegacySecureStorage,
    private val sharedPreferences: SharedPreferences,
    private val context: Context,
    keystoreAlias: String,
    alwaysUseAsymmetricKeyStorage: Boolean,
) {
    private val alwaysUseAsymmetricKey: Boolean = alwaysUseAsymmetricKeyStorage
    private val alias: String = keystoreAlias
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
    private val useSymmetricPreferenceKey: String = "essentials_use_symmetric"

    private val prefsMasterKey = "SecureStorageKey"
    private val initializationVectorLen = 12 // Android supports an IV of 12 for AES/GCM

    init {
        keyStore.load(null)
    }

    private fun getKey(): SecretKey {
        val useSymmetric = sharedPreferences.getBoolean(
            useSymmetricPreferenceKey,
            true,
        )

        return if (useSymmetric && !alwaysUseAsymmetricKey) {
            getSymmetricKey()
        } else {
            val keyPair = getAsymmetricKeyPair()

            val existingKeyStr = sharedPreferences.getString(prefsMasterKey, null)

            if (!existingKeyStr.isNullOrBlank()) {
                try {
                    val wrappedKey = Base64.decode(existingKeyStr, Base64.DEFAULT)
                    val unwrappedKey = unwrapKey(wrappedKey, keyPair.private)
                    return unwrappedKey as SecretKey
                } catch (ikEx: InvalidKeyException) {
                    // no-op
                } catch (ibsEx: IllegalBlockSizeException) {
                    // no-op
                } catch (paddingEx: BadPaddingException) {
                    // no-op
                }
                legacySecureStorage.removeAll()
            }

            val keyGenerator = KeyGenerator.getInstance("AES")
            val defSymmetricKey = keyGenerator.generateKey()

            val newWrappedKey = wrapKey(defSymmetricKey, keyPair.public)

            sharedPreferences.edit {
                putString(
                    prefsMasterKey,
                    Base64.encodeToString(newWrappedKey, Base64.DEFAULT),
                )
            }

            defSymmetricKey
        }
    }

    private fun getSymmetricKey(): SecretKey {
        sharedPreferences.edit {
            putBoolean(useSymmetricPreferenceKey, true)
        }

        val existingKey = keyStore.getKey(alias, null)

        return if (existingKey != null) {
            existingKey as SecretKey
        } else {
            val keyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val builder = KeyGenParameterSpec
                .Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(false)

            keyGenerator.init(builder.build())

            keyGenerator.generateKey()
        }
    }

    private fun getAsymmetricKeyPair(): KeyPair {
        sharedPreferences.edit {
            putBoolean(useSymmetricPreferenceKey, false)
        }

        val asymmetricAlias = "$alias.asymmetric"
        val privateKey = keyStore.getKey(asymmetricAlias, null) as PrivateKey?
        val publicKey = keyStore.getCertificate(asymmetricAlias)?.publicKey

        return if (privateKey != null && publicKey != null) {
            KeyPair(publicKey, privateKey)
        } else {
            val originalLocale = Locale.getDefault()
            try {
                Locale.setDefault(Locale.ENGLISH)
                val generator =
                    KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
                val end = ZonedDateTime.now(ZoneOffset.UTC).plusYears(20)
                val startDate = Date()

                @Suppress("DEPRECATION")
                val endDate = Date(end.year, end.month.value, end.dayOfMonth)

                @Suppress("DEPRECATION")
                val builder = KeyPairGeneratorSpec.Builder(context)
                    .setAlias(asymmetricAlias)
                    .setSerialNumber(BigInteger.ONE)
                    .setSubject(X500Principal("CN=$asymmetricAlias CA Certificate"))
                    .setStartDate(startDate)
                    .setEndDate(endDate)

                @Suppress("DEPRECATION")
                generator.initialize(builder.build())
                generator.generateKeyPair()
            } finally {
                Locale.setDefault(originalLocale)
            }
        }
    }

    private fun wrapKey(keyToWrap: Key, withKey: Key): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.WRAP_MODE, withKey)
        return cipher.wrap(keyToWrap)
    }

    private fun unwrapKey(wrappedData: ByteArray, withKey: Key): Key {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.UNWRAP_MODE, withKey)
        return cipher.unwrap(
            wrappedData,
            KeyProperties.KEY_ALGORITHM_AES,
            Cipher.SECRET_KEY,
        ) as SecretKey
    }

    @SuppressLint("GetInstance")
    fun decrypt(data: ByteArray): String? {
        if (data.size < initializationVectorLen) {
            return null
        }

        val key = getKey()

        val iv = ByteArray(initializationVectorLen)
        System.arraycopy(data, 0, iv, 0, initializationVectorLen)

        val cipher = try {
            Cipher.getInstance("AES/GCM/NoPadding")
        } catch (e: NoSuchAlgorithmException) {
            Cipher.getInstance("AES/ECB/PKCS5Padding") // Fallback for old devices
        }

        try {
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        } catch (e: InvalidAlgorithmParameterException) {
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        }

        val decryptedData =
            cipher.doFinal(data, initializationVectorLen, data.size - initializationVectorLen)

        return String(decryptedData, StandardCharsets.UTF_8)
    }
}
