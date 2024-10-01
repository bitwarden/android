package com.x8bit.bitwarden.data.platform.util

import com.x8bit.bitwarden.data.platform.repository.ServerConfigRepository
import kotlin.text.split
import kotlin.text.toIntOrNull

private const val VERSION_SEPARATOR = "."
private const val SUFFIX_SEPARATOR = "-"

/**
 * Checks if the server version is greater than another provided version, returns true if it is.
 */
 fun isServerVersionAtLeast(serverConfig: ServerConfigRepository, version: String): Boolean {
    val serverVersion = serverConfig
        .getLocalServerConfig()
        ?.serverData
        ?.version

    val serverVersionParts = getVersionComponents(serverVersion)
    val otherVersionParts = getVersionComponents(version)

    if (serverVersionParts == null || otherVersionParts == null) {
        return false
    }

    for (i in serverVersionParts.indices) {
        val serverPart = serverVersionParts.getOrNull(i)?.toIntOrNull() ?: 0
        val otherPart = otherVersionParts.getOrNull(i)?.toIntOrNull() ?: 0

        if (serverPart > otherPart) {
            return true
        } else if (serverPart < otherPart) {
            return false
        }
    }

    return true // Versions are equal
}

/**
 * Extracts the version components from a version string, disregarding any suffixes.
 */
private fun getVersionComponents(version: String?): List<String>? {
    val versionComponents = version?.split(SUFFIX_SEPARATOR)?.first()
    return versionComponents?.split(VERSION_SEPARATOR)
}
