@file:OmitFromCoverage

package com.bitwarden.ui.platform.components.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.nullableTestTag
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.button.model.BitwardenButtonData
import com.bitwarden.ui.platform.components.icon.BitwardenIcon
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.asText

/**
 * A Bitwarden-themed, re-usable empty state.
 *
 * @param text The primary text to display.
 * @param modifier The [Modifier] to be applied to the layout.
 * @param illustrationData Optional illustration to display above the text.
 * @param labelTestTag A test tag for the primary text.
 * @param title Optional title to display above the primary text.
 * @param titleTestTag A test tag for the title.
 * @param primaryButton Optional primary button to display.
 * @param secondaryButton Optional secondary button to display.
 */
@Suppress("LongMethod")
@Composable
fun BitwardenEmptyContent(
    text: String,
    modifier: Modifier = Modifier,
    illustrationData: IconData? = null,
    labelTestTag: String? = null,
    title: String? = null,
    titleTestTag: String? = null,
    primaryButton: BitwardenButtonData? = null,
    secondaryButton: BitwardenButtonData? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        illustrationData?.let {
            BitwardenIcon(
                iconData = it,
                modifier = Modifier.size(size = 124.dp),
            )
            Spacer(modifier = Modifier.height(height = 24.dp))
        }
        title?.let {
            Text(
                text = title,
                style = BitwardenTheme.typography.titleMedium,
                color = BitwardenTheme.colorScheme.text.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .nullableTestTag(tag = titleTestTag),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
        }
        Text(
            text = text,
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .nullableTestTag(tag = labelTestTag),
        )

        // If either of the optional buttons are present add a spacer between the text and the
        // buttons.
        if (primaryButton != null || secondaryButton != null) {
            Spacer(Modifier.height(12.dp))
        }

        primaryButton?.let {
            BitwardenFilledButton(
                label = it.label(),
                onClick = it.onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .nullableTestTag(tag = it.testTag),
            )
        }

        // If both buttons are visible add the standard button spacing.
        if (primaryButton != null && secondaryButton != null) {
            Spacer(Modifier.height(8.dp))
        }

        secondaryButton?.let {
            BitwardenOutlinedButton(
                label = it.label(),
                onClick = it.onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .nullableTestTag(tag = it.testTag),
            )
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Preview(showBackground = true, name = "Bitwarden empty content")
@Composable
private fun BitwardenEmptyContent_preview() {
    BitwardenScaffold {
        BitwardenEmptyContent(
            title = "Empty content",
            titleTestTag = "TitleTestTag",
            text = "There is no content to display",
            labelTestTag = "EmptyContentLabel",
            illustrationData = IconData.Local(BitwardenDrawable.ill_pin),
            primaryButton = BitwardenButtonData(
                label = "Primary button".asText(),
                testTag = "EmptyContentPositiveButton",
                onClick = { },
            ),
            secondaryButton = BitwardenButtonData(
                label = "Secondary button".asText(),
                testTag = "EmptyContentNegativeButton",
                onClick = { },
            ),
            modifier = Modifier
                .fillMaxSize()
                .standardHorizontalMargin(),
        )
    }
}
