package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.policies.PolicyType
import com.bitwarden.policies.PolicyView
import java.time.Instant

/**
 * Create a mock [PolicyView] with a given [number].
 */
@Suppress("LongParameterList")
fun createMockPolicyView(
    number: Int = 1,
    id: String = "mockId-$number",
    organizationId: String = "mockOrganizationId-$number",
    type: PolicyType = PolicyType.MASTER_PASSWORD,
    data: String? = null,
    enabled: Boolean = false,
    revisionDate: Instant? = null,
): PolicyView =
    PolicyView(
        id = id,
        organizationId = organizationId,
        type = type,
        data = data,
        enabled = enabled,
        revisionDate = revisionDate,
    )
