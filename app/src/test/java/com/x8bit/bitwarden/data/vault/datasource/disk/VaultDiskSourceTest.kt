package com.x8bit.bitwarden.data.vault.datasource.disk

import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.datasource.network.di.PlatformNetworkModule
import com.x8bit.bitwarden.data.util.assertJsonEquals
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.FakeCiphersDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.FakeCollectionsDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.FakeFoldersDao
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.CipherEntity
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.CollectionEntity
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.FolderEntity
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockCipher
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockCollection
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockFolder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class VaultDiskSourceTest {

    private val json = PlatformNetworkModule.providesJson()
    private lateinit var ciphersDao: FakeCiphersDao
    private lateinit var collectionsDao: FakeCollectionsDao
    private lateinit var foldersDao: FakeFoldersDao

    private lateinit var vaultDiskSource: VaultDiskSource

    @BeforeEach
    fun setup() {
        ciphersDao = FakeCiphersDao()
        collectionsDao = FakeCollectionsDao()
        foldersDao = FakeFoldersDao()
        vaultDiskSource = VaultDiskSourceImpl(
            ciphersDao = ciphersDao,
            collectionsDao = collectionsDao,
            foldersDao = foldersDao,
            json = json,
        )
    }

    @Test
    fun `getCiphers should emit all CiphersDao updates`() = runTest {
        val cipherEntities = listOf(CIPHER_ENTITY)
        val ciphers = listOf(CIPHER_1)

        vaultDiskSource
            .getCiphers(USER_ID)
            .test {
                assertEquals(emptyList<SyncResponseJson.Cipher>(), awaitItem())
                ciphersDao.insertCiphers(cipherEntities)
                assertEquals(ciphers, awaitItem())
            }
    }

    @Test
    fun `getCollections should emit all CollectionsDao updates`() = runTest {
        val collectionEntities = listOf(COLLECTION_ENTITY)
        val collection = listOf(COLLECTION_1)

        vaultDiskSource
            .getCollections(USER_ID)
            .test {
                assertEquals(emptyList<SyncResponseJson.Collection>(), awaitItem())
                collectionsDao.insertCollections(collectionEntities)
                assertEquals(collection, awaitItem())
            }
    }

    @Test
    fun `getFolders should emit all FoldersDao updates`() = runTest {
        val folderEntities = listOf(FOLDER_ENTITY)
        val folders = listOf(FOLDER_1)

        vaultDiskSource
            .getFolders(USER_ID)
            .test {
                assertEquals(emptyList<SyncResponseJson.Folder>(), awaitItem())
                foldersDao.insertFolders(folderEntities)
                assertEquals(folders, awaitItem())
            }
    }

    @Test
    fun `replaceVaultData should clear the daos and insert the new vault data`() = runTest {
        assertEquals(ciphersDao.storedCiphers, emptyList<CipherEntity>())
        assertEquals(collectionsDao.storedCollections, emptyList<CollectionEntity>())
        assertEquals(foldersDao.storedFolders, emptyList<FolderEntity>())

        vaultDiskSource.replaceVaultData(USER_ID, VAULT_DATA)

        assertEquals(1, ciphersDao.storedCiphers.size)
        assertEquals(1, foldersDao.storedFolders.size)

        // Verify the ciphers dao is updated
        val storedCipherEntity = ciphersDao.storedCiphers.first()
        // We cannot compare the JSON strings directly because of formatting differences
        // So we split that off into its own assertion.
        assertEquals(CIPHER_ENTITY.copy(cipherJson = ""), storedCipherEntity.copy(cipherJson = ""))
        assertJsonEquals(CIPHER_ENTITY.cipherJson, storedCipherEntity.cipherJson)

        // Verify the collections dao is updated
        assertEquals(listOf(COLLECTION_ENTITY), collectionsDao.storedCollections)

        // Verify the folders dao is updated
        assertEquals(listOf(FOLDER_ENTITY), foldersDao.storedFolders)
    }

    @Test
    fun `deleteVaultData should remove all vault data matching the user ID`() = runTest {
        assertFalse(ciphersDao.deleteCiphersCalled)
        assertFalse(collectionsDao.deleteCollectionsCalled)
        assertFalse(foldersDao.deleteFoldersCalled)
        vaultDiskSource.deleteVaultData(USER_ID)
        assertTrue(ciphersDao.deleteCiphersCalled)
        assertTrue(collectionsDao.deleteCollectionsCalled)
        assertTrue(foldersDao.deleteFoldersCalled)
    }
}

