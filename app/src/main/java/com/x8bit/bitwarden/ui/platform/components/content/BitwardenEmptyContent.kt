package com.x8bit.bitwarden.ui.platform.components.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.base.util.nullableTestTag
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.icon.BitwardenIcon
import com.x8bit.bitwarden.ui.platform.components.model.IconData

/**
 * A Bitwarden-themed, re-usable empty state.
 */
@Composable
fun BitwardenEmptyContent(
    text: String,
    modifier: Modifier = Modifier,
    illustrationData: IconData? = null,
    labelTestTag: String? = null,
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
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
