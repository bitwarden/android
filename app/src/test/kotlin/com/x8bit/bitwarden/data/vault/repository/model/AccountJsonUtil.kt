package com.x8bit.bitwarden.data.vault.repository.model

import com.bitwarden.network.model.KdfTypeJson
import com.bitwarden.network.model.UserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import java.time.ZonedDateTime

/**
 * Creates a mock [AccountJson.Profile] for testing purposes.
 */
@Suppress("LongParameterList")
fun createMockAccountJsonProfile(
    number: Int,
    userId: String = "mockId-$number",
    email: String = "mockEmail-$number",
    isEmailVerified: Boolean = true,
    name: String? = "mockName-$number",
    stamp: String = "mockSecurityStamp-$number",
    organizationId: String? = "mockOrganizationId-$number",
    avatarColorHex: String? = "mockAvatarColorHex-$number",
    hasPremium: Boolean = false,
    forcePasswordResetReason: ForcePasswordResetReason? = null,
    kdfType: KdfTypeJson? = null,
    kdfIterations: Int? = null,
    kdfMemory: Int? = null,
    kdfParallelism: Int? = null,
    userDecryptionOptions: UserDecryptionOptionsJson? = null,
    isTwoFactorEnabled: Boolean = false,
    creationDate: ZonedDateTime = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
): AccountJson.Profile = AccountJson.Profile(
    userId = userId,
    email = email,
    isEmailVerified = isEmailVerified,
    name = name,
    stamp = stamp,
    organizationId = organizationId,
    avatarColorHex = avatarColorHex,
    hasPremium = hasPremium,
    forcePasswordResetReason = forcePasswordResetReason,
    kdfType = kdfType,
    kdfIterations = kdfIterations,
    kdfMemory = kdfMemory,
    kdfParallelism = kdfParallelism,
    userDecryptionOptions = userDecryptionOptions,
    isTwoFactorEnabled = isTwoFactorEnabled,
    creationDate = creationDate,
)

/**
 * Creates a mock [AccountJson] for testing purposes.
 */
fun createMockAccountJson(
    number: Int,
    profile: AccountJson.Profile = createMockAccountJsonProfile(number),
    tokens: AccountTokensJson = AccountTokensJson(
        accessToken = "accessToken-$number",
        refreshToken = "refreshToken-$number",
    ),
    settings: AccountJson.Settings = AccountJson.Settings(
        environmentUrlData = null,
    ),
): AccountJson = AccountJson(
    profile = profile,
    tokens = tokens,
    settings = settings,
)
