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
            source = createMockLocalAuthenticatorItemSource(isFavorite = true),
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
            showOverflow = true,
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
            showOverflow = true,
            showMoveToBitwarden = false,
        )

        assertEquals(
            expectedFavoriteItem,
            favoriteItem.toDisplayItem(
                alertThresholdSeconds = alertThresholdSeconds,
                sharedVerificationCodesState = SharedVerificationCodesState.Error,
                showOverflow = true,
            ),
        )
        assertEquals(
            expectedNonFavoriteItem,
            nonFavoriteItem.toDisplayItem(
                alertThresholdSeconds = alertThresholdSeconds,
                sharedVerificationCodesState = SharedVerificationCodesState.Error,
                showOverflow = true,
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
                showOverflow = true,
                showMoveToBitwarden = false,
            )

        assertEquals(
            expectedDontShowMoveToBitwardenItem,
            item.toDisplayItem(
                alertThresholdSeconds = alertThresholdSeconds,
                sharedVerificationCodesState = SharedVerificationCodesState.AppNotInstalled,
                showOverflow = true,
            ),
        )
        assertEquals(
            expectedDontShowMoveToBitwardenItem,
            item.toDisplayItem(
                alertThresholdSeconds = alertThresholdSeconds,
                sharedVerificationCodesState = SharedVerificationCodesState.Error,
                showOverflow = true,
            ),
        )
        assertEquals(
            expectedDontShowMoveToBitwardenItem,
            item.toDisplayItem(
                alertThresholdSeconds = alertThresholdSeconds,
                sharedVerificationCodesState = SharedVerificationCodesState.FeatureNotEnabled,
                showOverflow = true,
            ),
        )
        assertEquals(
            expectedDontShowMoveToBitwardenItem,
            item.toDisplayItem(
                alertThresholdSeconds = alertThresholdSeconds,
                sharedVerificationCodesState = SharedVerificationCodesState.Loading,
                showOverflow = true,
            ),
        )
        assertEquals(
            expectedDontShowMoveToBitwardenItem,
            item.toDisplayItem(
                alertThresholdSeconds = alertThresholdSeconds,
                sharedVerificationCodesState = SharedVerificationCodesState.OsVersionNotSupported,
                showOverflow = true,
            ),
        )
        assertEquals(
            expectedDontShowMoveToBitwardenItem,
            item.toDisplayItem(
                alertThresholdSeconds = alertThresholdSeconds,
                sharedVerificationCodesState = SharedVerificationCodesState.SyncNotEnabled,
                showOverflow = true,
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
                showOverflow = true,
            ),
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `toDisplayItem should set nextAuthCode when showNextCode is true and timeLeftSeconds is within threshold`() {
        val item = createMockVerificationCodeItem(number = 1, nextCode = "654321", timeLeftSeconds = 5)
        val result = item.toDisplayItem(
            alertThresholdSeconds = 7,
            sharedVerificationCodesState = SharedVerificationCodesState.Error,
            showOverflow = true,
            showNextCode = true,
        )
        assertEquals("654321", result.nextAuthCode)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `toDisplayItem should set nextAuthCode when showNextCode is true and timeLeftSeconds equals threshold`() {
        val item = createMockVerificationCodeItem(number = 1, nextCode = "654321", timeLeftSeconds = 10)
        val result = item.toDisplayItem(
            alertThresholdSeconds = 7,
            sharedVerificationCodesState = SharedVerificationCodesState.Error,
            showOverflow = true,
            showNextCode = true,
        )
        assertEquals("654321", result.nextAuthCode)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `toDisplayItem should set nextAuthCode to null when showNextCode is true but timeLeftSeconds exceeds threshold`() {
        val item = createMockVerificationCodeItem(number = 1, nextCode = "654321", timeLeftSeconds = 11)
        val result = item.toDisplayItem(
            alertThresholdSeconds = 7,
            sharedVerificationCodesState = SharedVerificationCodesState.Error,
            showOverflow = true,
            showNextCode = true,
        )
        assertEquals(null, result.nextAuthCode)
    }

    @Test
    fun `toDisplayItem should set nextAuthCode to null when showNextCode is false`() {
        val item = createMockVerificationCodeItem(
            number = 1,
            nextCode = "654321",
            timeLeftSeconds = 5,
        )
        val result = item.toDisplayItem(
            alertThresholdSeconds = 7,
            sharedVerificationCodesState = SharedVerificationCodesState.Error,
            showOverflow = true,
            showNextCode = false,
        )
        assertEquals(null, result.nextAuthCode)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `toDisplayItem should set nextAuthCode to null when showNextCode true but nextCode is null`() {
        val item = createMockVerificationCodeItem(number = 1, nextCode = null, timeLeftSeconds = 5)
        val result = item.toDisplayItem(
            alertThresholdSeconds = 7,
            sharedVerificationCodesState = SharedVerificationCodesState.Error,
            showOverflow = true,
            showNextCode = true,
        )
        assertEquals(null, result.nextAuthCode)
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
            showOverflow = false,
            showMoveToBitwarden = false,
        )

        assertEquals(
            expectedFavoriteItem,
            favoriteItem.toDisplayItem(
                alertThresholdSeconds = alertThresholdSeconds,
                sharedVerificationCodesState = SharedVerificationCodesState.Error,
                showOverflow = false,
            ),
        )
    }
}
