package com.bitwarden.data.manager

/**
 * Abstraction for interacting with Android package manager.
 */
interface BitwardenPackageManager {

    /**
     * Gets the package installation source. The result may be `null` if the package is not
     * installed, the package is a system application, or the installing application has been
     * uninstalled.
     */
    fun getPackageInstallationSourceOrNull(packageName: String): String?

    /**
     * Checks if the package is installed.
     */
    fun isPackageInstalled(packageName: String): Boolean

    /**
     * Gets the app name from the package name or null if the package name is not found.
     */
    fun getAppLabelForPackageOrNull(packageName: String): String?
}
