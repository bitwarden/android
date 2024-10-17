package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.util

import com.bitwarden.authenticator.data.authenticator.manager.util.createMockVerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorItem
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.VerificationCodeDisplayItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VerificationCodeItemExtensionsTest {

    @Test
    fun `toDisplayItem should map Local items correctly`() {
        val alertThresholdSeconds = 7
        val favoriteItem = createMockVerificationCodeItem(number = 1, favorite = true)
        val nonFavoriteItem = createMockVerificationCodeItem(number = 2)

        val expectedFavoriteItem = VerificationCodeDisplayItem(
            id = favoriteItem.id,
            issuer = favoriteItem.issuer,
            label = favoriteItem.accountName,
            timeLeftSeconds = favoriteItem.timeLeftSeconds,
            periodSeconds = favoriteItem.periodSeconds,
            alertThresholdSeconds = alertThresholdSeconds,
            authCode = favoriteItem.code,
            favorite = (favoriteItem.source as AuthenticatorItem.Source.Local).isFavorite,
            allowLongPressActions = true,
        )

        val expectedNonFavoriteItem = VerificationCodeDisplayItem(
            id = nonFavoriteItem.id,
            issuer = nonFavoriteItem.issuer,
            label = nonFavoriteItem.accountName,
            timeLeftSeconds = nonFavoriteItem.timeLeftSeconds,
            periodSeconds = nonFavoriteItem.periodSeconds,
            alertThresholdSeconds = alertThresholdSeconds,
            authCode = nonFavoriteItem.code,
            favorite = (nonFavoriteItem.source as AuthenticatorItem.Source.Local).isFavorite,
            allowLongPressActions = true,
        )

        assertEquals(expectedFavoriteItem, favoriteItem.toDisplayItem(alertThresholdSeconds))
        assertEquals(expectedNonFavoriteItem, nonFavoriteItem.toDisplayItem(alertThresholdSeconds))
    }

    @Test
    fun `toDisplayItem should map Shared items correctly`() {
        val alertThresholdSeconds = 7
        val favoriteItem = createMockVerificationCodeItem(number = 1, favorite = true)
            .copy(
                source = AuthenticatorItem.Source.Shared(
                    userId = "1",
                    nameOfUser = "John Doe",
                    email = "test@bitwarden.com",
                    environmentLabel = "bitwarden.com",
                ),
            )

        val expectedFavoriteItem = VerificationCodeDisplayItem(
            id = favoriteItem.id,
            issuer = favoriteItem.issuer,
            label = favoriteItem.accountName,
            timeLeftSeconds = favoriteItem.timeLeftSeconds,
            periodSeconds = favoriteItem.periodSeconds,
            alertThresholdSeconds = alertThresholdSeconds,
            authCode = favoriteItem.code,
            favorite = false,
            allowLongPressActions = false,
        )

        assertEquals(expectedFavoriteItem, favoriteItem.toDisplayItem(alertThresholdSeconds))
    }
}
