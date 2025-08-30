package com.x8bit.bitwarden.ui.vault.feature.item.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.nullableTestTag
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.header.BitwardenExpandingHeader
import com.bitwarden.ui.platform.components.icon.BitwardenIcon
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.item.model.VaultItemLocation
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Reusable composable for displaying the cipher name, favorite status, and related locations.
 *
 * @param value The name of the cipher.
 * @param isFavorite Whether the cipher is a favorite.
 * @param relatedLocations The locations the cipher is assigned to.
 * @param iconData The icon to be displayed.
 * @param isExpanded Whether the related locations are expanded.
 * @param applyIconBackground Whether a background should be applied to the header icon.
 * @param iconTestTag The test tag for the icon.
 * @param textFieldTestTag The test tag for the name field.
 * @param onExpandClick The action to be performed when the expandable text row is clicked.
 */
@Suppress("CyclomaticComplexMethod", "LongMethod", "LongParameterList")
fun LazyListScope.itemHeader(
    value: String,
    isFavorite: Boolean,
    relatedLocations: ImmutableList<VaultItemLocation>,
    iconData: IconData,
    isExpanded: Boolean,
    applyIconBackground: Boolean,
    iconTestTag: String? = null,
    textFieldTestTag: String? = null,
    onExpandClick: () -> Unit,
) {
    val organizationLocation = relatedLocations
        .firstOrNull { it is VaultItemLocation.Organization }

    val collectionLocations = relatedLocations
        .filterIsInstance<VaultItemLocation.Collection>()

    val folderLocations = relatedLocations
        .filterIsInstance<VaultItemLocation.Folder>()

    item {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .defaultMinSize(minHeight = 60.dp)
                .cardStyle(
                    cardStyle = CardStyle.Top(),
                    paddingVertical = 0.dp,
                )
                .padding(start = 16.dp),
        ) {
            ItemHeaderIcon(
                iconData = iconData,
                testTag = iconTestTag,
                applyBackgroundFill = applyIconBackground,
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
                                BitwardenDrawable.ic_favorite_full
                            } else {
                                BitwardenDrawable.ic_favorite_empty
                            },
                        ),
                        contentDescription = stringResource(
                            id = if (isFavorite) {
                                BitwardenString.favorite
                            } else {
                                BitwardenString.unfavorite
                            },
                        ),
                        modifier = Modifier.padding(all = 12.dp),
                    )
                },
                textFieldTestTag = textFieldTestTag,
                cardStyle = null,
                textStyle = BitwardenTheme.typography.titleMedium,
            )
        }
    }

    // When the item does not belong to an Org and is not assigned to a collection or folder we
    // display the "No Folder" indicator.
    if (relatedLocations.isEmpty()) {
        item(key = "noFolder") {
            ItemLocationListItem(
                vectorPainter = rememberVectorPainter(BitwardenDrawable.ic_folder),
                text = stringResource(BitwardenString.no_folder),
                iconTestTag = "NoFolderIcon",
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth()
                    .animateItem()
                    .cardStyle(
                        cardStyle = CardStyle.Bottom,
                        paddingVertical = 0.dp,
                        paddingHorizontal = 16.dp,
                    ),
            )
        }
        return
    }

    // When the item is owned by an Org we display the organization name.
    if (organizationLocation != null) {
        item(key = "organization") {
            ItemLocationListItem(
                vectorPainter = rememberVectorPainter(organizationLocation.icon),
                iconTestTag = "ItemLocationIcon",
                text = organizationLocation.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .animateItem()
                    .cardStyle(
                        cardStyle = if (relatedLocations.size == 1) {
                            CardStyle.Bottom
                        } else {
                            CardStyle.Middle(hasDivider = false)
                        },
                        paddingVertical = 0.dp,
                        paddingHorizontal = 16.dp,
                    ),
            )
        }
    }

    // When the item is assigned to a single collection and a single folder we display both the
    // collection and folder names.
    if (collectionLocations.size == 1 && folderLocations.size == 1) {
        itemsIndexed(
            items = collectionLocations + folderLocations,
            key = { index, location -> "locations_$index" },
        ) { index, location ->
            ItemLocationListItem(
                vectorPainter = rememberVectorPainter(location.icon),
                iconTestTag = "ItemLocationIcon",
                text = location.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .animateItem()
                    .cardStyle(
                        cardStyle = if (index == 1) {
                            CardStyle.Bottom
                        } else {
                            CardStyle.Middle(hasDivider = false)
                        },
                        paddingVertical = 0.dp,
                        paddingHorizontal = 16.dp,
                    ),
            )
        }
        return
    }

    // When the item is assigned to a single folder and not a collection we display the folder name.
    if (folderLocations.isNotEmpty() && collectionLocations.isEmpty()) {
        val folderLocation = folderLocations.first()
        item(key = "folder") {
            ItemLocationListItem(
                vectorPainter = rememberVectorPainter(folderLocation.icon),
                iconTestTag = "ItemLocationIcon",
                text = folderLocation.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .animateItem()
                    .cardStyle(
                        cardStyle = CardStyle.Bottom,
                        paddingVertical = 0.dp,
                        paddingHorizontal = 16.dp,
                    ),
            )
        }
        return
    }

    // When the item is assigned to multiple collections we only display the first collection by
    // default and collapse the remaining locations.
    collectionLocations.firstOrNull()
        ?.let {
            item(key = "visibleCollection") {
                ItemLocationListItem(
                    vectorPainter = rememberVectorPainter(it.icon),
                    iconTestTag = "ItemLocationIcon",
                    text = if (collectionLocations.size > 1 && !isExpanded) {
                        stringResource(BitwardenString.x_ellipses, it.name)
                    } else {
                        it.name
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem()
                        .cardStyle(
                            cardStyle = if (collectionLocations.size > 1) {
                                CardStyle.Middle(hasDivider = false)
                            } else {
                                CardStyle.Bottom
                            },
                            paddingVertical = 0.dp,
                            paddingHorizontal = 16.dp,
                        ),
                )
            }
        }

    if (isExpanded) {
        itemsIndexed(
            key = { index, _ -> "expandableLocations_$index" },
            items = collectionLocations.drop(1) + folderLocations,
        ) { index, location ->
            ItemLocationListItem(
                vectorPainter = rememberVectorPainter(location.icon),
                text = location.name,
                iconTestTag = "ItemLocationIcon",
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .animateItem()
                    .cardStyle(
                        cardStyle = CardStyle.Middle(hasDivider = false),
                        paddingVertical = 0.dp,
                        paddingHorizontal = 16.dp,
                    ),
            )
        }
    }

    if (collectionLocations.size > 1) {
        item(key = "expandableLocationsExpansionIndicator") {
            BitwardenExpandingHeader(
                collapsedText = stringResource(BitwardenString.show_more),
                expandedText = stringResource(BitwardenString.show_less),
                isExpanded = isExpanded,
                onClick = onExpandClick,
                showExpansionIndicator = false,
                shape = RectangleShape,
                insets = PaddingValues(),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .animateItem()
                    .cardStyle(
                        cardStyle = CardStyle.Bottom,
                        paddingVertical = 0.dp,
                    ),
            )
        }
    }
}

