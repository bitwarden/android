package com.x8bit.bitwarden.ui.platform.components.content

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.indicator.BitwardenCircularProgressIndicator
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A way to obscure content with either a blurred effect or a semi-transparent background.
 * Dependent on the device's Android version. Blur is supported on Android 12 and above.
 * Only the exact components passed in via the [content] will have the effect applied.
 *
 * @param enabled Whether the content should be obscured.
 * @param modifier The modifier to be applied to the outer container. By nature any padding would be
 * applied to passed in content as well.
 * @param overlayContent Optional content to overlay on top of the obscured content.
 * (e.g. a loading indicator)
 * @param content The content to obscure.
 */
@Composable
fun ObscuredContent(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    overlayContent: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val isBlurSupported = !isBuildVersionBelow(Build.VERSION_CODES.S)
    val shouldApplyBlur = enabled && isBlurSupported
    // if blur is not available we use a semi-transparent scrim instead.
    val shouldApplyLegacyObscuring = enabled && !isBlurSupported
    Box(
        modifier = modifier,
    ) {
        val customModifier = if (shouldApplyBlur) {
            Modifier
                .matchParentSize()
                .blur(45.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        } else {
            Modifier.matchParentSize()
        }
        Box(modifier = customModifier) {
            content()
            if (enabled) {
                // Disables interaction with the obscured content
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            if (shouldApplyLegacyObscuring) {
                                BitwardenTheme.colorScheme.background.primary.copy(alpha = 0.95f)
                            } else {
                                Color.Transparent
                            },
                        ),
                )
            }
        }
        if (overlayContent != null && enabled) {
            overlayContent()
        }
    }
}

@Preview
@Composable
private fun ObscuredContent_preview() {
    BitwardenTheme {
        ObscuredContent(
            enabled = true,
            modifier = Modifier.fillMaxSize(),
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Spacer(Modifier.size(100.dp))
                    BitwardenFilledButton(
                        label = "Obscure Content",
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }
            },
            overlayContent = {
                BitwardenCircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            },
        )
    }
}

@Preview
@Composable
private fun InteractiveObscuredContent_preview() {
    var obscureContent by remember { mutableStateOf(false) }
    BackHandler(enabled = obscureContent) {
        obscureContent = false
    }
    BitwardenTheme {
        ObscuredContent(
            enabled = obscureContent,
            modifier = Modifier.fillMaxSize(),
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Spacer(Modifier.size(100.dp))
                    BitwardenFilledButton(
                        label = "Obscure Content",
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }
            },
            overlayContent = {
                BitwardenCircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            },
        )
    }
}
