package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.policies.Policy
import com.bitwarden.policies.PolicyType
import java.time.Instant

/**
 * Create a mock [Policy] with a given [number].
 */
@Suppress("LongParameterList")
fun createMockSdkPolicy(
    number: Int = 1,
    id: String = "mockId-$number",
    organizationId: String = "mockOrganizationId-$number",
    type: PolicyType = PolicyType.MASTER_PASSWORD,
    data: String? = null,
    enabled: Boolean = false,
    revisionDate: Instant? = null,
): Policy =
    Policy(
        id = id,
        organizationId = organizationId,
        type = type,
        data = data,
        enabled = enabled,
        revisionDate = revisionDate,
    )
