package com.x8bit.bitwarden.data.platform.repository

import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeSettingsDiskSource
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SettingsRepositoryTest {
    private val fakeSettingsDiskSource = FakeSettingsDiskSource()

    private val settingsRepository = SettingsRepositoryImpl(
        settingsDiskSource = fakeSettingsDiskSource,
        dispatcherManager = FakeDispatcherManager(),
    )

    @Test
    fun `getVaultTimeoutStateFlow should react to changes in SettingsDiskSource`() = runTest {
        val userId = "userId"
        settingsRepository
            .getVaultTimeoutStateFlow(userId = userId)
            .test {
                assertEquals(
                    VaultTimeout.Never,
                    awaitItem(),
                )
                VAULT_TIMEOUT_MAP.forEach { (vaultTimeout, vaultTimeoutInMinutes) ->
                    fakeSettingsDiskSource.storeVaultTimeoutInMinutes(
                        userId = userId,
                        vaultTimeoutInMinutes = vaultTimeoutInMinutes,
                    )
                    assertEquals(
                        vaultTimeout,
                        awaitItem(),
                    )
                }
            }
    }

    @Test
    fun `storeVaultTimeout should properly update SettingsDiskSource`() {
        val userId = "userId"
        VAULT_TIMEOUT_MAP.forEach { (vaultTimeout, vaultTimeoutInMinutes) ->
            settingsRepository.storeVaultTimeout(
                userId = userId,
                vaultTimeout = vaultTimeout,
            )
            assertEquals(
                vaultTimeoutInMinutes,
                fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = userId),
            )
        }
    }
}

/**
 * Maps a VaultTimeout to its expected vaultTimeoutInMinutes value.
 */
private val VAULT_TIMEOUT_MAP =
    mapOf(
        VaultTimeout.OneMinute to 1,
        VaultTimeout.FiveMinutes to 5,
        VaultTimeout.ThirtyMinutes to 30,
        VaultTimeout.OneHour to 60,
        VaultTimeout.FourHours to 240,
        VaultTimeout.OnAppRestart to -1,
        VaultTimeout.Never to null,
        VaultTimeout.Custom(vaultTimeoutInMinutes = 123) to 123,
    )
