package com.x8bit.bitwarden.data.vault.datasource.network.service

import android.net.Uri
import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import com.x8bit.bitwarden.data.vault.datasource.network.api.AzureApi
import com.x8bit.bitwarden.data.vault.datasource.network.api.CiphersApi
import com.x8bit.bitwarden.data.vault.datasource.network.model.CreateCipherInOrganizationJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.FileUploadType
import com.x8bit.bitwarden.data.vault.datasource.network.model.ShareCipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateCipherCollectionsJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateCipherResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockAttachment
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockAttachmentJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockAttachmentJsonResponse
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockCipher
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockCipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkAttachment
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.create
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CiphersServiceTest : BaseServiceTest() {
    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val azureApi: AzureApi = retrofit.create()
    private val ciphersApi: CiphersApi = retrofit.create()

    private val ciphersService: CiphersService = CiphersServiceImpl(
        azureApi = azureApi,
        ciphersApi = ciphersApi,
        json = json,
        clock = clock,
    )

    @BeforeEach
    fun setup() {
        mockkStatic(Uri::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `createCipher should return the correct response`() = runTest {
        server.enqueue(MockResponse().setBody(CREATE_RESTORE_UPDATE_CIPHER_SUCCESS_JSON))
        val result = ciphersService.createCipher(
            body = createMockCipherJsonRequest(number = 1),
        )
        assertEquals(
            createMockCipher(number = 1),
            result.getOrThrow(),
        )
    }

    @Test
    fun `createCipherInOrganization should return the correct response`() = runTest {
        server.enqueue(MockResponse().setBody(CREATE_RESTORE_UPDATE_CIPHER_SUCCESS_JSON))
        val result = ciphersService.createCipherInOrganization(
            body = CreateCipherInOrganizationJsonRequest(
                cipher = createMockCipherJsonRequest(number = 1),
                collectionIds = listOf("12345"),
            ),
        )
        assertEquals(
            createMockCipher(number = 1),
            result.getOrThrow(),
        )
    }

    @Test
    fun `createAttachment should return the correct response`() = runTest {
        server.enqueue(MockResponse().setBody(CREATE_ATTACHMENT_SUCCESS_JSON))
        val result = ciphersService.createAttachment(
            cipherId = "mockId-1",
            body = createMockAttachmentJsonRequest(number = 1),
        )
        assertEquals(
            createMockAttachmentJsonResponse(number = 1),
            result.getOrThrow(),
        )
    }

    @Test
    fun `uploadAttachment with Azure uploadFile success should return cipher`() = runTest {
        setupMockUri(url = "mockUrl-1", queryParams = mapOf("sv" to "2024-04-03"))
        val mockCipher = createMockCipher(number = 1)
        val attachmentJsonResponse = createMockAttachmentJsonResponse(
            number = 1,
            fileUploadType = FileUploadType.AZURE,
        )
        val encryptedFile = File.createTempFile("mockFile", "temp")
        server.enqueue(MockResponse().setResponseCode(201))

        val result = ciphersService.uploadAttachment(
            attachmentJsonResponse = attachmentJsonResponse,
            encryptedFile = encryptedFile,
        )

        assertEquals(mockCipher, result.getOrThrow())
    }

    @Test
    fun `uploadAttachment with Direct uploadFile success should return cipher`() = runTest {
        val mockCipher = createMockCipher(number = 1)
        val attachmentJsonResponse = createMockAttachmentJsonResponse(
            number = 1,
            fileUploadType = FileUploadType.DIRECT,
        )
        val encryptedFile = File.createTempFile("mockFile", "temp")
        server.enqueue(MockResponse().setResponseCode(201))

        val result = ciphersService.uploadAttachment(
            attachmentJsonResponse = attachmentJsonResponse,
            encryptedFile = encryptedFile,
        )

        assertEquals(mockCipher, result.getOrThrow())
    }

    @Test
    fun `updateCipher with success response should return a Success with the correct cipher`() =
        runTest {
            server.enqueue(MockResponse().setBody(CREATE_RESTORE_UPDATE_CIPHER_SUCCESS_JSON))
            val result = ciphersService.updateCipher(
                cipherId = "cipher-id-1",
                body = createMockCipherJsonRequest(number = 1),
            )
            assertEquals(
                UpdateCipherResponseJson.Success(
                    cipher = createMockCipher(number = 1),
                ),
                result.getOrThrow(),
            )
        }

    @Test
    fun `updateCipher with an invalid response should return an Invalid with the correct data`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(400).setBody(UPDATE_CIPHER_INVALID_JSON))
            val result = ciphersService.updateCipher(
                cipherId = "cipher-id-1",
                body = createMockCipherJsonRequest(number = 1),
            )
            assertEquals(
                UpdateCipherResponseJson.Invalid(
                    message = "You do not have permission to edit this.",
                    validationErrors = null,
                ),
                result.getOrThrow(),
            )
        }

    @Test
    fun `hardDeleteCipher should execute the hardDeleteCipher API`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        val cipherId = "cipherId"
        val result = ciphersService.hardDeleteCipher(cipherId = cipherId)
        assertEquals(Unit, result.getOrThrow())
    }

    @Test
    fun `softDeleteCipher should execute the softDeleteCipher API`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        val cipherId = "cipherId"
        val result = ciphersService.softDeleteCipher(cipherId = cipherId)
        assertEquals(Unit, result.getOrThrow())
    }

    @Test
    fun `deleteCipherAttachment should execute the deleteCipherAttachment API`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        val cipherId = "cipherId"
        val attachmentId = "attachmentId"
        val result = ciphersService.deleteCipherAttachment(
            cipherId = cipherId,
            attachmentId = attachmentId,
        )
        assertEquals(Unit, result.getOrThrow())
    }

    @Test
    fun `shareAttachment without attachment ID should return an error`() = runTest {
        val cipherId = "cipherId"
        val organizationId = "organizationId"
        val attachment = createMockSdkAttachment(number = 1).copy(id = null)
        val encryptedFile = File.createTempFile("mockFile", "temp")

        val result = ciphersService.shareAttachment(
            cipherId = cipherId,
            attachment = attachment,
            organizationId = organizationId,
            encryptedFile = encryptedFile,
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `shareAttachment without attachment key should return an error`() = runTest {
        val cipherId = "cipherId"
        val organizationId = "organizationId"
        val attachment = createMockSdkAttachment(number = 1, key = null)
        val encryptedFile = File.createTempFile("mockFile", "temp")

        val result = ciphersService.shareAttachment(
            cipherId = cipherId,
            attachment = attachment,
            organizationId = organizationId,
            encryptedFile = encryptedFile,
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `shareAttachment should execute the share attachment API`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        val cipherId = "cipherId"
        val organizationId = "organizationId"
        val attachment = createMockSdkAttachment(number = 1)
        val encryptedFile = File.createTempFile("mockFile", "temp")

        val result = ciphersService.shareAttachment(
            cipherId = cipherId,
            attachment = attachment,
            organizationId = organizationId,
            encryptedFile = encryptedFile,
        )

        assertEquals(Unit, result.getOrThrow())
    }

    @Test
    fun `shareCipher should execute the share cipher API`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(CREATE_RESTORE_UPDATE_CIPHER_SUCCESS_JSON),
        )

        val result = ciphersService.shareCipher(
            cipherId = "mockId-1",
            body = ShareCipherJsonRequest(
                cipher = createMockCipherJsonRequest(number = 1),
                collectionIds = listOf("mockId-1"),
            ),
        )
        assertEquals(
            createMockCipher(number = 1),
            result.getOrThrow(),
        )
    }

    @Test
    fun `updateCipherCollections should execute the updateCipherCollections API`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        val result = ciphersService.updateCipherCollections(
            cipherId = "mockId-1",
            body = UpdateCipherCollectionsJsonRequest(
                collectionIds = listOf("mockId-1"),
            ),
        )
        assertEquals(
            Unit,
            result.getOrThrow(),
        )
    }

    @Test
    fun `restoreCipher should execute the restoreCipher API`() = runTest {
        server.enqueue(MockResponse().setBody(CREATE_RESTORE_UPDATE_CIPHER_SUCCESS_JSON))
        val cipherId = "cipherId"
        val result = ciphersService.restoreCipher(cipherId = cipherId)
        assertEquals(createMockCipher(number = 1), result.getOrThrow())
    }

    @Test
    fun `getCipher should return the correct response`() = runTest {
        server.enqueue(MockResponse().setBody(CREATE_RESTORE_UPDATE_CIPHER_SUCCESS_JSON))
        val result = ciphersService.getCipher(cipherId = "mockId-1")
        assertEquals(
            createMockCipher(number = 1),
            result.getOrThrow(),
        )
    }

    @Test
    fun `getCipherAttachment should return the correct response`() = runTest {
        server.enqueue(MockResponse().setBody(GET_CIPHER_ATTACHMENT_SUCCESS_JSON))
        val result = ciphersService.getCipherAttachment(
            cipherId = "mockId-1",
            attachmentId = "mockId-1",
        )
        assertEquals(
            createMockAttachment(number = 1),
            result.testGetOrThrow(),
        )
    }

    @Test
    fun `hasUnassignedCiphers should return the correct response`() = runTest {
        server.enqueue(MockResponse().setBody("true"))
        val result = ciphersService.hasUnassignedCiphers()
        assertTrue(result.getOrThrow())
    }
}

