package com.x8bit.bitwarden.data.vault.manager

import androidx.credentials.providerevents.exception.ImportCredentialsUnknownErrorException
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.bitwarden.cxf.model.CredentialExchangeExportResponse
import com.bitwarden.cxf.model.CredentialExchangeProtocolMessage
import com.bitwarden.cxf.model.CredentialExchangeVersion
import com.bitwarden.network.model.ImportCiphersJsonRequest
import com.bitwarden.network.model.ImportCiphersResponseJson
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.service.CiphersService
import com.bitwarden.network.util.base64UrlDecodeOrNull
import com.bitwarden.vault.Cipher
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkCipher
import com.x8bit.bitwarden.data.vault.manager.model.ImportCxfPayloadResult
import com.x8bit.bitwarden.data.vault.manager.model.SyncVaultDataResult
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class CredentialExchangeImportManagerTest {

    private val vaultSdkSource: VaultSdkSource = mockk()
    private val ciphersService: CiphersService = mockk(relaxed = true)
    private val vaultSyncManager: VaultSyncManager = mockk()
    private val policyManager: PolicyManager = mockk {
        every {
            getActivePolicies(any())
        } returns emptyList()
    }
    private val json = mockk<Json> {
        every {
            decodeFromStringOrNull<CredentialExchangeProtocolMessage>(any())
        } returns DEFAULT_CXP_MESSAGE
        every {
            decodeFromStringOrNull<CredentialExchangeExportResponse>(any())
        } returns DEFAULT_CXF_EXPORT_RESPONSE
        every {
            encodeToString(value = DEFAULT_ACCOUNT, serializer = any())
        } returns DEFAULT_ACCOUNT_JSON
    }

    private val importManager = CredentialExchangeImportManagerImpl(
        vaultSdkSource = vaultSdkSource,
        ciphersService = ciphersService,
        vaultSyncManager = vaultSyncManager,
        policyManager = policyManager,
        json = json,
    )

    @BeforeEach
    fun setUp() {
        mockkStatic(String::base64UrlDecodeOrNull)
        every {
            DEFAULT_PAYLOAD.base64UrlDecodeOrNull()
        } returns DEFAULT_PAYLOAD
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(String::base64UrlDecodeOrNull)
    }

    @Test
    fun `when vaultSdkSource importCxf fails, should return Error`() = runTest {
        val exception = RuntimeException("SDK import failed")
        coEvery {
            vaultSdkSource.importCxf(
                userId = DEFAULT_USER_ID,
                payload = DEFAULT_ACCOUNT_JSON,
            )
        } returns exception.asFailure()

        coEvery {
            ciphersService.importCiphers(any())
        } just awaits

        val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

        assertEquals(ImportCxfPayloadResult.Error(exception), result)
        coVerify(exactly = 1) {
            vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_ACCOUNT_JSON)
        }
        coVerify(exactly = 0) {
            ciphersService.importCiphers(any())
        }
    }

    @Test
    fun `when ciphersService importCiphers fails, should return Error`() = runTest {
        coEvery {
            vaultSdkSource.importCxf(
                userId = DEFAULT_USER_ID,
                payload = DEFAULT_ACCOUNT_JSON,
            )
        } returns DEFAULT_CIPHER_LIST.asSuccess()

        val exception = RuntimeException("Network import failed")
        val capturedRequest = slot<ImportCiphersJsonRequest>()
        coEvery {
            ciphersService.importCiphers(capture(capturedRequest))
        } returns exception.asFailure()

        val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

        assertEquals(ImportCxfPayloadResult.Error(exception), result)
        assertEquals(1, capturedRequest.captured.ciphers.size)
        coVerify(exactly = 1) {
            vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_ACCOUNT_JSON)
            ciphersService.importCiphers(any())
        }
    }

    @Test
    fun `when ciphersService importCiphers returns Invalid, should return Error`() = runTest {
        coEvery {
            vaultSdkSource.importCxf(
                userId = DEFAULT_USER_ID,
                payload = DEFAULT_ACCOUNT_JSON,
            )
        } returns DEFAULT_CIPHER_LIST.asSuccess()

        coEvery {
            ciphersService.importCiphers(any())
        } returns ImportCiphersResponseJson
            .Invalid(validationErrors = emptyMap())
            .asSuccess()

        val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_ACCOUNT_JSON)

        val error = (result as? ImportCxfPayloadResult.Error)?.error
        assertNotNull(error)
        assertTrue(error is ImportCredentialsUnknownErrorException)
        coVerify(exactly = 1) {
            vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_ACCOUNT_JSON)
            ciphersService.importCiphers(any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when ciphersService importCiphers is Success and sync fails should return SyncFailed`() =
        runTest {
            coEvery {
                vaultSdkSource.importCxf(
                    userId = DEFAULT_USER_ID,
                    payload = DEFAULT_ACCOUNT_JSON,
                )
            } returns DEFAULT_CIPHER_LIST.asSuccess()

            coEvery {
                ciphersService.importCiphers(any())
            } returns ImportCiphersResponseJson.Success.asSuccess()
            val throwable = Throwable("Error!")
            coEvery {
                vaultSyncManager.syncForResult(forced = true)
            } returns SyncVaultDataResult.Error(throwable)

            val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_ACCOUNT_JSON)

            assertEquals(
                ImportCxfPayloadResult.SyncFailed(throwable),
                result,
            )
            coVerify(exactly = 1) {
                vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_ACCOUNT_JSON)
                ciphersService.importCiphers(any())
                vaultSyncManager.syncForResult(forced = true)
            }
        }

    @Test
    fun `when all steps succeed, should return Success`() = runTest {
        coEvery {
            vaultSdkSource.importCxf(
                userId = DEFAULT_USER_ID,
                payload = DEFAULT_ACCOUNT_JSON,
            )
        } returns DEFAULT_CIPHER_LIST.asSuccess()

        coEvery {
            ciphersService.importCiphers(any())
        } returns ImportCiphersResponseJson.Success.asSuccess()
        coEvery {
            vaultSyncManager.syncForResult(forced = true)
        } returns SyncVaultDataResult.Success(itemsAvailable = true)

        val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_ACCOUNT_JSON)

        assertEquals(ImportCxfPayloadResult.Success(itemCount = 1), result)
        coVerify(exactly = 1) {
            vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_ACCOUNT_JSON)
            ciphersService.importCiphers(any())
            vaultSyncManager.syncForResult(forced = true)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when importCxf returns empty cipher list, should skip importCiphers and sync and return NoItems`() =
        runTest {
            coEvery {
                vaultSdkSource.importCxf(
                    userId = DEFAULT_USER_ID,
                    payload = DEFAULT_ACCOUNT_JSON,
                )
            } returns emptyList<Cipher>().asSuccess()
            coEvery {
                ciphersService.importCiphers(any())
            } just awaits

            val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_ACCOUNT_JSON)

            assertEquals(ImportCxfPayloadResult.NoItems, result)
            coVerify(exactly = 1) {
                vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_ACCOUNT_JSON)
            }
            coVerify(exactly = 0) {
                ciphersService.importCiphers(any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `when payload cannot be deserialized into CredentialExchangeProtocolMessage, should return Error`() =
        runTest {
            every {
                json.decodeFromStringOrNull<CredentialExchangeProtocolMessage>(any())
            } returns null

            val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

            assertTrue(result is ImportCxfPayloadResult.Error)
        }

    @Test
    fun `when payload version is not supported, should return Error`() = runTest {
        // Verify unsupported major version returns Error
        every {
            json.decodeFromStringOrNull<CredentialExchangeProtocolMessage>(any())
        } returns DEFAULT_CXP_MESSAGE.copy(
            version = DEFAULT_CXP_VERSION.copy(major = 1),
        )

        var result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

        assertTrue(result is ImportCxfPayloadResult.Error)

        // Verify unsupported minor version returns Error
        every {
            json.decodeFromStringOrNull<CredentialExchangeProtocolMessage>(any())
        } returns DEFAULT_CXP_MESSAGE.copy(
            version = DEFAULT_CXP_VERSION.copy(minor = 1),
        )

        result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

        assertTrue(result is ImportCxfPayloadResult.Error)
    }

    @Test
    fun `when decodedPayload is null, should return Error`() = runTest {
        every {
            DEFAULT_CXP_MESSAGE.payload.base64UrlDecodeOrNull()
        } returns null

        val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

        assertTrue(result is ImportCxfPayloadResult.Error)
    }

    @Test
    fun `when CredentialExchangeExportResponse json is invalid, should return Error`() = runTest {
        every {
            json.decodeFromStringOrNull<CredentialExchangeExportResponse>(DEFAULT_PAYLOAD)
        } returns null

        val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

        assertTrue(result is ImportCxfPayloadResult.Error)
    }

    @Test
    fun `when CredentialExchangeExportResponse version is not supported, should return Error`() =
        runTest {
            every {
                json.decodeFromStringOrNull<CredentialExchangeExportResponse>(DEFAULT_PAYLOAD)
            } returns DEFAULT_CXF_EXPORT_RESPONSE.copy(
                version = DEFAULT_CXF_VERSION.copy(major = 2),
            )

            var result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

            assertTrue(result is ImportCxfPayloadResult.Error)

            every {
                json.decodeFromStringOrNull<CredentialExchangeExportResponse>(DEFAULT_PAYLOAD)
            } returns DEFAULT_CXF_EXPORT_RESPONSE.copy(
                version = DEFAULT_CXF_VERSION.copy(minor = 1),
            )

            result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

            assertTrue(result is ImportCxfPayloadResult.Error)
        }

    @Test
    fun `when CredentialExchangeExportResponse accounts is empty, should return NoItems`() =
        runTest {
            every {
                json.decodeFromStringOrNull<CredentialExchangeExportResponse>(DEFAULT_PAYLOAD)
            } returns DEFAULT_CXF_EXPORT_RESPONSE.copy(
                accounts = emptyList(),
            )

            val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

            assertTrue(result is ImportCxfPayloadResult.NoItems)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `when CredentialExchangeExportResponse account cannot be serialized, should return Error`() =
        runTest {
            every {
                json.encodeToString(DEFAULT_CXF_EXPORT_RESPONSE.accounts.firstOrNull())
            } throws SerializationException()

            val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

            assertTrue(result is ImportCxfPayloadResult.Error)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `when user has restrict item types policy, importCxf should not contain any card item`() =
        runTest {
            every {
                json.decodeFromStringOrNull<CredentialExchangeProtocolMessage>(
                    CARD_PAYLOAD,
                )
            } returns CARD_CXP_MESSAGE

            every {
                json.decodeFromStringOrNull<CredentialExchangeExportResponse>(CARD_PAYLOAD)
            } returns CARD_CXF_EXPORT_RESPONSE

            every {
                json.encodeToString(
                    serializer = any(),
                    value = DEFAULT_ACCOUNT_EMPTY,
                )
            } returns DEFAULT_ACCOUNT_CARD_STRIPPED_JSON

            every {
                CARD_PAYLOAD.base64UrlDecodeOrNull()
            } returns CARD_PAYLOAD

            coEvery {
                policyManager.getActivePolicies(PolicyTypeJson.RESTRICT_ITEM_TYPES)
            } returns listOf(
                SyncResponseJson.Policy(
                    id = "mockId-1",
                    organizationId = "mockId-1",
                    type = PolicyTypeJson.RESTRICT_ITEM_TYPES,
                    isEnabled = true,
                    data = null,
                ),
            )

            coEvery {
                vaultSdkSource.importCxf(
                    userId = DEFAULT_USER_ID,
                    payload = DEFAULT_ACCOUNT_CARD_STRIPPED_JSON,
                )
            } returns emptyList<Cipher>().asSuccess()

            val result = importManager.importCxfPayload(DEFAULT_USER_ID, CARD_PAYLOAD)

            assertEquals(ImportCxfPayloadResult.NoItems, result)
            coVerify(exactly = 1) {
                vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_ACCOUNT_CARD_STRIPPED_JSON)
            }
            coVerify(exactly = 0) {
                ciphersService.importCiphers(any())
            }
        }
}

private const val DEFAULT_USER_ID = "mockId-1"
private const val DEFAULT_PAYLOAD = "mockPayload-1"
private const val CARD_PAYLOAD = "mockPayload-Card"
private val DEFAULT_CIPHER: Cipher = createMockSdkCipher(number = 1)
private val DEFAULT_CIPHER_LIST: List<Cipher> = listOf(DEFAULT_CIPHER)
private val DEFAULT_CXP_VERSION = CredentialExchangeVersion(
    major = 0,
    minor = 0,
)
private val DEFAULT_CXF_VERSION = CredentialExchangeVersion(
    major = 1,
    minor = 0,
)
private val DEFAULT_CXP_MESSAGE: CredentialExchangeProtocolMessage =
    CredentialExchangeProtocolMessage(
        version = DEFAULT_CXP_VERSION,
        exporterRpId = "mockRpId-1",
        exporterDisplayName = "mockDisplayName-1",
        payload = DEFAULT_PAYLOAD,
    )
private val DEFAULT_ACCOUNT: CredentialExchangeExportResponse.Account =
    CredentialExchangeExportResponse.Account(
        id = "mockId-1",
        username = "mockUsername-1",
        email = "mockEmail-1",
        collections = JsonArray(content = emptyList()),
        items = JsonArray(content = emptyList()),
    )
private val DEFAULT_ACCOUNT_WITH_CARD: CredentialExchangeExportResponse.Account =
    CredentialExchangeExportResponse.Account(
        id = "mockId-2",
        username = "mockUsername-2",
        email = "mockEmail-2",
        collections = JsonArray(content = emptyList()),
        items = JsonArray(
            content = listOf(
                buildJsonObject {
                    put("id", JsonPrimitive("card-123"))
                    put("name", JsonPrimitive("Test Credit Card"))
                    put("credentials", buildJsonArray {
                        add(buildJsonObject {
                            put("type", JsonPrimitive("credit-card"))
                            put("cardNumber", JsonPrimitive("4111111111111111"))
                            put("cardholderName", JsonPrimitive("Test User"))
                        })
                    })
                },
            ),
        ),
    )
private val DEFAULT_CXF_EXPORT_RESPONSE: CredentialExchangeExportResponse =
    CredentialExchangeExportResponse(
        version = DEFAULT_CXF_VERSION,
        exporterRpId = "mockRpId-1",
        exporterDisplayName = "mockDisplayName-1",
        timestamp = 0,
        accounts = listOf(DEFAULT_ACCOUNT),
    )

private val CARD_CXP_MESSAGE: CredentialExchangeProtocolMessage =
    CredentialExchangeProtocolMessage(
        version = DEFAULT_CXP_VERSION,
        exporterRpId = "mockRpId-1",
        exporterDisplayName = "mockDisplayName-1",
        payload = CARD_PAYLOAD,
    )

private val CARD_CXF_EXPORT_RESPONSE: CredentialExchangeExportResponse =
    CredentialExchangeExportResponse(
        version = DEFAULT_CXF_VERSION,
        exporterRpId = "mockRpId-1",
        exporterDisplayName = "mockDisplayName-1",
        timestamp = 0,
        accounts = listOf(DEFAULT_ACCOUNT_WITH_CARD),
    )

private val DEFAULT_ACCOUNT_EMPTY: CredentialExchangeExportResponse.Account =
    CredentialExchangeExportResponse.Account(
        id = "mockId-2",
        username = "mockUsername-2",
        email = "mockEmail-2",
        collections = JsonArray(content = emptyList()),
        items = JsonArray(content = emptyList()),
    )

private val DEFAULT_ACCOUNT_JSON = """
    {
      "id": "$DEFAULT_USER_ID",
      "username": "username-1",
      "email": "mockEmail-1",
      "fullName": "fullName-1",
      "collections": [],
      "items": [
        {
          "id": "mockId-1",
          "creationAt": 1759783057,
          "modifiedAt": 1759783057,
          "title": "mockTitle-1",
          "favorite": false,
          "scope": {
            "urls": [
              "mockUrl-1"
            ],
            "androidApps": []
          },
          "credentials": [
            {
              "type": "mockType-1",
              "username": {
                "fieldType": "mockUsernameFieldType-1",
                "value": "mockUsernameValue-1"
              },
              "password": {
                "fieldType": "mockPasswordFieldType-1",
                "value": "mockPasswordValue-1"
              }
            }
          ]
        }
      ]
    }
"""
    .trimIndent()

private val DEFAULT_ACCOUNT_CARD_JSON = """
    {
      "id": "$DEFAULT_USER_ID",
      "username": "username-1",
      "email": "mockEmail-1",
      "fullName": "fullName-1",
      "collections": [],
      "items": [
        {
          "id": "mockId-1",
          "creationAt": 1759783057,
          "modifiedAt": 1759783057,
          "title": "mockTitle-1",
          "favorite": false,
          "scope": {
            "urls": [
              "mockUrl-1"
            ],
            "androidApps": []
          },
          "credentials": [
            {
              "type": "credit-card",
              "username": {
                "fieldType": "mockUsernameFieldType-1",
                "value": "mockUsernameValue-1"
              },
              "password": {
                "fieldType": "mockPasswordFieldType-1",
                "value": "mockPasswordValue-1"
              }
            }
          ]
        }
      ]
    }
"""
    .trimIndent()

private val DEFAULT_ACCOUNT_CARD_STRIPPED_JSON = """
    {
      "id": "$DEFAULT_USER_ID",
      "username": "username-1",
      "email": "mockEmail-1",
      "fullName": "fullName-1",
      "collections": [],
      "items": [
        {
          "id": "mockId-1",
          "creationAt": 1759783057,
          "modifiedAt": 1759783057,
          "title": "mockTitle-1",
          "favorite": false,
          "scope": {
            "urls": [
              "mockUrl-1"
            ],
            "androidApps": []
          },
          "credentials": [
          ]
        }
      ]
    }
"""
    .trimIndent()
