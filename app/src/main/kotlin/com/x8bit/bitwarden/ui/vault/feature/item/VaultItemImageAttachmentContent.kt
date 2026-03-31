package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.x8bit.bitwarden.ui.vault.feature.media.MediaPreviewState
import java.io.File

private const val IMAGE_ASPECT_RATIO = 16f / 9f

/**
 * Composable for rendering image-type attachment items with inline preview support.
 *
 * Renders different UI based on the [MediaPreviewState]:
 * - Masked: privacy veil with a lock icon
 * - Loading: download / decryption progress indicator
 * - ImageReady: Glide-backed thumbnail (file-path based, OOM-safe)
 * - Error: error message overlay
 *
 * Security: All Glide loads use [DiskCacheStrategy.NONE] and signal
 * [onBitmapRenderComplete] so the ViewModel can delete the temp file.
 */
@Suppress("LongMethod")
@Composable
fun ImageAttachmentItemContent(
    attachmentItem: VaultItemState.ViewState.Content.Common.AttachmentItem,
    previewState: MediaPreviewState,
    onAttachmentPreviewClick: (String) -> Unit,
    onAttachmentImageViewClick: (String) -> Unit,
    onBitmapRenderComplete: (String) -> Unit,
    onAttachmentDownloadClick: (
        VaultItemState.ViewState.Content.Common.AttachmentItem,
    ) -> Unit,
    onUpgradeToPremiumClick: () -> Unit,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    var shouldShowPremiumWarningDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var shouldShowSizeWarningDialog by rememberSaveable {
        mutableStateOf(false)
    }

    Column(
        modifier = modifier
            .cardStyle(
                cardStyle = cardStyle,
                paddingStart = 0.dp,
                paddingEnd = 0.dp,
            )
            .testTag("CipherImageAttachment"),
    ) {
        ImagePreviewArea(
            previewState = previewState,
            title = attachmentItem.title,
            attachmentId = attachmentItem.id,
            onAttachmentPreviewClick = onAttachmentPreviewClick,
            onAttachmentImageViewClick = onAttachmentImageViewClick,
            onBitmapRenderComplete = onBitmapRenderComplete,
        )

        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .fillMaxWidth()
                .padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = attachmentItem.title,
                color = BitwardenTheme.colorScheme.text.primary,
                style = BitwardenTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ImageAttachmentNameLabel"),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = attachmentItem.displaySize,
                color = BitwardenTheme.colorScheme.text.primary,
                style = BitwardenTheme.typography.labelSmall,
                modifier = Modifier
                    .testTag("ImageAttachmentSizeLabel"),
            )

            Spacer(modifier = Modifier.width(8.dp))

            BitwardenStandardIconButton(
                vectorIconRes = BitwardenDrawable.ic_download,
                contentDescription = stringResource(
                    id = BitwardenString.download,
                ),
                onClick = {
                    if (!attachmentItem.isDownloadAllowed) {
                        shouldShowPremiumWarningDialog = true
                        return@BitwardenStandardIconButton
                    }
                    if (attachmentItem.isLargeFile) {
                        shouldShowSizeWarningDialog = true
                        return@BitwardenStandardIconButton
                    }
                    onAttachmentDownloadClick(attachmentItem)
                },
                modifier = Modifier
                    .testTag("ImageAttachmentDownloadButton"),
            )
        }
    }

    if (shouldShowPremiumWarningDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = BitwardenString.attachments_unavailable),
            message = stringResource(
                id = BitwardenString.attachments_are_a_premium_feature,
            ),
            confirmButtonText = stringResource(
                id = BitwardenString.upgrade_to_premium,
            ),
            dismissButtonText = stringResource(id = BitwardenString.cancel),
            onConfirmClick = {
                shouldShowPremiumWarningDialog = false
                onUpgradeToPremiumClick()
            },
            onDismissClick = { shouldShowPremiumWarningDialog = false },
            onDismissRequest = { shouldShowPremiumWarningDialog = false },
        )
    }

    if (shouldShowSizeWarningDialog) {
        BitwardenTwoButtonDialog(
            title = null,
            message = stringResource(
                BitwardenString.attachment_large_warning,
                attachmentItem.displaySize,
            ),
            confirmButtonText = stringResource(BitwardenString.yes),
            dismissButtonText = stringResource(BitwardenString.no),
            onConfirmClick = {
                shouldShowSizeWarningDialog = false
                onAttachmentDownloadClick(attachmentItem)
            },
            onDismissClick = { shouldShowSizeWarningDialog = false },
            onDismissRequest = { shouldShowSizeWarningDialog = false },
        )
    }
}

