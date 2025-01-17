package com.x8bit.bitwarden.ui.vault.feature.movetoorganization.util

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.vault.datasource.network.model.OrganizationType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.movetoorganization.VaultMoveToOrganizationState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultMoveToOrganizationExtensionsTest {

    @Test
    @Suppress("MaxLineLength")
    fun `toViewState should transform a valid triple of CipherView, CollectionView list, and UserState into Content ViewState`() {
        val triple = Triple(
            first = createMockCipherView(number = 1),
            second = listOf(
                createMockCollectionView(number = 1),
                createMockCollectionView(number = 2),
                createMockCollectionView(number = 3),
            ),
            third = createMockUserState(),
        )

        val result = triple.toViewState()

        assertEquals(
            VaultMoveToOrganizationState.ViewState.Content(
                selectedOrganizationId = "mockOrganizationId-1",
                organizations = createMockOrganizationList(),
                cipherToMove = createMockCipherView(number = 1),
            ),
            result,
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `toViewState should transform a triple of null CipherView, CollectionView list, and UserState into Error ViewState`() {
        val triple = Triple(
            first = null,
            second = listOf(
                createMockCollectionView(number = 1),
                createMockCollectionView(number = 2),
                createMockCollectionView(number = 3),
            ),
            third = createMockUserState(),
        )

        val result = triple.toViewState()

        assertEquals(
            VaultMoveToOrganizationState.ViewState.Error(R.string.generic_error_message.asText()),
            result,
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `toViewState should transform a triple of CipherView, CollectionView list, and UserState without organizations into Empty ViewState`() {
        val triple = Triple(
            first = createMockCipherView(number = 1),
            second = listOf(
                createMockCollectionView(number = 1),
                createMockCollectionView(number = 2),
                createMockCollectionView(number = 3),
            ),
            third = createMockUserState(hasOrganizations = false),
        )

        val result = triple.toViewState()

        assertEquals(
            VaultMoveToOrganizationState.ViewState.Empty,
            result,
        )
    }
}

private fun createMockUserState(hasOrganizations: Boolean = true): UserState =
    UserState(
        activeUserId = "activeUserId",
        accounts = listOf(
            UserState.Account(
                userId = "activeUserId",
                name = "Active User",
                email = "active@bitwarden.com",
                avatarColorHex = "#aa00aa",
                environment = Environment.Us,
                isPremium = true,
                isLoggedIn = true,
                isVaultUnlocked = true,
                needsPasswordReset = false,
                isBiometricsEnabled = false,
                needsMasterPassword = false,
                organizations = if (hasOrganizations) {
                    listOf(
                        Organization(
                            id = "mockOrganizationId-1",
                            name = "mockOrganizationName-1",
                            shouldManageResetPassword = false,
                            shouldUseKeyConnector = false,
                            role = OrganizationType.ADMIN,
                            shouldUsersGetPremium = false,
                        ),
                        Organization(
                            id = "mockOrganizationId-2",
                            name = "mockOrganizationName-2",
                            shouldManageResetPassword = false,
                            shouldUseKeyConnector = false,
                            role = OrganizationType.ADMIN,
                            shouldUsersGetPremium = false,
                        ),
                        Organization(
                            id = "mockOrganizationId-3",
                            name = "mockOrganizationName-3",
                            shouldManageResetPassword = false,
                            shouldUseKeyConnector = false,
                            role = OrganizationType.ADMIN,
                            shouldUsersGetPremium = false,
                        ),
                    )
                } else {
                    emptyList()
                },
                trustedDevice = null,
                hasMasterPassword = true,
                isUsingKeyConnector = false,
                onboardingStatus = OnboardingStatus.COMPLETE,
                firstTimeState = FirstTimeState(showImportLoginsCard = true),
            ),
        ),
    )
