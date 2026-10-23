package com.x8bit.bitwarden.ui.vault.feature.pamapprovals

import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherListViewType
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.util.login
import com.x8bit.bitwarden.data.pam.repository.model.PamApprovalRequest
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toLoginIconData

/**
 * Names resolved from the decrypted local vault and the user's organizations, keyed by ID.
 *
 * The server only knows IDs for items, collections and organizations, because their names are
 * encrypted, so the names are looked up on the device.
 */
data class PamNames(
    val itemNames: Map<String, String>,
    val collectionNames: Map<String, String>,
    val organizationNames: Map<String, String>,
) {
    /**
     * The name of the item the [request] is for, if it is in the local vault.
     */
    fun itemName(request: PamApprovalRequest): String? = itemNames[request.cipherId]

    /**
     * The name of the collection the [request] is for, if it is in the local vault.
     */
    fun collectionName(request: PamApprovalRequest): String? =
        collectionNames[request.collectionId]

    /**
     * The name of the organization the [request] is for, if known.
     */
    fun organizationName(request: PamApprovalRequest): String? =
        request.organizationId?.let { organizationNames[it] }
}

/**
 * Resolves the [PamNames] from the data currently held by the [vaultRepository] and
 * [authRepository].
 */
fun resolvePamNames(
    vaultRepository: VaultRepository,
    authRepository: AuthRepository,
): PamNames = PamNames(
    itemNames = vaultRepository
        .decryptCipherListResultStateFlow
        .value
        .data
        ?.successes
        .orEmpty()
        .mapNotNull { cipher -> cipher.id?.let { it to cipher.name } }
        .toMap(),
    collectionNames = vaultRepository
        .collectionsStateFlow
        .value
        .data
        .orEmpty()
        .mapNotNull { collection -> collection.id?.let { it to collection.name } }
        .toMap(),
    organizationNames = authRepository
        .userStateFlow
        .value
        ?.activeAccount
        ?.organizations
        .orEmpty()
        .associate { it.id to it.name },
)

/**
 * Resolves the icon of the item a request is for, the same way the vault list does: the website
 * favicon for a login with a web address (unless website icons are turned off), otherwise the icon
 * for the item type.
 *
 * Approvers only get the ID of the item in the request, so the icon comes from the item in the
 * decrypted local vault. When the item is not in the local vault the generic login icon is used.
 * No extra requests are made beyond the favicon image the icon component loads.
 */
fun resolvePamItemIcon(
    vaultRepository: VaultRepository,
    cipherId: String,
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
): IconData {
    val cipher = vaultRepository
        .decryptCipherListResultStateFlow
        .value
        .data
        ?.successes
        .orEmpty()
        .firstOrNull { it.id == cipherId }
    return cipher.toPamIconData(
        baseIconUrl = baseIconUrl,
        isIconLoadingDisabled = isIconLoadingDisabled,
    )
}

private fun CipherListView?.toPamIconData(
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
): IconData = when (this?.type) {
    null -> IconData.Local(iconRes = BitwardenDrawable.ic_globe)
    is CipherListViewType.Login -> this?.login?.uris.toLoginIconData(
        baseIconUrl = baseIconUrl,
        isIconLoadingDisabled = isIconLoadingDisabled,
        usePasskeyDefaultIcon = false,
    )

    CipherListViewType.SecureNote -> IconData.Local(iconRes = BitwardenDrawable.ic_note)
    is CipherListViewType.Card,
    is CipherListViewType.BankAccount,
        -> IconData.Local(iconRes = BitwardenDrawable.ic_payment_card)

    CipherListViewType.Identity -> IconData.Local(iconRes = BitwardenDrawable.ic_id_card)
    CipherListViewType.SshKey -> IconData.Local(iconRes = BitwardenDrawable.ic_ssh_key)
    CipherListViewType.DriversLicense -> IconData.Local(iconRes = BitwardenDrawable.ic_note)
    CipherListViewType.Passport -> IconData.Local(iconRes = BitwardenDrawable.ic_passport)
}
