package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.collections.CollectionView
import com.bitwarden.core.data.util.flatMap
import com.bitwarden.network.model.CollectionJsonRequest
import com.bitwarden.network.model.UpdateCollectionResponseJson
import com.bitwarden.network.service.CollectionService
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.repository.model.CreateCollectionResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteCollectionResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCollectionResult
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCollection

/**
 * The default implementation of the [CollectionManager].
 */
class CollectionManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val collectionService: CollectionService,
    private val vaultDiskSource: VaultDiskSource,
    private val vaultSdkSource: VaultSdkSource,
) : CollectionManager {
    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    override suspend fun createCollection(
        organizationId: String,
        collectionView: CollectionView,
    ): CreateCollectionResult {
        val userId = activeUserId
            ?: return CreateCollectionResult.Error(error = NoActiveUserException())
        return vaultSdkSource
            .encryptCollection(userId = userId, collectionView = collectionView)
            .flatMap {
                collectionService.createCollection(
                    organizationId = organizationId,
                    body = CollectionJsonRequest(name = it.name),
                )
            }
            .onSuccess {
                vaultDiskSource.saveCollection(userId = userId, collection = it)
            }
            .flatMap {
                vaultSdkSource.decryptCollection(
                    userId = userId,
                    collection = it.toEncryptedSdkCollection(),
                )
            }
            .fold(
                onSuccess = { CreateCollectionResult.Success(collectionView = it) },
                onFailure = { CreateCollectionResult.Error(error = it) },
            )
    }

    override suspend fun deleteCollection(
        organizationId: String,
        collectionId: String,
    ): DeleteCollectionResult {
        val userId = activeUserId
            ?: return DeleteCollectionResult.Error(error = NoActiveUserException())
        return collectionService
            .deleteCollection(
                organizationId = organizationId,
                collectionId = collectionId,
            )
            .onSuccess {
                vaultDiskSource.deleteCollection(
                    userId = userId,
                    collectionId = collectionId,
                )
            }
            .fold(
                onSuccess = { DeleteCollectionResult.Success },
                onFailure = { DeleteCollectionResult.Error(error = it) },
            )
    }

    @Suppress("LongMethod")
    override suspend fun updateCollection(
        organizationId: String,
        collectionId: String,
        collectionView: CollectionView,
    ): UpdateCollectionResult {
        val userId = activeUserId
            ?: return UpdateCollectionResult.Error(error = NoActiveUserException())
        return collectionService
            .getCollectionDetails(
                organizationId = organizationId,
                collectionId = collectionId,
            )
            .flatMap { details ->
                vaultSdkSource
                    .encryptCollection(
                        userId = userId,
                        collectionView = collectionView,
                    )
                    .flatMap { collection ->
                        collectionService.updateCollection(
                            organizationId = organizationId,
                            collectionId = collectionId,
                            body = CollectionJsonRequest(
                                name = collection.name,
                                externalId = details.externalId,
                                groups = details.groups,
                                users = details.users,
                            ),
                        )
                    }
            }
            .fold(
                onSuccess = { response ->
                    when (response) {
                        is UpdateCollectionResponseJson.Success -> {
                            vaultDiskSource.saveCollection(
                                userId = userId,
                                collection = response.collection,
                            )
                            vaultSdkSource
                                .decryptCollection(
                                    userId = userId,
                                    collection = response.collection
                                        .toEncryptedSdkCollection(),
                                )
                                .fold(
                                    onSuccess = {
                                        UpdateCollectionResult.Success(it)
                                    },
                                    onFailure = {
                                        UpdateCollectionResult.Error(error = it)
                                    },
                                )
                        }

                        is UpdateCollectionResponseJson.Invalid -> {
                            UpdateCollectionResult.Error(
                                errorMessage = response.message,
                                error = null,
                            )
                        }
                    }
                },
                onFailure = { UpdateCollectionResult.Error(error = it) },
            )
    }
}
