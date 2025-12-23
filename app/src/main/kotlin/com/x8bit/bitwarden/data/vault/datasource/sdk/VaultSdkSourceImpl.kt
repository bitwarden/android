package com.x8bit.bitwarden.data.vault.datasource.sdk

import com.bitwarden.collections.Collection
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.DeriveKeyConnectorException
import com.bitwarden.core.DeriveKeyConnectorRequest
import com.bitwarden.core.EnrollPinResponse
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoRequest
import com.bitwarden.core.UpdateKdfResponse
import com.bitwarden.core.UpdatePasswordResponse
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.crypto.Kdf
import com.bitwarden.crypto.TrustDeviceResponse
import com.bitwarden.exporters.Account
import com.bitwarden.exporters.ExportFormat
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.fido.PublicKeyCredentialAuthenticatorAssertionResponse
import com.bitwarden.fido.PublicKeyCredentialAuthenticatorAttestationResponse
import com.bitwarden.sdk.BitwardenException
import com.bitwarden.sdk.Fido2CredentialStore
import com.bitwarden.sdk.VaultClient
import com.bitwarden.send.Send
import com.bitwarden.send.SendView
import com.bitwarden.vault.Attachment
import com.bitwarden.vault.AttachmentView
import com.bitwarden.vault.Cipher
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.DecryptCipherListResult
import com.bitwarden.vault.EncryptionContext
import com.bitwarden.vault.Folder
import com.bitwarden.vault.FolderView
import com.bitwarden.vault.PasswordHistory
import com.bitwarden.vault.PasswordHistoryView
import com.bitwarden.vault.TotpResponse
import com.x8bit.bitwarden.data.platform.datasource.sdk.BaseSdkSource
import com.x8bit.bitwarden.data.platform.manager.SdkClientManager
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.AuthenticateFido2CredentialRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.DeriveKeyConnectorResult
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.Fido2CredentialAuthenticationUserInterfaceImpl
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.Fido2CredentialRegistrationUserInterfaceImpl
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.Fido2CredentialSearchUserInterfaceImpl
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.RegisterFido2CredentialRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

/**
 * Primary implementation of [VaultSdkSource] that serves as a convenience wrapper around a
 * [VaultClient].
 */
