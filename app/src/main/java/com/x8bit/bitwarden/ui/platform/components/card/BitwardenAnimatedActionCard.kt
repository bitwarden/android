package com.x8bit.bitwarden.ui.platform.components.card

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A version of [BitwardenActionCard] which will animate the height of the card
 * based on the [isVisible] parameter.
 */
@Composable
fun BitwardenAnimatedActionCard(
    isVisible: Boolean,
    cardTitle: String,
    actionText: String,
    onActionClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: @Composable (() -> Unit)? = null,
    ) {
    Box(
        modifier = Modifier
            .animateContentSize()
            .then(
                if (isVisible) {
                    modifier
                } else {
                    Modifier.height(0.dp)
                },
            ),
    ) {
        BitwardenActionCard(
            cardTitle = cardTitle,
            actionText = actionText,
            onActionClick = onActionClick,
            onDismissClick = onDismissClick,
            leadingContent = leadingContent,
        )
    }
}
