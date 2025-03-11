package com.x8bit.bitwarden.data.platform.manager

/**
 * Abstraction for interacting with Android package manager.
 */
interface BitwardenPackageManager {

    /**
     * Checks if the package is installed.
     */
    fun isPackageInstalled(packageName: String): Boolean

    /**
     * Gets the app name from the package name or null if the package name is not found.
     */
    fun getAppNameFromPackageNameOrNull(packageName: String): String?
}
