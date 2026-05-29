package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.policies.PolicyType
import com.bitwarden.policies.PolicyView
import kotlinx.serialization.json.Json

/**
 * Converts a list of network [SyncResponseJson.Policy] models to a list of SDK [PolicyView].
 */
fun List<SyncResponseJson.Policy>.toSdkPolicyViews(): List<PolicyView> =
    this.map { it.toSdkPolicyView() }

/**
 * Converts a network [SyncResponseJson.Policy] model to an SDK [PolicyView].
 */
private fun SyncResponseJson.Policy.toSdkPolicyView(): PolicyView =
    PolicyView(
        organizationId = this.organizationId,
        id = this.id,
        type = this.type.toSdkPolicyType,
        enabled = this.isEnabled,
        data = this.data?.let { Json.encodeToString(it) },
        revisionDate = this.revisionDate,
    )

private val PolicyTypeJson.toSdkPolicyType: PolicyType
    get() = when (this) {
        PolicyTypeJson.TWO_FACTOR_AUTHENTICATION -> PolicyType.TWO_FACTOR_AUTHENTICATION
        PolicyTypeJson.MASTER_PASSWORD -> PolicyType.MASTER_PASSWORD
        PolicyTypeJson.PASSWORD_GENERATOR -> PolicyType.PASSWORD_GENERATOR
        PolicyTypeJson.ONLY_ORG -> PolicyType.SINGLE_ORG
        PolicyTypeJson.REQUIRE_SSO -> PolicyType.REQUIRE_SSO
        PolicyTypeJson.PERSONAL_OWNERSHIP -> PolicyType.ORGANIZATION_DATA_OWNERSHIP
        PolicyTypeJson.DISABLE_SEND -> PolicyType.DISABLE_SEND
        PolicyTypeJson.SEND_OPTIONS -> PolicyType.SEND_OPTIONS
        PolicyTypeJson.RESET_PASSWORD -> PolicyType.RESET_PASSWORD
        PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT -> PolicyType.MAXIMUM_VAULT_TIMEOUT
        PolicyTypeJson.DISABLE_PERSONAL_VAULT_EXPORT -> PolicyType.DISABLE_PERSONAL_VAULT_EXPORT
        PolicyTypeJson.ACTIVATE_AUTOFILL -> PolicyType.ACTIVATE_AUTOFILL
        PolicyTypeJson.AUTOMATIC_APP_LOG_IN -> PolicyType.AUTOMATIC_APP_LOG_IN
        PolicyTypeJson.FREE_FAMILIES_SPONSORSHIP_POLICY -> PolicyType.FREE_FAMILIES_SPONSORSHIP
        PolicyTypeJson.REMOVE_UNLOCK_WITH_PIN -> PolicyType.REMOVE_UNLOCK_WITH_PIN
        PolicyTypeJson.RESTRICT_ITEM_TYPES -> PolicyType.RESTRICTED_ITEM_TYPES
        PolicyTypeJson.URI_MATCH_DEFAULTS -> PolicyType.URI_MATCH_DEFAULTS
        PolicyTypeJson.AUTOTYPE_DEFAULT_SETTING -> PolicyType.AUTOTYPE_DEFAULT_SETTING
        PolicyTypeJson.AUTOMATIC_USER_CONFIRMATION -> PolicyType.AUTOMATIC_USER_CONFIRMATION
        PolicyTypeJson.BLOCK_CLAIMED_DOMAIN_ACCOUNT_CREATION -> {
            PolicyType.BLOCK_CLAIMED_DOMAIN_ACCOUNT_CREATION
        }

        PolicyTypeJson.ORGANIZATION_USER_NOTIFICATION -> PolicyType.ORGANIZATION_USER_NOTIFICATION
        PolicyTypeJson.SEND_CONTROLS -> PolicyType.SEND_CONTROLS
    }
