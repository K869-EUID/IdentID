/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package com.k689.identid.ui.component.wrap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.k689.identid.R
import com.k689.identid.extension.ui.exposeTestTagsAsResourceId
import com.k689.identid.extension.ui.optionalTestTag
import com.k689.identid.extension.ui.throttledClickable
import com.k689.identid.theme.values.divider
import com.k689.identid.theme.values.warning
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.IconDataUi
import com.k689.identid.ui.component.ModalOptionUi
import com.k689.identid.ui.component.preview.PreviewTheme
import com.k689.identid.ui.component.preview.ThemeModePreviews
import com.k689.identid.ui.component.utils.ALPHA_DISABLED
import com.k689.identid.ui.component.utils.ALPHA_ENABLED
import com.k689.identid.ui.component.utils.DEFAULT_BIG_ICON_SIZE
import com.k689.identid.ui.component.utils.DEFAULT_ICON_SIZE
import com.k689.identid.ui.component.utils.HSpacer
import com.k689.identid.ui.component.utils.SIZE_MEDIUM
import com.k689.identid.ui.component.utils.SIZE_SMALL
import com.k689.identid.ui.component.utils.SPACING_EXTRA_SMALL
import com.k689.identid.ui.component.utils.SPACING_LARGE
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import com.k689.identid.ui.component.utils.SPACING_SMALL
import com.k689.identid.ui.component.utils.VSpacer
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.util.ui.TestTag

private val defaultBottomSheetPadding: PaddingValues =
    PaddingValues(
        start = SPACING_LARGE.dp,
        end = SPACING_LARGE.dp,
        top = 0.dp,
        bottom = SPACING_LARGE.dp,
    )

private val bottomSheetDefaultBackgroundColor: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainerLowest

private val bottomSheetDefaultTextColor: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

private val bottomSheetSecondaryTextColor: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

/**
 * Data class representing the text content for a bottom sheet.
 *
 * This class holds the title, message, and button texts for a bottom sheet.
 * It also includes flags to indicate if a button should be styled as a warning.
 *
 * @property title The title of the bottom sheet.
 * @property message The message displayed in the bottom sheet.
 * @property positiveButtonText The text for the positive button (e.g., "OK", "Confirm"). Can be null if no positive button is needed.
 * @property isPositiveButtonWarning A flag indicating if the positive button should be styled as a warning (e.g., red color). Defaults to false.
 * @property negativeButtonText The text for the negative button (e.g., "Cancel", "Dismiss"). Can be null if no negative button is needed.
 * @property isNegativeButtonWarning A flag indicating if the negative button should be styled as a warning (e.g., red color). Defaults to false.
 */
data class BottomSheetTextDataUi(
    val title: String,
    val message: String,
    val positiveButtonText: String? = null,
    val isPositiveButtonWarning: Boolean = false,
    val negativeButtonText: String? = null,
    val isNegativeButtonWarning: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrapModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    dragHandle: @Composable (() -> Unit) = { BottomSheetDefaultHandle() },
    sheetContent: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier =
            Modifier
                .exposeTestTagsAsResourceId()
                .then(modifier),
        sheetState = sheetState,
        shape = shape,
        dragHandle = dragHandle,
        content = sheetContent,
    )
}

/**
 * A generic composable function for creating a bottom sheet.
 *
 * This function provides a basic structure for a bottom sheet, including a title and body section.
 * You can customize the content of the title and body by providing composable functions.
 *
 * The bottom sheet is displayed with a default background color and padding.
 *
 * @param titleContent A composable function that provides the content for the title section of the bottom sheet.
 * This content is displayed at the top of the bottom sheet.
 * @param bodyContent A composable function that provides the content for the body section of the bottom sheet.
 * This content is displayed below the title, separated by a medium vertical spacer.
 */
@Composable
fun GenericBottomSheet(
    titleContent: @Composable () -> Unit,
    bodyContent: @Composable () -> Unit,
    sheetBackgroundColor: Color = bottomSheetDefaultBackgroundColor,
    sheetPadding: PaddingValues = defaultBottomSheetPadding,
) {
    Column(
        modifier =
            Modifier
                .wrapContentHeight()
                .background(color = sheetBackgroundColor)
                .fillMaxWidth()
                .padding(sheetPadding),
    ) {
        titleContent()
        VSpacer.Medium()
        bodyContent()
    }
}

