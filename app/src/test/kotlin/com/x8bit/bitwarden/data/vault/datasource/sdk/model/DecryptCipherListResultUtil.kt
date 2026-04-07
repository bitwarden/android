package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.vault.Cipher
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.DecryptCipherListResult

/**
 * Creates a mock [DecryptCipherListResult] for testing purposes.
 */
fun createMockDecryptCipherListResult(
    number: Int,
    successes: List<CipherListView> = listOf(createMockCipherListView(number)),
    failures: List<Cipher> = emptyList(),
): DecryptCipherListResult = DecryptCipherListResult(
    successes = successes,
    failures = failures,
)
