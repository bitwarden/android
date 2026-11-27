package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.vault.CipherCreateRequest
import com.bitwarden.vault.CipherEditRequest
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.CipherViewType

/**
 * Converts this [CipherView] into the [CipherViewType] expected by the SDK cipher requests, or
 * `null` when the sub-view matching [CipherView.type] is missing.
 */
fun CipherView.toCipherViewType(): CipherViewType? =
    when (type) {
        CipherType.LOGIN -> login?.let { CipherViewType.Login(v1 = it) }
        CipherType.SECURE_NOTE -> secureNote?.let { CipherViewType.SecureNote(v1 = it) }
        CipherType.CARD -> card?.let { CipherViewType.Card(v1 = it) }
        CipherType.IDENTITY -> identity?.let { CipherViewType.Identity(v1 = it) }
        CipherType.SSH_KEY -> sshKey?.let { CipherViewType.SshKey(v1 = it) }
        CipherType.BANK_ACCOUNT -> bankAccount?.let { CipherViewType.BankAccount(v1 = it) }
        CipherType.DRIVERS_LICENSE -> {
            driversLicense?.let { CipherViewType.DriversLicense(v1 = it) }
        }

        CipherType.PASSPORT -> passport?.let { CipherViewType.Passport(v1 = it) }
    }

/**
 * Converts this [CipherView] into a [CipherCreateRequest] that adds the cipher to the given
 * [collectionIds], or `null` when the cipher type data is missing.
 */
fun CipherView.toCipherCreateRequest(
    collectionIds: List<String>,
): CipherCreateRequest? =
    toCipherViewType()?.let { cipherViewType ->
        CipherCreateRequest(
            organizationId = organizationId,
            collectionIds = collectionIds,
            folderId = folderId,
            name = name,
            notes = notes,
            favorite = favorite,
            reprompt = reprompt,
            type = cipherViewType,
            fields = fields.orEmpty(),
            archivedDate = archivedDate,
        )
    }

/**
 * Converts this [CipherView] into a [CipherEditRequest] for the cipher with the given [cipherId],
 * or `null` when the cipher type data is missing.
 */
fun CipherView.toCipherEditRequest(
    cipherId: String,
): CipherEditRequest? =
    toCipherViewType()?.let { cipherViewType ->
        CipherEditRequest(
            id = cipherId,
            organizationId = organizationId,
            folderId = folderId,
            favorite = favorite,
            reprompt = reprompt,
            name = name,
            notes = notes,
            fields = fields.orEmpty(),
            type = cipherViewType,
            revisionDate = revisionDate,
            archivedDate = archivedDate,
            attachments = attachments.orEmpty(),
            key = key,
        )
    }
