package com.x8bit.bitwarden.data.platform.datasource.disk.model

/**
 * Location of the key data.
 */
enum class MutualTlsKeyHost {
    /**
     * Key is stored in the system key chain.
     */
    KEY_CHAIN,

    /**
     * Key is stored in a private instance of the Android Key Store.
     */
    ANDROID_KEY_STORE,
}
