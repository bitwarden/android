package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.exporters.Account

/**
 * Creates a mock [com.bitwarden.exporters.Account] for testing purposes
 */
fun createMockAccount(
    number: Int,
    email: String = "mockEmail-$number",
    name: String? = "mockName-$number",
): Account = Account(
    id = "mockId-$number",
    email = email,
    name = name,
)
