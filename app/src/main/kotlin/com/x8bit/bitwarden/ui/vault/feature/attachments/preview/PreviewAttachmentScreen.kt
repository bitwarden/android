package com.x8bit.bitwarden.ui.vault.feature.attachments.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.button.model.BitwardenButtonData
import com.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.preview.ImagePreviewContent
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText

/**
 * Displays the preview attachment screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewAttachmentScreen(
    viewModel: PreviewAttachmentViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val fileChooserLauncher = intentManager.getActivityResultLauncher { activityResult ->
        val action = intentManager
            .getFileDataFromActivityResult(activityResult)
            ?.let { PreviewAttachmentAction.AttachmentFileLocationReceive(it.uri) }
            ?: PreviewAttachmentAction.NoAttachmentFileLocationReceive
        viewModel.trySendAction(action)
    }
    val snackbarHostState = rememberBitwardenSnackbarHostState()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            PreviewAttachmentEvent.NavigateBack -> onNavigateBack()
            is PreviewAttachmentEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.data)
            is PreviewAttachmentEvent.NavigateToSelectAttachmentSaveLocation -> {
                fileChooserLauncher.launch(intentManager.createDocumentIntent(event.fileName))
            }
        }
    }

    PreviewAttachmentDialogs(
        dialogState = state.dialogState,
        onDismissRequest = { viewModel.trySendAction(PreviewAttachmentAction.DismissDialog) },
        onCloseClick = { viewModel.trySendAction(PreviewAttachmentAction.CloseClick) },
    )

    BitwardenScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BitwardenTopAppBar(
                title = state.fileName,
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                navigationIcon = NavigationIcon(
                    navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
                    navigationIconContentDescription = stringResource(id = BitwardenString.back),
                    onNavigationIconClick = {
                        viewModel.trySendAction(PreviewAttachmentAction.BackClick)
                    },
                ),
                actions = {
                    BitwardenStandardIconButton(
                        vectorIconRes = BitwardenDrawable.ic_download,
                        contentDescription = stringResource(id = BitwardenString.download),
                        onClick = {
                            viewModel.trySendAction(PreviewAttachmentAction.DownloadClick)
                        },
                        modifier = Modifier.testTag("DownloadButton"),
                    )
                },
            )
        },
        snackbarHost = {
            BitwardenSnackbarHost(bitwardenHostState = snackbarHostState)
        },
    ) {
        when (val viewState = state.viewState) {
            is PreviewAttachmentState.ViewState.Content -> {
                ImagePreviewContent(
                    file = viewState.file,
                    onMissingFile = {
                        viewModel.trySendAction(PreviewAttachmentAction.FileMissing)
                    },
                    onLoaded = {
                        viewModel.trySendAction(PreviewAttachmentAction.BitmapRenderComplete)
                    },
                    onError = {
                        viewModel.trySendAction(PreviewAttachmentAction.BitmapRenderError)
                    },
                )
            }

            is PreviewAttachmentState.ViewState.Error -> BitwardenErrorContent(
                message = viewState.message(),
                illustrationData = IconData.Local(iconRes = viewState.illustrationRes),
                buttonData = BitwardenButtonData(
                    label = BitwardenString.download_file.asText(),
                    icon = rememberVectorPainter(id = BitwardenDrawable.ic_download),
                    onClick = { viewModel.trySendAction(PreviewAttachmentAction.DownloadClick) },
                ),
                modifier = Modifier.fillMaxSize(),
            )

            is PreviewAttachmentState.ViewState.Loading -> BitwardenLoadingContent(
                text = viewState.message(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PreviewAttachmentDialogs(
    dialogState: PreviewAttachmentState.DialogState?,
    onDismissRequest: () -> Unit,
    onCloseClick: () -> Unit,
) {
    when (dialogState) {
        is PreviewAttachmentState.DialogState.Error -> {
            BitwardenBasicDialog(
                title = dialogState.title?.invoke(),
                message = dialogState.message(),
                onDismissRequest = onDismissRequest,
            )
        }

        is PreviewAttachmentState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = dialogState.message())
        }

        PreviewAttachmentState.DialogState.PreviewUnavailable -> {
            BitwardenBasicDialog(
                title = stringResource(id = BitwardenString.preview_unavailable),
                message = stringResource(
                    id = BitwardenString
                        .bitwarden_could_not_decrypt_this_file_so_the_preview_cannot_be_displayed,
                ),
                confirmButtonLabel = stringResource(id = BitwardenString.close),
                onDismissRequest = onCloseClick,
            )
        }

        null -> Unit
    }
}
