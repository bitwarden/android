package com.bitwarden.ui.platform.components.button

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.interactiveBorder
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.platform.theme.animation.focusAnimationSpec
import com.bitwarden.ui.platform.theme.ripple.NoFocusRippleConfig

/**
 * A customized based button similar to the Material3 [Button] but with our own custom focus state.
 */
@Composable
internal fun BitwardenBaseButton(
    onClick: () -> Unit,
    colors: ButtonColors,
    focusColor: Color,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    enabled: Boolean = true,
    shape: Shape = BitwardenTheme.shapes.button,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp, horizontal = 24.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit,
) {
    val containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor
    val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
    val isFocused by interactionSource.collectIsFocusedAsState()
    CompositionLocalProvider(LocalRippleConfiguration provides NoFocusRippleConfig) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            border = border?.toAnimatedBorder(isFocused = isFocused),
            interactionSource = interactionSource,
            modifier = modifier.semantics { role = Role.Button },
        ) {
            Row(
                modifier = Modifier
                    .defaultMinSize(
                        minWidth = ButtonDefaults.MinWidth,
                        minHeight = ButtonDefaults.MinHeight,
                    )
                    .interactiveBorder(
                        interactionSource = interactionSource,
                        shape = shape,
                        color = focusColor,
                        inset = if (border == null) 2.dp else 4.dp,
                    )
                    .padding(paddingValues = contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

@Composable
private fun BorderStroke.toAnimatedBorder(
    isFocused: Boolean,
): BorderStroke {
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) this.width * 2f else this.width,
        animationSpec = focusAnimationSpec(),
    )
    return this.copy(width = borderWidth)
}
