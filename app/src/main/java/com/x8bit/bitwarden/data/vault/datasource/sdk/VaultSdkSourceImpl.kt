package com.x8bit.bitwarden.data.vault.datasource.sdk

import com.bitwarden.core.AttachmentEncryptResult
import com.bitwarden.core.AttachmentView
import com.bitwarden.core.Cipher
import com.bitwarden.core.CipherListView
import com.bitwarden.core.CipherView
import com.bitwarden.core.Collection
import com.bitwarden.core.CollectionView
import com.bitwarden.core.DateTime
import com.bitwarden.core.DerivePinKeyResponse
import com.bitwarden.core.Folder
import com.bitwarden.core.FolderView
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoRequest
import com.bitwarden.core.PasswordHistory
import com.bitwarden.core.PasswordHistoryView
import com.bitwarden.core.Send
import com.bitwarden.core.SendView
import com.bitwarden.core.TotpResponse
import com.bitwarden.sdk.BitwardenException
import com.bitwarden.sdk.Client
import com.bitwarden.sdk.ClientVault
import com.x8bit.bitwarden.data.platform.manager.SdkClientManager
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult

/**
 * Primary implementation of [VaultSdkSource] that serves as a convenience wrapper around a
 * [ClientVault].
 */
@Suppress("TooManyFunctions")
class VaultSdkSourceImpl(
    private val sdkClientManager: SdkClientManager,
) : VaultSdkSource {
    override fun clearCrypto(userId: String) {
        sdkClientManager.destroyClient(userId = userId)
    }

    override suspend fun derivePinKey(
        userId: String,
        pin: String,
    ): Result<DerivePinKeyResponse> =
        runCatching {
            getClient(userId = userId)
                .crypto()
                .derivePinKey(pin = pin)
        }

    override suspend fun derivePinProtectedUserKey(
        userId: String,
        encryptedPin: String,
    ): Result<String> =
        runCatching {
            getClient(userId = userId)
                .crypto()
                .derivePinUserKey(encryptedPin = encryptedPin)
        }

    override suspend fun getUserEncryptionKey(
        userId: String,
    ): Result<String> =
        runCatching {
            getClient(userId = userId)
                .crypto()
                .getUserEncryptionKey()
        }

    override suspend fun initializeCrypto(
        userId: String,
        request: InitUserCryptoRequest,
    ): Result<InitializeCryptoResult> =
        runCatching {
            try {
                getClient(userId = userId)
                    .crypto()
                    .initializeUserCrypto(req = request)
                InitializeCryptoResult.Success
            } catch (exception: BitwardenException) {
                // The only truly expected error from the SDK is an incorrect key/password.
                InitializeCryptoResult.AuthenticationError
            }
        }

    override suspend fun initializeOrganizationCrypto(
        userId: String,
        request: InitOrgCryptoRequest,
    ): Result<InitializeCryptoResult> =
        runCatching {
            try {
                getClient(userId = userId)
                    .crypto()
                    .initializeOrgCrypto(req = request)
                InitializeCryptoResult.Success
            } catch (exception: BitwardenException) {
                // The only truly expected error from the SDK is for incorrect keys.
                InitializeCryptoResult.AuthenticationError
            }
        }

    override suspend fun encryptSend(
        userId: String,
        sendView: SendView,
    ): Result<Send> =
        runCatching {
            getClient(userId = userId)
                .vault()
                .sends()
                .encrypt(sendView)
        }

    override suspend fun encryptBuffer(
        userId: String,
        send: Send,
        fileBuffer: ByteArray,
    ): Result<ByteArray> =
        runCatching {
            getClient(userId = userId)
                .vault()
                .sends()
                .encryptBuffer(
                    send = send,
                    buffer = fileBuffer,
                )
        }

    override suspend fun encryptAttachment(
        userId: String,
        cipher: Cipher,
        attachmentView: AttachmentView,
        fileBuffer: ByteArray,
    ): Result<AttachmentEncryptResult> =
        runCatching {
            getClient(userId = userId)
                .vault()
                .attachments()
                .encryptBuffer(
                    cipher = cipher,
                    attachment = attachmentView,
                    buffer = fileBuffer,
                )
        }

    override suspend fun encryptCipher(
        userId: String,
        cipherView: CipherView,
    ): Result<Cipher> =
        runCatching {
            getClient(userId = userId)
                .vault()
                .ciphers()
                .encrypt(cipherView)
        }

    override suspend fun decryptCipher(
        userId: String,
        cipher: Cipher,
    ): Result<CipherView> =
        runCatching {
            getClient(userId = userId)
                .vault()
                .ciphers()
                .decrypt(cipher)
        }

    override suspend fun decryptCipherListCollection(
        userId: String,
        cipherList: List<Cipher>,
    ): Result<List<CipherListView>> =
        runCatching {
            getClient(userId = userId)
                .vault().ciphers()
                .decryptList(cipherList)
        }

    override suspend fun decryptCipherList(
        userId: String,
        cipherList: List<Cipher>,
    ): Result<List<CipherView>> =
        runCatching {
            cipherList.map {
                getClient(userId = userId)
                    .vault()
                    .ciphers()
                    .decrypt(it)
            }
        }

    override suspend fun decryptCollection(
        userId: String,
        collection: Collection,
    ): Result<CollectionView> =
        runCatching {
            getClient(userId = userId)
                .vault()
                .collections()
                .decrypt(collection)
        }

    override suspend fun decryptCollectionList(
        userId: String,
        collectionList: List<Collection>,
    ): Result<List<CollectionView>> =
        runCatching {
            getClient(userId = userId)
                .vault()
                .collections()
                .decryptList(collectionList)
        }

    override suspend fun decryptSend(
        userId: String,
        send: Send,
    ): Result<SendView> =
        runCatching {
            getClient(userId = userId)
                .vault()
                .sends()
                .decrypt(send)
        }

    override suspend fun decryptSendList(
        userId: String,
        sendList: List<Send>,
    ): Result<List<SendView>> =
        runCatching {
            sendList.map {
                getClient(userId = userId)
                    .vault()
                    .sends()
                    .decrypt(it)
            }
        }

    override suspend fun decryptFolder(
        userId: String,
        folder: Folder,
    ): Result<FolderView> =
        runCatching {
            getClient(userId = userId)
                .vault()
                .folders()
                .decrypt(folder)
        }

    override suspend fun decryptFolderList(
        userId: String,
        folderList: List<Folder>,
    ): Result<List<FolderView>> =
        runCatching {
            getClient(userId = userId)
                .vault()
                .folders()
                .decryptList(folderList)
        }

    override suspend fun encryptPasswordHistory(
        userId: String,
        passwordHistory: PasswordHistoryView,
    ): Result<PasswordHistory> = runCatching {
        getClient(userId = userId)
            .vault()
            .passwordHistory()
            .encrypt(passwordHistory)
    }

    override suspend fun decryptPasswordHistoryList(
        userId: String,
        passwordHistoryList: List<PasswordHistory>,
    ): Result<List<PasswordHistoryView>> = runCatching {
        getClient(userId = userId)
            .vault()
            .passwordHistory()
            .decryptList(passwordHistoryList)
    }

    override suspend fun generateTotp(
        userId: String,
        totp: String,
        time: DateTime,
    ): Result<TotpResponse> = runCatching {
        getClient(userId = userId)
            .vault()
            .generateTotp(
                key = totp,
                time = time,
            )
    }

    private fun getClient(
        userId: String,
    ): Client = sdkClientManager.getOrCreateClient(userId = userId)
}
