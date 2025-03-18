package com.x8bit.bitwarden.ui.vault.feature.viewasqrcode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.attachments.handlers.AttachmentsHandlers
import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.handlers.ViewAsQrCodeHandlers
import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.model.QrCodeType
import kotlinx.collections.immutable.toImmutableList

/**
 * Displays the view as QR code screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewAsQrCodeScreen(
    viewModel: ViewAsQrCodeViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val viewAsQrCodeHandlers = remember(viewModel) { ViewAsQrCodeHandlers.create(viewModel) }

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            ViewAsQrCodeEvent.NavigateBack -> onNavigateBack()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    BitwardenScaffold(
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.view_as_qr_code),
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(ViewAsQrCodeAction.BackClick) }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) {
        when (val viewState = state.viewState) {
            is ViewAsQrCodeState.ViewState.Loading -> BitwardenLoadingContent(
                modifier = Modifier.fillMaxSize(),
            )

            is ViewAsQrCodeState.ViewState.Error -> BitwardenErrorContent(
                message = "ERROR",
                onTryAgainClick = remember(viewModel) {
                    { viewModel.trySendAction(ViewAsQrCodeAction.BackClick) }
                },
                modifier = Modifier.fillMaxSize(),
            )

            is ViewAsQrCodeState.ViewState.Content -> {
                //TODO add ViewAsQrCodeContent
                val contentState = state

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // QR Code display
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            //TODO set qrcode image
                            painter = rememberVectorPainter(id = R.drawable.bitwarden_logo),
                            colorFilter = ColorFilter.tint(BitwardenTheme.colorScheme.icon.secondary),

                            //bitmap = contentState.qrCodeBitmap.asImageBitmap(),
                            contentDescription = stringResource(id = R.string.qr_code),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
//
//                    // QR Code type selector
//                    BitwardenMultiSelectButton(
//                        label = stringResource(id = R.string.qr_code_type),
//                        options = contentState.qrCodeTypes.map { it.displayName() }.toImmutableList(),
//                        selectedOption = contentState.selectedQrCodeType.displayName(),
//                        onOptionSelected = { selectedOption ->
//                            val selectedType = contentState.qrCodeTypes.first {
//                                it.displayName() == selectedOption
//                            }
//                            viewModel.trySendAction(ViewAsQrCodeAction.QrCodeTypeSelect(selectedType))
//                        },
//                        modifier = Modifier.fillMaxWidth(),
//                    )
//
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    // Dynamic fields based on selected QR code type
//                    when (contentState.selectedQrCodeType) {
//                        QrCodeType.Text -> {
//                            BitwardenTextField(
//                                label = stringResource(id = R.string.text),
//                                value = contentState.fields["text"] ?: "",
//                                onValueChange = { newValue ->
//                                    viewModel.trySendAction(
//                                        ViewAsQrCodeAction.FieldValueChange("text", newValue)
//                                    )
//                                },
//                                modifier = Modifier.fillMaxWidth(),
//                            )
//                        }
//
//                        QrCodeType.Url -> {
//                            BitwardenTextField(
//                                label = stringResource(id = R.string.url),
//                                value = contentState.fields["url"] ?: "",
//                                onValueChange = { newValue ->
//                                    viewModel.trySendAction(
//                                        ViewAsQrCodeAction.FieldValueChange("url", newValue)
//                                    )
//                                },
//                                modifier = Modifier.fillMaxWidth(),
//                            )
//                        }
//
//                        QrCodeType.Email -> {
//                            BitwardenTextField(
//                                label = stringResource(id = R.string.email),
//                                value = contentState.fields["email"] ?: "",
//                                onValueChange = { newValue ->
//                                    viewModel.trySendAction(
//                                        ViewAsQrCodeAction.FieldValueChange("email", newValue)
//                                    )
//                                },
//                                modifier = Modifier.fillMaxWidth(),
//                            )
//
//                            Spacer(modifier = Modifier.height(8.dp))
//
//                            BitwardenTextField(
//                                label = stringResource(id = R.string.subject),
//                                value = contentState.fields["subject"] ?: "",
//                                onValueChange = { newValue ->
//                                    viewModel.trySendAction(
//                                        ViewAsQrCodeAction.FieldValueChange("subject", newValue)
//                                    )
//                                },
//                                modifier = Modifier.fillMaxWidth(),
//                            )
//
//                            Spacer(modifier = Modifier.height(8.dp))
//
//                            BitwardenTextField(
//                                label = stringResource(id = R.string.body),
//                                value = contentState.fields["body"] ?: "",
//                                onValueChange = { newValue ->
//                                    viewModel.trySendAction(
//                                        ViewAsQrCodeAction.FieldValueChange("body", newValue)
//                                    )
//                                },
//                                modifier = Modifier.fillMaxWidth(),
//                            )
//                        }
//
//                        QrCodeType.Phone -> {
//                            BitwardenTextField(
//                                label = stringResource(id = R.string.phone),
//                                value = contentState.fields["phone"] ?: "",
//                                onValueChange = { newValue ->
//                                    viewModel.trySendAction(
//                                        ViewAsQrCodeAction.FieldValueChange("phone", newValue)
//                                    )
//                                },
//                                modifier = Modifier.fillMaxWidth(),
//                            )
//                        }
//
//                        QrCodeType.SMS -> {
//                            BitwardenTextField(
//                                label = stringResource(id = R.string.phone),
//                                value = contentState.fields["phone"] ?: "",
//                                onValueChange = { newValue ->
//                                    viewModel.trySendAction(
//                                        ViewAsQrCodeAction.FieldValueChange("phone", newValue)
//                                    )
//                                },
//                                modifier = Modifier.fillMaxWidth(),
//                            )
//
//                            Spacer(modifier = Modifier.height(8.dp))
//
//                            BitwardenTextField(
//                                label = stringResource(id = R.string.message),
//                                value = contentState.fields["message"] ?: "",
//                                onValueChange = { newValue ->
//                                    viewModel.trySendAction(
//                                        ViewAsQrCodeAction.FieldValueChange("message", newValue)
//                                    )
//                                },
//                                modifier = Modifier.fillMaxWidth(),
//                            )
//                        }
//
//                        QrCodeType.WiFi -> {
//                            BitwardenTextField(
//                                label = stringResource(id = R.string.ssid),
//                                value = contentState.fields["ssid"] ?: "",
//                                onValueChange = { newValue ->
//                                    viewModel.trySendAction(
//                                        ViewAsQrCodeAction.FieldValueChange("ssid", newValue)
//                                    )
//                                },
//                                modifier = Modifier.fillMaxWidth(),
//                            )
//
//                            Spacer(modifier = Modifier.height(8.dp))
//
//                            BitwardenTextField(
//                                label = stringResource(id = R.string.password),
//                                value = contentState.fields["password"] ?: "",
//                                onValueChange = { newValue ->
//                                    viewModel.trySendAction(
//                                        ViewAsQrCodeAction.FieldValueChange("password", newValue)
//                                    )
//                                },
//                                modifier = Modifier.fillMaxWidth(),
//                            )
//
//                            Spacer(modifier = Modifier.height(8.dp))
//
//                            BitwardenMultiSelectButton(
//                                label = stringResource(id = R.string.encryption_type),
//                                options = listOf("WPA", "WEP", "None").toImmutableList(),
//                                selectedOption = contentState.fields["type"] ?: "WPA",
//                                onOptionSelected = { selectedOption ->
//                                    viewModel.trySendAction(
//                                        ViewAsQrCodeAction.FieldValueChange("type", selectedOption)
//                                    )
//                                },
//                                modifier = Modifier.fillMaxWidth(),
//                            )
//
//                            Spacer(modifier = Modifier.height(8.dp))
//
//                            BitwardenMultiSelectButton(
//                                label = stringResource(id = R.string.hidden),
//                                options = listOf("true", "false").toImmutableList(),
//                                selectedOption = contentState.fields["hidden"] ?: "false",
//                                onOptionSelected = { selectedOption ->
//                                    viewModel.trySendAction(
//                                        ViewAsQrCodeAction.FieldValueChange("hidden", selectedOption)
//                                    )
//                                },
//                                modifier = Modifier.fillMaxWidth(),
//                            )
//                        }
//
//                        QrCodeType.Contact -> {
//                            BitwardenTextField(
//                                label = stringResource(id = R.string.name),
//                                value = contentState.fields["name"] ?: "",
//                                onValueChange = { newValue ->
//                                    viewModel.trySendAction(
//                                        ViewAsQrCodeAction.FieldValueChange("name", newValue)
//                                    )
//                                },
//                                modifier = Modifier.fillMaxWidth(),
//                            )
//
//                            Spacer(modifier = Modifier.height(8.dp))
//
//                            BitwardenTextField(
//                                label = stringResource(id = R.string.phone),
//                                value = contentState.fields["phone"] ?: "",
//                                onValueChange = { newValue ->
//                                    viewModel.trySendAction(
//                                        ViewAsQrCodeAction.FieldValueChange("phone", newValue)
//                                    )
//                                },
//                                modifier = Modifier.fillMaxWidth(),
//                            )
//
//                            Spacer(modifier = Modifier.height(8.dp))
//
//                            BitwardenTextField(
//                                label = stringResource(id = R.string.email),
//                                value = contentState.fields["email"] ?: "",
//                                onValueChange = { newValue ->
//                                    viewModel.trySendAction(
//                                        ViewAsQrCodeAction.FieldValueChange("email", newValue)
//                                    )
//                                },
//                                modifier = Modifier.fillMaxWidth(),
//                            )
//
//                            Spacer(modifier = Modifier.height(8.dp))
//
//                            BitwardenTextField(
//                                label = stringResource(id = R.string.organization),
//                                value = contentState.fields["organization"] ?: "",
//                                onValueChange = { newValue ->
//                                    viewModel.trySendAction(
//                                        ViewAsQrCodeAction.FieldValueChange("organization", newValue)
//                                    )
//                                },
//                                modifier = Modifier.fillMaxWidth(),
//                            )
//
//                            Spacer(modifier = Modifier.height(8.dp))
//
//                            BitwardenTextField(
//                                label = stringResource(id = R.string.address),
//                                value = contentState.fields["address"] ?: "",
//                                onValueChange = { newValue ->
//                                    viewModel.trySendAction(
//                                        ViewAsQrCodeAction.FieldValueChange("address", newValue)
//                                    )
//                                },
//                                modifier = Modifier.fillMaxWidth(),
//                            )
//                        }
//                    }
                }
            }
        }
    }
}
