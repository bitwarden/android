package com.x8bit.bitwarden.ui.vault.feature.item.component

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.cardStyle
import com.x8bit.bitwarden.ui.platform.base.util.nullableTestTag
import com.x8bit.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenExpandingHeader
import com.x8bit.bitwarden.ui.platform.components.icon.BitwardenIcon
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.item.model.VaultItemLocation
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * The max number of items that can be displayed before the "show more" text is visible.
 */
private const val EXPANDABLE_THRESHOLD = 2

/**
 * Reusable composable for displaying the cipher name and favorite status.
 */
@OmitFromCoverage
@Suppress("LongMethod")
@Composable
fun ItemHeader(
    value: String,
    isFavorite: Boolean,
    relatedLocations: ImmutableList<VaultItemLocation>,
    iconData: IconData,
    modifier: Modifier = Modifier,
    iconTestTag: String? = null,
    textFieldTestTag: String? = null,
) {
    Column(
        modifier = modifier
            .cardStyle(CardStyle.Full)
            .fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            ItemHeaderIcon(
                iconData = iconData,
                testTag = iconTestTag,
                modifier = Modifier.size(36.dp),
            )
            BitwardenTextField(
                label = null,
                value = value,
                onValueChange = { },
                readOnly = true,
                singleLine = false,
                actions = {
                    Icon(
                        painter = painterResource(
                            id = if (isFavorite) {
                                R.drawable.ic_favorite_full
                            } else {
                                R.drawable.ic_favorite_empty
                            },
                        ),
                        contentDescription = stringResource(
                            id = if (isFavorite) R.string.favorite else R.string.unfavorite,
                        ),
                        modifier = Modifier.padding(all = 12.dp),
                    )
                },
                textFieldTestTag = textFieldTestTag,
                cardStyle = null,
                textStyle = BitwardenTheme.typography.titleMedium,
            )
        }

        BitwardenHorizontalDivider(Modifier.padding(start = 16.dp))

        Spacer(Modifier.height(8.dp))

        if (relatedLocations.isEmpty()) {
            ItemLocationListItem(
                vectorPainter = rememberVectorPainter(R.drawable.ic_folder),
                text = stringResource(R.string.no_folder),
                iconTestTag = "NoFolderIcon",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            return@Column
        }

        relatedLocations
            .take(EXPANDABLE_THRESHOLD)
            .forEach {
                ItemLocationListItem(
                    vectorPainter = rememberVectorPainter(it.icon),
                    iconTestTag = "ItemLocationIcon",
                    text = it.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

        ExpandingItemLocationContent(
            overflowLocations = relatedLocations
                .drop(EXPANDABLE_THRESHOLD)
                .toImmutableList(),
        )
    }
}

@Composable
private fun ColumnScope.ExpandingItemLocationContent(
    overflowLocations: ImmutableList<VaultItemLocation>,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = Modifier.clipToBounds(),
    ) {
        LazyColumn {
            items(overflowLocations) {
                ItemLocationListItem(
                    vectorPainter = rememberVectorPainter(it.icon),
                    text = it.name,
                    iconTestTag = "ItemLocationIcon",
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }

    if (overflowLocations.isNotEmpty()) {
        BitwardenExpandingHeader(
            collapsedText = stringResource(R.string.show_more),
            expandedText = stringResource(R.string.show_less),
            isExpanded = isExpanded,
            onClick = { isExpanded = !isExpanded },
            showExpansionIndicator = false,
        )
    }
}

@Composable
private fun ItemHeaderIcon(
    iconData: IconData,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = if (iconData is IconData.Local) {
            modifier.then(
                Modifier.background(
                    color = BitwardenTheme.colorScheme.illustration.backgroundPrimary,
                    shape = BitwardenTheme.shapes.favicon,
                ),
            )
        } else {
            modifier
        },
    ) {
        BitwardenIcon(
            iconData = iconData,
            contentDescription = null,
            tint = BitwardenTheme.colorScheme.illustration.outline,
            modifier = Modifier
                .nullableTestTag(testTag),
        )
    }
}

@Composable
private fun ItemLocationListItem(
    vectorPainter: VectorPainter,
    iconTestTag: String?,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .padding(8.dp),
    ) {
        Icon(
            painter = vectorPainter,
            tint = BitwardenTheme.colorScheme.icon.primary,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .nullableTestTag(iconTestTag),
        )
        Text(
            text = text,
            style = BitwardenTheme.typography.bodyLarge,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .padding(start = 16.dp)
                .testTag("ItemLocationText"),
        )
    }
}

//region Previews
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun ItemHeader_LocalIcon_Preview() {
    BitwardenTheme {
        LazyColumn {
            item {
                ItemHeader(
                    value = "Login without favicon",
                    isFavorite = true,
                    iconData = IconData.Local(
                        iconRes = R.drawable.ic_globe,
                    ),
                    relatedLocations = persistentListOf(),
                )
            }
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun ItemHeader_NetworkIcon_Preview() {
    BitwardenTheme {
        LazyColumn {
            item {
                ItemHeader(
                    value = "Login with favicon",
                    isFavorite = true,
                    iconData = IconData.Network(
                        uri = "mockuri",
                        fallbackIconRes = R.drawable.ic_globe,
                    ),
                    relatedLocations = persistentListOf(),
                )
            }
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ItemHeader_Organization_Preview() {
    BitwardenTheme {
        LazyColumn {
            item {
                ItemHeader(
                    value = "Login without favicon",
                    isFavorite = true,
                    iconData = IconData.Local(
                        iconRes = R.drawable.ic_globe,
                    ),
                    relatedLocations = persistentListOf(
                        VaultItemLocation.Organization("Stark Industries"),
                    ),
                )
            }
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun ItemNameField_Org_SingleCollection_Preview() {
    BitwardenTheme {
        LazyColumn {
            item {
                ItemHeader(
                    value = "Login without favicon",
                    isFavorite = true,
                    iconData = IconData.Local(
                        iconRes = R.drawable.ic_globe,
                    ),
                    relatedLocations = persistentListOf(
                        VaultItemLocation.Organization("Stark Industries"),
                        VaultItemLocation.Collection("Marketing"),
                    ),
                )
            }
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun ItemNameField_Org_MultiCollection_Preview() {
    BitwardenTheme {
        LazyColumn {
            item {
                ItemHeader(
                    value = "Login without favicon",
                    isFavorite = true,
                    iconData = IconData.Local(
                        iconRes = R.drawable.ic_globe,
                    ),
                    relatedLocations = persistentListOf(
                        VaultItemLocation.Organization("Stark Industries"),
                        VaultItemLocation.Collection("Marketing"),
                        VaultItemLocation.Collection("Product"),
                    ),
                )
            }
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun ItemNameField_Org_SingleCollection_Folder_Preview() {
    BitwardenTheme {
        LazyColumn {
            item {
                ItemHeader(
                    value = "Note without favicon",
                    isFavorite = true,
                    iconData = IconData.Local(
                        iconRes = R.drawable.ic_note,
                    ),
                    relatedLocations = persistentListOf(
                        VaultItemLocation.Organization("Stark Industries"),
                        VaultItemLocation.Collection("Marketing"),
                        VaultItemLocation.Folder("Competition"),
                    ),
                )
            }
        }
    }
}
//endregion Previews
