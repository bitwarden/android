package com.x8bit.bitwarden.ui.platform.feature.search

import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchScreenTest : BaseComposeTest() {
    private val mutableEventFlow = bufferedMutableSharedFlow<SearchEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<SearchViewModel> {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }
    private val intentManager: IntentManager = mockk {
        every { shareText(any()) } just runs
    }

    private var onNavigateBackCalled = false
    private var onNavigateToEditSendId: String? = null

    @Before
    fun setup() {
        composeTestRule.setContent {
            SearchScreen(
                viewModel = viewModel,
                intentManager = intentManager,
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToEditSend = { onNavigateToEditSendId = it },
            )
        }
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(SearchEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToEditSend should call onNavigateToEditSend`() {
        val sendId = "sendId"
        mutableEventFlow.tryEmit(SearchEvent.NavigateToEditSend(sendId))
        assertEquals(sendId, onNavigateToEditSendId)
    }

    @Test
    fun `ShowShareSheet should call onNavigateBack`() {
        val sendUrl = "www.test.com"
        mutableEventFlow.tryEmit(SearchEvent.ShowShareSheet(sendUrl))
        verify {
            intentManager.shareText(sendUrl)
        }
    }
}

private val DEFAULT_STATE: SearchState = SearchState(
    searchType = SearchType.Vault.All,
)
