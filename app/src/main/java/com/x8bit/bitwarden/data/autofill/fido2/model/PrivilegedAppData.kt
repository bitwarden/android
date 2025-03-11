package com.x8bit.bitwarden.data.autofill.fido2.model

/**
 * Represents privileged applications that are trusted by various sources.
 */
data class PrivilegedAppData(
    val googleTrustedApps: PrivilegedAppAllowListJson,
    val communityTrustedApps: PrivilegedAppAllowListJson,
    val userTrustedApps: PrivilegedAppAllowListJson,
)
