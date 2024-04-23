package com.x8bit.bitwarden.data.platform.repository.util

import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DataStateExtensionsTest {

    @Test
    fun `map on Loaded should call transform and return appropriate value for Loaded`() {
        val mapValue = 5
        val expected = DataState.Loaded(mapValue)
        val dataState = DataState.Loaded("Loaded")

        val result = dataState.map {
            mapValue
        }

        assertEquals(expected, result)
    }

    @Test
    fun `map on Loading should not call transform and return Loading`() {
        val dataState = DataState.Loading

        val result = dataState.map {
            5
        }

        assertEquals(dataState, result)
    }

    @Test
    fun `map on Pending should call transform and return Pending`() {
        val mapValue = 5
        val expected = DataState.Pending(mapValue)
        val dataState = DataState.Pending("Pending")

        val result = dataState.map {
            mapValue
        }

        assertEquals(expected, result)
    }

    @Test
    fun `map on Error should not call transform with null data and return Error`() {
        val error = Throwable()
        val dataState = DataState.Error(error, null)

        val result = dataState.map {
            5
        }

        assertEquals(dataState, result)
    }

    @Test
    fun `map on Error should call transform with nonnull data and return Error`() {
        val error = Throwable()
        val mapValue = 5
        val expected = DataState.Error(error, mapValue)
        val dataState = DataState.Error(error, "Error")

        val result = dataState.map {
            mapValue
        }

        assertEquals(expected, result)
    }

    @Test
    fun `map on NoNetwork should not call transform with null data and return NoNetwork`() {
        val dataState = DataState.NoNetwork(null)

        val result = dataState.map {
            5
        }

        assertEquals(dataState, result)
    }

    @Test
    fun `map on NoNetwork should call transform with nonnull data and return NoNetwork`() {
        val mapValue = 5
        val expected = DataState.NoNetwork(mapValue)
        val dataState = DataState.NoNetwork("NoNetwork")

        val result = dataState.map {
            mapValue
        }

        assertEquals(expected, result)
    }

    @Test
    fun `mapNullable on Loaded should call transform and return appropriate value for Loaded`() {
        val mapValue = 5
        val expected = DataState.Loaded(mapValue)
        val dataState = DataState.Loaded("Loaded")

        val result = dataState.mapNullable {
            mapValue
        }

        assertEquals(expected, result)
    }

    @Test
    fun `mapNullable on Loading should not call transform and return Loading`() {
        val dataState = DataState.Loading

        val result = dataState.mapNullable {
            5
        }

        assertEquals(dataState, result)
    }

    @Test
    fun `mapNullable on Pending should call transform and return Pending`() {
        val mapValue = 5
        val expected = DataState.Pending(mapValue)
        val dataState = DataState.Pending("Pending")

        val result = dataState.mapNullable {
            mapValue
        }

        assertEquals(expected, result)
    }

    @Test
    fun `mapNullable on Error should call transform with null data and return Error`() {
        val error = Throwable()
        val mapValue = 5
        val expected = DataState.Error(error, mapValue)
        val dataState = DataState.Error(error, null)

        val result = dataState.mapNullable {
            mapValue
        }

        assertEquals(expected, result)
    }

    @Test
    fun `mapNullable on Error should call transform with nonnull data and return Error`() {
        val error = Throwable()
        val mapValue = 5
        val expected = DataState.Error(error, mapValue)
        val dataState = DataState.Error(error, "Error")

        val result = dataState.mapNullable {
            mapValue
        }

        assertEquals(expected, result)
    }

    @Test
    fun `mapNullable on NoNetwork should call transform with null data and return NoNetwork`() {
        val mapValue = 5
        val expected = DataState.NoNetwork(mapValue)
        val dataState = DataState.NoNetwork(null)

        val result = dataState.mapNullable {
            mapValue
        }

        assertEquals(expected, result)
    }

    @Test
    fun `mapNullable on NoNetwork should call transform with nonnull data and return NoNetwork`() {
        val mapValue = 5
        val expected = DataState.NoNetwork(5)
        val dataState = DataState.NoNetwork("NoNetwork")

        val result = dataState.mapNullable {
            mapValue
        }

        assertEquals(expected, result)
    }

    @Test
    fun `takeUtilLoaded should complete after a Loaded state is emitted`() = runTest {
        val mutableStateFlow = MutableStateFlow<DataState<Unit>>(DataState.Loading)
        mutableStateFlow
            .takeUntilLoaded()
            .test {
                assertEquals(DataState.Loading, awaitItem())
                mutableStateFlow.value = DataState.NoNetwork(Unit)
                assertEquals(DataState.NoNetwork(Unit), awaitItem())
                mutableStateFlow.value = DataState.Loaded(Unit)
                assertEquals(DataState.Loaded(Unit), awaitItem())
                awaitComplete()
            }
    }

    @Test
    fun `updateToPendingOrLoading should change the DataState to Pending when data is present`() {
        val mutableStateFlow = MutableStateFlow<DataState<Unit>>(DataState.Loaded(Unit))

        mutableStateFlow.updateToPendingOrLoading()

        assertEquals(DataState.Pending(Unit), mutableStateFlow.value)
    }

    @Test
    fun `updateToPendingOrLoading should change the DataState to Loading when data is absent`() {
        val mutableStateFlow = MutableStateFlow<DataState<Unit>>(DataState.Error(Throwable("Fail")))

        mutableStateFlow.updateToPendingOrLoading()

        assertEquals(DataState.Loading, mutableStateFlow.value)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `combineDataStates should return an empty Error when the first dataState is Error without data`() {
        val throwable = Throwable("Fail")
        val dataState1 = DataState.Error<String>(throwable)
        val dataState2 = DataState.Loaded(5)

        val result = combineDataStates(
            dataState1 = dataState1,
            dataState2 = dataState2,
        ) { data1, data2 ->
            data1 to data2
        }

        assertEquals(DataState.Error<Pair<String, Int>>(throwable), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `combineDataStates should return a populated Error when the first dataState is Error with data`() {
        val throwable = Throwable("Fail")
        val dataState1 = DataState.Error(throwable, "data")
        val dataState2 = DataState.Loaded(5)

        val result = combineDataStates(
            dataState1 = dataState1,
            dataState2 = dataState2,
        ) { data1, data2 ->
            data1 to data2
        }

        assertEquals(DataState.Error(throwable, "data" to 5), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `combineDataStates should return an empty Error when the second dataState is Error without data`() {
        val throwable = Throwable("Fail")
        val dataState1 = DataState.Loaded(5)
        val dataState2 = DataState.Error<String>(throwable)

        val result = combineDataStates(
            dataState1 = dataState1,
            dataState2 = dataState2,
        ) { data1, data2 ->
            data1 to data2
        }

        assertEquals(DataState.Error<Pair<Int, String>>(throwable), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `combineDataStates should return a populated Error when the second dataState is Error with data`() {
        val throwable = Throwable("Fail")
        val dataState1 = DataState.Loaded(5)
        val dataState2 = DataState.Error(throwable, "data")

        val result = combineDataStates(
            dataState1 = dataState1,
            dataState2 = dataState2,
        ) { data1, data2 ->
            data1 to data2
        }

        assertEquals(DataState.Error(throwable, 5 to "data"), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `combineDataStates should return an empty NoNetwork when the first dataState is NoNetwork without data`() {
        val dataState1 = DataState.NoNetwork<Int>()
        val dataState2 = DataState.Loaded("data")

        val result = combineDataStates(
            dataState1 = dataState1,
            dataState2 = dataState2,
        ) { data1, data2 ->
            data1 to data2
        }

        assertEquals(DataState.NoNetwork<Pair<Int, String>>(), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `combineDataStates should return a populated NoNetwork when the first dataState is NoNetwork with data`() {
        val dataState1 = DataState.NoNetwork(5)
        val dataState2 = DataState.Loaded("data")

        val result = combineDataStates(
            dataState1 = dataState1,
            dataState2 = dataState2,
        ) { data1, data2 ->
            data1 to data2
        }

        assertEquals(DataState.NoNetwork(5 to "data"), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `combineDataStates should return an empty NoNetwork when the second dataState is NoNetwork without data`() {
        val dataState1 = DataState.Loaded("data")
        val dataState2 = DataState.NoNetwork<Int>()

        val result = combineDataStates(
            dataState1 = dataState1,
            dataState2 = dataState2,
        ) { data1, data2 ->
            data1 to data2
        }

        assertEquals(DataState.NoNetwork<Pair<String, Int>>(), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `combineDataStates should return a populated NoNetwork when the second dataState is NoNetwork with data`() {
        val dataState1 = DataState.Loaded("data")
        val dataState2 = DataState.NoNetwork(5)

        val result = combineDataStates(
            dataState1 = dataState1,
            dataState2 = dataState2,
        ) { data1, data2 ->
            data1 to data2
        }

        assertEquals(DataState.NoNetwork("data" to 5), result)
    }

    @Test
    fun `combineDataStates should return Loading when the first dataState is Loading`() {
        val dataState1: DataState<Int> = DataState.Loading
        val dataState2 = DataState.Loaded("data")

        val result = combineDataStates(
            dataState1 = dataState1,
            dataState2 = dataState2,
        ) { data1, data2 ->
            data1 to data2
        }

        assertEquals(DataState.Loading, result)
    }

    @Test
    fun `combineDataStates should return Loading when the second dataState is Loading`() {
        val dataState1 = DataState.Loaded("data")
        val dataState2: DataState<Int> = DataState.Loading

        val result = combineDataStates(
            dataState1 = dataState1,
            dataState2 = dataState2,
        ) { data1, data2 ->
            data1 to data2
        }

        assertEquals(DataState.Loading, result)
    }

    @Test
    fun `combineDataStates should return Pending when the first dataState is Pending`() {
        val dataState1 = DataState.Pending(5)
        val dataState2 = DataState.Loaded("data")

        val result = combineDataStates(
            dataState1 = dataState1,
            dataState2 = dataState2,
        ) { data1, data2 ->
            data1 to data2
        }

        assertEquals(DataState.Pending(5 to "data"), result)
    }

    @Test
    fun `combineDataStates should return Pending when the second dataState is Pending`() {
        val dataState1 = DataState.Loaded("data")
        val dataState2 = DataState.Pending(5)

        val result = combineDataStates(
            dataState1 = dataState1,
            dataState2 = dataState2,
        ) { data1, data2 ->
            data1 to data2
        }

        assertEquals(DataState.Pending("data" to 5), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `combineDataStates should return Pending when both dataStates are Pending with null data`() {
        val dataState1: DataState<String?> = DataState.Pending(null)
        val dataState2: DataState<Int?> = DataState.Pending(null)

        val result = combineDataStates(
            dataState1 = dataState1,
            dataState2 = dataState2,
        ) { data1, data2 ->
            data1 to data2
        }

        assertEquals(DataState.Pending<Pair<String?, Int?>>(null to null), result)
    }

    @Test
    fun `combineDataStates should return Loaded when the both dataStates are Loaded`() {
        val dataState1 = DataState.Loaded("data")
        val dataState2 = DataState.Loaded(5)

        val result = combineDataStates(
            dataState1 = dataState1,
            dataState2 = dataState2,
        ) { data1, data2 ->
            data1 to data2
        }

        assertEquals(DataState.Loaded("data" to 5), result)
    }

    @Test
    fun `combineDataStates should return Loaded when both dataStates are Loaded with null data`() {
        val dataState1: DataState<String?> = DataState.Loaded(null)
        val dataState2: DataState<Int?> = DataState.Loaded(null)

        val result = combineDataStates(
            dataState1 = dataState1,
            dataState2 = dataState2,
        ) { data1, data2 ->
            data1 to data2
        }

        assertEquals(DataState.Loaded<Pair<String?, Int?>>(null to null), result)
    }
}
