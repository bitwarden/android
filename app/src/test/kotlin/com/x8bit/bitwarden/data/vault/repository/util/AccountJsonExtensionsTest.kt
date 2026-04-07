package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.exporters.Account
import com.x8bit.bitwarden.data.vault.repository.model.createMockAccountJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AccountJsonExtensionsTest {

    @Test
    fun `toAccountData returns populated AccountData when account is non-null`() {
        val account = createMockAccountJson(number = 1)
        assertEquals(
            Account(
                id = "mockId-1",
                email = "mockEmail-1",
                name = "mockName-1",
            ),
            account.toSdkAccount(),
        )
    }
}
