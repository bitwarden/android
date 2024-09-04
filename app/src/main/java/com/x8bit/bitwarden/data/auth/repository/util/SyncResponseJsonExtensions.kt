package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.platform.util.decodeFromStringOrNull
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import kotlinx.serialization.json.Json

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
        var json = Json {
            ignoreUnknownKeys = true
        }

        when (type) {
            PolicyTypeJson.MASTER_PASSWORD -> {
                json.decodeFromStringOrNull<PolicyInformation.MasterPassword>(it)
            }

            PolicyTypeJson.PASSWORD_GENERATOR -> {
                json.decodeFromStringOrNull<PolicyInformation.PasswordGenerator>(it)
            }

            PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT -> {
                json.decodeFromStringOrNull<PolicyInformation.VaultTimeout>(it)
            }

            PolicyTypeJson.SEND_OPTIONS -> {
                json.decodeFromStringOrNull<PolicyInformation.SendOptions>(it)
            }

            else -> null
        }
    }
