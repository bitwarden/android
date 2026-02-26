package com.x8bit.bitwarden.data.platform.manager.sdk

import com.bitwarden.data.datasource.disk.ConfigDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.CookieDiskSource
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class SdkRepositoryFactoryTests {

    private val vaultDiskSource: VaultDiskSource = mockk()
    private val cookieDiskSource: CookieDiskSource = mockk()
    private val configDiskSource: ConfigDiskSource = mockk()
    private val authDiskSource: AuthDiskSource = mockk()

    private val sdkRepoFactory: SdkRepositoryFactory = SdkRepositoryFactoryImpl(
        vaultDiskSource = vaultDiskSource,
        cookieDiskSource = cookieDiskSource,
        configDiskSource = configDiskSource,
        authDiskSource = authDiskSource,
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

    @Test
    fun `getClientManagedTokens should create a new client`() {
        val userId = "userId"
        val firstClient = sdkRepoFactory.getClientManagedTokens(userId = userId)

        // Additional calls for the same userId should create a repo
        val secondClient = sdkRepoFactory.getClientManagedTokens(userId = userId)
        assertNotEquals(firstClient, secondClient)

        // Additional calls for different userIds should return a different repo
        val otherUserId = "otherUserId"
        val thirdClient = sdkRepoFactory.getClientManagedTokens(userId = otherUserId)
        assertNotEquals(firstClient, thirdClient)
    }

    @Test
    fun `getServerCommunicationConfigRepository should create a new repository`() {
        val firstRepo = sdkRepoFactory.getServerCommunicationConfigRepository()
        val secondRepo = sdkRepoFactory.getServerCommunicationConfigRepository()
        assertNotEquals(firstRepo, secondRepo)
    }
}
