package com.x8bit.bitwarden.data.vault.manager

import androidx.credentials.providerevents.exception.ImportCredentialsInvalidJsonException
import androidx.credentials.providerevents.exception.ImportCredentialsUnknownErrorException
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.cxf.model.CredentialExchangePayload
import com.bitwarden.cxf.parser.CredentialExchangePayloadParser
import com.bitwarden.network.model.ImportCiphersJsonRequest
import com.bitwarden.network.model.ImportCiphersResponseJson
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.createMockPolicy
import com.bitwarden.network.service.CiphersService
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
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
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
    private val credentialExchangePayloadParser: CredentialExchangePayloadParser = mockk {
        every { parse(DEFAULT_PAYLOAD) } returns CredentialExchangePayload.Importable(
            accountsJsonList = listOf(DEFAULT_ACCOUNT_JSON),
        )
    }

    private val importManager = CredentialExchangeImportManagerImpl(
        vaultSdkSource = vaultSdkSource,
        ciphersService = ciphersService,
        vaultSyncManager = vaultSyncManager,
        policyManager = policyManager,
        credentialExchangePayloadParser = credentialExchangePayloadParser,
    )

    @Nested
    inner class ParserResultHandling {
        @Test
        fun `when parser returns Error, should return Error`() = runTest {
            val parserException = ImportCredentialsInvalidJsonException("Invalid JSON")
            every {
                credentialExchangePayloadParser.parse(DEFAULT_PAYLOAD)
            } returns CredentialExchangePayload.Error(parserException)

            val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

            assertTrue(result is ImportCxfPayloadResult.Error)
            assertEquals(parserException, (result as ImportCxfPayloadResult.Error).error)
        }

        @Test
        fun `when parser returns NoItems, should return NoItems`() = runTest {
            every {
                credentialExchangePayloadParser.parse(DEFAULT_PAYLOAD)
            } returns CredentialExchangePayload.NoItems

            val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

            assertEquals(ImportCxfPayloadResult.NoItems, result)
        }
    }

    @Nested
    inner class ImportFlow {
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

            val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

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

                val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

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

            val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

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

                val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

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
        fun `when multiple accounts, should call importCxf for each and aggregate ciphers`() =
            runTest {
                every {
                    credentialExchangePayloadParser.parse(DEFAULT_PAYLOAD)
                } returns CredentialExchangePayload.Importable(
                    accountsJsonList = listOf(DEFAULT_ACCOUNT_JSON, DEFAULT_ACCOUNT_JSON_2),
                )

                val cipher1 = createMockSdkCipher(number = 1)
                val cipher2 = createMockSdkCipher(number = 2)
                coEvery {
                    vaultSdkSource.importCxf(
                        userId = DEFAULT_USER_ID,
                        payload = DEFAULT_ACCOUNT_JSON,
                    )
                } returns listOf(cipher1).asSuccess()
                coEvery {
                    vaultSdkSource.importCxf(
                        userId = DEFAULT_USER_ID,
                        payload = DEFAULT_ACCOUNT_JSON_2,
                    )
                } returns listOf(cipher2).asSuccess()

                val capturedRequest = slot<ImportCiphersJsonRequest>()
                coEvery {
                    ciphersService.importCiphers(capture(capturedRequest))
                } returns ImportCiphersResponseJson.Success.asSuccess()
                coEvery {
                    vaultSyncManager.syncForResult(forced = true)
                } returns SyncVaultDataResult.Success(itemsAvailable = true)

                val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

                assertEquals(ImportCxfPayloadResult.Success(itemCount = 2), result)
                assertEquals(2, capturedRequest.captured.ciphers.size)
                coVerify(exactly = 1) {
                    vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_ACCOUNT_JSON)
                    vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_ACCOUNT_JSON_2)
                    ciphersService.importCiphers(any())
                    vaultSyncManager.syncForResult(forced = true)
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `when second account importCxf fails, should return Error without uploading`() =
            runTest {
                every {
                    credentialExchangePayloadParser.parse(DEFAULT_PAYLOAD)
                } returns CredentialExchangePayload.Importable(
                    accountsJsonList = listOf(DEFAULT_ACCOUNT_JSON, DEFAULT_ACCOUNT_JSON_2),
                )

                coEvery {
                    vaultSdkSource.importCxf(
                        userId = DEFAULT_USER_ID,
                        payload = DEFAULT_ACCOUNT_JSON,
                    )
                } returns listOf(createMockSdkCipher(number = 1)).asSuccess()

                val exception = RuntimeException("SDK import failed on second account")
                coEvery {
                    vaultSdkSource.importCxf(
                        userId = DEFAULT_USER_ID,
                        payload = DEFAULT_ACCOUNT_JSON_2,
                    )
                } returns exception.asFailure()

                coEvery {
                    ciphersService.importCiphers(any())
                } just awaits

                val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

                assertEquals(ImportCxfPayloadResult.Error(exception), result)
                coVerify(exactly = 1) {
                    vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_ACCOUNT_JSON)
                    vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_ACCOUNT_JSON_2)
                }
                coVerify(exactly = 0) {
                    ciphersService.importCiphers(any())
                }
            }
    }

    @Nested
    inner class PolicyFiltering {
        @Test
        fun `when user has restrict item types policy, card ciphers should be filtered out`() =
            runTest {
                every {
                    policyManager.getActivePolicies(PolicyTypeJson.RESTRICT_ITEM_TYPES)
                } returns listOf(
                    createMockPolicy(
                        id = "mockId-1",
                        organizationId = "mockId-1",
                        type = PolicyTypeJson.RESTRICT_ITEM_TYPES,
                        isEnabled = true,
                        data = null,
                    ),
                )

                val loginCipher = createMockSdkCipher(number = 1)
                val cardCipher = createMockSdkCipher(number = 2).copy(
                    type = com.bitwarden.vault.CipherType.CARD,
                )
                val mixedCipherList = listOf(loginCipher, cardCipher)

                coEvery {
                    vaultSdkSource.importCxf(
                        userId = DEFAULT_USER_ID,
                        payload = DEFAULT_ACCOUNT_JSON,
                    )
                } returns mixedCipherList.asSuccess()

                val capturedRequest = slot<ImportCiphersJsonRequest>()
                coEvery {
                    ciphersService.importCiphers(capture(capturedRequest))
                } returns ImportCiphersResponseJson.Success.asSuccess()

                coEvery {
                    vaultSyncManager.syncForResult(forced = true)
                } returns SyncVaultDataResult.Success(itemsAvailable = true)

                val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

                assertEquals(ImportCxfPayloadResult.Success(itemCount = 1), result)
                // Verify only the login cipher was imported, card was filtered out
                assertEquals(1, capturedRequest.captured.ciphers.size)
                coVerify(exactly = 1) {
                    vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_ACCOUNT_JSON)
                    ciphersService.importCiphers(any())
                    vaultSyncManager.syncForResult(forced = true)
                }
            }

        @Test
        fun `when user has no restrict item types policy, card ciphers should not be filtered`() =
            runTest {
                every {
                    policyManager.getActivePolicies(PolicyTypeJson.RESTRICT_ITEM_TYPES)
                } returns emptyList()

                val loginCipher = createMockSdkCipher(number = 1)
                val cardCipher = createMockSdkCipher(number = 2).copy(
                    type = com.bitwarden.vault.CipherType.CARD,
                )
                val mixedCipherList = listOf(loginCipher, cardCipher)

                coEvery {
                    vaultSdkSource.importCxf(
                        userId = DEFAULT_USER_ID,
                        payload = DEFAULT_ACCOUNT_JSON,
                    )
                } returns mixedCipherList.asSuccess()

                val capturedRequest = slot<ImportCiphersJsonRequest>()
                coEvery {
                    ciphersService.importCiphers(capture(capturedRequest))
                } returns ImportCiphersResponseJson.Success.asSuccess()

                coEvery {
                    vaultSyncManager.syncForResult(forced = true)
                } returns SyncVaultDataResult.Success(itemsAvailable = true)

                val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

                assertEquals(ImportCxfPayloadResult.Success(itemCount = 2), result)
                // Verify both ciphers were imported
                assertEquals(2, capturedRequest.captured.ciphers.size)
                coVerify(exactly = 1) {
                    vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_ACCOUNT_JSON)
                    ciphersService.importCiphers(any())
                    vaultSyncManager.syncForResult(forced = true)
                }
            }

        @Test
        fun `when user has restrict policy disabled, card ciphers should not be filtered`() =
            runTest {
                every {
                    policyManager.getActivePolicies(PolicyTypeJson.RESTRICT_ITEM_TYPES)
                } returns listOf(
                    createMockPolicy(
                        id = "mockId-1",
                        organizationId = "mockId-1",
                        type = PolicyTypeJson.RESTRICT_ITEM_TYPES,
                        isEnabled = false,
                        data = null,
                    ),
                )

                val loginCipher = createMockSdkCipher(number = 1)
                val cardCipher = createMockSdkCipher(number = 2).copy(
                    type = com.bitwarden.vault.CipherType.CARD,
                )
                val mixedCipherList = listOf(loginCipher, cardCipher)

                coEvery {
                    vaultSdkSource.importCxf(
                        userId = DEFAULT_USER_ID,
                        payload = DEFAULT_ACCOUNT_JSON,
                    )
                } returns mixedCipherList.asSuccess()

                val capturedRequest = slot<ImportCiphersJsonRequest>()
                coEvery {
                    ciphersService.importCiphers(capture(capturedRequest))
                } returns ImportCiphersResponseJson.Success.asSuccess()

                coEvery {
                    vaultSyncManager.syncForResult(forced = true)
                } returns SyncVaultDataResult.Success(itemsAvailable = true)

                val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

                assertEquals(ImportCxfPayloadResult.Success(itemCount = 2), result)
                // Verify both ciphers were imported when policy is disabled
                assertEquals(2, capturedRequest.captured.ciphers.size)
                coVerify(exactly = 1) {
                    vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_ACCOUNT_JSON)
                    ciphersService.importCiphers(any())
                    vaultSyncManager.syncForResult(forced = true)
                }
            }

        @Test
        fun `when user has restrict policy and all ciphers are cards, should return NoItems`() =
            runTest {
                every {
                    policyManager.getActivePolicies(PolicyTypeJson.RESTRICT_ITEM_TYPES)
                } returns listOf(
                    createMockPolicy(
                        id = "mockId-1",
                        organizationId = "mockId-1",
                        type = PolicyTypeJson.RESTRICT_ITEM_TYPES,
                        isEnabled = true,
                        data = null,
                    ),
                )

                val cardCipher1 = createMockSdkCipher(number = 1).copy(
                    type = com.bitwarden.vault.CipherType.CARD,
                )
                val cardCipher2 = createMockSdkCipher(number = 2).copy(
                    type = com.bitwarden.vault.CipherType.CARD,
                )
                val allCardsList = listOf(cardCipher1, cardCipher2)

                coEvery {
                    vaultSdkSource.importCxf(
                        userId = DEFAULT_USER_ID,
                        payload = DEFAULT_ACCOUNT_JSON,
                    )
                } returns allCardsList.asSuccess()

                val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

                assertEquals(ImportCxfPayloadResult.NoItems, result)
                coVerify(exactly = 1) {
                    vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_ACCOUNT_JSON)
                }
                // Verify importCiphers was never called since all items were filtered
                coVerify(exactly = 0) {
                    ciphersService.importCiphers(any())
                }
            }
    }
}

private const val DEFAULT_USER_ID = "mockId-1"
private const val DEFAULT_PAYLOAD = "mockPayload-1"
private val DEFAULT_CIPHER: Cipher = createMockSdkCipher(number = 1)
private val DEFAULT_CIPHER_LIST: List<Cipher> = listOf(DEFAULT_CIPHER)

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

private val DEFAULT_ACCOUNT_JSON_2 = """
    {
      "id": "mockId-2",
      "username": "username-2",
      "email": "mockEmail-2",
      "fullName": "fullName-2",
      "collections": [],
      "items": [
        {
          "id": "mockId-2",
          "creationAt": 1759783057,
          "modifiedAt": 1759783057,
          "title": "mockTitle-2",
          "favorite": false,
          "scope": {
            "urls": [
              "mockUrl-2"
            ],
            "androidApps": []
          },
          "credentials": [
            {
              "type": "mockType-2",
              "username": {
                "fieldType": "mockUsernameFieldType-2",
                "value": "mockUsernameValue-2"
              },
              "password": {
                "fieldType": "mockPasswordFieldType-2",
                "value": "mockPasswordValue-2"
              }
            }
          ]
        }
      ]
    }
"""
    .trimIndent()
