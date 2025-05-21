package com.x8bit.bitwarden.data.autofill.accessibility.manager

/**
 * A manager for getting the launcher packages from the operating system.
 */
interface LauncherPackageNameManager {
    /**
     * A list of launcher packages from the operating system.
     */
    val launcherPackages: List<String>
}
