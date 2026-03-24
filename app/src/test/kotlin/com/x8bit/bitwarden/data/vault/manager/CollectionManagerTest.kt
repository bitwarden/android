package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.collections.CollectionType
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.network.model.CollectionAccessSelectionJson
import com.bitwarden.network.model.CollectionDetailsResponseJson
import com.bitwarden.network.model.CollectionJsonRequest
import com.bitwarden.network.model.UpdateCollectionResponseJson
import com.bitwarden.network.model.createMockCollection
import com.bitwarden.network.service.CollectionService
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkCollection
import com.x8bit.bitwarden.data.vault.repository.model.CreateCollectionResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteCollectionResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCollectionResult
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCollection
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkConstructor
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class CollectionManagerTest {
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val collectionService = mockk<CollectionService>()
    private val vaultDiskSource = mockk<VaultDiskSource>()
    private val vaultSdkSource = mockk<VaultSdkSource>()

    private val collectionManager: CollectionManager = CollectionManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        collectionService = collectionService,
        vaultDiskSource = vaultDiskSource,
        vaultSdkSource = vaultSdkSource,
    )

    @BeforeEach
    fun setup() {
        mockkConstructor(NoActiveUserException::class)
        every {
            anyConstructed<NoActiveUserException>() == any<NoActiveUserException>()
        } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkConstructor(NoActiveUserException::class)
    }

    // region createCollection

    @Test
    fun `createCollection with no active user should return Error`() =
        runTest {
            fakeAuthDiskSource.userState = null
            val result = collectionManager.createCollection(
                organizationId = DEFAULT_ORG_ID,
                collectionView = mockk(),
            )
            assertEquals(
                CreateCollectionResult.Error(
                    error = NoActiveUserException(),
                ),
                result,
            )
        }

    @Test
    fun `createCollection with encrypt failure should return Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val error = IllegalStateException()
            coEvery {
                vaultSdkSource.encryptCollection(
                    userId = ACTIVE_USER_ID,
                    collectionView = DEFAULT_COLLECTION_VIEW,
                )
            } returns error.asFailure()

            val result = collectionManager.createCollection(
                organizationId = DEFAULT_ORG_ID,
                collectionView = DEFAULT_COLLECTION_VIEW,
            )
            assertEquals(CreateCollectionResult.Error(error = error), result)
        }

    @Test
    fun `createCollection with service failure should return Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val sdkCollection = createMockSdkCollection(number = 1)
            val error = IllegalStateException()

            coEvery {
                vaultSdkSource.encryptCollection(
                    userId = ACTIVE_USER_ID,
                    collectionView = DEFAULT_COLLECTION_VIEW,
                )
            } returns sdkCollection.asSuccess()

            coEvery {
                collectionService.createCollection(
                    organizationId = DEFAULT_ORG_ID,
                    body = CollectionJsonRequest(name = sdkCollection.name),
                )
            } returns error.asFailure()

            val result = collectionManager.createCollection(
                organizationId = DEFAULT_ORG_ID,
                collectionView = DEFAULT_COLLECTION_VIEW,
            )
            assertEquals(CreateCollectionResult.Error(error = error), result)
        }

    @Test
    fun `createCollection with success should return Success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val sdkCollection = createMockSdkCollection(number = 1)
            val networkCollection = createMockCollection(number = 1)

            coEvery {
                vaultSdkSource.encryptCollection(
                    userId = ACTIVE_USER_ID,
                    collectionView = DEFAULT_COLLECTION_VIEW,
                )
            } returns sdkCollection.asSuccess()

            coEvery {
                collectionService.createCollection(
                    organizationId = DEFAULT_ORG_ID,
                    body = CollectionJsonRequest(name = sdkCollection.name),
                )
            } returns networkCollection.asSuccess()

            coEvery {
                vaultDiskSource.saveCollection(
                    userId = ACTIVE_USER_ID,
                    collection = networkCollection,
                )
            } just runs

            coEvery {
                vaultSdkSource.decryptCollection(
                    userId = ACTIVE_USER_ID,
                    collection = networkCollection
                        .toEncryptedSdkCollection(),
                )
            } returns DEFAULT_COLLECTION_VIEW.asSuccess()

            val result = collectionManager.createCollection(
                organizationId = DEFAULT_ORG_ID,
                collectionView = DEFAULT_COLLECTION_VIEW,
            )
            assertEquals(
                CreateCollectionResult.Success(
                    collectionView = DEFAULT_COLLECTION_VIEW,
                ),
                result,
            )
        }

    // endregion createCollection

    // region deleteCollection

    @Test
    fun `deleteCollection with no active user should return Error`() =
        runTest {
            fakeAuthDiskSource.userState = null
            val result = collectionManager.deleteCollection(
                organizationId = DEFAULT_ORG_ID,
                collectionId = DEFAULT_COLLECTION_ID,
            )
            assertEquals(
                DeleteCollectionResult.Error(
                    error = NoActiveUserException(),
                ),
                result,
            )
        }

    @Test
    fun `deleteCollection with service failure should return Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val error = Throwable("fail")
            coEvery {
                collectionService.deleteCollection(
                    organizationId = DEFAULT_ORG_ID,
                    collectionId = DEFAULT_COLLECTION_ID,
                )
            } returns error.asFailure()

            val result = collectionManager.deleteCollection(
                organizationId = DEFAULT_ORG_ID,
                collectionId = DEFAULT_COLLECTION_ID,
            )
            assertEquals(
                DeleteCollectionResult.Error(error = error),
                result,
            )
        }

    @Test
    fun `deleteCollection with success should return Success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                collectionService.deleteCollection(
                    organizationId = DEFAULT_ORG_ID,
                    collectionId = DEFAULT_COLLECTION_ID,
                )
            } returns Unit.asSuccess()

            coEvery {
                vaultDiskSource.deleteCollection(
                    userId = ACTIVE_USER_ID,
                    collectionId = DEFAULT_COLLECTION_ID,
                )
            } just runs

            val result = collectionManager.deleteCollection(
                organizationId = DEFAULT_ORG_ID,
                collectionId = DEFAULT_COLLECTION_ID,
            )
            assertEquals(DeleteCollectionResult.Success, result)
            coVerify {
                vaultDiskSource.deleteCollection(
                    userId = ACTIVE_USER_ID,
                    collectionId = DEFAULT_COLLECTION_ID,
                )
            }
        }

    // endregion deleteCollection

    // region updateCollection

    @Test
    fun `updateCollection with no active user should return Error`() =
        runTest {
            fakeAuthDiskSource.userState = null
            val result = collectionManager.updateCollection(
                organizationId = DEFAULT_ORG_ID,
                collectionId = DEFAULT_COLLECTION_ID,
                collectionView = mockk(),
            )
            assertEquals(
                UpdateCollectionResult.Error(
                    error = NoActiveUserException(),
                ),
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `updateCollection with getCollectionDetails failure should return Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val error = IllegalStateException()
            coEvery {
                collectionService.getCollectionDetails(
                    organizationId = DEFAULT_ORG_ID,
                    collectionId = DEFAULT_COLLECTION_ID,
                )
            } returns error.asFailure()

            val result = collectionManager.updateCollection(
                organizationId = DEFAULT_ORG_ID,
                collectionId = DEFAULT_COLLECTION_ID,
                collectionView = DEFAULT_COLLECTION_VIEW,
            )
            assertEquals(
                UpdateCollectionResult.Error(error = error),
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `updateCollection with encrypt failure should return Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val error = IllegalStateException()
            coEvery {
                collectionService.getCollectionDetails(
                    organizationId = DEFAULT_ORG_ID,
                    collectionId = DEFAULT_COLLECTION_ID,
                )
            } returns DEFAULT_DETAILS_RESPONSE.asSuccess()

            coEvery {
                vaultSdkSource.encryptCollection(
                    userId = ACTIVE_USER_ID,
                    collectionView = DEFAULT_COLLECTION_VIEW,
                )
            } returns error.asFailure()

            val result = collectionManager.updateCollection(
                organizationId = DEFAULT_ORG_ID,
                collectionId = DEFAULT_COLLECTION_ID,
                collectionView = DEFAULT_COLLECTION_VIEW,
            )
            assertEquals(
                UpdateCollectionResult.Error(error = error),
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `updateCollection with service failure should return Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val sdkCollection = createMockSdkCollection(number = 1)
            val error = IllegalStateException()

            coEvery {
                collectionService.getCollectionDetails(
                    organizationId = DEFAULT_ORG_ID,
                    collectionId = DEFAULT_COLLECTION_ID,
                )
            } returns DEFAULT_DETAILS_RESPONSE.asSuccess()

            coEvery {
                vaultSdkSource.encryptCollection(
                    userId = ACTIVE_USER_ID,
                    collectionView = DEFAULT_COLLECTION_VIEW,
                )
            } returns sdkCollection.asSuccess()

            coEvery {
                collectionService.updateCollection(
                    organizationId = DEFAULT_ORG_ID,
                    collectionId = DEFAULT_COLLECTION_ID,
                    body = CollectionJsonRequest(
                        name = sdkCollection.name,
                        externalId = DEFAULT_DETAILS_RESPONSE.externalId,
                        groups = DEFAULT_DETAILS_RESPONSE.groups,
                        users = DEFAULT_DETAILS_RESPONSE.users,
                    ),
                )
            } returns error.asFailure()

            val result = collectionManager.updateCollection(
                organizationId = DEFAULT_ORG_ID,
                collectionId = DEFAULT_COLLECTION_ID,
                collectionView = DEFAULT_COLLECTION_VIEW,
            )
            assertEquals(
                UpdateCollectionResult.Error(error = error),
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `updateCollection with Invalid response should return Error with message`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val sdkCollection = createMockSdkCollection(number = 1)

            coEvery {
                collectionService.getCollectionDetails(
                    organizationId = DEFAULT_ORG_ID,
                    collectionId = DEFAULT_COLLECTION_ID,
                )
            } returns DEFAULT_DETAILS_RESPONSE.asSuccess()

            coEvery {
                vaultSdkSource.encryptCollection(
                    userId = ACTIVE_USER_ID,
                    collectionView = DEFAULT_COLLECTION_VIEW,
                )
            } returns sdkCollection.asSuccess()

            coEvery {
                collectionService.updateCollection(
                    organizationId = DEFAULT_ORG_ID,
                    collectionId = DEFAULT_COLLECTION_ID,
                    body = CollectionJsonRequest(
                        name = sdkCollection.name,
                        externalId = DEFAULT_DETAILS_RESPONSE.externalId,
                        groups = DEFAULT_DETAILS_RESPONSE.groups,
                        users = DEFAULT_DETAILS_RESPONSE.users,
                    ),
                )
            } returns UpdateCollectionResponseJson
                .Invalid(
                    message = "Permission denied.",
                    validationErrors = null,
                )
                .asSuccess()

            val result = collectionManager.updateCollection(
                organizationId = DEFAULT_ORG_ID,
                collectionId = DEFAULT_COLLECTION_ID,
                collectionView = DEFAULT_COLLECTION_VIEW,
            )
            assertEquals(
                UpdateCollectionResult.Error(
                    errorMessage = "Permission denied.",
                    error = null,
                ),
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `updateCollection with success should return Success and include access permissions`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val sdkCollection = createMockSdkCollection(number = 1)
            val networkCollection = createMockCollection(number = 1)

            coEvery {
                collectionService.getCollectionDetails(
                    organizationId = DEFAULT_ORG_ID,
                    collectionId = DEFAULT_COLLECTION_ID,
                )
            } returns DEFAULT_DETAILS_RESPONSE.asSuccess()

            coEvery {
                vaultSdkSource.encryptCollection(
                    userId = ACTIVE_USER_ID,
                    collectionView = DEFAULT_COLLECTION_VIEW,
                )
            } returns sdkCollection.asSuccess()

            coEvery {
                collectionService.updateCollection(
                    organizationId = DEFAULT_ORG_ID,
                    collectionId = DEFAULT_COLLECTION_ID,
                    body = CollectionJsonRequest(
                        name = sdkCollection.name,
                        externalId = DEFAULT_DETAILS_RESPONSE.externalId,
                        groups = DEFAULT_DETAILS_RESPONSE.groups,
                        users = DEFAULT_DETAILS_RESPONSE.users,
                    ),
                )
            } returns UpdateCollectionResponseJson
                .Success(collection = networkCollection)
                .asSuccess()

            coEvery {
                vaultDiskSource.saveCollection(
                    userId = ACTIVE_USER_ID,
                    collection = networkCollection,
                )
            } just runs

            coEvery {
                vaultSdkSource.decryptCollection(
                    userId = ACTIVE_USER_ID,
                    collection = networkCollection
                        .toEncryptedSdkCollection(),
                )
            } returns DEFAULT_COLLECTION_VIEW.asSuccess()

            val result = collectionManager.updateCollection(
                organizationId = DEFAULT_ORG_ID,
                collectionId = DEFAULT_COLLECTION_ID,
                collectionView = DEFAULT_COLLECTION_VIEW,
            )
            assertEquals(
                UpdateCollectionResult.Success(DEFAULT_COLLECTION_VIEW),
                result,
            )

            coVerify {
                collectionService.updateCollection(
                    organizationId = DEFAULT_ORG_ID,
                    collectionId = DEFAULT_COLLECTION_ID,
                    body = CollectionJsonRequest(
                        name = sdkCollection.name,
                        externalId = DEFAULT_DETAILS_RESPONSE.externalId,
                        groups = DEFAULT_DETAILS_RESPONSE.groups,
                        users = DEFAULT_DETAILS_RESPONSE.users,
                    ),
                )
            }
        }

    // endregion updateCollection
}

private const val ACTIVE_USER_ID: String = "mockId-1"
private const val DEFAULT_ORG_ID = "orgId-1"
private const val DEFAULT_COLLECTION_ID = "collectionId-1"

private val DEFAULT_COLLECTION_VIEW = CollectionView(
    id = DEFAULT_COLLECTION_ID,
    organizationId = DEFAULT_ORG_ID,
    name = "TestCollection",
    externalId = null,
    hidePasswords = false,
    readOnly = false,
    manage = true,
    type = CollectionType.SHARED_COLLECTION,
)

private val DEFAULT_DETAILS_RESPONSE = CollectionDetailsResponseJson(
    id = DEFAULT_COLLECTION_ID,
    organizationId = DEFAULT_ORG_ID,
    name = "encryptedName",
    externalId = "externalId-1",
    groups = listOf(
        CollectionAccessSelectionJson(
            id = "groupId-1",
            readOnly = false,
            hidePasswords = false,
            manage = true,
        ),
    ),
    users = listOf(
        CollectionAccessSelectionJson(
            id = "userId-1",
            readOnly = false,
            hidePasswords = false,
            manage = true,
        ),
    ),
)

private val MOCK_PROFILE = AccountJson.Profile(
    userId = ACTIVE_USER_ID,
    email = "email",
    isEmailVerified = true,
    name = null,
    stamp = "mockSecurityStamp-1",
    organizationId = null,
    avatarColorHex = null,
    hasPremium = false,
    forcePasswordResetReason = null,
    kdfType = null,
    kdfIterations = null,
    kdfMemory = null,
    kdfParallelism = null,
    userDecryptionOptions = null,
    isTwoFactorEnabled = false,
    creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
)

private val MOCK_ACCOUNT = AccountJson(
    profile = MOCK_PROFILE,
    tokens = AccountTokensJson(
        accessToken = "accessToken",
        refreshToken = "refreshToken",
    ),
    settings = AccountJson.Settings(
        environmentUrlData = null,
    ),
)

private val MOCK_USER_STATE = UserStateJson(
    activeUserId = ACTIVE_USER_ID,
    accounts = mapOf(
        ACTIVE_USER_ID to MOCK_ACCOUNT,
    ),
)
