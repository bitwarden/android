package com.bitwarden.authenticator.ui.authenticator.feature.edititem

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.ui.authenticator.feature.edititem.model.EditItemData
import com.bitwarden.authenticator.ui.platform.base.util.EventsEffect
import com.bitwarden.authenticator.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.authenticator.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.authenticator.ui.platform.components.dialog.BasicDialogState
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.LoadingDialogState
import com.bitwarden.authenticator.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.authenticator.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.authenticator.ui.platform.components.field.BitwardenTextField
import com.bitwarden.authenticator.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.authenticator.ui.platform.components.icon.BitwardenIcon
import com.bitwarden.authenticator.ui.platform.components.model.IconData
import com.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.authenticator.ui.platform.theme.DEFAULT_FADE_TRANSITION_TIME_MS
import com.bitwarden.authenticator.ui.platform.theme.DEFAULT_STAY_TRANSITION_TIME_MS
import kotlinx.collections.immutable.toImmutableList

/**
 * Displays the edit authenticator item screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemScreen(
    viewModel: EditItemViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = { },
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val resources = context.resources

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            EditItemEvent.NavigateBack -> {
                onNavigateBack()
            }

            is EditItemEvent.ShowToast -> {
                Toast
                    .makeText(
                        context,
                        event.message(resources),
                        Toast.LENGTH_LONG
                    )
                    .show()
            }
        }
    }

    EditItemDialogs(
        dialogState = state.dialog,
        onDismissRequest = { viewModel.trySendAction(EditItemAction.DismissDialog) }
    )

    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(
                    id = R.string.edit_item,
                ),
                scrollBehavior = scrollBehavior,
                navigationIcon = painterResource(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(EditItemAction.CancelClick)
                    }
                },
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.save),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(EditItemAction.SaveClick) }
                        },
                        modifier = Modifier.semantics { testTag = "SaveButton" },
                    )
                }
            )
        },
        floatingActionButtonPosition = FabPosition.EndOverlay,
    ) { innerPadding ->
        when (val viewState = state.viewState) {
            is EditItemState.ViewState.Content -> {
                EditItemContent(
                    modifier = Modifier
                        .imePadding()
                        .padding(innerPadding),
                    viewState = viewState,
                    onIssuerNameTextChange = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                EditItemAction.IssuerNameTextChange(it)
                            )
                        }
                    },
                    onUsernameTextChange = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                EditItemAction.UsernameTextChange(it)
                            )
                        }
                    },
                    onTypeOptionClicked = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                EditItemAction.TypeOptionClick(it)
                            )
                        }
                    },
                    onTotpCodeTextChange = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                EditItemAction.TotpCodeTextChange(it)
                            )
                        }
                    },
                    onAlgorithmOptionClicked = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                EditItemAction.AlgorithmOptionClick(it)
                            )
                        }
                    },
                    onRefreshPeriodOptionClicked = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                EditItemAction.RefreshPeriodOptionClick(it)
                            )
                        }
                    },
                    onNumberOfDigitsOptionClicked = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                EditItemAction.NumberOfDigitsOptionClick(it)
                            )
                        }
                    },
                    onExpandAdvancedOptionsClicked = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                EditItemAction.ExpandAdvancedOptionsClick
                            )
                        }
                    }
                )
            }

            is EditItemState.ViewState.Error -> {
                /*ItemErrorContent(state)*/
            }

            EditItemState.ViewState.Loading -> EditItemState.ViewState.Loading
        }
    }
}

