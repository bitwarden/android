package com.x8bit.bitwarden.ui.vault.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.vault.util.Constants.CARD_NUMBER_PLACEHOLDER_TEXT
import com.x8bit.bitwarden.ui.vault.util.Constants.CARD_CVV_PLACEHOLDER_TEXT
import com.x8bit.bitwarden.ui.vault.util.Constants.CARD_EXPIRATION_PLACEHOLDER_TEXT
import com.x8bit.bitwarden.ui.vault.util.Constants.CARD_HOLDER_NAME_PLACEHOLDER_TEXT

/**
 * A composable function to display a credit/debit card UI item with options to show/hide details and copy information.
 *
 * @param holderName The name of the card holder.
 * @param number The card number.
 * @param expiry The expiry date of the card.
 * @param cvv The CVV (Card Verification Value) of the card.
 * @param modifier A [Modifier] for this composable.
 * @param editable A boolean flag to indicate if the card is in edit mode.
 */
@Composable
fun BankCardContent(
    holderName: String,
    number: String,
    expiry: String,
    cvv: String,
    modifier: Modifier = Modifier,
    editable: Boolean = false,
    onCardNumberClick: () -> Unit = {},
    onSecurityCodeClick: () -> Unit = {}
) {
    var showCardDetails by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize(align = Alignment.TopCenter)
            .widthIn(max = 360.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            Box(
                modifier = Modifier.aspectRatio(1.6f),
                content = {
                    // Background image of the card
                    Image(
                        painter = painterResource(id = R.drawable.purple_card_bg),
                        contentDescription = stringResource(id = R.string.type_card),
                        contentScale = ContentScale.Crop
                    )

                    // Column for card details
                    Column(
                        modifier = Modifier
                            .padding(32.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        content = {
                            // Card holder name
                            BankCardLabelAndText(
                                labelText = stringResource(id = R.string.cardholder_name),
                                valueText = holderName.ifEmpty { CARD_HOLDER_NAME_PLACEHOLDER_TEXT },
                                isValueTextWidthFixed = holderName == CARD_HOLDER_NAME_PLACEHOLDER_TEXT
                            )

                            // Card number and copy button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                content = {
                                    CardNumberAnimator(
                                        cardNumber = if (showCardDetails || editable) number.endPaddedText(
                                            padLength = 16
                                        ) else CARD_NUMBER_PLACEHOLDER_TEXT
                                    )

                                    // Copy card number button (hidden in edit mode)
                                    if (!editable) {
                                        CardIconButton(
                                            icon = R.drawable.copy_icon,
                                            onClick = onCardNumberClick,
                                            contentDescription = stringResource(id = R.string.copy_number)
                                        )
                                    }
                                }
                            )

                            // Expiry date, CVV, and action buttons
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                content = {
                                    // Expiry date label and text
                                    BankCardLabelAndText(
                                        labelText = stringResource(id = R.string.expiration),
                                        valueText = if (!showCardDetails || editable) {
                                            expiry.endPaddedText(padLength = 6)
                                        } else {
                                            CARD_EXPIRATION_PLACEHOLDER_TEXT
                                        },
                                        isValueTextWidthFixed = true
                                    )

                                    // CVV label and text
                                    BankCardLabelAndText(
                                        labelText = stringResource(id = R.string.security),
                                        valueText = if (!showCardDetails || editable) {
                                            cvv.endPaddedText(padLength = 3)
                                        } else {
                                            CARD_CVV_PLACEHOLDER_TEXT
                                        },
                                        isValueTextWidthFixed = true
                                    )

                                    // Action buttons (copy CVV and show/hide details)
                                    if (!editable) {
                                        CardIconButton(
                                            icon = R.drawable.copy_icon,
                                            onClick = onSecurityCodeClick,
                                            contentDescription = stringResource(id = R.string.copy_security_code)
                                        )
                                        Spacer(modifier = Modifier.weight(1f))

                                        @DrawableRes
                                        val painterRes = if (showCardDetails) {
                                            R.drawable.icon_eye_off
                                        } else {
                                            R.drawable.icon_eye
                                        }

                                        @StringRes
                                        val contentDescriptionRes = if (showCardDetails) R.string.hide else R.string.show
                                        CardIconButton(
                                            icon = painterRes,
                                            contentDescription = stringResource(id = contentDescriptionRes),
                                            onClick = { showCardDetails = !showCardDetails }
                                        )
                                    }
                                }
                            )
                        }
                    )
                }
            )
        }
    )
}

/**
 * A composable function that displays a label and its associated value text,
 * with optional customization for colors, spacing, and animation.
 *
 * @param labelText The text to be displayed as the label.
 * @param valueText The text to be displayed as the value.
 * @param modifier The modifier to be applied to the composable.
 * @param labelTextColor The color of the label text. Default is [Color.Black].
 * @param valueTextColor The color of the value text. Default is [Color.White].
 * @param isValueTextWidthFixed A boolean flag indicating whether [valueText] characters should have fixed width. Default is false.
 */
