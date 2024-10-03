package com.x8bit.bitwarden.data.platform.util

import com.x8bit.bitwarden.data.platform.datasource.disk.model.ServerConfig
import kotlin.text.split
import kotlin.text.toIntOrNull

private const val VERSION_SEPARATOR = "."
private const val SUFFIX_SEPARATOR = "-"

/**
 * Checks if the server version is greater than another provided version, returns true if it is.
 */
fun isServerVersionAtLeast(serverConfig: ServerConfig?, version: String): Boolean {
    val serverVersion = serverConfig
        ?.serverData
        ?.version

    if (serverVersion.isNullOrEmpty() || version.isEmpty()) {
        return false
    }

    val serverVersionParts = getVersionComponents(serverVersion)
    val otherVersionParts = getVersionComponents(version)

    if (serverVersionParts.isNullOrEmpty() || otherVersionParts.isNullOrEmpty()) {
        return false
    }

    // Must iterate through all indices to establish if versions are equal
    for (i in serverVersionParts.indices) {
        val serverPart = serverVersionParts.getOrNull(i)?.toIntOrNull() ?: 0
        val otherPart = otherVersionParts.getOrNull(i)?.toIntOrNull() ?: 0

        if (serverPart > otherPart) {
            return true
        } else if (serverPart < otherPart) {
            return false
        }
    }

    // Versions are equal
    return true
}

/**
 * Extracts the version components from a version string, disregarding any suffixes.
 */
private fun getVersionComponents(version: String?): List<String>? {
    val versionComponents = version?.split(SUFFIX_SEPARATOR)?.first()
    return versionComponents?.split(VERSION_SEPARATOR)
}
