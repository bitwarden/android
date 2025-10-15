package com.bitwarden.authenticator.ui.authenticator.feature.util

import com.bitwarden.authenticator.data.authenticator.manager.util.createMockLocalAuthenticatorItemSource
import com.bitwarden.authenticator.data.authenticator.manager.util.createMockSharedAuthenticatorItemSource
import com.bitwarden.authenticator.data.authenticator.manager.util.createMockVerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorItem
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.ui.platform.components.listitem.model.VerificationCodeDisplayItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VerificationCodeItemExtensionsTest {

    @Test
    fun `toDisplayItem should map Local items correctly`() {
        val alertThresholdSeconds = 7
        val favoriteItem = createMockVerificationCodeItem(
            number = 1,
            source = createMockLocalAuthenticatorItemSource(number = 1, isFavorite = true),
        )
        val nonFavoriteItem = createMockVerificationCodeItem(number = 2)

        val expectedFavoriteItem = VerificationCodeDisplayItem(
            id = favoriteItem.id,
            title = favoriteItem.issuer!!,
            subtitle = favoriteItem.label,
            timeLeftSeconds = favoriteItem.timeLeftSeconds,
            periodSeconds = favoriteItem.periodSeconds,
            alertThresholdSeconds = alertThresholdSeconds,
            authCode = favoriteItem.code,
            favorite = (favoriteItem.source as AuthenticatorItem.Source.Local).isFavorite,
            allowLongPressActions = true,
            showMoveToBitwarden = false,
        )

        val expectedNonFavoriteItem = VerificationCodeDisplayItem(
            id = nonFavoriteItem.id,
            title = nonFavoriteItem.issuer!!,
            subtitle = nonFavoriteItem.label,
            timeLeftSeconds = nonFavoriteItem.timeLeftSeconds,
            periodSeconds = nonFavoriteItem.periodSeconds,
            alertThresholdSeconds = alertThresholdSeconds,
            authCode = nonFavoriteItem.code,
            favorite = (nonFavoriteItem.source as AuthenticatorItem.Source.Local).isFavorite,
            allowLongPressActions = true,
            showMoveToBitwarden = false,
        )

        assertEquals(
            expectedFavoriteItem,
            favoriteItem.toDisplayItem(
                alertThresholdSeconds = alertThresholdSeconds,
                sharedVerificationCodesState = SharedVerificationCodesState.Error,
                allowLongPressActions = true,
            ),
        )
        assertEquals(
            expectedNonFavoriteItem,
            nonFavoriteItem.toDisplayItem(
                alertThresholdSeconds = alertThresholdSeconds,
                sharedVerificationCodesState = SharedVerificationCodesState.Error,
                allowLongPressActions = true,
            ),
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `toDisplayItem should only showMoveToBitwarden when SharedVerificationCodesState is Success`() {
        val alertThresholdSeconds = 7
        val item = createMockVerificationCodeItem(1)
        val expectedDontShowMoveToBitwardenItem =
            VerificationCodeDisplayItem(
                id = item.id,
                title = item.issuer!!,
                subtitle = item.label,
                timeLeftSeconds = item.timeLeftSeconds,
                periodSeconds = item.periodSeconds,
                alertThresholdSeconds = alertThresholdSeconds,
                authCode = item.code,
                favorite = false,
                allowLongPressActions = true,
                showMoveToBitwarden = false,
            )

        assertEquals(
            expectedDontShowMoveToBitwardenItem,
            item.toDisplayItem(
                alertThresholdSeconds = alertThresholdSeconds,
                sharedVerificationCodesState = SharedVerificationCodesState.AppNotInstalled,
                allowLongPressActions = true,
            ),
        )
        assertEquals(
            expectedDontShowMoveToBitwardenItem,
            item.toDisplayItem(
                alertThresholdSeconds = alertThresholdSeconds,
                sharedVerificationCodesState = SharedVerificationCodesState.Error,
                allowLongPressActions = true,
            ),
        )
        assertEquals(
            expectedDontShowMoveToBitwardenItem,
            item.toDisplayItem(
                alertThresholdSeconds = alertThresholdSeconds,
                sharedVerificationCodesState = SharedVerificationCodesState.FeatureNotEnabled,
                allowLongPressActions = true,
            ),
        )
        assertEquals(
            expectedDontShowMoveToBitwardenItem,
            item.toDisplayItem(
                alertThresholdSeconds = alertThresholdSeconds,
                sharedVerificationCodesState = SharedVerificationCodesState.Loading,
                allowLongPressActions = true,
            ),
        )
        assertEquals(
            expectedDontShowMoveToBitwardenItem,
            item.toDisplayItem(
                alertThresholdSeconds = alertThresholdSeconds,
                sharedVerificationCodesState = SharedVerificationCodesState.OsVersionNotSupported,
                allowLongPressActions = true,
            ),
        )
        assertEquals(
            expectedDontShowMoveToBitwardenItem,
            item.toDisplayItem(
                alertThresholdSeconds = alertThresholdSeconds,
                sharedVerificationCodesState = SharedVerificationCodesState.SyncNotEnabled,
                allowLongPressActions = true,
            ),
        )

        val expectedShouldShowMoveToBitwardenItem = expectedDontShowMoveToBitwardenItem.copy(
            showMoveToBitwarden = true,
        )
        assertEquals(
            expectedShouldShowMoveToBitwardenItem,
            item.toDisplayItem(
                alertThresholdSeconds = alertThresholdSeconds,
                sharedVerificationCodesState = SharedVerificationCodesState.Success(emptyList()),
                allowLongPressActions = true,
            ),
        )
    }

    @Test
    fun `toDisplayItem should map Shared items correctly`() {
        val alertThresholdSeconds = 7
        val favoriteItem = createMockVerificationCodeItem(
            number = 1,
            source = createMockSharedAuthenticatorItemSource(
                number = 1,
                userId = "1",
                nameOfUser = "John Doe",
                email = "test@bitwarden.com",
                environmentLabel = "bitwarden.com",
            ),
        )

        val expectedFavoriteItem = VerificationCodeDisplayItem(
            id = favoriteItem.id,
            title = favoriteItem.issuer!!,
            subtitle = favoriteItem.label,
            timeLeftSeconds = favoriteItem.timeLeftSeconds,
            periodSeconds = favoriteItem.periodSeconds,
            alertThresholdSeconds = alertThresholdSeconds,
            authCode = favoriteItem.code,
            favorite = false,
            allowLongPressActions = false,
            showMoveToBitwarden = false,
        )

        assertEquals(
            expectedFavoriteItem,
            favoriteItem.toDisplayItem(
                alertThresholdSeconds = alertThresholdSeconds,
                sharedVerificationCodesState = SharedVerificationCodesState.Error,
                allowLongPressActions = false,
            ),
        )
    }
}
