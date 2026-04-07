package com.bitwarden.data.datasource.disk

import androidx.core.content.edit
import app.cash.turbine.test
import com.bitwarden.core.data.util.assertJsonEquals
import com.bitwarden.core.di.CoreModule
import com.bitwarden.data.datasource.disk.base.FakeSharedPreferences
import com.bitwarden.data.datasource.disk.model.FlightRecorderDataSet
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FlightRecorderDiskSourceTest {
    private val fakeSharedPreferences = FakeSharedPreferences()
    private val json = CoreModule.providesJson()

    private val flightRecorderDiskSource = FlightRecorderDiskSourceImpl(
        sharedPreferences = fakeSharedPreferences,
        json = json,
    )

    @Test
    fun `flightRecorderData should pull from SharedPreferences`() {
        val flightRecorderKey = "bwPreferencesStorage:flightRecorderData"
        val encodedData = """
            {
              "data": [
                {
                  "id": "51",
                  "fileName": "flight_recorder_2025-04-03_14-22-40",
                  "startTime": 1744059882,
                  "duration": 3600,
                  "isActive": false
                }
              ]
            }
        """
            .trimIndent()
        val expected = FlightRecorderDataSet(
            data = setOf(
                FlightRecorderDataSet.FlightRecorderData(
                    id = "51",
                    fileName = "flight_recorder_2025-04-03_14-22-40",
                    startTimeMs = 1_744_059_882L,
                    durationMs = 3_600L,
                    isActive = false,
                ),
            ),
        )

        // Verify initial value is null and disk source matches shared preferences.
        assertNull(fakeSharedPreferences.getString(flightRecorderKey, null))
        assertNull(flightRecorderDiskSource.flightRecorderData)

        // Updating the shared preferences should update disk source.
        fakeSharedPreferences.edit { putString(flightRecorderKey, encodedData) }
        val actual = flightRecorderDiskSource.flightRecorderData
        assertEquals(expected, actual)
    }

    @Test
    fun `flightRecorderDataFlow should react to changes in isFlightRecorderEnabled`() = runTest {
        val expected = FlightRecorderDataSet(
            data = setOf(
                FlightRecorderDataSet.FlightRecorderData(
                    id = "52",
                    fileName = "flight_recorder_2025-04-03_14-22-40",
                    startTimeMs = 1_744_059_882L,
                    durationMs = 3_600L,
                    isActive = true,
                ),
            ),
        )
        flightRecorderDiskSource.flightRecorderDataFlow.test {
            // The initial values of the Flow and the property are in sync
            assertNull(flightRecorderDiskSource.flightRecorderData)
            assertNull(awaitItem())

            flightRecorderDiskSource.flightRecorderData = expected
            assertEquals(expected, awaitItem())

            flightRecorderDiskSource.flightRecorderData = null
            assertNull(awaitItem())
        }
    }

    @Test
    fun `setting flightRecorderData should update SharedPreferences`() {
        val flightRecorderKey = "bwPreferencesStorage:flightRecorderData"
        val data = FlightRecorderDataSet(
            data = setOf(
                FlightRecorderDataSet.FlightRecorderData(
                    id = "53",
                    fileName = "flight_recorder_2025-04-03_14-22-40",
                    startTimeMs = 1_744_059_882L,
                    durationMs = 3_600L,
                    isActive = true,
                ),
            ),
        )
        val expected = """
            {
              "data": [
                {
                  "id": "53",
                  "fileName": "flight_recorder_2025-04-03_14-22-40",
                  "startTime": 1744059882,
                  "duration": 3600,
                  "isActive": true
                }
              ]
            }
        """
            .trimIndent()
        flightRecorderDiskSource.flightRecorderData = data
        val actual = fakeSharedPreferences.getString(flightRecorderKey, null)
        assertJsonEquals(expected, actual!!)
    }
}