@Composable
fun EditItemContent(
    modifier: Modifier = Modifier,
    viewState: EditItemState.ViewState.Content,
    onIssuerNameTextChange: (String) -> Unit = {},
    onUsernameTextChange: (String) -> Unit = {},
    onTypeOptionClicked: (AuthenticatorItemType) -> Unit = {},
    onTotpCodeTextChange: (String) -> Unit = {},
    onAlgorithmOptionClicked: (AuthenticatorItemAlgorithm) -> Unit = {},
    onRefreshPeriodOptionClicked: (AuthenticatorRefreshPeriodOption) -> Unit = {},
    onNumberOfDigitsOptionClicked: (VerificationCodeDigitsOption) -> Unit = {},
    onExpandAdvancedOptionsClicked: () -> Unit = {},
) {
    Column(modifier = modifier) {
        LazyColumn {
            item {
                BitwardenListHeaderText(
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    label = stringResource(id = R.string.information),
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                BitwardenTextField(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    label = stringResource(id = R.string.name),
                    value = viewState.itemData.issuer,
                    onValueChange = onIssuerNameTextChange,
                    singleLine = true,
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenPasswordField(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    label = stringResource(id = R.string.secret_key),
                    value = viewState.itemData.totpCode,
                    onValueChange = onTotpCodeTextChange,
                    singleLine = true,
                )
            }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    BitwardenTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        label = stringResource(id = R.string.username),
                        value = viewState.itemData.username.orEmpty(),
                        onValueChange = onUsernameTextChange,
                        singleLine = true,
                    )
                }
        }

        Spacer(modifier = Modifier.height(8.dp))

        AdvancedOptions(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            viewState = viewState,
            onExpandStateChange = onExpandAdvancedOptionsClicked,
            onAlgorithmOptionClicked = onAlgorithmOptionClicked,
            onTypeOptionClicked = onTypeOptionClicked,
            onRefreshPeriodOptionClicked = onRefreshPeriodOptionClicked,
            onNumberOfDigitsOptionClicked = onNumberOfDigitsOptionClicked
        )
    }
}

