package com.x8bit.bitwarden.ui.vault.feature.movetoorganization.util

import com.bitwarden.core.CipherView
import com.bitwarden.core.CollectionView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.movetoorganization.VaultMoveToOrganizationState

/**
 * Transforms a triple of [CipherView] (nullable), list of [CollectionView],
 * and [UserState] (nullable) into a [VaultMoveToOrganizationState.ViewState].
 */
fun Triple<CipherView?, List<CollectionView>, UserState?>.toViewState():
    VaultMoveToOrganizationState.ViewState {
    val userOrganizations = third
        ?.activeAccount
        ?.organizations

    val currentCipher = first

    val collections = second
        .filter { !it.readOnly }

    return when {
        (currentCipher == null) -> {
            VaultMoveToOrganizationState.ViewState.Error(R.string.generic_error_message.asText())
        }
        (userOrganizations?.isNotEmpty() == true) -> {
            VaultMoveToOrganizationState.ViewState.Content(
                selectedOrganizationId = currentCipher
                    .organizationId
                    ?: userOrganizations
                        .first()
                        .id,
                organizations = userOrganizations.map { organization ->
                    VaultMoveToOrganizationState.ViewState.Content.Organization(
                        id = organization.id,
                        name = organization
                            .name
                            .orEmpty(),
                        collections = collections
                            .filter { collection ->
                                collection.organizationId == organization.id &&
                                    collection.id != null
                            }
                            .map { collection ->
                                VaultMoveToOrganizationState.ViewState.Content.Collection(
                                    id = collection.id.orEmpty(),
                                    name = collection.name,
                                    isSelected = currentCipher
                                        .collectionIds
                                        .contains(collection.id),
                                )
                            },
                    )
                },
                cipherToMove = currentCipher,
            )
        }
        else -> VaultMoveToOrganizationState.ViewState.Empty
    }
}
