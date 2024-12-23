package com.x8bit.bitwarden.data.auth.manager

import app.cash.turbine.test
import com.bitwarden.core.AuthRequestResponse
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.PendingAuthRequestJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestsResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.service.AuthRequestsService
import com.x8bit.bitwarden.data.auth.datasource.network.service.NewAuthRequestService
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequest
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestResult
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestType
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestUpdatesResult
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestsResult
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestsUpdatesResult
import com.x8bit.bitwarden.data.auth.manager.model.CreateAuthRequestResult
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Suppress("LargeClass")
class AuthRequestManagerTest {
    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val authRequestsService: AuthRequestsService = mockk()
    private val newAuthRequestService: NewAuthRequestService = mockk()
    private val authSdkSource: AuthSdkSource = mockk()
    private val vaultSdkSource = mockk<VaultSdkSource> {
        coEvery {
            getAuthRequestKey(publicKey = PUBLIC_KEY, userId = USER_ID)
        } returns "AsymmetricEncString".asSuccess()
    }
    private val fakeAuthDiskSource = FakeAuthDiskSource()

    private val repository: AuthRequestManager = AuthRequestManagerImpl(
        clock = fixedClock,
        authRequestsService = authRequestsService,
        newAuthRequestService = newAuthRequestService,
        authSdkSource = authSdkSource,
        vaultSdkSource = vaultSdkSource,
        authDiskSource = fakeAuthDiskSource,
    )

