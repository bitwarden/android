package com.x8bit.bitwarden.data.platform.repository

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeSettingsDiskSource
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettingsRepositoryTest {
    private val authDiskSource: AuthDiskSource = mockk()
    private val fakeSettingsDiskSource = FakeSettingsDiskSource()

    private val settingsRepository = SettingsRepositoryImpl(
        authDiskSource = authDiskSource,
        settingsDiskSource = fakeSettingsDiskSource,
        dispatcherManager = FakeDispatcherManager(),
    )

    @Test
    fun `clearData should clear all necessary data for the given user`() {
        val userId = "userId"

        fakeSettingsDiskSource.apply {
            storeVaultTimeoutInMinutes(
                userId = userId,
                vaultTimeoutInMinutes = 30,
            )
            storeVaultTimeoutAction(
                userId = userId,
                vaultTimeoutAction = VaultTimeoutAction.LOCK,
            )
        }

        settingsRepository.clearData(userId = userId)

        assertNull(fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = userId))
        assertNull(fakeSettingsDiskSource.getVaultTimeoutAction(userId = userId))
    }

    @Test
    fun `setDefaultsIfNecessary should set default values for the given user if necessary`() {
        val userId = "userId"
        assertNull(fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = userId))
        assertNull(fakeSettingsDiskSource.getVaultTimeoutAction(userId = userId))

        settingsRepository.setDefaultsIfNecessary(userId = userId)

        // Calling once sets values
        assertEquals(
            30,
            fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = userId),
        )
        assertEquals(
            VaultTimeoutAction.LOCK,
            fakeSettingsDiskSource.getVaultTimeoutAction(userId = userId),
        )

        // Updating the Vault settings values and calling setDefaultsIfNecessary again has no effect
        // on the currently stored values.
        fakeSettingsDiskSource.apply {
            storeVaultTimeoutInMinutes(
                userId = userId,
                vaultTimeoutInMinutes = 240,
            )
            storeVaultTimeoutAction(
                userId = userId,
                vaultTimeoutAction = VaultTimeoutAction.LOGOUT,
            )
        }
        settingsRepository.setDefaultsIfNecessary(userId = userId)
        assertEquals(
            240,
            fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = userId),
        )
        assertEquals(
            VaultTimeoutAction.LOGOUT,
            fakeSettingsDiskSource.getVaultTimeoutAction(userId = userId),
        )
    }

    @Test
    fun `appLanguage should pull from and update SettingsDiskSource`() {
        assertEquals(
            AppLanguage.DEFAULT,
            settingsRepository.appLanguage,
        )

        // Updates to the disk source change the repository value.
        fakeSettingsDiskSource.appLanguage = AppLanguage.ENGLISH
        assertEquals(
            AppLanguage.ENGLISH,
            settingsRepository.appLanguage,
        )

        // Updates to the repository value change the disk source.
        settingsRepository.appLanguage = AppLanguage.DUTCH
        assertEquals(
            AppLanguage.DUTCH,
            fakeSettingsDiskSource.appLanguage,
        )
    }

    @Test
    fun `isIconLoadingDisabled should pull from and update SettingsDiskSource`() {
        assertFalse(settingsRepository.isIconLoadingDisabled)

        // Updates to the disk source change the repository value.
        fakeSettingsDiskSource.isIconLoadingDisabled = true
        assertTrue(settingsRepository.isIconLoadingDisabled)

        // Updates to the repository change the disk source value
        settingsRepository.isIconLoadingDisabled = false
        assertFalse(fakeSettingsDiskSource.isIconLoadingDisabled!!)
    }

    @Test
    fun `vaultTimeout should pull from and update SettingsDiskSource for the current user`() {
        every { authDiskSource.userState?.activeUserId } returns null
        assertEquals(
            VaultTimeout.Never,
            settingsRepository.vaultTimeout,
        )

        val userId = "userId"
        every { authDiskSource.userState?.activeUserId } returns userId

        // Updates to the disk source change the repository value
        VAULT_TIMEOUT_MAP.forEach { (vaultTimeout, vaultTimeoutInMinutes) ->
            fakeSettingsDiskSource.storeVaultTimeoutInMinutes(
                userId = userId,
                vaultTimeoutInMinutes = vaultTimeoutInMinutes,
            )
            assertEquals(
                vaultTimeout,
                settingsRepository.vaultTimeout,
            )
        }

        // Updates to the repository value change the disk source
        VAULT_TIMEOUT_MAP.forEach { (vaultTimeout, vaultTimeoutInMinutes) ->
            settingsRepository.vaultTimeout = vaultTimeout
            assertEquals(
                vaultTimeoutInMinutes,
                fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = userId),
            )
        }
    }

    @Test
    fun `vaultTimeoutAction should pull from and update SettingsDiskSource`() {
        every { authDiskSource.userState?.activeUserId } returns null
        assertEquals(
            VaultTimeoutAction.LOCK,
            settingsRepository.vaultTimeoutAction,
        )

        val userId = "userId"
        every { authDiskSource.userState?.activeUserId } returns userId

        // Updates to the disk source change the repository value
        VAULT_TIMEOUT_ACTIONS.forEach { vaultTimeoutAction ->
            fakeSettingsDiskSource.storeVaultTimeoutAction(
                userId = userId,
                vaultTimeoutAction = vaultTimeoutAction,
            )
            assertEquals(
                vaultTimeoutAction,
                settingsRepository.vaultTimeoutAction,
            )
        }

        // Updates to the repository value change the disk source
        VAULT_TIMEOUT_ACTIONS.forEach { vaultTimeoutAction ->
            settingsRepository.vaultTimeoutAction = vaultTimeoutAction
            assertEquals(
                vaultTimeoutAction,
                fakeSettingsDiskSource.getVaultTimeoutAction(userId = userId),
            )
        }
    }

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

    @Test
    fun `getVaultTimeoutActionStateFlow should react to changes in SettingsDiskSource`() = runTest {
        val userId = "userId"
        settingsRepository
            .getVaultTimeoutActionStateFlow(userId = userId)
            .test {
                assertEquals(
                    VaultTimeoutAction.LOCK,
                    awaitItem(),
                )
                VAULT_TIMEOUT_ACTIONS.forEach { vaultTimeoutAction ->
                    fakeSettingsDiskSource.storeVaultTimeoutAction(
                        userId = userId,
                        vaultTimeoutAction = vaultTimeoutAction,
                    )
                    assertEquals(
                        vaultTimeoutAction,
                        awaitItem(),
                    )
                }
            }
    }

    @Test
    fun `isVaultTimeoutActionSet when no value is persisted should return false`() {
        val userId = "userId"
        assertFalse(
            settingsRepository.isVaultTimeoutActionSet(userId = userId),
        )
    }

    @Test
    fun `isVaultTimeoutActionSet when a value is persisted should return true`() {
        val userId = "userId"
        fakeSettingsDiskSource.storeVaultTimeoutAction(
            userId = userId,
            vaultTimeoutAction = VaultTimeoutAction.LOGOUT,
        )
        assertTrue(
            settingsRepository.isVaultTimeoutActionSet(userId = userId),
        )
    }

    @Test
    fun `storeVaultTimeoutAction should properly update SettingsDiskSource`() {
        val userId = "userId"
        VAULT_TIMEOUT_ACTIONS.forEach { vaultTimeoutAction ->
            settingsRepository.storeVaultTimeoutAction(
                userId = userId,
                vaultTimeoutAction = vaultTimeoutAction,
            )
            assertEquals(
                vaultTimeoutAction,
                fakeSettingsDiskSource.getVaultTimeoutAction(userId = userId),
            )
        }
    }

    @Test
    fun `getPullToRefreshEnabledFlow should react to changes in SettingsDiskSource`() = runTest {
        val userId = "userId"
        val userState = mockk<UserStateJson> {
            every { activeUserId } returns userId
        }
        coEvery { authDiskSource.userState } returns userState
        settingsRepository
            .getPullToRefreshEnabledFlow()
            .test {
                assertFalse(awaitItem())
                fakeSettingsDiskSource.storePullToRefreshEnabled(
                    userId = userId,
                    isPullToRefreshEnabled = true,
                )
                assertTrue(awaitItem())
                fakeSettingsDiskSource.storePullToRefreshEnabled(
                    userId = userId,
                    isPullToRefreshEnabled = false,
                )
                assertFalse(awaitItem())
            }
    }

    @Test
    fun `storePullToRefreshEnabled should properly update SettingsDiskSource`() {
        val userId = "userId"
        every { authDiskSource.userState?.activeUserId } returns userId
        settingsRepository.storePullToRefreshEnabled(true)
        assertEquals(true, fakeSettingsDiskSource.getPullToRefreshEnabled(userId = userId))
    }
}

/**
 * A list of all [VaultTimeoutAction].
 *
 * The order is reversed here in order to ensure that the first value differs from the default.
 */
private val VAULT_TIMEOUT_ACTIONS = VaultTimeoutAction.entries.reversed()

/**
 * Maps a VaultTimeout to its expected vaultTimeoutInMinutes value.
 */
private val VAULT_TIMEOUT_MAP =
    mapOf(
        VaultTimeout.OneMinute to 1,
        VaultTimeout.FiveMinutes to 5,
        VaultTimeout.FifteenMinutes to 15,
        VaultTimeout.ThirtyMinutes to 30,
        VaultTimeout.OneHour to 60,
        VaultTimeout.FourHours to 240,
        VaultTimeout.OnAppRestart to -1,
        VaultTimeout.Never to null,
        VaultTimeout.Custom(vaultTimeoutInMinutes = 123) to 123,
    )
