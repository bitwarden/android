package com.x8bit.bitwarden.ui.vault.feature.viewasqrcode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.cardStyle
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.handlers.ViewAsQrCodeHandlers
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
        val viewState = state
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(height = 12.dp))

            // QR Code display
            Box(
                modifier = Modifier
                    .standardHorizontalMargin()
                    .cardStyle(CardStyle.Full)
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = state.qrCodeBitmap.asImageBitmap(),
                    contentDescription = stringResource(id = R.string.qr_code),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // QR Code type selector
            val resources = LocalContext.current.resources
            BitwardenMultiSelectButton(
                label = stringResource(id = R.string.qr_code_type),
                options = viewState.qrCodeTypes.map { it.displayName() }.toImmutableList(),
                selectedOption = viewState.selectedQrCodeType.displayName(),
                onOptionSelected = { selectedOption ->
                    val selectedType = viewState.qrCodeTypes.first {
                        it.displayName.toString(resources) == selectedOption
                    }
                    viewModel.trySendAction(ViewAsQrCodeAction.QrCodeTypeSelect(selectedType))
                },
                //supportingText = stringResource(id = R.string.default_uri_match_detection_description),
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .testTag("QRCodeType")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )

            //QR Code Type dropdowns
            Spacer(modifier = Modifier.height(8.dp))
            viewState.qrCodeTypeFields.forEachIndexed { i, field ->
                val cipherFieldsTextList =
                    viewState.cipherFields.map { it() }.toImmutableList()
                val fieldsListSize = viewState.qrCodeTypeFields.size
                val lastFieldIndex = fieldsListSize - 1;
                BitwardenMultiSelectButton(
                    label = field.displayName(),
                    options = cipherFieldsTextList,
                    selectedOption = field.value(),
                    onOptionSelected = { selectedOption ->
                        viewModel.trySendAction(
                            ViewAsQrCodeAction.FieldValueChange(
                                field, //TODO memory leak?
                                selectedOption
                            )
                        )
                    },
                    cardStyle = when (i) {
                        0 -> when (fieldsListSize) {
                            1 -> CardStyle.Full
                            else -> CardStyle.Top()
                        }

                        lastFieldIndex -> CardStyle.Bottom
                        else -> CardStyle.Middle()
                    },
                    modifier = Modifier
                        .testTag("QRCodeField_${field.key}")
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }
        }
    }
}
