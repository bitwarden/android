package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.toUnscaledTextUnit

/**
 * Displays an icon representing a Bitwarden account with the user's initials superimposed.
 * The icon is typically a colored circle with the initials centered on it.
 *
 * @param initials The initials of the user to be displayed on top of the icon.
 * @param color The color to be applied as the tint for the icon.
 * @param onClick An action to be invoked when the icon is clicked.
 */
@Composable
fun BitwardenAccountActionItem(
    initials: String,
    color: Color,
    onClick: () -> Unit,
) {
    val iconPainter = painterResource(id = R.drawable.ic_account_initials_container)
    val contentDescription = stringResource(id = R.string.account)

    Box(
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick) {
            Icon(
                painter = iconPainter,
                contentDescription = contentDescription,
                tint = color,
            )
        }
        Text(
            text = initials,
            style = TextStyle(
                fontSize = 11.dp.toUnscaledTextUnit(),
                lineHeight = 13.dp.toUnscaledTextUnit(),
                fontFamily = FontFamily(Font(R.font.sf_pro)),
                fontWeight = FontWeight.W400,
            ),
            color = colorResource(id = R.color.white),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenAccountActionItem_preview() {
    val mockInitials = "BW"
    val mockColor = colorResource(id = R.color.primary)

    BitwardenAccountActionItem(
        initials = mockInitials,
        color = mockColor,
        onClick = {},
    )
}