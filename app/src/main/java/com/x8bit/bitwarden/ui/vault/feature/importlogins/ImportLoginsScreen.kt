package com.x8bit.bitwarden.ui.vault.feature.importlogins

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.bitwardenBoldSpanStyle
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.base.util.toAnnotatedString
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.x8bit.bitwarden.ui.platform.components.bottomsheet.BitwardenModalBottomSheet
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenContentCard
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenFullScreenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.model.ContentBlockData
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.importlogins.components.ImportLoginsInstructionStep
import com.x8bit.bitwarden.ui.vault.feature.importlogins.handlers.ImportLoginHandler
import com.x8bit.bitwarden.ui.vault.feature.importlogins.handlers.rememberImportLoginHandler
import kotlinx.collections.immutable.persistentListOf

private const val IMPORT_HELP_URL = "https://bitwarden.com/help/import-data/"

/**
 * Top level component for the import logins screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportLoginsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImportLoginsViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val handler = rememberImportLoginHandler(viewModel = viewModel)

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            ImportLoginsEvent.NavigateBack -> onNavigateBack()
            ImportLoginsEvent.OpenHelpLink -> {
                intentManager.startCustomTabsActivity(IMPORT_HELP_URL.toUri())
            }
        }
    }

    ImportLoginsDialogContent(state = state, handler = handler)

    BackHandler(enabled = true) {
        state.viewState.backAction?.let {
            viewModel.trySendAction(it)
        }
    }

    BitwardenModalBottomSheet(
        showBottomSheet = state.showBottomSheet,
        sheetTitle = stringResource(R.string.bitwarden_tools),
        onDismiss = handler.onSuccessfulSyncAcknowledged,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.statusBarsPadding(),
    ) { animatedOnDismiss ->
        ImportLoginsSuccessBottomSheetContent(
            onCompleteImportLogins = animatedOnDismiss,
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenFullScreenLoadingContent(
        modifier = Modifier.fillMaxSize(),
        showLoadingState = state.isVaultSyncing,
        message = stringResource(R.string.syncing_logins_loading_message),
    ) {
        BitwardenScaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                BitwardenTopAppBar(
                    title = stringResource(R.string.import_logins),
                    navigationIcon = NavigationIcon(
                        navigationIcon = rememberVectorPainter(R.drawable.ic_close),
                        onNavigationIconClick = handler.onCloseClick,
                        navigationIconContentDescription = stringResource(R.string.close),
                    ),
                    scrollBehavior = scrollBehavior,
                )
            },
        ) {
            Crossfade(
                targetState = state.viewState,
                label = "CrossfadeBetweenViewStates",
                modifier = Modifier.fillMaxSize(),
            ) { viewState ->
                when (viewState) {
                    ImportLoginsState.ViewState.InitialContent -> {
                        InitialImportLoginsContent(
                            onGetStartedClick = handler.onGetStartedClick,
                            onImportLaterClick = handler.onImportLaterClick,
                        )
                    }

                    ImportLoginsState.ViewState.ImportStepOne -> {
                        ImportLoginsStepOneContent(
                            onBackClick = handler.onMoveToInitialContent,
                            onContinueClick = handler.onMoveToStepTwo,
                            onHelpClick = handler.onHelpClick,
                        )
                    }

                    ImportLoginsState.ViewState.ImportStepTwo -> {
                        ImportLoginsStepTwoContent(
                            vaultUrl = state.currentWebVaultUrl,
                            onBackClick = handler.onMoveToStepOne,
                            onContinueClick = handler.onMoveToStepThree,
                            onHelpClick = handler.onHelpClick,
                        )
                    }

                    ImportLoginsState.ViewState.ImportStepThree -> {
                        ImportLoginsStepThreeContent(
                            onBackClick = handler.onMoveToStepTwo,
                            onContinueClick = handler.onMoveToSyncInProgress,
                            onHelpClick = handler.onHelpClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportLoginsDialogContent(
    state: ImportLoginsState,
    handler: ImportLoginHandler,
) {
    val confirmButtonText = stringResource(R.string.confirm)
    val dismissButtonText = stringResource(R.string.cancel)
    when (val dialogState = state.dialogState) {
        ImportLoginsState.DialogState.GetStarted -> {
            BitwardenTwoButtonDialog(
                title = dialogState.title?.invoke(),
                message = dialogState.message(),
                onDismissRequest = handler.onDismissDialog,
                confirmButtonText = confirmButtonText,
                dismissButtonText = dismissButtonText,
                onConfirmClick = handler.onConfirmGetStarted,
                onDismissClick = handler.onDismissDialog,
            )
        }

        ImportLoginsState.DialogState.ImportLater -> {
            BitwardenTwoButtonDialog(
                title = dialogState.title?.invoke(),
                message = dialogState.message(),
                onDismissRequest = handler.onDismissDialog,
                confirmButtonText = confirmButtonText,
                dismissButtonText = dismissButtonText,
                onConfirmClick = handler.onConfirmImportLater,
                onDismissClick = handler.onDismissDialog,
            )
        }

        is ImportLoginsState.DialogState.Error -> {
            BitwardenTwoButtonDialog(
                title = dialogState.title?.invoke(),
                message = dialogState.message(),
                onDismissRequest = handler.onDismissDialog,
                confirmButtonText = stringResource(R.string.try_again),
                dismissButtonText = stringResource(R.string.import_logins_later),
                onConfirmClick = handler.onRetrySync,
                onDismissClick = handler.onFailedSyncAcknowledged,
            )
        }

        null -> Unit
    }
}

@Composable
private fun InitialImportLoginsContent(
    onGetStartedClick: () -> Unit,
    onImportLaterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = rememberVectorPainter(R.drawable.img_import_logins),
            contentDescription = null,
            modifier = Modifier
                .standardHorizontalMargin()
                .size(124.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.give_your_vault_a_head_start),
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.standardHorizontalMargin(),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(
                R.string.from_your_computer_follow_these_instructions_to_export_saved_passwords,
            ),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.standardHorizontalMargin(),
        )
        Spacer(Modifier.height(24.dp))
        BitwardenFilledButton(
            label = stringResource(R.string.get_started),
            onClick = onGetStartedClick,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        BitwardenOutlinedButton(
            label = stringResource(R.string.import_logins_later),
            onClick = onImportLaterClick,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Suppress("MaxLineLength")
@Composable
private fun ImportLoginsStepOneContent(
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val instruction1 =
        R.string.on_your_computer_log_in_to_your_current_browser_or_password_manager.toAnnotatedString()
    val instruction2 =
        R.string.export_your_passwords_this_option_is_usually_found_in_your_settings.toAnnotatedString()
    val instruction3 =
        R.string.save_the_exported_file_somewhere_on_your_computer_you_can_find_easily
            .toAnnotatedString()
    ImportLoginsInstructionStep(
        stepText = stringResource(R.string.step_1_of_3),
        stepTitle = stringResource(R.string.export_your_saved_logins),
        instructions = persistentListOf(
            ContentBlockData(
                iconVectorResource = R.drawable.ic_number1,
                headerText = instruction1,
                subtitleText = null,
            ),
            ContentBlockData(
                iconVectorResource = R.drawable.ic_number2,
                headerText = instruction2,
                subtitleText = null,
            ),
            ContentBlockData(
                iconVectorResource = R.drawable.ic_number3,
                headerText = instruction3,
                subtitleText = stringResource(R.string.delete_this_file_after_import_is_complete)
                    .toAnnotatedString(),
            ),
        ),
        onBackClick = onBackClick,
        onContinueClick = onContinueClick,
        onHelpClick = onHelpClick,
        modifier = modifier,
    )
}

@Composable
private fun ImportLoginsStepTwoContent(
    vaultUrl: String,
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val instruction1 =
        R.string.on_your_computer_open_a_new_browser_tab_and_go_to_vault_bitwarden_com
            .toAnnotatedString(
                vaultUrl,
            )
    val instruction2Text = stringResource(R.string.log_in_to_the_bitwarden_web_app)
    val instruction2 = buildAnnotatedString {
        withStyle(bitwardenBoldSpanStyle) {
            append(instruction2Text)
        }
    }
    ImportLoginsInstructionStep(
        stepText = stringResource(R.string.step_2_of_3),
        stepTitle = stringResource(R.string.log_in_to_bitwarden),
        instructions = persistentListOf(
            ContentBlockData(
                iconVectorResource = R.drawable.ic_number1,
                headerText = instruction1,
                subtitleText = null,
            ),
            ContentBlockData(
                iconVectorResource = R.drawable.ic_number2,
                headerText = instruction2,
                subtitleText = null,
            ),
        ),
        onBackClick = onBackClick,
        onContinueClick = onContinueClick,
        onHelpClick = onHelpClick,
        modifier = modifier,
    )
}

@Suppress("LongMethod", "MaxLineLength")
@Composable
private fun ImportLoginsStepThreeContent(
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val instruction1 =
        R.string.in_the_bitwarden_navigation_find_the_tools_option_and_select_import_data
            .toAnnotatedString()
    val instruction2 = R.string.fill_out_the_form_and_import_your_saved_password_file
        .toAnnotatedString()
    val instruction3 = R.string.select_import_data_in_the_web_app_then_done_to_finish_syncing
        .toAnnotatedString()
    val instruction4 = R.string.for_your_security_be_sure_to_delete_your_saved_password_file
        .toAnnotatedString()
    ImportLoginsInstructionStep(
        stepText = stringResource(R.string.step_3_of_3),
        stepTitle = stringResource(R.string.import_logins_to_bitwarden),
        instructions = persistentListOf(
            ContentBlockData(
                iconVectorResource = R.drawable.ic_number1,
                headerText = instruction1,
                subtitleText = null,
            ),
            ContentBlockData(
                iconVectorResource = R.drawable.ic_number2,
                headerText = instruction2,
                subtitleText = null,
            ),
            ContentBlockData(
                iconVectorResource = R.drawable.ic_number3,
                headerText = instruction3,
                subtitleText = null,
            ),
            ContentBlockData(
                iconVectorResource = R.drawable.ic_number4,
                headerText = instruction4,
                subtitleText = null,
            ),
        ),
        onBackClick = onBackClick,
        ctaText = stringResource(R.string.done_text),
        onContinueClick = onContinueClick,
        onHelpClick = onHelpClick,
        modifier = modifier,
    )
}

@Suppress("LongMethod")
@Composable
private fun ImportLoginsSuccessBottomSheetContent(
    onCompleteImportLogins: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Image(
            painter = rememberVectorPainter(R.drawable.img_secure_devices),
            contentDescription = null,
            modifier = Modifier
                .standardHorizontalMargin()
                .size(124.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.import_successful),
            style = BitwardenTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.standardHorizontalMargin(),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(
                R.string.manage_your_logins_from_anywhere_with_bitwarden_tools,
            ),
            style = BitwardenTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.standardHorizontalMargin(),
        )
        Spacer(Modifier.height(24.dp))
        BitwardenContentCard(
            contentItems = persistentListOf(
                ContentBlockData(
                    headerText = stringResource(R.string.download_the_browser_extension),
                    subtitleText = stringResource(
                        R.string.go_to_bitwarden_com_download_to_integrate_bitwarden_into_browser,
                    ),
                    iconVectorResource = R.drawable.ic_puzzle,
                ),
                ContentBlockData(
                    headerText = stringResource(R.string.use_the_web_app),
                    subtitleText = stringResource(
                        R.string.log_in_at_bitwarden_com_to_easily_manage_your_account,
                    ),
                    iconVectorResource = R.drawable.ic_desktop,
                ),
                ContentBlockData(
                    headerText = stringResource(R.string.autofill_passwords),
                    subtitleText = stringResource(R.string.set_up_autofill_on_all_your_devices),
                    iconVectorResource = R.drawable.ic_shield,
                ),
            ),
            modifier = Modifier.standardHorizontalMargin(),
            contentSubtitleTextStyle = BitwardenTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(24.dp))
        BitwardenFilledButton(
            label = stringResource(R.string.got_it),
            onClick = onCompleteImportLogins,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ImportLoginsInitialContent_preview() {
    BitwardenTheme {
        Column(
            modifier = Modifier.background(
                BitwardenTheme.colorScheme.background.primary,
            ),
        ) {
            InitialImportLoginsContent(
                onGetStartedClick = {},
                onImportLaterClick = {},
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ImportLoginsScreenDialog_preview(
    @PreviewParameter(ImportLoginsDialogContentPreviewProvider::class) state: ImportLoginsState,
) {
    BitwardenTheme {
        Column(
            modifier = Modifier.background(
                BitwardenTheme.colorScheme.background.primary,
            ),
        ) {
            ImportLoginsDialogContent(
                state = state,
                handler = ImportLoginHandler(
                    onDismissDialog = {},
                    onConfirmGetStarted = {},
                    onConfirmImportLater = {},
                    onCloseClick = {},
                    onGetStartedClick = {},
                    onImportLaterClick = {},
                    onHelpClick = {},
                    onMoveToInitialContent = {},
                    onMoveToStepOne = {},
                    onMoveToStepTwo = {},
                    onMoveToStepThree = {},
                    onMoveToSyncInProgress = {},
                    onRetrySync = {},
                    onFailedSyncAcknowledged = {},
                    onSuccessfulSyncAcknowledged = {},
                ),
            )
            InitialImportLoginsContent(
                onGetStartedClick = {},
                onImportLaterClick = {},
            )
        }
    }
}

@OmitFromCoverage
private class ImportLoginsDialogContentPreviewProvider :
    PreviewParameterProvider<ImportLoginsState> {
    override val values: Sequence<ImportLoginsState>
        get() = sequenceOf(
            ImportLoginsState(
                dialogState = ImportLoginsState.DialogState.GetStarted,
                viewState = ImportLoginsState.ViewState.InitialContent,
                isVaultSyncing = false,
                showBottomSheet = false,
                currentWebVaultUrl = "vault.bitwarden.com",
                snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
            ),
            ImportLoginsState(
                dialogState = ImportLoginsState.DialogState.ImportLater,
                viewState = ImportLoginsState.ViewState.InitialContent,
                isVaultSyncing = false,
                showBottomSheet = false,
                currentWebVaultUrl = "vault.bitwarden.com",
                snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
            ),
        )
}
