package com.x8bit.bitwarden.data.platform.manager.sdk

import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class SdkRepositoryFactoryTests {

    private val vaultDiskSource: VaultDiskSource = mockk()

    private val sdkRepoFactory: SdkRepositoryFactory = SdkRepositoryFactoryImpl(
        vaultDiskSource = vaultDiskSource,
    )

    @Test
    fun `getCipherRepository should create a new client`() {
        val userId = "userId"
        val firstClient = sdkRepoFactory.getCipherRepository(userId = userId)

        // Additional calls for the same userId should create a repo
        val secondClient = sdkRepoFactory.getCipherRepository(userId = userId)
        assertNotEquals(firstClient, secondClient)

        // Additional calls for different userIds should return a different repo
        val otherUserId = "otherUserId"
        val thirdClient = sdkRepoFactory.getCipherRepository(userId = otherUserId)
        assertNotEquals(firstClient, thirdClient)
    }
}
