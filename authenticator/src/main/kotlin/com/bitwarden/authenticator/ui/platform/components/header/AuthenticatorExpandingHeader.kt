package com.bitwarden.authenticator.ui.platform.components.header

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A header that can be expanded and collapsed.
 */
@Composable
fun AuthenticatorExpandingHeader(
    label: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onClickLabel: String,
    modifier: Modifier = Modifier,
    insets: PaddingValues = PaddingValues(top = 16.dp, bottom = 8.dp),
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = BitwardenTheme.shapes.content)
            .clickable(
                onClickLabel = onClickLabel,
                onClick = onClick,
            )
            .minimumInteractiveComponentSize()
            .padding(paddingValues = insets)
            .padding(horizontal = 16.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        val iconRotationDegrees = animateFloatAsState(
            targetValue = if (isExpanded) 0f else 180f,
            label = "expanderIconRotationAnimation",
        )
        Text(
            text = label.uppercase(),
            style = BitwardenTheme.typography.eyebrowMedium,
            color = BitwardenTheme.colorScheme.text.secondary,
            modifier = Modifier.weight(weight = 1f, fill = false),
        )
        Spacer(modifier = Modifier.width(width = 8.dp))
        Icon(
            painter = rememberVectorPainter(id = BitwardenDrawable.ic_chevron_up_small),
            contentDescription = null,
            tint = BitwardenTheme.colorScheme.icon.primary,
            modifier = Modifier.rotate(degrees = iconRotationDegrees.value),
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun ExpandingHeaderPreview() {
    Column {
        AuthenticatorExpandingHeader(
            label = "Label Collapsed",
            isExpanded = false,
            onClick = { },
            onClickLabel = "",
        )
        AuthenticatorExpandingHeader(
            label = "Label Expanded",
            isExpanded = true,
            onClick = { },
            onClickLabel = "",
        )
    }
}
