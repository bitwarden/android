package com.x8bit.bitwarden.data.platform.manager

/**
 * The minimum GMS Core version required for Credential Exchange Protocol (CXP) features.
 */
const val MINIMUM_CXP_GMS_VERSION: Int = 261031035

/**
 * Manages checks against the installed Google Mobile Services (GMS) Core version.
 */
interface GmsManager {

    /**
     * Returns `true` if the installed GMS Core version is at least [version], or `false` if
     * GMS Core is not installed or does not meet the minimum version.
     */
    fun isVersionAtLeast(version: Int): Boolean
}
