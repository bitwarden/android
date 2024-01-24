package com.x8bit.bitwarden.ui.vault.feature.attachments

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.NavigationIcon
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.LocalIntentManager
import com.x8bit.bitwarden.ui.vault.feature.attachments.handlers.AttachmentsHandlers

/**
 * Displays the attachments screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentsScreen(
    viewModel: AttachmentsViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val attachmentsHandlers = remember(viewModel) { AttachmentsHandlers.create(viewModel) }
    val fileChooserLauncher = intentManager.launchActivityForResult { activityResult ->
        intentManager.getFileDataFromActivityResult(activityResult)?.let {
            attachmentsHandlers.onFileChoose(it)
        }
    }
    val context = LocalContext.current
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            AttachmentsEvent.NavigateBack -> onNavigateBack()

            AttachmentsEvent.ShowChooserSheet -> {
                fileChooserLauncher.launch(
                    intentManager.createFileChooserIntent(withCameraIntents = false),
                )
            }

            is AttachmentsEvent.ShowToast -> {
                Toast
                    .makeText(context, event.message(context.resources), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.attachments),
                scrollBehavior = scrollBehavior,
                navigationIcon = NavigationIcon(
                    navigationIcon = painterResource(id = R.drawable.ic_back),
                    navigationIconContentDescription = stringResource(id = R.string.back),
                    onNavigationIconClick = attachmentsHandlers.onBackClick,
                ),
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.save),
                        onClick = attachmentsHandlers.onSaveClick,
                    )
                },
            )
        },
    ) { innerPadding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when (val viewState = state.viewState) {
            is AttachmentsState.ViewState.Content -> AttachmentsContent(
                viewState = viewState,
                attachmentsHandlers = attachmentsHandlers,
                modifier = modifier,
            )

            is AttachmentsState.ViewState.Error -> BitwardenErrorContent(
                message = viewState.message(),
                modifier = modifier,
            )

            AttachmentsState.ViewState.Loading -> BitwardenLoadingContent(
                modifier = modifier,
            )
        }
    }
}
