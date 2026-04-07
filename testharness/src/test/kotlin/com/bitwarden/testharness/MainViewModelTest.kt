package com.bitwarden.testharness

import app.cash.turbine.test
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MainViewModelTest {

    private lateinit var viewModel: MainViewModel

    @BeforeEach
    fun setup() {
        viewModel = MainViewModel()
    }

    @Test
    fun `initial state has default theme`() = runTest {
        viewModel.stateFlow.test {
            val state = awaitItem()
            assertEquals(AppTheme.DEFAULT, state.theme)
        }
    }
}
