package com.bitwarden.authenticator.data.authenticator.repository.util

import com.bitwarden.authenticator.data.authenticator.datasource.entity.createMockAuthenticatorItemEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuthenticatorItemEntityExtensionsTest {
    @Suppress("MaxLineLength")
    @Test
    fun `toSortAlphabetically should sort ciphers by name`() {
        val list = listOf(
            createMockAuthenticatorItemEntity(1).copy(issuer = "c"),
            createMockAuthenticatorItemEntity(1).copy(issuer = "B"),
            createMockAuthenticatorItemEntity(1).copy(issuer = "z"),
            createMockAuthenticatorItemEntity(1).copy(issuer = "8"),
            createMockAuthenticatorItemEntity(1).copy(issuer = "7"),
            createMockAuthenticatorItemEntity(1).copy(issuer = "_"),
            createMockAuthenticatorItemEntity(1).copy(issuer = "A"),
            createMockAuthenticatorItemEntity(1).copy(issuer = "D"),
            createMockAuthenticatorItemEntity(1).copy(issuer = "AbA"),
            createMockAuthenticatorItemEntity(1).copy(issuer = "aAb"),
        )

        val expected = listOf(
            createMockAuthenticatorItemEntity(1).copy(issuer = "_"),
            createMockAuthenticatorItemEntity(1).copy(issuer = "7"),
            createMockAuthenticatorItemEntity(1).copy(issuer = "8"),
            createMockAuthenticatorItemEntity(1).copy(issuer = "aAb"),
            createMockAuthenticatorItemEntity(1).copy(issuer = "A"),
            createMockAuthenticatorItemEntity(1).copy(issuer = "AbA"),
            createMockAuthenticatorItemEntity(1).copy(issuer = "B"),
            createMockAuthenticatorItemEntity(1).copy(issuer = "c"),
            createMockAuthenticatorItemEntity(1).copy(issuer = "D"),
            createMockAuthenticatorItemEntity(1).copy(issuer = "z"),
        )

        assertEquals(
            expected,
            list.sortAlphabetically(),
        )
    }
}
