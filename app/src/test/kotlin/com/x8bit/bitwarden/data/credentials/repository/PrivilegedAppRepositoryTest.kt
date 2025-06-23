package com.x8bit.bitwarden.data.credentials.repository

import app.cash.turbine.test
import com.x8bit.bitwarden.data.credentials.datasource.disk.PrivilegedAppDiskSource
import com.x8bit.bitwarden.data.credentials.datasource.disk.entity.PrivilegedAppEntity
import com.x8bit.bitwarden.data.credentials.model.PrivilegedAppAllowListJson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivilegedAppRepositoryTest {

    private val mutableUserTrustedPrivilegedAppsFlow =
        MutableStateFlow<List<PrivilegedAppEntity>>(emptyList())
    private val mockPrivilegedAppDiskSource = mockk<PrivilegedAppDiskSource> {
        every { userTrustedPrivilegedAppsFlow } returns mutableUserTrustedPrivilegedAppsFlow
        coEvery {
            getAllUserTrustedPrivilegedApps()
        } returns mutableUserTrustedPrivilegedAppsFlow.value
        coEvery { isPrivilegedAppTrustedByUser(any(), any()) } returns false
        coEvery { removeTrustedPrivilegedApp(any(), any()) } just runs
        coEvery { addTrustedPrivilegedApp(any(), any()) } just runs
    }
    private val mockJson = mockk<Json>()
    private val repository = PrivilegedAppRepositoryImpl(
        privilegedAppDiskSource = mockPrivilegedAppDiskSource,
        json = mockJson,
    )

    @Test
    fun `getAllUserTrustedPrivilegedApps should return empty list when disk source is empty`() =
        runTest {
            coEvery {
                mockPrivilegedAppDiskSource.getAllUserTrustedPrivilegedApps()
            } returns emptyList()

            val result = repository.getAllUserTrustedPrivilegedApps()

            assertTrue(result.apps.isEmpty())
        }

    @Test
    fun `getAllUserTrustedPrivilegedApps should return correct data when disk source has data`() =
        runTest {
            val diskApps = listOf(
                createMockPrivilegedAppEntity(number = 1),
                createMockPrivilegedAppEntity(number = 2),
            )
            coEvery {
                mockPrivilegedAppDiskSource.getAllUserTrustedPrivilegedApps()
            } returns diskApps

            val result = repository.getAllUserTrustedPrivilegedApps()

            assertEquals(
                PrivilegedAppAllowListJson(
                    apps = listOf(
                        createMockPrivilegedAppJson(number = 1),
                        createMockPrivilegedAppJson(number = 2),
                    ),
                ),
                result,
            )
        }

    @Test
    fun `userTrustedPrivilegedAppsFlow should emit updates from disk source`() = runTest {
        repository.userTrustedPrivilegedAppsFlow.test {
            // Verify the initial state is empty
            assertEquals(PrivilegedAppAllowListJson(apps = emptyList()), awaitItem())

            mutableUserTrustedPrivilegedAppsFlow.emit(
                listOf(createMockPrivilegedAppEntity(number = 1)),
            )

            // Verify the updated state is correct
            assertEquals(
                PrivilegedAppAllowListJson(apps = listOf(createMockPrivilegedAppJson(number = 1))),
                awaitItem(),
            )
        }
    }

    @Test
    fun `removeTrustedPrivilegedApp should call the disk source`() = runTest {
        repository.removeTrustedPrivilegedApp(
            packageName = "mockPackageName",
            signature = "mockSignature",
        )

        coVerify {
            mockPrivilegedAppDiskSource.removeTrustedPrivilegedApp(
                "mockPackageName",
                "mockSignature",
            )
        }
    }

    @Test
    fun `isPrivilegedAppAllowed should call the disk source`() = runTest {
        repository.isPrivilegedAppAllowed(
            packageName = "mockPackageName",
            signature = "mockSignature",
        )

        coVerify {
            mockPrivilegedAppDiskSource.isPrivilegedAppTrustedByUser(
                "mockPackageName",
                "mockSignature",
            )
        }
    }

    @Test
    fun `addTrustedPrivilegedApp should call the disk source`() = runTest {
        repository.addTrustedPrivilegedApp(
            packageName = "mockPackageName",
            signature = "mockSignature",
        )

        coVerify {
            mockPrivilegedAppDiskSource.addTrustedPrivilegedApp(
                "mockPackageName",
                "mockSignature",
            )
        }
    }

    @Test
    fun `getUserTrustedAllowListJson should return correct JSON string with empty list`() =
        runTest {
            every {
                mockJson.encodeToString(PrivilegedAppAllowListJson(apps = emptyList()))
            } returns """{"apps":[]}"""

            val result = repository.getUserTrustedAllowListJson()
            assertEquals("""{"apps":[]}""", result)
        }

    @Test
    fun `getUserTrustedAllowListJson should return correct JSON string with populated list`() =
        runTest {
            val diskApps = listOf(
                createMockPrivilegedAppEntity(number = 1),
                createMockPrivilegedAppEntity(number = 2),
            )
            coEvery {
                mockPrivilegedAppDiskSource.getAllUserTrustedPrivilegedApps()
            } returns diskApps

            val expectedPrivilegedAppAllowListJson = PrivilegedAppAllowListJson(
                apps = listOf(
                    createMockPrivilegedAppJson(number = 1),
                    createMockPrivilegedAppJson(number = 2),
                ),
            )

            every {
                mockJson.encodeToString(expectedPrivilegedAppAllowListJson)
            } returns ALLOW_LIST_JSON

            assertEquals(
                ALLOW_LIST_JSON,
                repository.getUserTrustedAllowListJson(),
            )
        }
}

private val ALLOW_LIST_JSON = """
{
  "apps": [
    {
      "type": "android",
      "info": {
        "packageName": "mockPackageName-1",
        "signatures": [
          {
            "build": "release",
            "certFingerprintSha256": "mockSignature-1"
          }
        ]
      }
    },
    {
      "type": "android",
      "info": {
        "packageName": "mockPackageName-2",
        "signatures": [
          {
            "build": "release",
            "certFingerprintSha256": "mockSignature-2"
          }
        ]
      }
    }
  ]
}
"""
    .trimIndent()

private fun createMockPrivilegedAppEntity(number: Int) = PrivilegedAppEntity(
    packageName = "mockPackageName-$number",
    signature = "mockSignature-$number",
)

private fun createMockPrivilegedAppJson(number: Int) = PrivilegedAppAllowListJson.PrivilegedAppJson(
    type = "android",
    info = PrivilegedAppAllowListJson.PrivilegedAppJson.InfoJson(
        packageName = "mockPackageName-$number",
        signatures = listOf(
            PrivilegedAppAllowListJson.PrivilegedAppJson.InfoJson.SignatureJson(
                build = "release",
                certFingerprintSha256 = "mockSignature-$number",
            ),
        ),
    ),
)
