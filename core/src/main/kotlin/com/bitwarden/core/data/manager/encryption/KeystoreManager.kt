package com.bitwarden.core.data.manager.encryption

import java.security.Key

/**
 * Responsible for storing, retrieving, and removing symmetric keys in the Android Keystore.
 *
 * All aliases are automatically namespaced with the application ID before being written to or
 * read from the keystore.
 */
interface KeystoreManager {
    /**
     * Returns the AES [Key] stored under [alias], generating and storing a new key if one
     * does not already exist.
     */
    fun getOrCreateKey(alias: String): Result<Key>

    /**
     * Returns the AES [Key] stored under [alias], or `null` if no key exists or the key
     * could not be recovered.
     */
    fun getKeyOrNull(alias: String): Result<Key?>

    /**
     * Returns `true` if a key exists in the keystore under [alias].
     */
    fun hasKey(alias: String): Boolean

    /**
     * Removes the key stored under [alias] from the keystore. Returns `true` if the entry was
     * removed successfully or `false` if the operation failed.
     */
    fun removeKey(alias: String): Boolean
}
