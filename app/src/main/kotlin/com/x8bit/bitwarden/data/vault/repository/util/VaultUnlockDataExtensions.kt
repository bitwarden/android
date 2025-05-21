package com.x8bit.bitwarden.data.vault.repository.util

import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData

/**
 * Get the vault unlock status for a [userId] from a list of [VaultUnlockData].
 */
fun List<VaultUnlockData>.statusFor(userId: String): VaultUnlockData.Status? =
    firstOrNull { it.userId == userId }?.status

/**
 * Update the vault unlock status for a [userId] in a list of [VaultUnlockData].
 */
fun List<VaultUnlockData>.update(
    userId: String,
    status: VaultUnlockData.Status?,
): List<VaultUnlockData> {
    val updatedList = filter {
        it.userId != userId
    }
    return if (status == null) {
        updatedList
    } else {
        updatedList
            .plus(
                VaultUnlockData(
                    userId = userId,
                    status = status,
                ),
            )
    }
}
