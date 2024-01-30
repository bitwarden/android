package com.x8bit.bitwarden.ui.vault.feature.movetoorganization.util

import com.x8bit.bitwarden.ui.vault.feature.movetoorganization.VaultMoveToOrganizationState
import com.x8bit.bitwarden.ui.vault.model.VaultCollection

/**
 * Creates a list of mock [VaultMoveToOrganizationState.ViewState.Content.Organization].
 */
fun createMockOrganizationList():
    List<VaultMoveToOrganizationState.ViewState.Content.Organization> =
    listOf(
        createMockOrganization(number = 1),
        createMockOrganization(number = 2, isCollectionSelected = false),
        createMockOrganization(number = 3, isCollectionSelected = false),
    )

/**
 * Creates a [VaultMoveToOrganizationState.ViewState.Content.Organization] with a given number.
 */
fun createMockOrganization(
    number: Int,
    isCollectionSelected: Boolean = true,
): VaultMoveToOrganizationState.ViewState.Content.Organization =
    VaultMoveToOrganizationState.ViewState.Content.Organization(
        id = "mockOrganizationId-$number",
        name = "mockOrganizationName-$number",
        collections = listOf(
            VaultCollection(
                id = "mockId-$number",
                name = "mockName-$number",
                isSelected = isCollectionSelected,
            ),
        ),
    )
