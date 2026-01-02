package com.x8bit.bitwarden.data.vault.datasource.disk

import app.cash.turbine.test
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.util.assertJsonEquals
import com.bitwarden.core.di.CoreModule
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.createMockCipher
import com.bitwarden.network.model.createMockCollection
import com.bitwarden.network.model.createMockDomains
import com.bitwarden.network.model.createMockFolder
import com.bitwarden.network.model.createMockSend
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.FakeCiphersDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.FakeCollectionsDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.FakeDomainsDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.FakeFoldersDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.FakeSendsDao
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.CipherEntity
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.CollectionEntity
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.DomainsEntity
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.FolderEntity
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.SendEntity
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class VaultDiskSourceTest {

    private val json = CoreModule.providesJson()
    private val dispatcherManager: FakeDispatcherManager = FakeDispatcherManager()
    private lateinit var ciphersDao: FakeCiphersDao
    private lateinit var collectionsDao: FakeCollectionsDao
    private lateinit var domainsDao: FakeDomainsDao
    private lateinit var foldersDao: FakeFoldersDao
    private lateinit var sendsDao: FakeSendsDao

    private lateinit var vaultDiskSource: VaultDiskSource

    @BeforeEach
    fun setup() {
        ciphersDao = FakeCiphersDao()
        collectionsDao = FakeCollectionsDao()
        domainsDao = FakeDomainsDao()
        foldersDao = FakeFoldersDao()
        sendsDao = FakeSendsDao()
        vaultDiskSource = VaultDiskSourceImpl(
            ciphersDao = ciphersDao,
            collectionsDao = collectionsDao,
            domainsDao = domainsDao,
            foldersDao = foldersDao,
            sendsDao = sendsDao,
            json = json,
            dispatcherManager = dispatcherManager,
        )
    }

    @Test
    fun `saveCipher should call insertCiphers`() = runTest {
        assertFalse(ciphersDao.insertCiphersCalled)
        assertEquals(0, ciphersDao.storedCiphers.size)

        vaultDiskSource.saveCipher(USER_ID, CIPHER_1)

        // Verify the ciphers dao is updated
        assertTrue(ciphersDao.insertCiphersCalled)
        assertEquals(1, ciphersDao.storedCiphers.size)
        val storedCipherEntity = ciphersDao.storedCiphers.first()
        // We cannot compare the JSON strings directly because of formatting differences
        // So we split that off into its own assertion.
        assertEquals(CIPHER_ENTITY.copy(cipherJson = ""), storedCipherEntity.copy(cipherJson = ""))
        assertJsonEquals(CIPHER_ENTITY.cipherJson, storedCipherEntity.cipherJson)
    }

    @Test
    fun `getCiphersFlow should emit all CiphersDao updates`() = runTest {
        val cipherEntities = listOf(CIPHER_ENTITY)
        val ciphers = listOf(CIPHER_1)

        vaultDiskSource
            .getCiphersFlow(USER_ID)
            .test {
                assertEquals(emptyList<SyncResponseJson.Cipher>(), awaitItem())
                ciphersDao.insertCiphers(cipherEntities)
                assertEquals(ciphers, awaitItem())
            }
    }

    @Test
    fun `getCiphers should return all CiphersDao ciphers`() = runTest {
        val cipherEntities = listOf(CIPHER_ENTITY)
        val ciphers = listOf(CIPHER_1)

        val result1 = vaultDiskSource.getCiphers(USER_ID)
        assertEquals(emptyList<SyncResponseJson.Cipher>(), result1)
        ciphersDao.insertCiphers(cipherEntities)
        val result2 = vaultDiskSource.getCiphers(USER_ID)
        assertEquals(ciphers, result2)
    }

    @Test
    fun `getSelectedCiphers should return selected CiphersDao ciphers`() = runTest {
        val cipherEntities = listOf(
            CIPHER_ENTITY,
            CIPHER_ENTITY.copy(id = "otherCipherId"),
        )
        val ciphers = listOf(CIPHER_1)
        val cipherIds = listOf("mockId-1")

        val result1 = vaultDiskSource.getSelectedCiphers(USER_ID, cipherIds)
        assertEquals(emptyList<SyncResponseJson.Cipher>(), result1)
        ciphersDao.insertCiphers(cipherEntities)
        val result2 = vaultDiskSource.getSelectedCiphers(USER_ID, cipherIds)
        assertEquals(ciphers, result2)
    }

    @Test
    fun `getTotpCiphers should return all CiphersDao totp ciphers`() = runTest {
        val cipherEntities = listOf(
            CIPHER_ENTITY,
            CIPHER_ENTITY.copy(id = "otherCipherId", hasTotp = false),
        )
        val ciphers = listOf(CIPHER_1)

        val result1 = vaultDiskSource.getTotpCiphers(USER_ID)
        assertEquals(emptyList<SyncResponseJson.Cipher>(), result1)
        ciphersDao.insertCiphers(cipherEntities)
        val result2 = vaultDiskSource.getTotpCiphers(USER_ID)
        assertEquals(ciphers, result2)
    }

    @Test
    fun `getCipher should return CiphersDao cipher`() = runTest {
        val cipherEntities = listOf(CIPHER_ENTITY)

        val result1 = vaultDiskSource.getCipher(userId = USER_ID, cipherId = CIPHER_ENTITY.id)
        assertNull(result1)
        ciphersDao.insertCiphers(cipherEntities)
        val result2 = vaultDiskSource.getCipher(userId = USER_ID, cipherId = CIPHER_ENTITY.id)
        assertEquals(CIPHER_1, result2)
    }

    @Test
    fun `DeleteCipher should call deleteCipher`() = runTest {
        assertFalse(ciphersDao.deleteCipherCalled)
        ciphersDao.storedCiphers.add(CIPHER_ENTITY)
        assertEquals(1, ciphersDao.storedCiphers.size)

        vaultDiskSource.deleteCipher(USER_ID, CIPHER_1.id)

        assertTrue(ciphersDao.deleteCipherCalled)
        assertEquals(emptyList<CipherEntity>(), ciphersDao.storedCiphers)
    }

    @Test
    fun `saveCollection should call insertCollection`() = runTest {
        assertFalse(collectionsDao.insertCollectionCalled)
        assertEquals(0, collectionsDao.storedCollections.size)

        vaultDiskSource.saveCollection(USER_ID, COLLECTION_1)

        assertTrue(collectionsDao.insertCollectionCalled)
        assertEquals(listOf(COLLECTION_ENTITY), collectionsDao.storedCollections)
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
    fun `getDomains should emit DomainsDao updates`() = runTest {
        vaultDiskSource
            .getDomains(USER_ID)
            .test {
                expectNoEvents()
                domainsDao.insertDomains(DOMAINS_ENTITY)
                assertEquals(DOMAINS_1, awaitItem())
            }
    }

    @Test
    fun `DeleteFolder should call deleteFolder`() = runTest {
        assertFalse(foldersDao.deleteFolderCalled)
        vaultDiskSource.saveFolder(USER_ID, FOLDER_1)
        assertEquals(1, foldersDao.storedFolders.size)

        vaultDiskSource.deleteFolder(USER_ID, FOLDER_1.id)

        assertTrue(foldersDao.deleteFolderCalled)
        assertEquals(emptyList<FolderEntity>(), foldersDao.storedFolders)
    }

    @Test
    fun `saveFolder should call insertFolder`() = runTest {
        assertFalse(foldersDao.insertFolderCalled)
        assertEquals(0, foldersDao.storedFolders.size)

        vaultDiskSource.saveFolder(USER_ID, FOLDER_1)

        assertTrue(foldersDao.insertFolderCalled)
        assertEquals(listOf(FOLDER_ENTITY), foldersDao.storedFolders)
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
    fun `saveSend should call insertSend`() = runTest {
        assertFalse(sendsDao.insertSendsCalled)
        assertEquals(0, sendsDao.storedSends.size)

        vaultDiskSource.saveSend(USER_ID, SEND_1)

        // Verify the sends dao is updated
        assertTrue(sendsDao.insertSendsCalled)
        assertEquals(1, sendsDao.storedSends.size)
        val storedSendEntity = sendsDao.storedSends.first()
        // We cannot compare the JSON strings directly because of formatting differences
        // So we split that off into its own assertion.
        assertEquals(SEND_ENTITY.copy(sendJson = ""), storedSendEntity.copy(sendJson = ""))
        assertJsonEquals(SEND_ENTITY.sendJson, storedSendEntity.sendJson)
    }

    @Test
    fun `DeleteSend should call deleteSend`() = runTest {
        assertFalse(sendsDao.deleteSendCalled)
        sendsDao.storedSends.add(SEND_ENTITY)
        assertEquals(1, sendsDao.storedSends.size)

        vaultDiskSource.deleteSend(USER_ID, SEND_1.id)

        assertTrue(sendsDao.deleteSendCalled)
        assertEquals(emptyList<SendEntity>(), sendsDao.storedSends)
    }

    @Test
    fun `getSends should emit all SendsDao updates`() = runTest {
        val sendEntities = listOf(SEND_ENTITY)
        val sends = listOf(SEND_1)

        vaultDiskSource
            .getSends(USER_ID)
            .test {
                assertEquals(emptyList<SyncResponseJson.Send>(), awaitItem())
                sendsDao.insertSends(sendEntities)
                assertEquals(sends, awaitItem())
            }
    }

    @Test
    fun `replaceVaultData should clear the daos and insert the new vault data`() = runTest {
        assertEquals(ciphersDao.storedCiphers, emptyList<CipherEntity>())
        assertEquals(collectionsDao.storedCollections, emptyList<CollectionEntity>())
        assertNull(domainsDao.storedDomains)
        assertEquals(foldersDao.storedFolders, emptyList<FolderEntity>())
        assertEquals(sendsDao.storedSends, emptyList<SendEntity>())

        vaultDiskSource.replaceVaultData(USER_ID, VAULT_DATA)

        assertEquals(1, ciphersDao.storedCiphers.size)
        // Verify the ciphers dao is updated
        val storedCipherEntity = ciphersDao.storedCiphers.first()
        // We cannot compare the JSON strings directly because of formatting differences
        // So we split that off into its own assertion.
        assertEquals(CIPHER_ENTITY.copy(cipherJson = ""), storedCipherEntity.copy(cipherJson = ""))
        assertJsonEquals(CIPHER_ENTITY.cipherJson, storedCipherEntity.cipherJson)

        // Verify the collections dao is updated
        assertEquals(listOf(COLLECTION_ENTITY), collectionsDao.storedCollections)

        assertNotNull(domainsDao.storedDomains)
        // Verify the domains dao is updated
        val storedDomainsEntity = requireNotNull(domainsDao.storedDomains)
        // We cannot compare the JSON strings directly because of formatting differences
        // So we split that off into its own assertion.
        assertEquals(
            DOMAINS_ENTITY.copy(domainsJson = null),
            storedDomainsEntity.copy(domainsJson = null),
        )
        assertJsonEquals(
            requireNotNull(DOMAINS_ENTITY.domainsJson),
            requireNotNull(storedDomainsEntity.domainsJson),
        )

        // Verify the folders dao is updated
        assertEquals(listOf(FOLDER_ENTITY), foldersDao.storedFolders)

        assertEquals(1, sendsDao.storedSends.size)
        // Verify the ciphers dao is updated
        val storedSendEntity = sendsDao.storedSends.first()
        // We cannot compare the JSON strings directly because of formatting differences
        // So we split that off into its own assertion.
        assertEquals(SEND_ENTITY.copy(sendJson = ""), storedSendEntity.copy(sendJson = ""))
        assertJsonEquals(SEND_ENTITY.sendJson, storedSendEntity.sendJson)
    }

    @Test
    fun `deleteVaultData should remove all vault data matching the user ID`() = runTest {
        assertFalse(ciphersDao.deleteCiphersCalled)
        assertFalse(collectionsDao.deleteCollectionsCalled)
        assertFalse(domainsDao.deleteDomainsCalled)
        assertFalse(foldersDao.deleteFoldersCalled)
        assertFalse(sendsDao.deleteSendsCalled)
        vaultDiskSource.deleteVaultData(USER_ID)
        assertTrue(ciphersDao.deleteCiphersCalled)
        assertTrue(collectionsDao.deleteCollectionsCalled)
        assertTrue(domainsDao.deleteDomainsCalled)
        assertTrue(foldersDao.deleteFoldersCalled)
        assertTrue(sendsDao.deleteSendsCalled)
    }
}

private const val USER_ID: String = "test_user_id"

private val CIPHER_1: SyncResponseJson.Cipher = createMockCipher(1)
private val COLLECTION_1: SyncResponseJson.Collection = createMockCollection(3)
private val DOMAINS_1: SyncResponseJson.Domains = createMockDomains(1)
private val FOLDER_1: SyncResponseJson.Folder = createMockFolder(2)
private val SEND_1: SyncResponseJson.Send = createMockSend(1)

private val VAULT_DATA: SyncResponseJson = SyncResponseJson(
    folders = listOf(FOLDER_1),
    collections = listOf(COLLECTION_1),
    profile = mockk<SyncResponseJson.Profile> {
        every { id } returns USER_ID
    },
    ciphers = listOf(CIPHER_1),
    policies = null,
    domains = DOMAINS_1,
    sends = listOf(SEND_1),
    userDecryption = null,
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
  "permissions": {
    "delete": true,
    "restore": true
  },
  "revisionDate": "2023-10-27T12:00:00.000Z",
  "type": 1,
  "login": {
    "uris": [
      {
        "match": 1,
        "uri": "mockUri-1",
        "uriChecksum": "mockUriChecksum-1"
      }
    ],
    "totp": "mockTotp-1",
    "password": "mockPassword-1",
    "passwordRevisionDate": "2023-10-27T12:00:00.000Z",
    "autofillOnPageLoad": false,
    "uri": "mockUri-1",
    "username": "mockUsername-1",
    "fido2Credentials": [
      {
        "credentialId": "mockCredentialId-1",
        "keyType": "mockKeyType-1",
        "keyAlgorithm": "mockKeyAlgorithm-1",
        "keyCurve": "mockKeyCurve-1",
        "keyValue": "mockKeyValue-1",
        "rpId": "mockRpId-1",
        "rpName": "mockRpName-1",
        "userHandle": "mockUserHandle-1",
        "userName": "mockUserName-1",
        "userDisplayName": "mockUserDisplayName-1",
        "counter": "mockCounter-1",
        "discoverable": "mockDiscoverable-1",
        "creationDate": "2023-10-27T12:00:00.000Z"
      }
    ]
  },
  "creationDate": "2023-10-27T12:00:00.000Z",
  "secureNote": {
    "type": 0
  },
  "folderId": "mockFolderId-1",
  "organizationId": "mockOrganizationId-1",
  "deletedDate": "2023-10-27T12:00:00.000Z",
  "archivedDate": "2023-10-27T12:00:00.000Z",
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
  "key": "mockKey-1",
  "sshKey": {
    "publicKey": "mockPublicKey-1",
    "privateKey": "mockPrivateKey-1",
    "keyFingerprint": "mockKeyFingerprint-1"
  },
  "encryptedFor": "mockEncryptedFor-1"
}
"""

private val CIPHER_ENTITY = CipherEntity(
    id = "mockId-1",
    userId = USER_ID,
    hasTotp = true,
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
    canManage = true,
    defaultUserCollectionEmail = "mockOffboardedUserEmail-3",
    type = "0",
)

private const val DOMAINS_JSON = """
{
  "globalEquivalentDomains": [
    {
      "excluded": false,
      "domains": [
        "mockDomain-1"
      ],
      "type": 1
    }
  ],
  "equivalentDomains": [
    [
      "mockEquivalentDomain-1"
    ]
  ]
}
"""

private val DOMAINS_ENTITY = DomainsEntity(
    domainsJson = DOMAINS_JSON,
    userId = USER_ID,
)

private val FOLDER_ENTITY = FolderEntity(
    id = "mockId-2",
    userId = USER_ID,
    name = "mockName-2",
    revisionDate = ZonedDateTime.parse("2023-10-27T12:00Z"),
)

private const val SEND_JSON = """
{
  "accessCount": 1,
  "notes": "mockNotes-1",
  "revisionDate": "2023-10-27T12:00:00.000Z",
  "maxAccessCount": 1,
  "hideEmail": false,
  "type": 1,
  "accessId": "mockAccessId-1",
  "password": "mockPassword-1",
  "file": {
    "fileName": "mockFileName-1",
    "size": 1,
    "sizeName": "mockSizeName-1",
    "id": "mockId-1"
  },
  "deletionDate": "2023-10-27T12:00:00.000Z",
  "name": "mockName-1",
  "disabled": false,
  "id": "mockId-1",
  "text": {
    "hidden": false,
    "text": "mockText-1"
  },
  "key": "mockKey-1",
  "expirationDate": "2023-10-27T12:00:00.000Z"
}
"""

private val SEND_ENTITY = SendEntity(
    id = "mockId-1",
    userId = USER_ID,
    sendType = "1",
    sendJson = SEND_JSON,
)
