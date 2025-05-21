package com.x8bit.bitwarden.data.platform.manager

/**
 * Manages reading assets.
 */
interface AssetManager {

    /**
     * Read [fileName] from the assets folder. A successful result will contain the contents as a
     * String.
     */
    suspend fun readAsset(fileName: String): Result<String>
}
