package com.x8bit.bitwarden.data.auth.repository.util

import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import kotlinx.serialization.json.Json

private val JSON = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/**
 * Maps the given [SyncResponseJson.Profile.Organization] to an [Organization].
 */
fun SyncResponseJson.Profile.Organization.toOrganization(): Organization =
    Organization(
        id = this.id,
        name = this.name,
        shouldUseKeyConnector = this.shouldUseKeyConnector,
        role = this.type,
        shouldManageResetPassword = this.permissions.shouldManageResetPassword,
        keyConnectorUrl = this.keyConnectorUrl,
    )

/**
 * Maps the given list of [SyncResponseJson.Profile.Organization] to a list of
 * [Organization]s.
 */
fun List<SyncResponseJson.Profile.Organization>.toOrganizations(): List<Organization> =
    this.map { it.toOrganization() }

/**
 * Convert the JSON data of the [SyncResponseJson.Policy] object into [PolicyInformation] data.
 */
val SyncResponseJson.Policy.policyInformation: PolicyInformation?
    get() = data?.toString()?.let {

        when (type) {
            PolicyTypeJson.MASTER_PASSWORD -> {
                JSON.decodeFromStringOrNull<PolicyInformation.MasterPassword>(it)
            }

            PolicyTypeJson.PASSWORD_GENERATOR -> {
                JSON.decodeFromStringOrNull<PolicyInformation.PasswordGenerator>(it)
            }

            PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT -> {
                JSON.decodeFromStringOrNull<PolicyInformation.VaultTimeout>(it)
            }

            PolicyTypeJson.SEND_OPTIONS -> {
                JSON.decodeFromStringOrNull<PolicyInformation.SendOptions>(it)
            }

            else -> null
        }
    }