/**
 * A composable function that displays a dialog-style bottom sheet.
 *
 * This bottom sheet presents information to the user with optional icons,
 * title, message, and two buttons for positive and negative actions.
 * Buttons are laid out side-by-side and fill equal width.
 *
 * @param textData Data class containing the text content for the bottom sheet.
 * @param leadingIcon An optional icon to be displayed at the beginning of the title.
 * @param leadingIconTint An optional tint color for the leading icon.
 * @param onPositiveClick A lambda function to be executed when the positive button is clicked.
 * @param onNegativeClick A lambda function to be executed when the negative button is clicked.
 */
@Composable
fun DialogBottomSheet(
    textData: BottomSheetTextDataUi,
    leadingIcon: IconDataUi? = null,
    leadingIconTint: Color? = null,
    onPositiveClick: () -> Unit = {},
    positiveButtonTestTag: String? = null,
    onNegativeClick: () -> Unit = {},
    negativeButtonTestTag: String? = null,
) {
    BaseBottomSheet(
        textData = textData,
        leadingIcon = leadingIcon,
        leadingIconTint = leadingIconTint,
        bodyContent = {
            VSpacer.Small()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                textData.negativeButtonText?.let { safeNegativeButtonText ->
                    WrapButton(
                        modifier =
                            Modifier
                                .optionalTestTag(negativeButtonTestTag)
                                .weight(1f),
                        buttonConfig =
                            ButtonConfig(
                                type = ButtonType.SECONDARY,
                                onClick = onNegativeClick,
                                isWarning = textData.isNegativeButtonWarning,
                            ),
                    ) {
                        Text(
                            text = safeNegativeButtonText,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                textData.positiveButtonText?.let { safePositiveButtonText ->
                    WrapButton(
                        modifier =
                            Modifier
                                .optionalTestTag(positiveButtonTestTag)
                                .weight(1f),
                        buttonConfig =
                            ButtonConfig(
                                type = ButtonType.PRIMARY,
                                onClick = onPositiveClick,
                                isWarning = textData.isPositiveButtonWarning,
                            ),
                    ) {
                        Text(
                            text = safePositiveButtonText,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
    )
}

/**
 * A simple bottom sheet composable function.
 *
 * This function displays a basic bottom sheet with a title and message.
 * It can optionally include a leading icon with a custom tint.
 *
 * @param textData An object containing the title and message to be displayed.
 * @param leadingIcon An optional icon displayed at the leading edge of the title.
 * @param leadingIconTint An optional tint color for the leading icon.
 */
@Composable
fun SimpleBottomSheet(
    textData: BottomSheetTextDataUi,
    leadingIcon: IconDataUi? = null,
    leadingIconTint: Color? = null,
) {
    BaseBottomSheet(
        textData = textData,
        leadingIcon = leadingIcon,
        leadingIconTint = leadingIconTint,
    )
}

@Composable
private fun BaseBottomSheet(
    textData: BottomSheetTextDataUi,
    leadingIcon: IconDataUi? = null,
    leadingIconTint: Color? = null,
    bodyContent: @Composable (() -> Unit)? = null,
    sheetBackgroundColor: Color = bottomSheetDefaultBackgroundColor,
    sheetPadding: PaddingValues = defaultBottomSheetPadding,
) {
    Column(
        modifier =
            Modifier
                .wrapContentHeight()
                .background(color = sheetBackgroundColor)
                .fillMaxWidth()
                .padding(sheetPadding),
        verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.let { safeLeadingIcon ->
                WrapIcon(
                    modifier = Modifier.size(DEFAULT_ICON_SIZE.dp),
                    iconData = safeLeadingIcon,
                    customTint = leadingIconTint,
                )
            }
            Text(
                text = textData.title,
                style =
                    MaterialTheme.typography.headlineSmall.copy(
                        color = bottomSheetDefaultTextColor,
                    ),
            )
        }

        Text(
            text = textData.message,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = bottomSheetSecondaryTextColor,
                ),
        )

        bodyContent?.let { safeBodyContent ->
            safeBodyContent()
        }
    }
}

/**
 * A bottom sheet composable that presents two prominent action choices side by side.
 *
 * Each option is rendered as a fully-clickable card with a large icon and a label.
 * A circular "or" badge is overlaid at the center without consuming horizontal space.
 * Both cards are forced to the same height via [IntrinsicSize.Max].
 *
 * @param textData Text content for the header (title + message).
 * @param options Exactly two [ModalOptionUi] items to display.
 * @param onEventSent Callback invoked with the selected option's event.
 * @param hostTab Optional identifier used for generating test tags.
 */
@Composable
fun <T : ViewEvent> BottomSheetWithTwoBigIcons(
    textData: BottomSheetTextDataUi,
    options: List<ModalOptionUi<T>>,
    onEventSent: (T) -> Unit,
    hostTab: String? = null,
) {
    if (options.size == 2) {
        BaseBottomSheet(
            textData = textData,
            bodyContent = {
                VSpacer.Small()

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                ) {
                    BigIconOptionCard(
                        modifier = Modifier.weight(1f),
                        item = options[0],
                        index = 0,
                        hostTab = hostTab,
                        onEventSent = onEventSent,
                    )

                    HSpacer.Medium()

                    BigIconOptionCard(
                        modifier = Modifier.weight(1f),
                        item = options[1],
                        index = 1,
                        hostTab = hostTab,
                        onEventSent = onEventSent,
                    )
                }

                VSpacer.Small()
            },
        )
    }
}

/**
 * A single clickable option card used inside [BottomSheetWithTwoBigIcons].
 *
 * The entire card is the tap target. It shows a large icon at the top and
 * the option title below it, centered. Both cards share the same height
 * via the parent's [IntrinsicSize.Max] constraint.
 */
@Composable
private fun <T : ViewEvent> BigIconOptionCard(
    modifier: Modifier = Modifier,
    item: ModalOptionUi<T>,
    index: Int,
    hostTab: String?,
    onEventSent: (T) -> Unit,
) {
    val cardShape = RoundedCornerShape(SIZE_MEDIUM.dp)
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .clip(cardShape)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = cardShape,
                ).background(containerColor)
                .alpha(
                    alpha = ALPHA_ENABLED.takeIf { item.enabled } ?: ALPHA_DISABLED,
                ).optionalTestTag(
                    hostTab?.let { safeHostTab ->
                        TestTag.buttonInBottomSheetWithTwoBigIcons(
                            hostTab = safeHostTab,
                            index = index,
                        )
                    },
                ).throttledClickable(enabled = item.enabled) {
                    onEventSent(item.event)
                }.padding(SPACING_MEDIUM.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item.leadingIcon?.let { safeLeadingIcon ->
            WrapImage(
                modifier = Modifier.size(DEFAULT_BIG_ICON_SIZE.dp),
                iconData = safeLeadingIcon,
            )
            VSpacer.Medium()
        }

        Text(
            text = item.title,
            style =
                MaterialTheme.typography.titleSmall.copy(
                    color = bottomSheetDefaultTextColor,
                ),
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun <T : ViewEvent> BottomSheetWithOptionsList(
    textData: BottomSheetTextDataUi,
    options: List<ModalOptionUi<T>>,
    onEventSent: (T) -> Unit,
) {
    if (options.isNotEmpty()) {
        BaseBottomSheet(
            textData = textData,
            bodyContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start,
                ) {
                    OptionsList(
                        optionItems = options,
                        itemSelected = onEventSent,
                    )
                }
            },
        )
    }
}

@Composable
private fun <T : ViewEvent> OptionsList(
    optionItems: List<ModalOptionUi<T>>,
    itemSelected: (T) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        itemsIndexed(optionItems) { index, item ->

            OptionListItem(
                item = item,
                itemSelected = itemSelected,
            )

            if (index < optionItems.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = SPACING_EXTRA_SMALL.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun <T : ViewEvent> OptionListItem(
    item: ModalOptionUi<T>,
    itemSelected: (T) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(SIZE_SMALL.dp))
                .throttledClickable {
                    itemSelected(item.event)
                }.padding(
                    horizontal = SPACING_EXTRA_SMALL.dp,
                    vertical = SPACING_MEDIUM.dp,
                ),
        horizontalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item.leadingIcon?.let { safeLeadingIcon ->
            WrapIcon(
                modifier = Modifier.size(DEFAULT_ICON_SIZE.dp),
                iconData = safeLeadingIcon,
                customTint = item.leadingIconTint,
            )
        }

        Text(
            modifier = Modifier.weight(1f),
            text = item.title,
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    color = bottomSheetDefaultTextColor,
                ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        item.trailingIcon?.let { safeTrailingIcon ->
            WrapIcon(
                modifier = Modifier.size(DEFAULT_ICON_SIZE.dp),
                iconData = safeTrailingIcon,
                customTint = item.trailingIconTint,
            )
        }
    }
}

@Composable
private fun BottomSheetDefaultHandle() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(bottomSheetDefaultBackgroundColor)
                .padding(vertical = SPACING_MEDIUM.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WrapIcon(
            iconData = AppIcons.HandleBar,
            customTint = MaterialTheme.colorScheme.divider,
        )
    }
}

// region Previews

@ThemeModePreviews
@Composable
private fun BottomSheetDefaultHandlePreview() {
    PreviewTheme {
        BottomSheetDefaultHandle()
    }
}

@ThemeModePreviews
@Composable
private fun SimpleBottomSheetPreview() {
    PreviewTheme {
        SimpleBottomSheet(
            textData =
                BottomSheetTextDataUi(
                    title = "Title",
                    message = "Message",
                ),
        )
    }
}

@ThemeModePreviews
@Composable
private fun SimpleBottomSheetWithLeadingIconPreview() {
    PreviewTheme {
        SimpleBottomSheet(
            textData =
                BottomSheetTextDataUi(
                    title = "Title",
                    message = "Message",
                ),
            leadingIcon = AppIcons.Warning,
            leadingIconTint = MaterialTheme.colorScheme.warning,
        )
    }
}

@ThemeModePreviews
@Composable
private fun SimpleBottomSheetLongContentPreview() {
    PreviewTheme {
        SimpleBottomSheet(
            textData =
                BottomSheetTextDataUi(
                    title = "Verify your identity",
                    message =
                        "To proceed, we need to verify your identity. " +
                            "Please have your government-issued ID ready and " +
                            "ensure you are in a well-lit environment.",
                ),
            leadingIcon = AppIcons.IdStroke,
            leadingIconTint = MaterialTheme.colorScheme.primary,
        )
    }
}

@ThemeModePreviews
@Composable
private fun DialogBottomSheetPreview() {
    PreviewTheme {
        DialogBottomSheet(
            textData =
                BottomSheetTextDataUi(
                    title = "Title",
                    message = "Message",
                    positiveButtonText = "OK",
                    negativeButtonText = "Cancel",
                ),
        )
    }
}

@ThemeModePreviews
@Composable
private fun DialogBottomSheetWarningPreview() {
    PreviewTheme {
        DialogBottomSheet(
            textData =
                BottomSheetTextDataUi(
                    title = "Delete document?",
                    message = "This action cannot be undone. The document will be permanently removed from your wallet.",
                    positiveButtonText = "Delete",
                    isPositiveButtonWarning = true,
                    negativeButtonText = "Keep it",
                ),
            leadingIcon = AppIcons.Warning,
            leadingIconTint = MaterialTheme.colorScheme.warning,
        )
    }
}

@ThemeModePreviews
@Composable
private fun DialogBottomSheetSingleButtonPreview() {
    PreviewTheme {
        DialogBottomSheet(
            textData =
                BottomSheetTextDataUi(
                    title = "Success",
                    message = "Your document has been added to the wallet.",
                    positiveButtonText = "Got it",
                ),
            leadingIcon = AppIcons.Verified,
            leadingIconTint = MaterialTheme.colorScheme.primary,
        )
    }
}

private data object DummyEventForPreview : ViewEvent

@ThemeModePreviews
@Composable
private fun BottomSheetWithOptionsListPreview() {
    PreviewTheme {
        BottomSheetWithOptionsList(
            textData =
                BottomSheetTextDataUi(
                    title = "Choose an option",
                    message = "Select how you want to proceed",
                ),
            options =
                listOf(
                    ModalOptionUi(
                        title = "Option with no icons",
                        event = DummyEventForPreview,
                    ),
                    ModalOptionUi(
                        title = "Option with leading icon",
                        leadingIcon = AppIcons.Verified,
                        leadingIconTint = MaterialTheme.colorScheme.primary,
                        event = DummyEventForPreview,
                    ),
                    ModalOptionUi(
                        title = "Option with trailing icon",
                        trailingIcon = AppIcons.Edit,
                        trailingIconTint = MaterialTheme.colorScheme.primary,
                        event = DummyEventForPreview,
                    ),
                    ModalOptionUi(
                        title = "Option with leading and trailing icon",
                        leadingIcon = AppIcons.Add,
                        leadingIconTint = MaterialTheme.colorScheme.primary,
                        trailingIcon = AppIcons.ClockTimer,
                        trailingIconTint = MaterialTheme.colorScheme.primary,
                        event = DummyEventForPreview,
                    ),
                    ModalOptionUi(
                        title = "Option with leading and trailing icon and really really really really really long text",
                        leadingIcon = AppIcons.Add,
                        leadingIconTint = MaterialTheme.colorScheme.primary,
                        trailingIcon = AppIcons.ClockTimer,
                        trailingIconTint = MaterialTheme.colorScheme.primary,
                        event = DummyEventForPreview,
                    ),
                ),
            onEventSent = {},
        )
    }
}

@ThemeModePreviews
@Composable
private fun BottomSheetWithTwoBigIconsShortTextPreview() {
    PreviewTheme {
        BottomSheetWithTwoBigIcons(
            textData =
                BottomSheetTextDataUi(
                    title = "Add document",
                    message = "Add documents to your wallet by choosing from a list or by scanning a provided QR code.",
                ),
            options =
                listOf(
                    ModalOptionUi(
                        title = "From authorized source",
                        leadingIcon = AppIcons.AddDocumentFromList,
                        event = DummyEventForPreview,
                        enabled = true,
                    ),
                    ModalOptionUi(
                        title = "Scan QR",
                        leadingIcon = AppIcons.AddDocumentFromQr,
                        event = DummyEventForPreview,
                        enabled = true,
                    ),
                ),
            onEventSent = {},
        )
    }
}

@ThemeModePreviews
@Composable
private fun BottomSheetWithTwoBigIconsEvenTextPreview() {
    PreviewTheme {
        BottomSheetWithTwoBigIcons(
            textData =
                BottomSheetTextDataUi(
                    title = "Present document",
                    message = "Choose how to present your document",
                ),
            options =
                listOf(
                    ModalOptionUi(
                        title = "Enabled Option with icon",
                        leadingIcon = AppIcons.PresentDocumentInPerson,
                        leadingIconTint = MaterialTheme.colorScheme.primary,
                        event = DummyEventForPreview,
                        enabled = true,
                    ),
                    ModalOptionUi(
                        title = "Disabled Option with icon",
                        leadingIcon = AppIcons.PresentDocumentOnline,
                        leadingIconTint = MaterialTheme.colorScheme.primary,
                        event = DummyEventForPreview,
                        enabled = false,
                    ),
                ),
            onEventSent = {},
        )
    }
}

@ThemeModePreviews
@Composable
private fun BottomSheetWithTwoBigIconsLongTextPreview() {
    PreviewTheme {
        BottomSheetWithTwoBigIcons(
            textData =
                BottomSheetTextDataUi(
                    title = "Sign document",
                    message = "Choose how you would like to sign your document",
                ),
            options =
                listOf(
                    ModalOptionUi(
                        title = "Present document in person at a physical location",
                        leadingIcon = AppIcons.PresentDocumentInPerson,
                        leadingIconTint = MaterialTheme.colorScheme.primary,
                        event = DummyEventForPreview,
                        enabled = true,
                    ),
                    ModalOptionUi(
                        title = "Present document remotely via online verification",
                        leadingIcon = AppIcons.PresentDocumentOnline,
                        leadingIconTint = MaterialTheme.colorScheme.primary,
                        event = DummyEventForPreview,
                        enabled = true,
                    ),
                ),
            onEventSent = {},
        )
    }
}

@ThemeModePreviews
@Composable
private fun BottomSheetWithTwoBigIconsUnevenTextPreview() {
    PreviewTheme {
        BottomSheetWithTwoBigIcons(
            textData =
                BottomSheetTextDataUi(
                    title = "Add document",
                    message = "Choose how to add your document",
                ),
            options =
                listOf(
                    ModalOptionUi(
                        title = "Enabled option with a lot of descriptive text here",
                        leadingIcon = AppIcons.PresentDocumentInPerson,
                        leadingIconTint = MaterialTheme.colorScheme.primary,
                        event = DummyEventForPreview,
                        enabled = true,
                    ),
                    ModalOptionUi(
                        title = "Short",
                        leadingIcon = AppIcons.PresentDocumentOnline,
                        leadingIconTint = MaterialTheme.colorScheme.primary,
                        event = DummyEventForPreview,
                        enabled = true,
                    ),
                ),
            onEventSent = {},
        )
    }
}

// endregion
