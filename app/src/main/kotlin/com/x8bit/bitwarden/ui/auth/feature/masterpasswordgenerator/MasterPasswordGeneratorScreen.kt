package com.x8bit.bitwarden.ui.auth.feature.masterpasswordgenerator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.text.BitwardenClickableText
import com.bitwarden.ui.platform.components.util.nonLetterColorVisualTransformation
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Top level composable for the master password generator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun MasterPasswordGeneratorScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPreventLockout: () -> Unit,
    onNavigateBackWithPassword: () -> Unit,
    viewModel: MasterPasswordGeneratorViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = rememberBitwardenSnackbarHostState()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            MasterPasswordGeneratorEvent.NavigateBack -> onNavigateBack()
            MasterPasswordGeneratorEvent.NavigateToPreventLockout -> onNavigateToPreventLockout()
            is MasterPasswordGeneratorEvent.ShowSnackbar -> {
                snackbarHostState.showSnackbar(
                    snackbarData = BitwardenSnackbarData(message = event.text),
                    duration = SnackbarDuration.Short,
                )
            }

            is MasterPasswordGeneratorEvent.NavigateBackToRegistration -> {
                onNavigateBackWithPassword()
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MasterPasswordGeneratorTopBar(
                scrollBehavior = scrollBehavior,
                onBackClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(MasterPasswordGeneratorAction.BackClickAction)
                    }
                },
                onSaveClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(
                            MasterPasswordGeneratorAction.SavePasswordClickAction,
                        )
                    }
                },
            )
        },
        snackbarHost = {
            BitwardenSnackbarHost(bitwardenHostState = snackbarHostState)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            MasterPasswordGeneratorContent(
                generatedPassword = state.generatedPassword,
                onGenerateNewPassword = remember(viewModel) {
                    {
                        viewModel.trySendAction(
                            MasterPasswordGeneratorAction.GeneratePasswordClickAction,
                        )
                    }
                },
                onLearnToPreventLockout = remember(viewModel) {
                    {
                        viewModel.trySendAction(
                            MasterPasswordGeneratorAction.PreventLockoutClickAction,
                        )
                    }
                },
                modifier = Modifier.standardHorizontalMargin(),
            )
            Spacer(modifier = Modifier.height(height = 16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun MasterPasswordGeneratorContent(
    generatedPassword: String,
    onGenerateNewPassword: () -> Unit,
    onLearnToPreventLockout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Spacer(modifier = Modifier.height(height = 12.dp))
        BitwardenTextField(
            label = null,
            value = generatedPassword,
            onValueChange = {},
            readOnly = true,
            shouldAddCustomLineBreaks = true,
            textStyle = BitwardenTheme.typography.sensitiveInfoSmall,
            visualTransformation = nonLetterColorVisualTransformation(),
            cardStyle = CardStyle.Full,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))
        BitwardenFilledButton(
            label = stringResource(BitwardenString.generate_button_label),
            onClick = onGenerateNewPassword,
            icon = rememberVectorPainter(id = BitwardenDrawable.ic_generate),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(
                BitwardenString.write_this_password_down_and_keep_it_somewhere_safe,
            ),
            style = BitwardenTheme.typography.bodySmall,
            color = BitwardenTheme.colorScheme.text.primary,
        )
        BitwardenClickableText(
            label = stringResource(
                BitwardenString.learn_about_other_ways_to_prevent_account_lockout,
            ),
            style = BitwardenTheme.typography.labelMedium,
            onClick = onLearnToPreventLockout,
            innerPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MasterPasswordGeneratorTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    BitwardenTopAppBar(
        title = stringResource(BitwardenString.generate_master_password),
        scrollBehavior = scrollBehavior,
        navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
        navigationIconContentDescription = stringResource(id = BitwardenString.back),
        onNavigationIconClick = onBackClick,
        actions = {
            BitwardenTextButton(
                label = stringResource(id = BitwardenString.save),
                onClick = onSaveClick,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun MasterPasswordGeneratorTopBarPreview() {
    BitwardenTheme {
        MasterPasswordGeneratorTopBar(
            scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
            onBackClick = { },
            onSaveClick = { },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MasterPasswordGeneratorContentPreview() {
    BitwardenTheme {
        MasterPasswordGeneratorContent(
            generatedPassword = "really-secure-password",
            onGenerateNewPassword = { },
            onLearnToPreventLockout = { },
            modifier = Modifier.padding(16.dp),
        )
    }
}
