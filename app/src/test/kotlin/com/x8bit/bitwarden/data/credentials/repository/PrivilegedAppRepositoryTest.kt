package com.x8bit.bitwarden.data.credentials.repository

import app.cash.turbine.test
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.x8bit.bitwarden.data.credentials.datasource.disk.PrivilegedAppDiskSource
import com.x8bit.bitwarden.data.credentials.datasource.disk.entity.PrivilegedAppEntity
import com.x8bit.bitwarden.data.credentials.model.PrivilegedAppAllowListJson
import com.x8bit.bitwarden.data.credentials.repository.model.PrivilegedAppData
import com.x8bit.bitwarden.data.credentials.util.createMockPrivilegedAppJson
import com.x8bit.bitwarden.data.platform.manager.AssetManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

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
    private val mockPrivilegedAppAllowListJson = mockk<PrivilegedAppAllowListJson>()
    private val mockJson = mockk<Json> {
        every {
            decodeFromString<PrivilegedAppAllowListJson>(any())
        } returns mockPrivilegedAppAllowListJson
        every {
            decodeFromStringOrNull<PrivilegedAppAllowListJson>(any())
        } returns mockPrivilegedAppAllowListJson
        every { encodeToString(any<PrivilegedAppAllowListJson>()) } returns ALLOW_LIST_JSON
    }
    private val mockAssetManager = mockk<AssetManager> {
        coEvery { readAsset(any()) } returns ALLOW_LIST_JSON.asSuccess()
    }
    private val mockDispatcherManager = FakeDispatcherManager()
    private val repository = PrivilegedAppRepositoryImpl(
        privilegedAppDiskSource = mockPrivilegedAppDiskSource,
        json = mockJson,
        assetManager = mockAssetManager,
        dispatcherManager = mockDispatcherManager,
    )

    @Test
    fun `trustedAppDataStateFlow should emit loaded with merged data`() = runTest {
        repository.trustedAppDataStateFlow.test {
            assertEquals(
                DataState.Loaded(
                    PrivilegedAppData(
                        googleTrustedApps = mockPrivilegedAppAllowListJson,
                        communityTrustedApps = mockPrivilegedAppAllowListJson,
                        userTrustedApps = PrivilegedAppAllowListJson(apps = emptyList()),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `getAllUserTrustedPrivilegedApps should return empty list when disk source is empty`() =
        runTest {
            coEvery {
                mockPrivilegedAppDiskSource.getAllUserTrustedPrivilegedApps()
            } returns emptyList()

            val result = repository.getUserTrustedPrivilegedAppsOrNull()

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

            val result = repository.getUserTrustedPrivilegedAppsOrNull()

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
        mutableUserTrustedPrivilegedAppsFlow.emit(
            listOf(createMockPrivilegedAppEntity(number = 1)),
        )
        repository.userTrustedAppsFlow.test {

            // Verify the updated state is correct
            assertEquals(
                DataState.Loaded(
                    data = PrivilegedAppAllowListJson(
                        apps = listOf(createMockPrivilegedAppJson(number = 1)),
                    ),
                ),
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

    @Test
    fun `getGoogleTrustedPrivilegedAppsOrNull should return correct data`() = runTest {
        assertEquals(
            mockPrivilegedAppAllowListJson,
            repository.getGoogleTrustedPrivilegedAppsOrNull(),
        )
    }

    @Test
    fun `getGoogleTrustedPrivilegedAppsOrNull should return null when asset manager fails`() =
        runTest {
            coEvery {
                mockAssetManager.readAsset(any())
            } returns Result.failure(Exception())

            assertNull(repository.getGoogleTrustedPrivilegedAppsOrNull())
        }

    @Test
    fun `getGoogleTrustedPrivilegedAppsOrNull should return null when deserialization fails`() =
        runTest {
            every {
                mockJson.decodeFromStringOrNull<PrivilegedAppAllowListJson>(any())
            } returns null
            assertNull(repository.getGoogleTrustedPrivilegedAppsOrNull())
        }

    @Test
    fun `getCommunityTrustedPrivilegedAppsOrNull should return correct data`() = runTest {
        assertEquals(
            mockPrivilegedAppAllowListJson,
            repository.getCommunityTrustedPrivilegedAppsOrNull(),
        )
    }

    @Test
    fun `getCommunityTrustedPrivilegedAppsOrNull should return null when asset manager fails`() =
        runTest {
            coEvery {
                mockAssetManager.readAsset(any())
            } returns Result.failure(Exception())

            assertNull(repository.getCommunityTrustedPrivilegedAppsOrNull())
        }

    @Test
    fun `getCommunityTrustedPrivilegedAppsOrNull should return null when deserialization fails`() =
        runTest {
            every {
                mockJson.decodeFromStringOrNull<PrivilegedAppAllowListJson>(any())
            } returns null
            assertNull(repository.getCommunityTrustedPrivilegedAppsOrNull())
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
