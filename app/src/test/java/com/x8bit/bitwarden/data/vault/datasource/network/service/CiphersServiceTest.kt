package com.x8bit.bitwarden.data.vault.datasource.network.service

import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import com.x8bit.bitwarden.data.vault.datasource.network.api.CiphersApi
import com.x8bit.bitwarden.data.vault.datasource.network.model.ShareCipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateCipherResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockCipher
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockCipherJsonRequest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import retrofit2.create

class CiphersServiceTest : BaseServiceTest() {
    private val ciphersApi: CiphersApi = retrofit.create()

    private val ciphersService: CiphersService = CiphersServiceImpl(
        ciphersApi = ciphersApi,
        json = json,
    )

    @Test
    fun `createCipher should return the correct response`() = runTest {
        server.enqueue(MockResponse().setBody(CREATE_UPDATE_CIPHER_SUCCESS_JSON))
        val result = ciphersService.createCipher(
            body = createMockCipherJsonRequest(number = 1),
        )
        assertEquals(
            createMockCipher(number = 1),
            result.getOrThrow(),
        )
    }

    @Test
    fun `updateCipher with success response should return a Success with the correct cipher`() =
        runTest {
            server.enqueue(MockResponse().setBody(CREATE_UPDATE_CIPHER_SUCCESS_JSON))
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
    fun `shareCipher should execute the share cipher API`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(CREATE_UPDATE_CIPHER_SUCCESS_JSON),
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
    fun `restoreCipher should execute the restoreCipher API`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        val cipherId = "cipherId"
        val result = ciphersService.restoreCipher(cipherId = cipherId)
        assertEquals(Unit, result.getOrThrow())
    }
}

private const val CREATE_UPDATE_CIPHER_SUCCESS_JSON = """
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
        "uri": "mockUri-1"
      }
    ],
    "totp": "mockTotp-1",
    "password": "mockPassword-1",
    "passwordRevisionDate": "2023-10-27T12:00:00.00Z",
    "autofillOnPageLoad": false,
    "uri": "mockUri-1",
    "username": "mockUsername-1"
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
