package com.bitwarden.authenticator.data.authenticator.manager.util

import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorItem

/**
 * Creates a mock [VerificationCodeItem] for testing purposes.
 *
 * @param number A number used to generate unique values for the mock item.
 * @return A [VerificationCodeItem] with mock data based on the provided number.
 */
@Suppress("LongParameterList")
fun createMockVerificationCodeItem(
    number: Int,
    id: String = "mockId-$number",
    code: String = "mockCode-$number",
    periodSeconds: Int = 30,
    timeLeftSeconds: Int = 120,
    issueTime: Long = 0,
    label: String = "mockLabel-$number",
    issuer: String = "mockIssuer-$number",
    source: AuthenticatorItem.Source = createMockLocalAuthenticatorItemSource(number = number),
): VerificationCodeItem =
    VerificationCodeItem(
        code = code,
        periodSeconds = periodSeconds,
        timeLeftSeconds = timeLeftSeconds,
        issueTime = issueTime,
        id = id,
        label = label,
        issuer = issuer,
        source = source,
    )

/**
 * Creates a mock [AuthenticatorItem.Source.Local] for testing purposes.
 */
fun createMockLocalAuthenticatorItemSource(
    number: Int,
    cipherId: String = "mockId-$number",
    isFavorite: Boolean = false,
): AuthenticatorItem.Source.Local =
    AuthenticatorItem.Source.Local(
        cipherId = cipherId,
        isFavorite = isFavorite,
    )

/**
 * Creates a mock [AuthenticatorItem.Source.Shared] for testing purposes.
 */
fun createMockSharedAuthenticatorItemSource(
    number: Int,
    userId: String = "mockUserId-$number",
    nameOfUser: String? = "mockkNameOfUser-$number",
    email: String = "mockEmail-$number",
    environmentLabel: String = "mockkEnvironmentLabel-$number",
): AuthenticatorItem.Source.Shared =
    AuthenticatorItem.Source.Shared(
        userId = userId,
        nameOfUser = nameOfUser,
        email = email,
        environmentLabel = environmentLabel,
    )
