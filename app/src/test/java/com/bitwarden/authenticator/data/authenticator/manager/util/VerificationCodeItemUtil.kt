package com.bitwarden.authenticator.data.authenticator.manager.util

import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem

/**
 * Creates a mock [VerificationCodeItem] for testing purposes.
 *
 * @param number A number used to generate unique values for the mock item.
 * @param favorite Whether the mock item should be marked as favorite. Defaults to false.
 * @return A [VerificationCodeItem] with mock data based on the provided number.
 */
fun createMockVerificationCodeItem(
    number: Int,
    favorite: Boolean = false,
): VerificationCodeItem =
    VerificationCodeItem(
        code = "mockCode-$number",
        totpCode = "mockTotpCode-$number",
        periodSeconds = 30,
        timeLeftSeconds = 120,
        issueTime = 0,
        id = "mockId-$number",
        username = "mockUsername-$number",
        issuer = "mockIssuer-$number",
        favorite = favorite,
    )
