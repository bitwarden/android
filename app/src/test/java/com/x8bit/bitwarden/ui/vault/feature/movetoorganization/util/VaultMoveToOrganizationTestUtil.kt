package com.x8bit.bitwarden.ui.vault.feature.movetoorganization.util

import com.x8bit.bitwarden.ui.vault.feature.movetoorganization.VaultMoveToOrganizationState

/**
 * Creates a list of mock organizations.
 */
fun createMockOrganizationList():
    List<VaultMoveToOrganizationState.ViewState.Content.Organization> =
    listOf(
        VaultMoveToOrganizationState.ViewState.Content.Organization(
            id = "1",
            name = "Organization 1",
            collections = listOf(
                VaultMoveToOrganizationState.ViewState.Content.Collection(
                    id = "1",
                    name = "Collection 1",
                    isSelected = true,
                ),
                VaultMoveToOrganizationState.ViewState.Content.Collection(
                    id = "2",
                    name = "Collection 2",
                    isSelected = false,
                ),
                VaultMoveToOrganizationState.ViewState.Content.Collection(
                    id = "3",
                    name = "Collection 3",
                    isSelected = false,
                ),
            ),
        ),
        VaultMoveToOrganizationState.ViewState.Content.Organization(
            id = "2",
            name = "Organization 2",
            collections = listOf(
                VaultMoveToOrganizationState.ViewState.Content.Collection(
                    id = "1",
                    name = "Collection 1",
                    isSelected = true,
                ),
                VaultMoveToOrganizationState.ViewState.Content.Collection(
                    id = "2",
                    name = "Collection 2",
                    isSelected = false,
                ),
                VaultMoveToOrganizationState.ViewState.Content.Collection(
                    id = "3",
                    name = "Collection 3",
                    isSelected = false,
                ),
            ),
        ),
        VaultMoveToOrganizationState.ViewState.Content.Organization(
            id = "3",
            name = "Organization 3",
            collections = emptyList(),
        ),
    )
