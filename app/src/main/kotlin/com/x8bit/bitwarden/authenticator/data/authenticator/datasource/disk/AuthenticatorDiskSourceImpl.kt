package com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk

import com.bitwarden.core.CipherRepromptType
import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.bitwarden.core.DateTime
import com.bitwarden.core.LoginView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class AuthenticatorDiskSourceImpl @Inject constructor() : AuthenticatorDiskSource {

    private val ciphers = STATIC_CIPHER_CACHE.toMutableList()

    override suspend fun saveCipher(cipher: CipherView) {
        ciphers.add(cipher)
    }

    override fun getCiphers(): Flow<List<CipherView>> {
        return flowOf(ciphers)
    }

    override suspend fun deleteCipher(cipherId: String) {
        ciphers.removeIf { it.id == cipherId }
    }
}

private val STATIC_CIPHER_CACHE = listOf(
    CipherView(
        id = "1",
        organizationId = null,
        folderId = null,
        collectionIds = emptyList(),
        key = null,
        name = "TOTP test 1",
        notes = null,
        type = CipherType.LOGIN,
        login = LoginView(
            null,
            null,
            null,
            null,
            "JBSWY3DPEHPK3PXP",
            null,
            null
        ),
        identity = null,
        card = null,
        secureNote = null,
        favorite = true,
        reprompt = CipherRepromptType.NONE,
        organizationUseTotp = false,
        edit = false,
        viewPassword = true,
        localData = null,
        attachments = null,
        fields = null,
        passwordHistory = null,
        creationDate = DateTime.now(),
        deletedDate = null,
        revisionDate = DateTime.now()
    ),
    CipherView(
        id = "2",
        organizationId = null,
        folderId = null,
        collectionIds = emptyList(),
        key = null,
        name = "TOTP test 2",
        notes = null,
        type = CipherType.LOGIN,
        login = LoginView(
            null,
            null,
            null,
            null,
            "JBSWY3DPEHPK3PXP",
            null,
            null
        ),
        identity = null,
        card = null,
        secureNote = null,
        favorite = true,
        reprompt = CipherRepromptType.NONE,
        organizationUseTotp = false,
        edit = false,
        viewPassword = true,
        localData = null,
        attachments = null,
        fields = null,
        passwordHistory = null,
        creationDate = DateTime.now(),
        deletedDate = null,
        revisionDate = DateTime.now()
    )
)
