package com.x8bit.bitwarden.ui.auth.feature.masterpasswordgenerator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButtonWithIcon
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.text.BitwardenClickableText
import com.x8bit.bitwarden.ui.platform.components.util.nonLetterColorVisualTransformation
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.theme.LocalNonMaterialTypography

/**
 * Top level composable for the master password generator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun MasterPasswordGeneratorScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPreventLockout: () -> Unit,
    viewModel: MasterPasswordGeneratorViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources
    val snackbarHostState = remember {
        SnackbarHostState()
    }
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            MasterPasswordGeneratorEvent.NavigateBack -> onNavigateBack()
            MasterPasswordGeneratorEvent.NavigateToPreventLockout -> onNavigateToPreventLockout()
            is MasterPasswordGeneratorEvent.ShowSnackbar -> {
                snackbarHostState.showSnackbar(
                    message = event.text.toString(resources),
                    duration = SnackbarDuration.Short,
                )
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
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
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
        Text(
            text = stringResource(R.string.generate_master_password),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(24.dp))
        BitwardenTextField(
            label = "",
            value = generatedPassword,
            onValueChange = {},
            readOnly = true,
            shouldAddCustomLineBreaks = true,
            textStyle = LocalNonMaterialTypography.current.sensitiveInfoSmall,
            visualTransformation = nonLetterColorVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))
        BitwardenFilledButtonWithIcon(
            label = stringResource(R.string.generate_button_label),
            onClick = onGenerateNewPassword,
            icon = rememberVectorPainter(id = R.drawable.ic_generator),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.write_this_password_down_and_keep_it_somewhere_safe),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BitwardenClickableText(
            label = stringResource(R.string.learn_about_other_ways_to_prevent_account_lockout),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            onClick = onLearnToPreventLockout,
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
        title = "",
        scrollBehavior = scrollBehavior,
        navigationIcon = rememberVectorPainter(id = R.drawable.ic_back),
        navigationIconContentDescription = stringResource(id = R.string.back),
        onNavigationIconClick = onBackClick,
        actions = {
            BitwardenTextButton(
                label = stringResource(id = R.string.save),
                labelTextColor = MaterialTheme.colorScheme.primary,
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
        Surface {
            MasterPasswordGeneratorTopBar(
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                onBackClick = { },
                onSaveClick = { },
            )
        }
    }
}

@Preview
@Composable
private fun MasterPasswordGeneratorContentPreview() {
    BitwardenTheme {
        Surface {
            MasterPasswordGeneratorContent(
                generatedPassword = "really-secure-password",
                onGenerateNewPassword = { },
                onLearnToPreventLockout = { },
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
