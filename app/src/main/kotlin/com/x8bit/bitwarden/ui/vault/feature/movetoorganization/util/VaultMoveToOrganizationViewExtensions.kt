package com.x8bit.bitwarden.ui.vault.feature.movetoorganization.util

import com.bitwarden.collections.CollectionType
import com.bitwarden.collections.CollectionView
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.ui.vault.feature.movetoorganization.VaultMoveToOrganizationState
import com.x8bit.bitwarden.ui.vault.model.VaultCollection

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
            VaultMoveToOrganizationState.ViewState.Error(
                BitwardenString.generic_error_message.asText(),
            )
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
                                VaultCollection(
                                    id = collection.id.orEmpty(),
                                    name = collection.name,
                                    isSelected = currentCipher
                                        .collectionIds
                                        .contains(collection.id),
                                    isDefaultUserCollection =
                                        collection.type == CollectionType.DEFAULT_USER_COLLECTION,
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