@Composable
private fun BankCardLabelAndText(
    labelText: String,
    valueText: String,
    modifier: Modifier = Modifier,
    labelTextColor: Color = Color.Black,
    valueTextColor: Color = Color.White,
    isValueTextWidthFixed: Boolean = false
) {
    // Column to arrange label and value vertically with spacing
    Column(
        modifier = modifier.wrapContentSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        content = {
            // Display the label text with animation and styling
            AnimatedText(
                text = labelText.uppercase(),
                textStyle = TextStyle(
                    letterSpacing = 1.sp,
                    fontSize = 12.sp,
                    color = labelTextColor
                )
            )

            // Spacer to provide vertical spacing between label and value
            Spacer(modifier = Modifier.height(4.dp))

            // Row to display each character of the value text horizontally
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                content = {
                    // Iterate over each character in the value text
                    valueText.toCharArray().forEach { char ->
                        AnimatedText(
                            // Apply spacing between characters if isValueTextWidthFixed is true
                            modifier = if (isValueTextWidthFixed) Modifier.width(12.dp) else Modifier,
                            text = char.toString(),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                letterSpacing = 1.sp,
                                color = valueTextColor,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            )
        }
    )
}

/**
 * A composable function that animates the display of a credit/debit card number.
 * Each character in the card number is displayed with an animation.
 *
 * @param cardNumber The credit card number to display.
 * @param modifier The modifier to be applied to the Row composable.
 */
@Composable
private fun CardNumberAnimator(
    cardNumber: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        content = {
            // Iterate through each character in the card number
            cardNumber.toCharArray().forEachIndexed { index, char ->

                // Display each character with animation
                AnimatedText(
                    modifier = Modifier.width(12.dp),
                    text = char.toString(),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        letterSpacing = 1.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    )
                )

                val isNextSegment = ((index + 1) % 4) == 0
                val isEndOfCardNumber = index == 15
                if (isNextSegment && !isEndOfCardNumber) {
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        }
    )
}

/**
 * A Composable function that displays animated text with a vertical slide-in and slide-out transition.
 *
 * @param text The text to be displayed and animated.
 * @param modifier A [Modifier] to be applied to the composable. Defaults to [Modifier].
 * @param textStyle A [TextStyle] to be applied to the animated text. Defaults to a text style with
 * font size of 16.sp, letter spacing of 1.sp, and black color.
 */
@Composable
private fun AnimatedText(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        letterSpacing = 1.sp,
        color = Color.Black
    )
) {
    AnimatedContent(
        modifier = modifier,
        targetState = text,
        transitionSpec = {
            slideInVertically { -it } togetherWith slideOutVertically { it }
        },
        label = text,
        content = { animatedText ->
            Text(
                text = animatedText,
                style = textStyle
            )
        }
    )
}

/**
 * A composable function that displays an icon button with customizable properties.
 *
 * @param icon The drawable resource ID of the icon to be displayed.
 * @param contentDescription Text to describe the icon used.
 * @param onClick The callback that is triggered when the button is clicked.
 * @param modifier A Modifier for this button to apply customizations such as padding and size. Default is [Modifier].
 * @param iconSize The size of the icon in [Dp]. Default is 24.dp.
 * @param tint The color to tint the icon. Default is [Color.White].
 */
@Composable
private fun CardIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    tint: Color = Color.White
) {
    IconButton(
        modifier = modifier.size(iconSize),
        content = {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize - 4.dp),
                tint = tint
            )
        },
        onClick = onClick
    )
}

/**
 * Pads the end of the string with the specified character up to the given length.
 *
 * @receiver The original string to be padded.
 * @param padLength The total length of the resulting string after padding.
 * @param padChar The character to pad the string with. Defaults to 'x'.
 * @return A new string padded at the end with the specified character up to the given length.
 *
 * Example usage:
 * ```
 * val original = "hello"
 * val padded = original.endPaddedText(10) // Result: "helloxxxxx"
 * val customPadded = original.endPaddedText(8, '-') // Result: "hello---"
 * ```
 */
private fun String.endPaddedText(
    padLength: Int,
    padChar: Char = 'x'
): String {
    return this.padEnd(
        length = padLength,
        padChar = padChar
    )
}

@PreviewScreenSizes
@Composable
private fun BankCardNewPreview() {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = {
            // Empty Card
            BankCardContent(
                holderName = "",
                number = "",
                expiry = "",
                cvv = "",
                editable = true
            )

            // Card with content
            BankCardContent(
                holderName = "Bitwarden",
                number = "1234567890123456",
                expiry = "12/3456",
                cvv = "123",
            )
        }
    )
}
