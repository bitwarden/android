package com.x8bit.bitwarden.data.platform.repository.util

import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DataStateExtensionsTest {

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
}