private fun setupMockUri(
    url: String,
    queryParams: Map<String, String>,
): Uri {
    val mockUri = mockk<Uri> {
        queryParams.forEach {
            every { getQueryParameter(it.key) } returns it.value
        }
    }
    every { Uri.parse(url) } returns mockUri
    return mockUri
}

/**
 * A helper method to attempt validate that the the value is being boxed and causing test to
 * inconsistently fail.
 *
 * This was modified from the code found here:
 * * https://github.com/mockk/mockk/issues/485#issuecomment-1973170516
 */
@Suppress("INVISIBLE_REFERENCE", "UNCHECKED_CAST")
private fun <T> Result<T>.testGetOrThrow(): T =
    when (val unboxed: Any? = value) {
        is Result.Failure -> throw unboxed.exception
        !is Result<*> -> unboxed as T
        else -> {
            // This means the result is boxed, we could make this recursive to address the issue.
            println("Unboxed value = $unboxed")
            unboxed as T
        }
    }

private const val CREATE_ATTACHMENT_SUCCESS_JSON = """
{
  "attachmentId":"mockAttachmentId-1",
  "url":"mockUrl-1",
  "fileUploadType":1,
  "cipherResponse":{
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
        "lastUsedDate": "2023-10-27T12:00:00.00Z"
      }
    ],
    "revisionDate": "2023-10-27T12:00:00.00Z",
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
      "passwordRevisionDate": "2023-10-27T12:00:00.00Z",
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
          "creationDate": "2023-10-27T12:00:00.00Z"
        }
      ]
    },
    "creationDate": "2023-10-27T12:00:00.00Z",
    "secureNote": {
      "type": 0
    },
    "folderId": "mockFolderId-1",
    "organizationId": "mockOrganizationId-1",
    "deletedDate": "2023-10-27T12:00:00.00Z",
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
    "id": "mockId-1"
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
}
"""

private const val CREATE_RESTORE_UPDATE_CIPHER_SUCCESS_JSON = """
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
      "lastUsedDate": "2023-10-27T12:00:00.00Z"
    }
  ],
  "revisionDate": "2023-10-27T12:00:00.00Z",
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
    "passwordRevisionDate": "2023-10-27T12:00:00.00Z",
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
        "creationDate": "2023-10-27T12:00:00.00Z"
      }
    ]
  },
  "creationDate": "2023-10-27T12:00:00.00Z",
  "secureNote": {
    "type": 0
  },
  "folderId": "mockFolderId-1",
  "organizationId": "mockOrganizationId-1",
  "deletedDate": "2023-10-27T12:00:00.00Z",
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
  "id": "mockId-1"
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

private const val UPDATE_CIPHER_INVALID_JSON = """
{
  "message": "You do not have permission to edit this.",
  "validationErrors": null
}
"""

private const val GET_CIPHER_ATTACHMENT_SUCCESS_JSON = """
{
  "fileName": "mockFileName-1",
  "size": 1,
  "sizeName": "mockSizeName-1",
  "id": "mockId-1",
  "url": "mockUrl-1",
  "key": "mockKey-1"
}
"""