@Suppress("TooManyFunctions")
class VaultSdkSourceImpl(
    sdkClientManager: SdkClientManager,
    private val dispatcherManager: DispatcherManager,
) : BaseSdkSource(sdkClientManager = sdkClientManager),
    VaultSdkSource {

    override fun clearCrypto(userId: String) {
        sdkClientManager.destroyClient(userId = userId)
    }

    override suspend fun getTrustDevice(
        userId: String,
    ): Result<TrustDeviceResponse> = runCatchingWithLogs {
        getClient(userId = userId)
            .auth()
            .trustDevice()
    }

    override suspend fun deriveKeyConnector(
        userId: String,
        userKeyEncrypted: String,
        email: String,
        password: String,
        kdf: Kdf,
    ): Result<DeriveKeyConnectorResult> =
        runCatchingWithLogs {
            try {
                val key = getClient(userId = userId)
                    .crypto()
                    .deriveKeyConnector(
                        request = DeriveKeyConnectorRequest(
                            userKeyEncrypted = userKeyEncrypted,
                            password = password,
                            kdf = kdf,
                            email = email,
                        ),
                    )
                DeriveKeyConnectorResult.Success(key)
            } catch (ex: BitwardenException.DeriveKeyConnector) {
                when (ex.v1) {
                    is DeriveKeyConnectorException.WrongPassword -> {
                        DeriveKeyConnectorResult.WrongPasswordError
                    }
                    is DeriveKeyConnectorException.Crypto -> {
                        DeriveKeyConnectorResult.Error(error = ex)
                    }
                }
            } catch (exception: BitwardenException) {
                DeriveKeyConnectorResult.Error(error = exception)
            }
        }

    override suspend fun enrollPin(
        userId: String,
        pin: String,
    ): Result<EnrollPinResponse> =
        runCatchingWithLogs {
            getClient(userId = userId)
                .crypto()
                .enrollPin(pin = pin)
        }

    override suspend fun enrollPinWithEncryptedPin(
        userId: String,
        encryptedPin: String,
    ): Result<EnrollPinResponse> =
        runCatchingWithLogs {
            getClient(userId = userId)
                .crypto()
                .enrollPinWithEncryptedPin(encryptedPin = encryptedPin)
        }

    override suspend fun validatePin(
        userId: String,
        pin: String,
        pinProtectedUserKey: String,
    ): Result<Boolean> =
        runCatchingWithLogs {
            getClient(userId = userId)
                .auth()
                .validatePin(pin = pin, pinProtectedUserKey = pinProtectedUserKey)
        }

    override suspend fun getAuthRequestKey(
        publicKey: String,
        userId: String,
    ): Result<String> =
        runCatchingWithLogs {
            getClient(userId = userId)
                .auth()
                .approveAuthRequest(publicKey = publicKey)
        }

    override suspend fun getResetPasswordKey(
        orgPublicKey: String,
        userId: String,
    ): Result<String> = runCatchingWithLogs {
        getClient(userId = userId)
            .crypto()
            .enrollAdminPasswordReset(publicKey = orgPublicKey)
    }

    override suspend fun getUserEncryptionKey(
        userId: String,
    ): Result<String> =
        runCatchingWithLogs {
            getClient(userId = userId)
                .crypto()
                .getUserEncryptionKey()
        }

    override suspend fun getUserFingerprint(
        userId: String,
    ): Result<String> =
        runCatchingWithLogs {
            getClient(userId = userId)
                .platform()
                .userFingerprint(fingerprintMaterial = userId)
        }

    override suspend fun initializeCrypto(
        userId: String,
        request: InitUserCryptoRequest,
    ): Result<InitializeCryptoResult> =
        runCatchingWithLogs {
            try {
                getClient(userId = userId)
                    .crypto()
                    .initializeUserCrypto(req = request)
                InitializeCryptoResult.Success
            } catch (exception: BitwardenException) {
                // The only truly expected error from the SDK is an incorrect key/password.
                InitializeCryptoResult.AuthenticationError(
                    message = exception.message,
                    error = exception,
                )
            }
        }

    override suspend fun initializeOrganizationCrypto(
        userId: String,
        request: InitOrgCryptoRequest,
    ): Result<InitializeCryptoResult> =
        runCatchingWithLogs {
            try {
                getClient(userId = userId)
                    .crypto()
                    .initializeOrgCrypto(req = request)
                InitializeCryptoResult.Success
            } catch (exception: BitwardenException) {
                // The only truly expected error from the SDK is for incorrect keys.
                InitializeCryptoResult.AuthenticationError(
                    message = exception.message,
                    error = exception,
                )
            }
        }

    override suspend fun encryptSend(
        userId: String,
        sendView: SendView,
    ): Result<Send> =
        runCatchingWithLogs {
            getClient(userId = userId)
                .sends()
                .encrypt(send = sendView)
        }

    override suspend fun encryptBuffer(
        userId: String,
        send: Send,
        fileBuffer: ByteArray,
    ): Result<ByteArray> =
        runCatchingWithLogs {
            getClient(userId = userId)
                .sends()
                .encryptBuffer(
                    send = send,
                    buffer = fileBuffer,
                )
        }

    override suspend fun encryptFile(
        userId: String,
        send: Send,
        path: String,
        destinationFilePath: String,
    ): Result<File> =
        runCatchingWithLogs {
            getClient(userId = userId)
                .sends()
                .encryptFile(
                    send = send,
                    decryptedFilePath = path,
                    encryptedFilePath = destinationFilePath,
                )
            File(destinationFilePath)
        }

    override suspend fun encryptAttachment(
        userId: String,
        cipher: Cipher,
        attachmentView: AttachmentView,
        decryptedFilePath: String,
        encryptedFilePath: String,
    ): Result<Attachment> =
        runCatchingWithLogs {
            getClient(userId = userId)
                .vault()
                .attachments()
                .encryptFile(
                    cipher = cipher,
                    attachment = attachmentView,
                    decryptedFilePath = decryptedFilePath,
                    encryptedFilePath = encryptedFilePath,
                )
        }

    override suspend fun encryptCipher(
        userId: String,
        cipherView: CipherView,
    ): Result<EncryptionContext> =
        runCatchingWithLogs {
            getClient(userId = userId)
                .vault()
                .ciphers()
                .encrypt(cipherView = cipherView)
        }

    override suspend fun decryptCipher(
        userId: String,
        cipher: Cipher,
    ): Result<CipherView> =
        runCatchingWithLogs {
            getClient(userId = userId)
                .vault()
                .ciphers()
                .decrypt(cipher = cipher)
        }

    override suspend fun decryptCipherListWithFailures(
        userId: String,
        cipherList: List<Cipher>,
    ): Result<DecryptCipherListResult> =
        runCatchingWithLogs {
            getClient(userId = userId)
                .vault()
                .ciphers()
                .decryptListWithFailures(cipherList)
        }

    override suspend fun decryptCollection(
        userId: String,
        collection: Collection,
    ): Result<CollectionView> =
        runCatchingWithLogs {
            getClient(userId = userId)
                .vault()
                .collections()
                .decrypt(collection = collection)
        }

    override suspend fun decryptCollectionList(
        userId: String,
        collectionList: List<Collection>,
    ): Result<List<CollectionView>> =
        runCatchingWithLogs {
            getClient(userId = userId)
                .vault()
                .collections()
                .decryptList(collections = collectionList)
        }

    override suspend fun decryptSend(
        userId: String,
        send: Send,
    ): Result<SendView> =
        runCatchingWithLogs {
            getClient(userId = userId)
                .sends()
                .decrypt(send = send)
        }

    override suspend fun decryptSendList(
        userId: String,
        sendList: List<Send>,
    ): Result<List<SendView>> =
        runCatchingWithLogs {
            val sends = getClient(userId = userId).sends()
            withContext(dispatcherManager.default) {
                sendList.map { async { sends.decrypt(send = it) } }.awaitAll()
            }
        }

    override suspend fun encryptFolder(
        userId: String,
        folder: FolderView,
    ): Result<Folder> =
        runCatchingWithLogs {
            getClient(userId = userId)
                .vault()
                .folders()
                .encrypt(folder = folder)
        }

    override suspend fun decryptFolder(
        userId: String,
        folder: Folder,
    ): Result<FolderView> =
        runCatchingWithLogs {
            getClient(userId = userId)
                .vault()
                .folders()
                .decrypt(folder = folder)
        }

    override suspend fun decryptFolderList(
        userId: String,
        folderList: List<Folder>,
    ): Result<List<FolderView>> =
        runCatchingWithLogs {
            getClient(userId = userId)
                .vault()
                .folders()
                .decryptList(folders = folderList)
        }

    override suspend fun decryptFile(
        userId: String,
        cipher: Cipher,
        attachmentView: AttachmentView,
        encryptedFilePath: String,
        decryptedFilePath: String,
    ): Result<Unit> =
        runCatchingWithLogs {
            getClient(userId = userId)
                .vault()
                .attachments()
                .decryptFile(
                    cipher = cipher,
                    attachment = attachmentView,
                    encryptedFilePath = encryptedFilePath,
                    decryptedFilePath = decryptedFilePath,
                )
        }

    override suspend fun encryptPasswordHistory(
        userId: String,
        passwordHistory: PasswordHistoryView,
    ): Result<PasswordHistory> = runCatchingWithLogs {
        getClient(userId = userId)
            .vault()
            .passwordHistory()
            .encrypt(passwordHistory = passwordHistory)
    }

    override suspend fun decryptPasswordHistoryList(
        userId: String,
        passwordHistoryList: List<PasswordHistory>,
    ): Result<List<PasswordHistoryView>> = runCatchingWithLogs {
        getClient(userId = userId)
            .vault()
            .passwordHistory()
            .decryptList(list = passwordHistoryList)
    }

    override suspend fun generateTotpForCipherListView(
        userId: String,
        cipherListView: CipherListView,
        time: Instant?,
    ): Result<TotpResponse> = runCatchingWithLogs {
        getClient(userId = userId)
            .vault()
            .generateTotpCipherView(
                view = cipherListView,
                time = time,
            )
    }

    override suspend fun moveToOrganization(
        userId: String,
        organizationId: String,
        cipherView: CipherView,
    ): Result<CipherView> = runCatchingWithLogs {
        getClient(userId = userId)
            .vault()
            .ciphers()
            .moveToOrganization(cipher = cipherView, organizationId = organizationId)
    }

    override suspend fun validatePassword(
        userId: String,
        password: String,
        passwordHash: String,
    ): Result<Boolean> = runCatchingWithLogs {
        getClient(userId = userId)
            .auth()
            .validatePassword(
                password = password,
                passwordHash = passwordHash,
            )
    }

    override suspend fun validatePasswordUserKey(
        userId: String,
        password: String,
        encryptedUserKey: String,
    ): Result<String> = runCatchingWithLogs {
        getClient(userId = userId)
            .auth()
            .validatePasswordUserKey(
                password = password,
                encryptedUserKey = encryptedUserKey,
            )
    }

    override suspend fun updatePassword(
        userId: String,
        newPassword: String,
    ): Result<UpdatePasswordResponse> = runCatchingWithLogs {
        getClient(userId = userId)
            .crypto()
            .makeUpdatePassword(newPassword = newPassword)
    }

    override suspend fun exportVaultDataToString(
        userId: String,
        folders: List<Folder>,
        ciphers: List<Cipher>,
        format: ExportFormat,
    ): Result<String> = runCatchingWithLogs {
        getClient(userId = userId)
            .exporters()
            .exportVault(
                folders = folders,
                ciphers = ciphers,
                format = format,
            )
    }

    override suspend fun exportVaultDataToCxf(
        userId: String,
        account: Account,
        ciphers: List<Cipher>,
    ): Result<String> = runCatchingWithLogs {
        getClient(userId = userId)
            .exporters()
            .exportCxf(
                account = account,
                ciphers = ciphers,
            )
    }

    override suspend fun importCxf(
        userId: String,
        payload: String,
    ): Result<List<Cipher>> = runCatchingWithLogs {
        getClient(userId = userId)
            .exporters()
            .importCxf(payload = payload)
    }

    override suspend fun registerFido2Credential(
        request: RegisterFido2CredentialRequest,
        fido2CredentialStore: Fido2CredentialStore,
    ): Result<PublicKeyCredentialAuthenticatorAttestationResponse> = runCatchingWithLogs {
        callbackFlow {
            try {
                val client = getClient(request.userId)
                    .platform()
                    .fido2()
                    .client(
                        userInterface = Fido2CredentialRegistrationUserInterfaceImpl(
                            selectedCipherView = request.selectedCipherView,
                            isVerificationSupported = request.isUserVerificationSupported,
                        ),
                        credentialStore = fido2CredentialStore,
                    )

                val result = client
                    .register(
                        origin = request.origin,
                        request = request.requestJson,
                        clientData = request.clientData,
                    )

                send(result)
                close()
            } catch (e: BitwardenException) {
                close(e)
            }
            awaitClose()
        }
            .first()
    }

    override suspend fun authenticateFido2Credential(
        request: AuthenticateFido2CredentialRequest,
        fido2CredentialStore: Fido2CredentialStore,
    ): Result<PublicKeyCredentialAuthenticatorAssertionResponse> = runCatchingWithLogs {
        callbackFlow {
            try {
                val client = getClient(request.userId)
                    .platform()
                    .fido2()
                    .client(
                        userInterface = Fido2CredentialAuthenticationUserInterfaceImpl(
                            selectedCipherView = request.selectedCipherView,
                            isVerificationSupported = request.isUserVerificationSupported,
                        ),
                        credentialStore = fido2CredentialStore,
                    )

                val result = client.authenticate(
                    origin = request.origin,
                    request = request.requestJson,
                    clientData = request.clientData,
                )

                send(result)
                close()
            } catch (e: BitwardenException) {
                close(e)
            }

            awaitClose()
        }
            .first()
    }

    override suspend fun decryptFido2CredentialAutofillViews(
        userId: String,
        vararg cipherViews: CipherView,
    ): Result<List<Fido2CredentialAutofillView>> = runCatchingWithLogs {
        val fido2 = getClient(userId = userId).platform().fido2()
        cipherViews.flatMap { fido2.decryptFido2AutofillCredentials(cipherView = it) }
    }

    override suspend fun silentlyDiscoverCredentials(
        userId: String,
        fido2CredentialStore: Fido2CredentialStore,
        relyingPartyId: String,
        userHandle: String?,
    ): Result<List<Fido2CredentialAutofillView>> = runCatchingWithLogs {
        getClient(userId)
            .platform()
            .fido2()
            .authenticator(
                userInterface = Fido2CredentialSearchUserInterfaceImpl(),
                credentialStore = fido2CredentialStore,
            )
            .silentlyDiscoverCredentials(relyingPartyId, userHandle?.toByteArray())
    }

    override suspend fun makeUpdateKdf(
        userId: String,
        password: String,
        kdf: Kdf,
    ): Result<UpdateKdfResponse> = runCatchingWithLogs {
        getClient(userId = userId)
            .crypto()
            .makeUpdateKdf(password = password, kdf = kdf)
    }
}
