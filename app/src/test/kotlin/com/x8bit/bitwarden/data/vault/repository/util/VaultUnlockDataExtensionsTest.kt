package com.x8bit.bitwarden.data.vault.repository.util

import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultUnlockDataExtensionsTest {
    @Test
    fun `statusFor returns the correct status for a userId in the list`() {
        val list = listOf(
            VaultUnlockData(
                userId = USER_ID_1,
                status = VaultUnlockData.Status.UNLOCKING,
            ),
            VaultUnlockData(
                userId = USER_ID_2,
                status = VaultUnlockData.Status.UNLOCKED,
            ),
        )

        assertEquals(
            VaultUnlockData.Status.UNLOCKING,
            list.statusFor(USER_ID_1),
        )
        assertEquals(
            VaultUnlockData.Status.UNLOCKED,
            list.statusFor(USER_ID_2),
        )
    }

    @Test
    fun `update updates the status for a user id in the list`() {
        val list = listOf(
            VaultUnlockData(
                userId = USER_ID_1,
                status = VaultUnlockData.Status.UNLOCKING,
            ),
            VaultUnlockData(
                userId = USER_ID_2,
                status = VaultUnlockData.Status.UNLOCKED,
            ),
        )

        val updatedList = list.update(
            userId = USER_ID_1,
            status = VaultUnlockData.Status.UNLOCKED,
        )
        assertEquals(
            VaultUnlockData.Status.UNLOCKED,
            updatedList.statusFor(USER_ID_1),
        )
    }
}

private const val USER_ID_1 = "userId_1"
private const val USER_ID_2 = "userId_2"