    @Suppress("MaxLineLength")
    @Test
    fun `createAuthRequestWithUpdates with newAuthRequestService createAuthRequest error should emit Error`() =
        runTest {
            val email = "email@email.com"
            val authRequestResponse = AUTH_REQUEST_RESPONSE
            coEvery {
                authSdkSource.getNewAuthRequest(email = email)
            } returns authRequestResponse.asSuccess()
            coEvery {
                newAuthRequestService.createAuthRequest(
                    email = email,
                    publicKey = authRequestResponse.publicKey,
                    deviceId = fakeAuthDiskSource.uniqueAppId,
                    accessCode = authRequestResponse.accessCode,
                    fingerprint = authRequestResponse.fingerprint,
                    authRequestType = AuthRequestTypeJson.LOGIN_WITH_DEVICE,
                )
            } returns Throwable("Fail").asFailure()

            repository
                .createAuthRequestWithUpdates(
                    email = email,
                    authRequestType = AuthRequestType.OTHER_DEVICE,
                )
                .test {
                    assertEquals(CreateAuthRequestResult.Error, awaitItem())
                    awaitComplete()
                }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createAuthRequestWithUpdates with createNewAuthRequest Success and getAuthRequestUpdate with approval should emit Success`() =
        runTest {
            val email = "email@email.com"
            val authRequestResponse = AUTH_REQUEST_RESPONSE
            val authRequestResponseJson = AuthRequestsResponseJson.AuthRequest(
                id = "1",
                publicKey = PUBLIC_KEY,
                platform = "Android",
                ipAddress = "192.168.0.1",
                key = "public",
                masterPasswordHash = "verySecureHash",
                creationDate = ZonedDateTime.parse("2024-09-13T00:00Z"),
                responseDate = null,
                requestApproved = false,
                originUrl = "www.bitwarden.com",
            )
            val updatedAuthRequestResponseJson = authRequestResponseJson.copy(
                requestApproved = true,
            )
            val authRequest = AuthRequest(
                id = authRequestResponseJson.id,
                publicKey = authRequestResponseJson.publicKey,
                platform = authRequestResponseJson.platform,
                ipAddress = authRequestResponseJson.ipAddress,
                key = authRequestResponseJson.key,
                masterPasswordHash = authRequestResponseJson.masterPasswordHash,
                creationDate = authRequestResponseJson.creationDate,
                responseDate = authRequestResponseJson.responseDate,
                requestApproved = authRequestResponseJson.requestApproved ?: false,
                originUrl = authRequestResponseJson.originUrl,
                fingerprint = authRequestResponse.fingerprint,
            )
            coEvery {
                authSdkSource.getNewAuthRequest(email = email)
            } returns authRequestResponse.asSuccess()
            coEvery {
                newAuthRequestService.createAuthRequest(
                    email = email,
                    publicKey = authRequestResponse.publicKey,
                    deviceId = fakeAuthDiskSource.uniqueAppId,
                    accessCode = authRequestResponse.accessCode,
                    fingerprint = authRequestResponse.fingerprint,
                    authRequestType = AuthRequestTypeJson.LOGIN_WITH_DEVICE,
                )
            } returns authRequestResponseJson.asSuccess()
            coEvery {
                newAuthRequestService.getAuthRequestUpdate(
                    requestId = authRequest.id,
                    accessCode = authRequestResponse.accessCode,
                    isSso = false,
                )
            } returnsMany listOf(
                authRequestResponseJson.asSuccess(),
                updatedAuthRequestResponseJson.asSuccess(),
            )

            repository
                .createAuthRequestWithUpdates(
                    email = email,
                    authRequestType = AuthRequestType.OTHER_DEVICE,
                )
                .test {
                    assertEquals(CreateAuthRequestResult.Update(authRequest), awaitItem())
                    assertEquals(CreateAuthRequestResult.Update(authRequest), awaitItem())
                    assertEquals(
                        CreateAuthRequestResult.Success(
                            authRequest = authRequest.copy(requestApproved = true),
                            privateKey = authRequestResponse.privateKey,
                            accessCode = authRequestResponse.accessCode,
                        ),
                        awaitItem(),
                    )
                    awaitComplete()
                }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createAuthRequestWithUpdates with a pending admin request Success and getAuthRequestUpdate with approval should emit Success`() =
        runTest {
            val email = "email@email.com"
            val authRequestResponse = AUTH_REQUEST_RESPONSE
            val authRequestResponseJson = AuthRequestsResponseJson.AuthRequest(
                id = "1",
                publicKey = PUBLIC_KEY,
                platform = "Android",
                ipAddress = "192.168.0.1",
                key = "public",
                masterPasswordHash = "verySecureHash",
                creationDate = ZonedDateTime.parse("2024-09-13T00:00Z"),
                responseDate = null,
                requestApproved = false,
                originUrl = "www.bitwarden.com",
            )
            fakeAuthDiskSource.userState = SINGLE_USER_STATE
            fakeAuthDiskSource.storePendingAuthRequest(
                userId = USER_ID,
                pendingAuthRequest = PendingAuthRequestJson(
                    requestId = authRequestResponseJson.id,
                    requestPrivateKey = authRequestResponse.privateKey,
                    requestAccessCode = authRequestResponse.accessCode,
                    requestFingerprint = authRequestResponse.fingerprint,
                ),
            )
            val updatedAuthRequestResponseJson = authRequestResponseJson.copy(
                requestApproved = true,
            )
            val authRequest = AuthRequest(
                id = authRequestResponseJson.id,
                publicKey = authRequestResponseJson.publicKey,
                platform = authRequestResponseJson.platform,
                ipAddress = authRequestResponseJson.ipAddress,
                key = authRequestResponseJson.key,
                masterPasswordHash = authRequestResponseJson.masterPasswordHash,
                creationDate = authRequestResponseJson.creationDate,
                responseDate = authRequestResponseJson.responseDate,
                requestApproved = authRequestResponseJson.requestApproved ?: false,
                originUrl = authRequestResponseJson.originUrl,
                fingerprint = authRequestResponse.fingerprint,
            )
            coEvery {
                authRequestsService.getAuthRequest(
                    requestId = authRequest.id,
                )
            } returns authRequestResponseJson.asSuccess()
            coEvery {
                newAuthRequestService.getAuthRequestUpdate(
                    requestId = authRequest.id,
                    accessCode = authRequestResponse.accessCode,
                    isSso = true,
                )
            } returnsMany listOf(
                authRequestResponseJson.asSuccess(),
                updatedAuthRequestResponseJson.asSuccess(),
            )

            repository
                .createAuthRequestWithUpdates(
                    email = email,
                    authRequestType = AuthRequestType.SSO_ADMIN_APPROVAL,
                )
                .test {
                    assertEquals(CreateAuthRequestResult.Update(authRequest), awaitItem())
                    assertEquals(CreateAuthRequestResult.Update(authRequest), awaitItem())
                    assertEquals(
                        CreateAuthRequestResult.Success(
                            authRequest = authRequest.copy(requestApproved = true),
                            privateKey = authRequestResponse.privateKey,
                            accessCode = authRequestResponse.accessCode,
                        ),
                        awaitItem(),
                    )
                    awaitComplete()
                }
            coVerify(exactly = 0) { authSdkSource.getNewAuthRequest(any()) }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createAuthRequestWithUpdates with createNewAuthRequest Success and getAuthRequestUpdate with response date and no approval should emit Declined`() =
        runTest {
            val email = "email@email.com"
            val authRequestResponse = AUTH_REQUEST_RESPONSE
            val authRequestResponseJson = AuthRequestsResponseJson.AuthRequest(
                id = "1",
                publicKey = PUBLIC_KEY,
                platform = "Android",
                ipAddress = "192.168.0.1",
                key = "public",
                masterPasswordHash = "verySecureHash",
                creationDate = ZonedDateTime.parse("2024-09-13T00:00Z"),
                responseDate = null,
                requestApproved = false,
                originUrl = "www.bitwarden.com",
            )
            val updatedAuthRequestResponseJson = authRequestResponseJson.copy(
                responseDate = ZonedDateTime.parse("2024-09-13T00:00Z"),
            )
            val authRequest = AuthRequest(
                id = authRequestResponseJson.id,
                publicKey = authRequestResponseJson.publicKey,
                platform = authRequestResponseJson.platform,
                ipAddress = authRequestResponseJson.ipAddress,
                key = authRequestResponseJson.key,
                masterPasswordHash = authRequestResponseJson.masterPasswordHash,
                creationDate = authRequestResponseJson.creationDate,
                responseDate = authRequestResponseJson.responseDate,
                requestApproved = authRequestResponseJson.requestApproved ?: false,
                originUrl = authRequestResponseJson.originUrl,
                fingerprint = authRequestResponse.fingerprint,
            )
            coEvery {
                authSdkSource.getNewAuthRequest(email = email)
            } returns authRequestResponse.asSuccess()
            coEvery {
                newAuthRequestService.createAuthRequest(
                    email = email,
                    publicKey = authRequestResponse.publicKey,
                    deviceId = fakeAuthDiskSource.uniqueAppId,
                    accessCode = authRequestResponse.accessCode,
                    fingerprint = authRequestResponse.fingerprint,
                    authRequestType = AuthRequestTypeJson.LOGIN_WITH_DEVICE,
                )
            } returns authRequestResponseJson.asSuccess()
            coEvery {
                newAuthRequestService.getAuthRequestUpdate(
                    requestId = authRequest.id,
                    accessCode = authRequestResponse.accessCode,
                    isSso = false,
                )
            } returns updatedAuthRequestResponseJson.asSuccess()

            repository
                .createAuthRequestWithUpdates(
                    email = email,
                    authRequestType = AuthRequestType.OTHER_DEVICE,
                )
                .test {
                    assertEquals(CreateAuthRequestResult.Update(authRequest), awaitItem())
                    assertEquals(CreateAuthRequestResult.Declined, awaitItem())
                    awaitComplete()
                }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createAuthRequestWithUpdates with createNewAuthRequest Success and getAuthRequestUpdate with old creation date should emit Expired`() =
        runTest {
            val email = "email@email.com"
            fakeAuthDiskSource.userState = SINGLE_USER_STATE
            val authRequestResponse = AUTH_REQUEST_RESPONSE
            val authRequestResponseJson = AuthRequestsResponseJson.AuthRequest(
                id = "1",
                publicKey = PUBLIC_KEY,
                platform = "Android",
                ipAddress = "192.168.0.1",
                key = "public",
                masterPasswordHash = "verySecureHash",
                creationDate = ZonedDateTime.parse("2024-09-13T00:00Z"),
                responseDate = null,
                requestApproved = false,
                originUrl = "www.bitwarden.com",
            )
            val updatedAuthRequestResponseJson = authRequestResponseJson.copy(
                creationDate = ZonedDateTime.parse("2023-09-13T00:00Z"),
            )
            val authRequest = AuthRequest(
                id = authRequestResponseJson.id,
                publicKey = authRequestResponseJson.publicKey,
                platform = authRequestResponseJson.platform,
                ipAddress = authRequestResponseJson.ipAddress,
                key = authRequestResponseJson.key,
                masterPasswordHash = authRequestResponseJson.masterPasswordHash,
                creationDate = authRequestResponseJson.creationDate,
                responseDate = authRequestResponseJson.responseDate,
                requestApproved = authRequestResponseJson.requestApproved ?: false,
                originUrl = authRequestResponseJson.originUrl,
                fingerprint = authRequestResponse.fingerprint,
            )
            coEvery {
                authSdkSource.getNewAuthRequest(email = email)
            } returns authRequestResponse.asSuccess()
            coEvery {
                newAuthRequestService.createAuthRequest(
                    email = email,
                    publicKey = authRequestResponse.publicKey,
                    deviceId = fakeAuthDiskSource.uniqueAppId,
                    accessCode = authRequestResponse.accessCode,
                    fingerprint = authRequestResponse.fingerprint,
                    authRequestType = AuthRequestTypeJson.ADMIN_APPROVAL,
                )
            } returns authRequestResponseJson.asSuccess()
            coEvery {
                newAuthRequestService.getAuthRequestUpdate(
                    requestId = authRequest.id,
                    accessCode = authRequestResponse.accessCode,
                    isSso = true,
                )
            } returns updatedAuthRequestResponseJson.asSuccess()

            repository
                .createAuthRequestWithUpdates(
                    email = email,
                    authRequestType = AuthRequestType.SSO_ADMIN_APPROVAL,
                )
                .test {
                    assertEquals(CreateAuthRequestResult.Update(authRequest), awaitItem())
                    fakeAuthDiskSource.assertPendingAuthRequest(
                        userId = USER_ID,
                        pendingAuthRequest = PendingAuthRequestJson(
                            requestId = authRequestResponseJson.id,
                            requestPrivateKey = authRequestResponse.privateKey,
                            requestAccessCode = authRequestResponse.accessCode,
                            requestFingerprint = authRequestResponse.fingerprint,
                        ),
                    )
                    assertEquals(CreateAuthRequestResult.Expired, awaitItem())
                    fakeAuthDiskSource.assertPendingAuthRequest(
                        userId = USER_ID,
                        pendingAuthRequest = null,
                    )
                    awaitComplete()
                }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createAuthRequestWithUpdates with authSdkSource getNewAuthRequest error should emit Error`() =
        runTest {
            val email = "email@email.com"
            coEvery {
                authSdkSource.getNewAuthRequest(email = email)
            } returns Throwable("Fail").asFailure()

            repository
                .createAuthRequestWithUpdates(
                    email = email,
                    authRequestType = AuthRequestType.OTHER_DEVICE,
                )
                .test {
                    assertEquals(CreateAuthRequestResult.Error, awaitItem())
                    awaitComplete()
                }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getAuthRequestByFingerprintFlow should emit failure and cancel flow when getAuthRequests fails`() =
        runTest {
            val fingerprint = "fingerprint"
            coEvery { authRequestsService.getAuthRequests() } returns Throwable("Fail").asFailure()

            repository
                .getAuthRequestByFingerprintFlow(fingerprint)
                .test {
                    assertEquals(AuthRequestUpdatesResult.Error, awaitItem())
                    awaitComplete()
                }

            coVerify(exactly = 1) {
                authRequestsService.getAuthRequests()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getAuthRequestByFingerprintFlow should emit update then not cancel on failure when initial request succeeds and second fails`() =
        runTest {
            val authRequestsResponseJson = AuthRequestsResponseJson(
                authRequests = listOf(AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE),
            )
            val authRequest = AUTH_REQUEST
            val expectedOne = AuthRequestUpdatesResult.Update(authRequest = authRequest)
            val expectedTwo = AuthRequestUpdatesResult.Error
            coEvery {
                authSdkSource.getUserFingerprint(email = EMAIL, publicKey = PUBLIC_KEY)
            } returns FINGER_PRINT.asSuccess()
            coEvery {
                authRequestsService.getAuthRequests()
            } returns authRequestsResponseJson.asSuccess()
            coEvery {
                authRequestsService.getAuthRequest(requestId = REQUEST_ID)
            } returns Throwable("Fail").asFailure()
            fakeAuthDiskSource.userState = SINGLE_USER_STATE

            repository
                .getAuthRequestByFingerprintFlow(FINGER_PRINT)
                .test {
                    assertEquals(expectedOne, awaitItem())
                    assertEquals(expectedTwo, awaitItem())
                    cancelAndConsumeRemainingEvents()
                }

            coVerify(exactly = 1) {
                authRequestsService.getAuthRequests()
                authSdkSource.getUserFingerprint(EMAIL, PUBLIC_KEY)
                authRequestsService.getAuthRequest(requestId = REQUEST_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getAuthRequestByFingerprintFlow should emit update then approved and cancel when initial request succeeds and second succeeds with requestApproved`() =
        runTest {
            val responseJsonOne = AuthRequestsResponseJson(
                authRequests = listOf(AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE),
            )
            val authRequestsResponse = AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE.copy(
                requestApproved = true,
            )
            val expectedOne = AuthRequestUpdatesResult.Update(authRequest = AUTH_REQUEST)
            val expectedTwo = AuthRequestUpdatesResult.Approved
            coEvery {
                authSdkSource.getUserFingerprint(email = EMAIL, publicKey = PUBLIC_KEY)
            } returns FINGER_PRINT.asSuccess()
            coEvery { authRequestsService.getAuthRequests() } returns responseJsonOne.asSuccess()
            coEvery {
                authRequestsService.getAuthRequest(requestId = REQUEST_ID)
            } returns authRequestsResponse.asSuccess()
            fakeAuthDiskSource.userState = SINGLE_USER_STATE

            repository
                .getAuthRequestByFingerprintFlow(FINGER_PRINT)
                .test {
                    assertEquals(expectedOne, awaitItem())
                    assertEquals(expectedTwo, awaitItem())
                    awaitComplete()
                }

            coVerify(exactly = 1) {
                authRequestsService.getAuthRequests()
                authSdkSource.getUserFingerprint(EMAIL, PUBLIC_KEY)
                authRequestsService.getAuthRequest(requestId = REQUEST_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getAuthRequestByFingerprintFlow should emit update then declined and cancel when initial request succeeds and second succeeds with valid response data`() =
        runTest {
            val responseJsonOne = AuthRequestsResponseJson(
                authRequests = listOf(AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE),
            )
            val authRequestsResponse = AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE.copy(
                responseDate = mockk(),
                requestApproved = false,
            )
            val expectedOne = AuthRequestUpdatesResult.Update(authRequest = AUTH_REQUEST)
            val expectedTwo = AuthRequestUpdatesResult.Declined
            coEvery {
                authSdkSource.getUserFingerprint(email = EMAIL, publicKey = PUBLIC_KEY)
            } returns FINGER_PRINT.asSuccess()
            coEvery { authRequestsService.getAuthRequests() } returns responseJsonOne.asSuccess()
            coEvery {
                authRequestsService.getAuthRequest(requestId = REQUEST_ID)
            } returns authRequestsResponse.asSuccess()
            fakeAuthDiskSource.userState = SINGLE_USER_STATE

            repository
                .getAuthRequestByFingerprintFlow(FINGER_PRINT)
                .test {
                    assertEquals(expectedOne, awaitItem())
                    assertEquals(expectedTwo, awaitItem())
                    awaitComplete()
                }

            coVerify(exactly = 1) {
                authRequestsService.getAuthRequests()
                authSdkSource.getUserFingerprint(EMAIL, PUBLIC_KEY)
                authRequestsService.getAuthRequest(requestId = REQUEST_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getAuthRequestByFingerprintFlow should emit update then expired and cancel when initial request succeeds and second succeeds after 15 mins have passed`() =
        runTest {
            val responseJsonOne = AuthRequestsResponseJson(
                authRequests = listOf(AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE),
            )
            val fixedClock: Clock = Clock.fixed(
                Instant.parse("2022-11-12T00:00:00Z"),
                ZoneOffset.UTC,
            )
            val authRequestsResponse = AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE.copy(
                creationDate = ZonedDateTime.ofInstant(fixedClock.instant(), ZoneOffset.UTC),
                requestApproved = false,
            )
            val expectedOne = AuthRequestUpdatesResult.Update(authRequest = AUTH_REQUEST)
            val expectedTwo = AuthRequestUpdatesResult.Expired
            coEvery {
                authSdkSource.getUserFingerprint(email = EMAIL, publicKey = PUBLIC_KEY)
            } returns FINGER_PRINT.asSuccess()
            coEvery {
                authRequestsService.getAuthRequests()
            } returns responseJsonOne.asSuccess()
            coEvery {
                authRequestsService.getAuthRequest(requestId = REQUEST_ID)
            } returns authRequestsResponse.asSuccess()
            fakeAuthDiskSource.userState = SINGLE_USER_STATE

            repository
                .getAuthRequestByFingerprintFlow(FINGER_PRINT)
                .test {
                    assertEquals(expectedOne, awaitItem())
                    assertEquals(expectedTwo, awaitItem())
                    awaitComplete()
                }

            coVerify(exactly = 1) {
                authRequestsService.getAuthRequests()
                authSdkSource.getUserFingerprint(EMAIL, PUBLIC_KEY)
                authRequestsService.getAuthRequest(requestId = REQUEST_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getAuthRequestByFingerprintFlow should emit update then update and not cancel when initial request succeeds and second succeeds before 15 mins passes`() =
        runTest {
            val responseJsonOne = AuthRequestsResponseJson(
                authRequests = listOf(AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE),
            )
            val newHash = "evenMoreSecureHash"
            val authRequestsResponse = AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE.copy(
                masterPasswordHash = newHash,
                requestApproved = false,
            )
            val authRequest = AUTH_REQUEST.copy(
                masterPasswordHash = newHash,
                requestApproved = false,
            )
            val expectedOne = AuthRequestUpdatesResult.Update(authRequest = AUTH_REQUEST)
            val expectedTwo = AuthRequestUpdatesResult.Update(authRequest = authRequest)
            coEvery {
                authSdkSource.getUserFingerprint(email = EMAIL, publicKey = PUBLIC_KEY)
            } returns FINGER_PRINT.asSuccess()
            coEvery { authRequestsService.getAuthRequests() } returns responseJsonOne.asSuccess()
            coEvery {
                authRequestsService.getAuthRequest(requestId = REQUEST_ID)
            } returns authRequestsResponse.asSuccess()
            fakeAuthDiskSource.userState = SINGLE_USER_STATE

            repository
                .getAuthRequestByFingerprintFlow(FINGER_PRINT)
                .test {
                    assertEquals(expectedOne, awaitItem())
                    assertEquals(expectedTwo, awaitItem())
                    cancelAndConsumeRemainingEvents()
                }

            coVerify(exactly = 1) {
                authRequestsService.getAuthRequests()
                authSdkSource.getUserFingerprint(EMAIL, PUBLIC_KEY)
                authRequestsService.getAuthRequest(requestId = REQUEST_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getAuthRequestByIdFlow should emit failure and cancel flow when getAuthRequests fails`() =
        runTest {
            coEvery {
                authRequestsService.getAuthRequest(REQUEST_ID)
            } returns Throwable("Fail").asFailure()

            repository
                .getAuthRequestByIdFlow(REQUEST_ID)
                .test {
                    assertEquals(AuthRequestUpdatesResult.Error, awaitItem())
                    awaitComplete()
                }

            coVerify(exactly = 1) {
                authRequestsService.getAuthRequest(REQUEST_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getAuthRequestByIdFlow should emit update then not cancel on failure when initial request succeeds and second fails`() =
        runTest {
            val authRequestResponseOne = AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE.asSuccess()
            val authRequestResponseTwo = Throwable("Fail").asFailure()
            val authRequest = AUTH_REQUEST.copy(id = REQUEST_ID)
            val expectedOne = AuthRequestUpdatesResult.Update(authRequest = authRequest)
            val expectedTwo = AuthRequestUpdatesResult.Error
            coEvery {
                authSdkSource.getUserFingerprint(email = EMAIL, publicKey = PUBLIC_KEY)
            } returns FINGER_PRINT.asSuccess()
            coEvery {
                authRequestsService.getAuthRequest(requestId = REQUEST_ID)
            } returns authRequestResponseOne andThen authRequestResponseTwo
            fakeAuthDiskSource.userState = SINGLE_USER_STATE

            repository
                .getAuthRequestByIdFlow(REQUEST_ID)
                .test {
                    assertEquals(expectedOne, awaitItem())
                    assertEquals(expectedTwo, awaitItem())
                    cancelAndConsumeRemainingEvents()
                }

            coVerify(exactly = 1) {
                authSdkSource.getUserFingerprint(EMAIL, PUBLIC_KEY)
            }
            coVerify(exactly = 2) {
                authRequestsService.getAuthRequest(REQUEST_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getAuthRequestByIdFlow should emit update then approved and cancel when initial request succeeds and second succeeds with requestApproved`() =
        runTest {
            val authRequestResponseOne = AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE.asSuccess()
            val authRequestResponseJson = AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE.copy(
                requestApproved = true,
            )
            val authRequestResponseTwo = authRequestResponseJson.asSuccess()
            val expectedOne = AuthRequestUpdatesResult.Update(authRequest = AUTH_REQUEST)
            val expectedTwo = AuthRequestUpdatesResult.Approved
            coEvery {
                authSdkSource.getUserFingerprint(email = EMAIL, publicKey = PUBLIC_KEY)
            } returns FINGER_PRINT.asSuccess()
            coEvery {
                authRequestsService.getAuthRequest(requestId = REQUEST_ID)
            } returns authRequestResponseOne andThen authRequestResponseTwo
            fakeAuthDiskSource.userState = SINGLE_USER_STATE

            repository
                .getAuthRequestByIdFlow(REQUEST_ID)
                .test {
                    assertEquals(expectedOne, awaitItem())
                    assertEquals(expectedTwo, awaitItem())
                    awaitComplete()
                }

            coVerify(exactly = 1) {
                authSdkSource.getUserFingerprint(EMAIL, PUBLIC_KEY)
            }
            coVerify(exactly = 2) {
                authRequestsService.getAuthRequest(REQUEST_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getAuthRequestByIdFlow should emit update then declined and cancel when initial request succeeds and second succeeds with valid response data`() =
        runTest {
            val authRequestResponseOne = AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE.asSuccess()
            val authRequestResponseJson = AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE.copy(
                responseDate = mockk(),
                requestApproved = false,
            )
            val authRequestResponseTwo = authRequestResponseJson.asSuccess()
            val expectedOne = AuthRequestUpdatesResult.Update(authRequest = AUTH_REQUEST)
            val expectedTwo = AuthRequestUpdatesResult.Declined
            coEvery {
                authSdkSource.getUserFingerprint(email = EMAIL, publicKey = PUBLIC_KEY)
            } returns FINGER_PRINT.asSuccess()
            coEvery {
                authRequestsService.getAuthRequest(requestId = REQUEST_ID)
            } returns authRequestResponseOne andThen authRequestResponseTwo
            fakeAuthDiskSource.userState = SINGLE_USER_STATE

            repository
                .getAuthRequestByIdFlow(REQUEST_ID)
                .test {
                    assertEquals(expectedOne, awaitItem())
                    assertEquals(expectedTwo, awaitItem())
                    awaitComplete()
                }

            coVerify(exactly = 1) {
                authSdkSource.getUserFingerprint(EMAIL, PUBLIC_KEY)
            }
            coVerify(exactly = 2) {
                authRequestsService.getAuthRequest(REQUEST_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getAuthRequestByIdFlow should emit update then expired and cancel when initial request succeeds and second succeeds after 15 mins have passed`() =
        runTest {
            val fixedClock: Clock = Clock.fixed(
                Instant.parse("2022-11-12T00:00:00Z"),
                ZoneOffset.UTC,
            )
            val authRequestResponseOne = AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE.asSuccess()
            val authRequestResponseJson = AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE.copy(
                creationDate = ZonedDateTime.ofInstant(fixedClock.instant(), ZoneOffset.UTC),
                requestApproved = false,
            )
            val authRequestResponseTwo = authRequestResponseJson.asSuccess()
            val expectedOne = AuthRequestUpdatesResult.Update(authRequest = AUTH_REQUEST)
            val expectedTwo = AuthRequestUpdatesResult.Expired
            coEvery {
                authSdkSource.getUserFingerprint(email = EMAIL, publicKey = PUBLIC_KEY)
            } returns FINGER_PRINT.asSuccess()
            coEvery {
                authRequestsService.getAuthRequest(requestId = REQUEST_ID)
            } returns authRequestResponseOne andThen authRequestResponseTwo
            fakeAuthDiskSource.userState = SINGLE_USER_STATE

            repository
                .getAuthRequestByIdFlow(REQUEST_ID)
                .test {
                    assertEquals(expectedOne, awaitItem())
                    assertEquals(expectedTwo, awaitItem())
                    awaitComplete()
                }

            coVerify(exactly = 1) {
                authSdkSource.getUserFingerprint(EMAIL, PUBLIC_KEY)
            }
            coVerify(exactly = 2) {
                authRequestsService.getAuthRequest(REQUEST_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getAuthRequestByIdFlow should emit update then update and not cancel when initial request succeeds and second succeeds before 15 mins passes`() =
        runTest {
            val newHash = "evenMoreSecureHash"
            val authRequestResponseOne = AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE.asSuccess()
            val authRequestResponseJson = AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE.copy(
                masterPasswordHash = newHash,
                requestApproved = false,
            )
            val authRequestResponseTwo = authRequestResponseJson.asSuccess()
            val authRequest = AUTH_REQUEST.copy(
                masterPasswordHash = newHash,
                requestApproved = false,
            )
            val expectedOne = AuthRequestUpdatesResult.Update(authRequest = AUTH_REQUEST)
            val expectedTwo = AuthRequestUpdatesResult.Update(authRequest = authRequest)
            coEvery {
                authSdkSource.getUserFingerprint(email = EMAIL, publicKey = PUBLIC_KEY)
            } returns FINGER_PRINT.asSuccess()
            coEvery {
                authRequestsService.getAuthRequest(requestId = REQUEST_ID)
            } returns authRequestResponseOne andThen authRequestResponseTwo
            fakeAuthDiskSource.userState = SINGLE_USER_STATE

            repository
                .getAuthRequestByIdFlow(REQUEST_ID)
                .test {
                    assertEquals(expectedOne, awaitItem())
                    assertEquals(expectedTwo, awaitItem())
                    cancelAndConsumeRemainingEvents()
                }

            coVerify(exactly = 1) {
                authSdkSource.getUserFingerprint(EMAIL, PUBLIC_KEY)
            }
            coVerify(exactly = 2) {
                authRequestsService.getAuthRequest(REQUEST_ID)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Suppress("MaxLineLength")
    @Test
    fun `getAuthRequestsWithUpdates should emit error then success and not cancel flow when getAuthRequests fails then succeeds`() =
        runTest {
            val threeMinutes = 3L * 60L * 1_000L
            val authRequests = listOf(AUTH_REQUEST)
            val authRequestsResponseJson = AuthRequestsResponseJson(
                authRequests = listOf(AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE),
            )
            val expectedOne = AuthRequestsUpdatesResult.Error
            val expectedTwo = AuthRequestsUpdatesResult.Update(authRequests = authRequests)
            coEvery {
                authRequestsService.getAuthRequests()
            } returns Throwable("Fail").asFailure() andThen authRequestsResponseJson.asSuccess()
            coEvery {
                authSdkSource.getUserFingerprint(email = EMAIL, publicKey = PUBLIC_KEY)
            } returns FINGER_PRINT.asSuccess()
            fakeAuthDiskSource.userState = SINGLE_USER_STATE

            repository
                .getAuthRequestsWithUpdates()
                .test {
                    assertEquals(expectedOne, awaitItem())
                    advanceTimeBy(threeMinutes)
                    expectNoEvents()
                    advanceTimeBy(threeMinutes)
                    assertEquals(expectedTwo, awaitItem())
                    advanceTimeBy(threeMinutes)
                    cancelAndIgnoreRemainingEvents()
                }

            coVerify(exactly = 2) {
                authRequestsService.getAuthRequests()
            }
        }

    @Test
    fun `getAuthRequestIfApproved should return failure when service returns failure`() = runTest {
        val requestId = "requestId"
        coEvery {
            authRequestsService.getAuthRequest(requestId)
        } returns Throwable("Fail").asFailure()

        val result = repository.getAuthRequestIfApproved(requestId)

        coVerify(exactly = 1) {
            authRequestsService.getAuthRequest(requestId)
        }
        assertTrue(result.isFailure)
    }

    @Test
    fun `getAuthRequestIfApproved should return failure when request is not approved`() = runTest {
        val requestId = "requestId"
        val response = AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE.copy(requestApproved = false)
        coEvery { authRequestsService.getAuthRequest(requestId) } returns response.asSuccess()

        val result = repository.getAuthRequestIfApproved(requestId)

        coVerify(exactly = 1) {
            authRequestsService.getAuthRequest(requestId)
        }
        assertTrue(result.isFailure)
    }

    @Test
    fun `getAuthRequestIfApproved should return success when request is approved`() = runTest {
        val requestId = "requestId"
        fakeAuthDiskSource.userState = SINGLE_USER_STATE
        coEvery {
            authRequestsService.getAuthRequest(requestId)
        } returns AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE.asSuccess()
        fakeAuthDiskSource.userState = SINGLE_USER_STATE
        coEvery {
            authSdkSource.getUserFingerprint(
                email = EMAIL,
                publicKey = AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE.publicKey,
            )
        } returns FINGER_PRINT.asSuccess()

        val result = repository.getAuthRequestIfApproved(requestId)

        coVerify(exactly = 1) {
            authRequestsService.getAuthRequest(requestId)
            authSdkSource.getUserFingerprint(
                email = EMAIL,
                publicKey = AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE.publicKey,
            )
        }
        assertEquals(AUTH_REQUEST.asSuccess(), result)
    }

    @Test
    fun `getAuthRequests should return failure when service returns failure`() = runTest {
        coEvery { authRequestsService.getAuthRequests() } returns Throwable("Fail").asFailure()

        val result = repository.getAuthRequests()

        coVerify(exactly = 1) {
            authRequestsService.getAuthRequests()
        }
        assertEquals(AuthRequestsResult.Error, result)
    }

    @Test
    fun `getAuthRequests should return success when service returns success`() = runTest {
        val fingerprint = "fingerprint"
        val responseJson = AuthRequestsResponseJson(
            authRequests = listOf(
                AuthRequestsResponseJson.AuthRequest(
                    id = "1",
                    publicKey = PUBLIC_KEY,
                    platform = "Android",
                    ipAddress = "192.168.0.1",
                    key = "public",
                    masterPasswordHash = "verySecureHash",
                    creationDate = ZonedDateTime.parse("2024-09-13T00:00Z"),
                    responseDate = null,
                    requestApproved = true,
                    originUrl = "www.bitwarden.com",
                ),
            ),
        )
        val expected = AuthRequestsResult.Success(
            authRequests = listOf(
                AuthRequest(
                    id = "1",
                    publicKey = PUBLIC_KEY,
                    platform = "Android",
                    ipAddress = "192.168.0.1",
                    key = "public",
                    masterPasswordHash = "verySecureHash",
                    creationDate = ZonedDateTime.parse("2024-09-13T00:00Z"),
                    responseDate = null,
                    requestApproved = true,
                    originUrl = "www.bitwarden.com",
                    fingerprint = fingerprint,
                ),
            ),
        )
        coEvery {
            authSdkSource.getUserFingerprint(email = EMAIL, publicKey = PUBLIC_KEY)
        } returns fingerprint.asSuccess()
        coEvery { authRequestsService.getAuthRequests() } returns responseJson.asSuccess()
        fakeAuthDiskSource.userState = SINGLE_USER_STATE

        val result = repository.getAuthRequests()

        coVerify(exactly = 1) {
            authRequestsService.getAuthRequests()
            authSdkSource.getUserFingerprint(EMAIL, PUBLIC_KEY)
        }
        assertEquals(expected, result)
    }

    @Test
    fun `getAuthRequests should return empty list when user profile is null`() = runTest {
        val responseJson = AuthRequestsResponseJson(
            authRequests = listOf(
                AuthRequestsResponseJson.AuthRequest(
                    id = "1",
                    publicKey = PUBLIC_KEY,
                    platform = "Android",
                    ipAddress = "192.168.0.1",
                    key = "public",
                    masterPasswordHash = "verySecureHash",
                    creationDate = ZonedDateTime.parse("2024-09-13T00:00Z"),
                    responseDate = null,
                    requestApproved = true,
                    originUrl = "www.bitwarden.com",
                ),
            ),
        )
        val expected = AuthRequestsResult.Success(emptyList())
        coEvery { authRequestsService.getAuthRequests() } returns responseJson.asSuccess()

        val result = repository.getAuthRequests()

        coVerify(exactly = 1) {
            authRequestsService.getAuthRequests()
        }
        assertEquals(expected, result)
    }

    @Test
    fun `updateAuthRequest should return failure when sdk returns failure`() = runTest {
        coEvery {
            vaultSdkSource.getAuthRequestKey(publicKey = PUBLIC_KEY, userId = USER_ID)
        } returns Throwable("Fail").asFailure()
        fakeAuthDiskSource.userState = SINGLE_USER_STATE

        val result = repository.updateAuthRequest(
            requestId = "requestId",
            masterPasswordHash = "masterPasswordHash",
            publicKey = PUBLIC_KEY,
            isApproved = false,
        )

        coVerify(exactly = 1) {
            vaultSdkSource.getAuthRequestKey(publicKey = PUBLIC_KEY, userId = USER_ID)
        }
        assertEquals(AuthRequestResult.Error, result)
    }

    @Test
    fun `updateAuthRequest should return failure when service returns failure`() = runTest {
        val requestId = "requestId"
        val passwordHash = "masterPasswordHash"
        val encodedKey = "encodedKey"
        coEvery {
            vaultSdkSource.getAuthRequestKey(publicKey = PUBLIC_KEY, userId = USER_ID)
        } returns encodedKey.asSuccess()
        coEvery {
            authRequestsService.updateAuthRequest(
                requestId = requestId,
                masterPasswordHash = null,
                key = encodedKey,
                deviceId = UNIQUE_APP_ID,
                isApproved = false,
            )
        } returns Throwable("Mission failed").asFailure()
        fakeAuthDiskSource.userState = SINGLE_USER_STATE

        val result = repository.updateAuthRequest(
            requestId = requestId,
            masterPasswordHash = passwordHash,
            publicKey = PUBLIC_KEY,
            isApproved = false,
        )

        coVerify(exactly = 1) {
            vaultSdkSource.getAuthRequestKey(publicKey = PUBLIC_KEY, userId = USER_ID)
        }
        assertEquals(AuthRequestResult.Error, result)
    }

    @Test
    fun `updateAuthRequest should return success when service & sdk return success`() = runTest {
        val requestId = "requestId"
        val passwordHash = "masterPasswordHash"
        val encodedKey = "encodedKey"
        val responseJson = AuthRequestsResponseJson.AuthRequest(
            id = requestId,
            publicKey = PUBLIC_KEY,
            platform = "Android",
            ipAddress = "192.168.0.1",
            key = "key",
            masterPasswordHash = passwordHash,
            creationDate = ZonedDateTime.parse("2024-09-13T00:00Z"),
            responseDate = null,
            requestApproved = true,
            originUrl = "www.bitwarden.com",
        )
        val expected = AuthRequestResult.Success(
            authRequest = AuthRequest(
                id = requestId,
                publicKey = PUBLIC_KEY,
                platform = "Android",
                ipAddress = "192.168.0.1",
                key = "key",
                masterPasswordHash = passwordHash,
                creationDate = ZonedDateTime.parse("2024-09-13T00:00Z"),
                responseDate = null,
                requestApproved = true,
                originUrl = "www.bitwarden.com",
                fingerprint = "",
            ),
        )
        coEvery {
            vaultSdkSource.getAuthRequestKey(publicKey = PUBLIC_KEY, userId = USER_ID)
        } returns encodedKey.asSuccess()
        coEvery {
            authRequestsService.updateAuthRequest(
                requestId = requestId,
                masterPasswordHash = null,
                key = encodedKey,
                deviceId = UNIQUE_APP_ID,
                isApproved = false,
            )
        } returns responseJson.asSuccess()
        fakeAuthDiskSource.userState = SINGLE_USER_STATE

        val result = repository.updateAuthRequest(
            requestId = requestId,
            masterPasswordHash = passwordHash,
            publicKey = PUBLIC_KEY,
            isApproved = false,
        )

        coVerify(exactly = 1) {
            vaultSdkSource.getAuthRequestKey(publicKey = PUBLIC_KEY, userId = USER_ID)
            authRequestsService.updateAuthRequest(
                requestId = requestId,
                masterPasswordHash = null,
                key = encodedKey,
                deviceId = UNIQUE_APP_ID,
                isApproved = false,
            )
        }
        assertEquals(expected, result)
    }
}

private const val EMAIL: String = "test@bitwarden.com"
private const val UNIQUE_APP_ID: String = "testUniqueAppId"
private const val PRIVATE_KEY: String = "privateKey"
private const val PUBLIC_KEY: String = "PublicKey"
private const val USER_ID: String = "2a135b23-e1fb-42c9-bec3-573857bc8181"
private const val ACCESS_TOKEN: String = "accessToken"
private const val REFRESH_TOKEN: String = "refreshToken"
private const val REQUEST_ID: String = "REQUEST_ID"
private const val FINGER_PRINT: String = "FINGER_PRINT"

private val ACCOUNT: AccountJson = AccountJson(
    profile = AccountJson.Profile(
        userId = USER_ID,
        email = EMAIL,
        isEmailVerified = true,
        name = "Bitwarden Tester",
        hasPremium = false,
        stamp = null,
        organizationId = null,
        avatarColorHex = null,
        forcePasswordResetReason = null,
        kdfType = KdfTypeJson.ARGON2_ID,
        kdfIterations = 600000,
        kdfMemory = 16,
        kdfParallelism = 4,
        userDecryptionOptions = null,
        isTwoFactorEnabled = false,
        creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
    ),
    tokens = AccountTokensJson(
        accessToken = ACCESS_TOKEN,
        refreshToken = REFRESH_TOKEN,
    ),
    settings = AccountJson.Settings(
        environmentUrlData = null,
    ),
)

private val SINGLE_USER_STATE: UserStateJson = UserStateJson(
    activeUserId = USER_ID,
    accounts = mapOf(
        USER_ID to ACCOUNT,
    ),
)

private val AUTH_REQUESTS_RESPONSE_JSON_AUTH_RESPONSE: AuthRequestsResponseJson.AuthRequest =
    AuthRequestsResponseJson.AuthRequest(
        id = REQUEST_ID,
        publicKey = PUBLIC_KEY,
        platform = "Android",
        ipAddress = "192.168.0.1",
        key = "public",
        masterPasswordHash = "verySecureHash",
        creationDate = ZonedDateTime.parse("2024-09-13T00:00Z"),
        responseDate = null,
        requestApproved = true,
        originUrl = "www.bitwarden.com",
    )

private val AUTH_REQUEST: AuthRequest = AuthRequest(
    id = REQUEST_ID,
    publicKey = PUBLIC_KEY,
    platform = "Android",
    ipAddress = "192.168.0.1",
    key = "public",
    masterPasswordHash = "verySecureHash",
    creationDate = ZonedDateTime.parse("2024-09-13T00:00Z"),
    responseDate = null,
    requestApproved = true,
    originUrl = "www.bitwarden.com",
    fingerprint = FINGER_PRINT,
)

private val AUTH_REQUEST_RESPONSE: AuthRequestResponse = AuthRequestResponse(
    privateKey = PRIVATE_KEY,
    publicKey = PUBLIC_KEY,
    accessCode = "accessCode",
    fingerprint = "fingerprint",
)
