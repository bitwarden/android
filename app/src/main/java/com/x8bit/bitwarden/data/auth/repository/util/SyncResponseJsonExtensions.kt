package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson

/**
 * Maps the given [SyncResponseJson.Profile.Organization] to an [Organization].
 */
fun SyncResponseJson.Profile.Organization.toOrganization(): Organization =
    Organization(
        id = this.id,
        name = this.name,
    )

/**
 * Maps the given list of [SyncResponseJson.Profile.Organization] to a list of
 * [Organization]s.
 */
fun List<SyncResponseJson.Profile.Organization>.toOrganizations(): List<Organization> =
    this.map { it.toOrganization() }
