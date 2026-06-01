package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.createMockPolicy
import com.bitwarden.policies.PolicyType
import com.bitwarden.policies.PolicyView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockPolicyView
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class VaultSdkPolicyExtensionsTest {

    @Test
    fun `toSdkPolicyViews should return empty list when given empty list`() {
        assertEquals(
            emptyList<PolicyView>(),
            emptyList<SyncResponseJson.Policy>().toSdkPolicyViews(),
        )
    }

    @Test
    fun `toSdkPolicyViews should convert all policies in a list`() {
        assertEquals(
            listOf(
                createMockPolicyView(number = 1),
                createMockPolicyView(number = 2),
            ),
            listOf(
                createMockPolicy(number = 1),
                createMockPolicy(number = 2),
            )
                .toSdkPolicyViews(),
        )
    }

    @Test
    fun `toSdkPolicyViews should serialize JsonObject data to a JSON string`() {
        assertEquals(
            listOf(createMockPolicyView(data = """{"key":"value"}""")),
            listOf(
                createMockPolicy(data = buildJsonObject { put("key", JsonPrimitive("value")) }),
            )
                .toSdkPolicyViews(),
        )
    }

    @Test
    fun `toSdkPolicyViews should map all PolicyTypeJson values to their SDK equivalents`() {
        POLICY_TYPE_MAP.forEach { (inputType, expectedType) ->
            assertEquals(
                listOf(createMockPolicyView(type = expectedType)),
                listOf(createMockPolicy(type = inputType)).toSdkPolicyViews(),
            )
        }
    }
}

private val POLICY_TYPE_MAP: Map<PolicyTypeJson, PolicyType> = mapOf(
    PolicyTypeJson.TWO_FACTOR_AUTHENTICATION to PolicyType.TWO_FACTOR_AUTHENTICATION,
    PolicyTypeJson.MASTER_PASSWORD to PolicyType.MASTER_PASSWORD,
    PolicyTypeJson.PASSWORD_GENERATOR to PolicyType.PASSWORD_GENERATOR,
    PolicyTypeJson.ONLY_ORG to PolicyType.SINGLE_ORG,
    PolicyTypeJson.REQUIRE_SSO to PolicyType.REQUIRE_SSO,
    PolicyTypeJson.PERSONAL_OWNERSHIP to PolicyType.ORGANIZATION_DATA_OWNERSHIP,
    PolicyTypeJson.DISABLE_SEND to PolicyType.DISABLE_SEND,
    PolicyTypeJson.SEND_OPTIONS to PolicyType.SEND_OPTIONS,
    PolicyTypeJson.RESET_PASSWORD to PolicyType.RESET_PASSWORD,
    PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT to PolicyType.MAXIMUM_VAULT_TIMEOUT,
    PolicyTypeJson.DISABLE_PERSONAL_VAULT_EXPORT to PolicyType.DISABLE_PERSONAL_VAULT_EXPORT,
    PolicyTypeJson.ACTIVATE_AUTOFILL to PolicyType.ACTIVATE_AUTOFILL,
    PolicyTypeJson.AUTOMATIC_APP_LOG_IN to PolicyType.AUTOMATIC_APP_LOG_IN,
    PolicyTypeJson.FREE_FAMILIES_SPONSORSHIP_POLICY to PolicyType.FREE_FAMILIES_SPONSORSHIP,
    PolicyTypeJson.REMOVE_UNLOCK_WITH_PIN to PolicyType.REMOVE_UNLOCK_WITH_PIN,
    PolicyTypeJson.RESTRICT_ITEM_TYPES to PolicyType.RESTRICTED_ITEM_TYPES,
    PolicyTypeJson.URI_MATCH_DEFAULTS to PolicyType.URI_MATCH_DEFAULTS,
    PolicyTypeJson.AUTOTYPE_DEFAULT_SETTING to PolicyType.AUTOTYPE_DEFAULT_SETTING,
    PolicyTypeJson.AUTOMATIC_USER_CONFIRMATION to PolicyType.AUTOMATIC_USER_CONFIRMATION,
    PolicyTypeJson.BLOCK_CLAIMED_DOMAIN_ACCOUNT_CREATION to
        PolicyType.BLOCK_CLAIMED_DOMAIN_ACCOUNT_CREATION,
    PolicyTypeJson.ORGANIZATION_USER_NOTIFICATION to PolicyType.ORGANIZATION_USER_NOTIFICATION,
    PolicyTypeJson.SEND_CONTROLS to PolicyType.SEND_CONTROLS,
)