@Composable
private fun AdvancedOptions(
    modifier: Modifier = Modifier,
    viewState: EditItemState.ViewState.Content,
    onExpandStateChange: () -> Unit,
    onAlgorithmOptionClicked: (AuthenticatorItemAlgorithm) -> Unit,
    onTypeOptionClicked: (AuthenticatorItemType) -> Unit,
    onRefreshPeriodOptionClicked: (AuthenticatorRefreshPeriodOption) -> Unit,
    onNumberOfDigitsOptionClicked: (VerificationCodeDigitsOption) -> Unit,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(28.dp))
                .clickable(
                    indication = rememberRipple(
                        bounded = true,
                        color = MaterialTheme.colorScheme.primary,
                    ),
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onExpandStateChange()
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.advanced),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            BitwardenIcon(
                iconData = IconData.Local(
                    iconRes = if (viewState.isAdvancedOptionsExpanded) {
                        R.drawable.ic_chevron_up
                    } else {
                        R.drawable.ic_chevron_down
                    }
                ),
                contentDescription = stringResource(
                    id = R.string.collapse_advanced_options
                ),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        AnimatedVisibility(
            visible = viewState.isAdvancedOptionsExpanded,
            enter = fadeIn(tween(DEFAULT_FADE_TRANSITION_TIME_MS))
                + expandVertically(tween(DEFAULT_STAY_TRANSITION_TIME_MS)),
            exit = fadeOut(tween(DEFAULT_FADE_TRANSITION_TIME_MS))
                + shrinkVertically(tween(DEFAULT_STAY_TRANSITION_TIME_MS)),
        ) {
            LazyColumn {
                item {
                    val possibleTypeOptions = AuthenticatorItemType.entries
                    val typeOptionsWithStrings =
                        possibleTypeOptions.associateWith { it.name }
                    Spacer(modifier = Modifier.height(8.dp))
                    BitwardenMultiSelectButton(
                        modifier = Modifier
                            .fillMaxSize()
                            .semantics { testTag = "ItemTypePicker" },
                        label = stringResource(id = R.string.otp_authentication),
                        options = typeOptionsWithStrings.values.toImmutableList(),
                        selectedOption = viewState.itemData.type.name,
                        onOptionSelected = { selectedOption ->
                            val selectedOptionName = typeOptionsWithStrings
                                .entries
                                .first { it.value == selectedOption }
                                .key

                            onTypeOptionClicked(selectedOptionName)
                        },
                    )
                }

                item {
                    val possibleAlgorithmOptions = AuthenticatorItemAlgorithm.entries
                    val algorithmOptionsWithStrings =
                        possibleAlgorithmOptions.associateWith { it.name }
                    Spacer(Modifier.height(8.dp))
                    BitwardenMultiSelectButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        label = stringResource(id = R.string.algorithm),
                        options = algorithmOptionsWithStrings.values.toImmutableList(),
                        selectedOption = viewState.itemData.algorithm.name,
                        onOptionSelected = { selectedOption ->
                            val selectedOptionName = algorithmOptionsWithStrings
                                .entries
                                .first { it.value == selectedOption }
                                .key

                            onAlgorithmOptionClicked(selectedOptionName)
                        },
                    )
                }

                item {
                    val possibleRefreshPeriodOptions = AuthenticatorRefreshPeriodOption.entries
                    val refreshPeriodOptionsWithStrings = possibleRefreshPeriodOptions
                        .associateWith {
                            stringResource(
                                id = R.string.refresh_period_seconds,
                                it.seconds
                            )
                        }
                    Spacer(modifier = Modifier.height(8.dp))
                    BitwardenMultiSelectButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        label = stringResource(id = R.string.refresh_period),
                        options = refreshPeriodOptionsWithStrings.values.toImmutableList(),
                        selectedOption = stringResource(
                            id = R.string.refresh_period_seconds,
                            viewState.itemData.refreshPeriod.seconds,
                        ),
                        onOptionSelected = remember(viewState) {
                            { selectedOption ->
                                val selectedOptionName = refreshPeriodOptionsWithStrings
                                    .entries
                                    .first { it.value == selectedOption }
                                    .key

                                onRefreshPeriodOptionClicked(selectedOptionName)
                            }
                        }
                    )
                }

                item {
                    val possibleDigitOptions = VerificationCodeDigitsOption.entries
                    val digitOptionsWithStrings =
                        possibleDigitOptions.associateWith { it.length.toString() }
                    Spacer(modifier = Modifier.height(8.dp))
                    BitwardenMultiSelectButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        label = stringResource(id = R.string.number_of_digits),
                        options = digitOptionsWithStrings.values.toImmutableList(),
                        selectedOption = viewState.itemData.digits.length.toString(),
                        onOptionSelected = remember(viewState) {
                            { selectedOption ->
                                val selectedOptionName = digitOptionsWithStrings
                                    .entries
                                    .first { it.value == selectedOption }
                                    .key

                                onNumberOfDigitsOptionClicked(selectedOptionName)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EditItemDialogs(
    dialogState: EditItemState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is EditItemState.DialogState.Generic -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = dialogState.title,
                    message = dialogState.message
                ),
                onDismissRequest = onDismissRequest,
            )
        }

        is EditItemState.DialogState.Loading -> {
            BitwardenLoadingDialog(
                visibilityState = LoadingDialogState.Shown(dialogState.message),
            )
        }

        null -> Unit
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun EditItemContentExpandedOptionsPreview() {
    EditItemContent(
        viewState = EditItemState.ViewState.Content(
            itemData = EditItemData(
                refreshPeriod = AuthenticatorRefreshPeriodOption.THIRTY,
                totpCode = "123456",
                type = AuthenticatorItemType.TOTP,
                username = "account name",
                issuer = "issuer",
                algorithm = AuthenticatorItemAlgorithm.SHA1,
                digits = VerificationCodeDigitsOption.SIX
            ),
            isAdvancedOptionsExpanded = true,
        )
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun EditItemContentCollapsedOptionsPreview() {
    EditItemContent(
        viewState = EditItemState.ViewState.Content(
            itemData = EditItemData(
                refreshPeriod = AuthenticatorRefreshPeriodOption.THIRTY,
                totpCode = "123456",
                type = AuthenticatorItemType.TOTP,
                username = "account name",
                issuer = "issuer",
                algorithm = AuthenticatorItemAlgorithm.SHA1,
                digits = VerificationCodeDigitsOption.SIX
            ),
            isAdvancedOptionsExpanded = false,
        )
    )
}
