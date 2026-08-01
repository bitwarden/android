package com.x8bit.bitwarden.data.platform.manager

import com.bitwarden.data.repository.util.baseApiUrl
import com.bitwarden.data.repository.util.baseEventsUrl
import com.bitwarden.data.repository.util.baseIconUrl
import com.bitwarden.data.repository.util.baseIdentityUrl
import com.bitwarden.data.repository.util.baseWebVaultUrlOrNull
import com.bitwarden.data.repository.util.toEnvironmentUrls
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.CustomHeadersDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSource
import com.x8bit.bitwarden.data.platform.util.toUriOrNull
import java.util.UUID

/**
 * Default implementation of [CustomHeadersManager].
 */
class CustomHeadersManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val customHeadersDiskSource: CustomHeadersDiskSource,
    private val environmentDiskSource: EnvironmentDiskSource,
) : CustomHeadersManager {

    override fun getCustomHeaders(url: String): Map<String, String> {
        val environmentUrlData = environmentDiskSource.preAuthEnvironmentUrlData
        val id = environmentUrlData?.customHeadersId ?: return emptyMap()
        val requestUri = url.toUriOrNull() ?: return emptyMap()
        val requestHost = requestUri.host ?: return emptyMap()

        // Scope the headers to the environment's URLs, matching on both scheme and host, so
        // their values, which may contain credentials, are never sent to third parties or
        // downgraded to a cleartext connection.
        val environment = environmentUrlData.toEnvironmentUrls()
        val isEnvironmentUrl = listOfNotNull(
            environment.baseApiUrl,
            environment.baseEventsUrl,
            environment.baseIconUrl,
            environment.baseIdentityUrl,
            environment.baseWebVaultUrlOrNull,
        )
            .mapNotNull { it.toUriOrNull() }
            .any { it.scheme == requestUri.scheme && it.host == requestHost }
        if (!isEnvironmentUrl) return emptyMap()

        return customHeadersDiskSource.getCustomHeaders(id = id).orEmpty()
    }

    override fun getStoredCustomHeaders(id: String): Map<String, String>? =
        customHeadersDiskSource.getCustomHeaders(id = id)

    override fun saveCustomHeaders(headers: Map<String, String>): String {
        val id = UUID.randomUUID().toString()
        customHeadersDiskSource.storeCustomHeaders(id = id, headers = headers)
        return id
    }

    override fun removeCustomHeaders(id: String) {
        if (isIdInUse(id = id)) return
        customHeadersDiskSource.storeCustomHeaders(id = id, headers = null)
    }

    override fun removeCustomHeadersForUser(userId: String) {
        val id = authDiskSource
            .userState
            ?.accounts
            ?.get(userId)
            ?.settings
            ?.environmentUrlData
            ?.customHeadersId
            ?: return
        if (isIdInUse(id = id, excludedUserId = userId)) return
        customHeadersDiskSource.storeCustomHeaders(id = id, headers = null)
    }

    /**
     * Returns whether the pre-auth environment or any account's environment, other than the
     * [excludedUserId] account's, still references the given custom headers [id].
     */
    private fun isIdInUse(id: String, excludedUserId: String? = null): Boolean {
        if (environmentDiskSource.preAuthEnvironmentUrlData?.customHeadersId == id) return true
        return authDiskSource
            .userState
            ?.accounts
            .orEmpty()
            .any { (userId, account) ->
                userId != excludedUserId &&
                    account.settings.environmentUrlData?.customHeadersId == id
            }
    }
}