private const val USER_ID: String = "test_user_id"

private val CIPHER_1: SyncResponseJson.Cipher = createMockCipher(1)
private val COLLECTION_1: SyncResponseJson.Collection = createMockCollection(3)
private val FOLDER_1: SyncResponseJson.Folder = createMockFolder(2)

private val VAULT_DATA: SyncResponseJson = SyncResponseJson(
    folders = listOf(FOLDER_1),
    collections = listOf(COLLECTION_1),
    profile = mockk<SyncResponseJson.Profile> {
        every { id } returns USER_ID
    },
    ciphers = listOf(CIPHER_1),
    policies = null,
    domains = SyncResponseJson.Domains(
        globalEquivalentDomains = null,
        equivalentDomains = null,
    ),
    sends = null,
)

private const val CIPHER_JSON = """
{
  "notes": "mockNotes-1",
  "attachments": [
    {
      "fileName": "mockFileName-1",
      "size": 1,
      "sizeName": "mockSizeName-1",
      "id": "mockId-1",
      "url": "mockUrl-1",
      "key": "mockKey-1"
    }
  ],
  "organizationUseTotp": false,
  "reprompt": 0,
  "edit": false,
  "passwordHistory": [
    {
      "password": "mockPassword-1",
      "lastUsedDate": "2023-10-27T12:00:00.000Z"
    }
  ],
  "revisionDate": "2023-10-27T12:00:00.000Z",
  "type": 1,
  "login": {
    "uris": [
      {
        "match": 1,
        "uri": "mockUri-1"
      }
    ],
    "totp": "mockTotp-1",
    "password": "mockPassword-1",
    "passwordRevisionDate": "2023-10-27T12:00:00.000Z",
    "autofillOnPageLoad": false,
    "uri": "mockUri-1",
    "username": "mockUsername-1"
  },
  "creationDate": "2023-10-27T12:00:00.000Z",
  "secureNote": {
    "type": 0
  },
  "folderId": "mockFolderId-1",
  "organizationId": "mockOrganizationId-1",
  "deletedDate": "2023-10-27T12:00:00.000Z",
  "identity": {
    "passportNumber": "mockPassportNumber-1",
    "lastName": "mockLastName-1",
    "address3": "mockAddress3-1",
    "address2": "mockAddress2-1",
    "city": "mockCity-1",
    "country": "mockCountry-1",
    "address1": "mockAddress1-1",
    "postalCode": "mockPostalCode-1",
    "title": "mockTitle-1",
    "ssn": "mockSsn-1",
    "firstName": "mockFirstName-1",
    "phone": "mockPhone-1",
    "middleName": "mockMiddleName-1",
    "company": "mockCompany-1",
    "licenseNumber": "mockLicenseNumber-1",
    "state": "mockState-1",
    "email": "mockEmail-1",
    "username": "mockUsername-1"
  },
  "collectionIds": [
    "mockCollectionId-1"
  ],
  "name": "mockName-1",
  "id": "mockId-1",
  "fields": [
    {
      "linkedId": 100,
      "name": "mockName-1",
      "type": 1,
      "value": "mockValue-1"
    }
  ],
  "viewPassword": false,
  "favorite": false,
  "card": {
    "number": "mockNumber-1",
    "expMonth": "mockExpMonth-1",
    "code": "mockCode-1",
    "expYear": "mockExpirationYear-1",
    "cardholderName": "mockCardholderName-1",
    "brand": "mockBrand-1"
  },
  "key": "mockKey-1"
}
"""

private val CIPHER_ENTITY = CipherEntity(
    id = "mockId-1",
    userId = USER_ID,
    cipherType = "1",
    cipherJson = CIPHER_JSON,
)

private val COLLECTION_ENTITY = CollectionEntity(
    id = "mockId-3",
    userId = USER_ID,
    organizationId = "mockOrganizationId-3",
    shouldHidePasswords = false,
    name = "mockName-3",
    externalId = "mockExternalId-3",
    isReadOnly = false,
)

private val FOLDER_ENTITY = FolderEntity(
    id = "mockId-2",
    userId = USER_ID,
    name = "mockName-2",
    revisionDate = ZonedDateTime.parse("2023-10-27T12:00Z"),
)
