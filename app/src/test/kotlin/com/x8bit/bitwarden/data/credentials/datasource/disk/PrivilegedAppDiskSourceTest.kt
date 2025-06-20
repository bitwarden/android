package com.x8bit.bitwarden.data.credentials.datasource.disk

import app.cash.turbine.test
import com.x8bit.bitwarden.data.credentials.datasource.disk.dao.PrivilegedAppDao
import com.x8bit.bitwarden.data.credentials.datasource.disk.entity.PrivilegedAppEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PrivilegedAppDiskSourceTest {

    private val mutableUserTrustedPrivilegedAppsFlow =
        MutableStateFlow<List<PrivilegedAppEntity>>(emptyList())
    private val mockPrivilegedAppDao = mockk<PrivilegedAppDao> {
        coEvery { getUserTrustedPrivilegedAppsFlow() } returns mutableUserTrustedPrivilegedAppsFlow
        coEvery {
            getAllUserTrustedPrivilegedApps()
        } returns mutableUserTrustedPrivilegedAppsFlow.value
        coEvery { isPrivilegedAppTrustedByUser(any(), any()) } returns false
        coEvery { removeTrustedPrivilegedApp(any(), any()) } just runs
        coEvery { addTrustedPrivilegedApp(any()) } just runs
    }
    private val privilegedAppDiskSource = PrivilegedAppDiskSourceImpl(
        privilegedAppDao = mockPrivilegedAppDao,
    )

    @Test
    fun `getAllUserTrustedPrivilegedApps should call getAllUserTrustedPrivilegedApps on the dao`() =
        runTest {
            privilegedAppDiskSource.getAllUserTrustedPrivilegedApps()

            coVerify { mockPrivilegedAppDao.getAllUserTrustedPrivilegedApps() }
        }

    @Test
    fun `isPrivilegedAppTrustedByUser should call isPrivilegedAppTrustedByUser on the dao`() =
        runTest {
            privilegedAppDiskSource.isPrivilegedAppTrustedByUser(
                packageName = "mockPackageName",
                signature = "mockSignature",
            )

            coVerify {
                mockPrivilegedAppDao.isPrivilegedAppTrustedByUser(
                    "mockPackageName",
                    "mockSignature",
                )
            }
        }

    @Test
    fun `addTrustedPrivilegedApp should call addTrustedPrivilegedApp on the dao`() = runTest {
        privilegedAppDiskSource.addTrustedPrivilegedApp(
            packageName = "mockPackageName",
            signature = "mockSignature",
        )

        coVerify {
            mockPrivilegedAppDao.addTrustedPrivilegedApp(
                PrivilegedAppEntity(
                    packageName = "mockPackageName",
                    signature = "mockSignature",
                ),
            )
        }
    }

    @Test
    fun `removeTrustedPrivilegedApp should call removeTrustedPrivilegedApp on the dao`() = runTest {
        privilegedAppDiskSource.removeTrustedPrivilegedApp(
            packageName = "mockPackageName",
            signature = "mockSignature",
        )

        coVerify {
            mockPrivilegedAppDao.removeTrustedPrivilegedApp(
                "mockPackageName",
                "mockSignature",
            )
        }
    }

    @Test
    fun `userTrustedPrivilegedAppsFlow should emit updates from the dao`() = runTest {
        privilegedAppDiskSource.userTrustedPrivilegedAppsFlow.test {
            // Verify the initial state is empty
            assertEquals(emptyList<PrivilegedAppEntity>(), awaitItem())

            mutableUserTrustedPrivilegedAppsFlow.emit(
                listOf(
                    PrivilegedAppEntity(
                        packageName = "mockPackageName",
                        signature = "mockSignature",
                    ),
                ),
            )

            // Verify the updated state is correct
            assertEquals(
                listOf(
                    PrivilegedAppEntity(
                        packageName = "mockPackageName",
                        signature = "mockSignature",
                    ),
                ),
                awaitItem(),
            )
        }
    }
}