/**
 * Renders the image preview area based on the current [MediaPreviewState].
 * Injects [DiskCacheStrategy.NONE] and fires [onBitmapRenderComplete]
 * once the bitmap is loaded into RAM.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ImagePreviewArea(
    previewState: MediaPreviewState,
    title: String,
    attachmentId: String,
    onAttachmentPreviewClick: (String) -> Unit,
    onAttachmentImageViewClick: (String) -> Unit,
    onBitmapRenderComplete: (String) -> Unit,
) {
    when (previewState) {
        is MediaPreviewState.Masked -> {
            MaskedOverlay(
                onClick = { onAttachmentPreviewClick(attachmentId) },
            )
        }

        is MediaPreviewState.Loading -> {
            LoadingOverlay()
        }

        is MediaPreviewState.ImageReady -> {
            GlideImage(
                model = File(previewState.decryptedFilePath),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                requestBuilderTransform = { requestBuilder ->
                    requestBuilder
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(false)
                        .addListener(
                            BurnAfterReadingListener(
                                attachmentId = attachmentId,
                                onComplete = onBitmapRenderComplete,
                            ),
                        )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio = IMAGE_ASPECT_RATIO)
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                        ),
                    )
                    .clickable {
                        onAttachmentImageViewClick(attachmentId)
                    }
                    .testTag("ImagePreviewRevealed"),
            )
        }

        is MediaPreviewState.PdfReady -> {
            // Fallback safely for non-image types.
            ErrorOverlay(message = "Unsupported preview type")
        }

        is MediaPreviewState.Error -> {
            ErrorOverlay(message = previewState.message)
        }
    }
}

/**
 * Privacy mask overlay — prompts the user to tap to decrypt the image.
 */
@Composable
private fun MaskedOverlay(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio = IMAGE_ASPECT_RATIO)
            .background(
                color = BitwardenTheme.colorScheme.background.tertiary,
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                ),
            )
            .clickable(onClick = onClick)
            .testTag("ImagePreviewMasked"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = rememberVectorPainter(
                    id = BitwardenDrawable.ic_locked,
                ),
                contentDescription = null,
                tint = BitwardenTheme.colorScheme.icon.secondary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = BitwardenString.attachments),
                style = BitwardenTheme.typography.labelMedium,
                color = BitwardenTheme.colorScheme.text.secondary,
            )
        }
    }
}

/**
 * Loading overlay — shown while the attachment is being decrypted.
 */
@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio = IMAGE_ASPECT_RATIO)
            .background(
                color = BitwardenTheme.colorScheme.background.tertiary,
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                ),
            )
            .testTag("ImagePreviewLoading"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                color = BitwardenTheme.colorScheme.icon.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = BitwardenString.downloading),
                style = BitwardenTheme.typography.labelMedium,
                color = BitwardenTheme.colorScheme.text.secondary,
            )
        }
    }
}

/**
 * Error overlay — shown when decryption fails.
 */
@Composable
private fun ErrorOverlay(
    message: String?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio = IMAGE_ASPECT_RATIO)
            .background(
                color = BitwardenTheme.colorScheme.background.tertiary,
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                ),
            )
            .testTag("ImagePreviewError"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = rememberVectorPainter(
                    id = BitwardenDrawable.ic_warning,
                ),
                contentDescription = null,
                tint = BitwardenTheme.colorScheme.status.error,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message ?: stringResource(
                    id = BitwardenString.generic_error_message,
                ),
                style = BitwardenTheme.typography.labelMedium,
                color = BitwardenTheme.colorScheme.text.secondary,
            )
        }
    }
}