@Composable
private fun ItemHeaderIcon(
    iconData: IconData,
    applyBackgroundFill: Boolean,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.then(
            if (applyBackgroundFill) {
                Modifier.background(
                    color = BitwardenTheme.colorScheme.illustration.backgroundPrimary,
                    shape = BitwardenTheme.shapes.favicon,
                )
            } else {
                Modifier
            },
        ),
    ) {
        BitwardenIcon(
            iconData = iconData,
            tint = if (applyBackgroundFill) {
                BitwardenTheme.colorScheme.illustration.outline
            } else {
                Color.Unspecified
            },
            modifier = Modifier
                .nullableTestTag(testTag)
                .then(
                    if (!applyBackgroundFill) Modifier.fillMaxSize() else Modifier,
                ),
        )
    }
}

@Composable
private fun LazyItemScope.ItemLocationListItem(
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
@Preview
private fun ItemHeaderWithLocalIcon_Preview() {
    var isExpanded by remember { mutableStateOf(false) }
    BitwardenTheme {
        LazyColumn {
            itemHeader(
                value = "Login without favicon",
                isFavorite = true,
                iconData = IconData.Local(
                    iconRes = BitwardenDrawable.ic_globe,
                ),
                relatedLocations = persistentListOf(),
                isExpanded = isExpanded,
                onExpandClick = { isExpanded = !isExpanded },
                applyIconBackground = true,
            )
        }
    }
}

@Composable
@Preview
private fun ItemHeaderWithNetworkIcon_Preview() {
    var isExpanded by remember { mutableStateOf(false) }
    BitwardenTheme {
        LazyColumn {
            itemHeader(
                value = "Login with favicon",
                isFavorite = true,
                iconData = IconData.Network(
                    uri = "mockuri",
                    fallbackIconRes = BitwardenDrawable.ic_globe,
                ),
                relatedLocations = persistentListOf(),
                isExpanded = isExpanded,
                onExpandClick = { isExpanded = !isExpanded },
                applyIconBackground = false,
            )
        }
    }
}

@Composable
@Preview
private fun ItemHeaderWithOrganization_Preview() {
    var isExpanded by remember { mutableStateOf(false) }
    BitwardenTheme {
        LazyColumn {
            itemHeader(
                value = "Login without favicon",
                isFavorite = true,
                iconData = IconData.Local(
                    iconRes = BitwardenDrawable.ic_globe,
                ),
                relatedLocations = persistentListOf(
                    VaultItemLocation.Organization("Stark Industries"),
                ),
                isExpanded = isExpanded,
                onExpandClick = { isExpanded = !isExpanded },
                applyIconBackground = true,
            )
        }
    }
}

@Composable
@Preview
private fun ItemHeaderWithOrgAndSingleCollection_Preview() {
    var isExpanded by remember { mutableStateOf(false) }
    BitwardenTheme {
        LazyColumn {
            itemHeader(
                value = "Login without favicon",
                isFavorite = true,
                iconData = IconData.Local(
                    iconRes = BitwardenDrawable.ic_globe,
                ),
                relatedLocations = persistentListOf(
                    VaultItemLocation.Organization("Stark Industries"),
                    VaultItemLocation.Collection("Marketing"),
                ),
                isExpanded = isExpanded,
                onExpandClick = { isExpanded = !isExpanded },
                applyIconBackground = true,
            )
        }
    }
}

@Composable
@Preview
private fun ItemHeaderWithOrgAndMultiCollection_Preview() {
    var isExpanded by remember { mutableStateOf(false) }
    BitwardenTheme {
        LazyColumn {
            itemHeader(
                value = "Login without favicon",
                isFavorite = true,
                iconData = IconData.Local(
                    iconRes = BitwardenDrawable.ic_payment_card_brand_visa,
                ),
                relatedLocations = persistentListOf(
                    VaultItemLocation.Organization("Stark Industries"),
                    VaultItemLocation.Collection("Marketing"),
                    VaultItemLocation.Collection("Product"),
                ),
                isExpanded = isExpanded,
                onExpandClick = { isExpanded = !isExpanded },
                applyIconBackground = false,
            )
        }
    }
}

@Composable
@Preview
private fun ItemHeaderWithOrgSingleCollectionAndFolder_Preview() {
    var isExpanded by remember { mutableStateOf(false) }
    BitwardenTheme {
        LazyColumn {
            itemHeader(
                value = "Note without favicon",
                isFavorite = true,
                iconData = IconData.Local(
                    iconRes = BitwardenDrawable.ic_note,
                ),
                relatedLocations = persistentListOf(
                    VaultItemLocation.Organization("Stark Industries"),
                    VaultItemLocation.Collection("Marketing"),
                    VaultItemLocation.Folder("Competition"),
                ),
                isExpanded = isExpanded,
                onExpandClick = { isExpanded = !isExpanded },
                applyIconBackground = true,
            )
        }
    }
}

@Composable
@Preview
private fun ItemHeaderFolderOnly_Preview() {
    var isExpanded by remember { mutableStateOf(false) }
    BitwardenTheme {
        LazyColumn {
            itemHeader(
                value = "SSH key in a folder",
                isFavorite = true,
                iconData = IconData.Local(
                    iconRes = BitwardenDrawable.ic_ssh_key,
                ),
                relatedLocations = persistentListOf(
                    VaultItemLocation.Folder("Competition"),
                ),
                isExpanded = isExpanded,
                onExpandClick = { isExpanded = !isExpanded },
                applyIconBackground = true,
            )
        }
    }
}
//endregion Previews
