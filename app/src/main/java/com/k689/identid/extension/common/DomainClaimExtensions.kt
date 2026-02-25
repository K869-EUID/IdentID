/*
 * Copyright (c) 2025 European Commission
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

package com.k689.identid.extension.common

import com.k689.identid.model.core.ClaimDomain
import com.k689.identid.ui.common.request.model.DocumentPayloadDomain
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.ListItemDataUi
import com.k689.identid.ui.component.ListItemLeadingContentDataUi
import com.k689.identid.ui.component.ListItemMainContentDataUi
import com.k689.identid.ui.component.ListItemTrailingContentDataUi
import com.k689.identid.ui.component.wrap.CheckboxDataUi
import com.k689.identid.ui.component.wrap.ExpandableListItemUi
import com.k689.identid.util.common.keyIsPortrait
import com.k689.identid.util.common.keyIsSignature
import eu.europa.ec.eudi.wallet.document.ElementIdentifier

fun DocumentPayloadDomain.toSelectiveExpandableListItems(): List<ExpandableListItemUi> =
    this.docClaimsDomain.map { claim ->
        claim.toSelectiveExpandableListItems(docId)
    }

fun ClaimDomain.toSelectiveExpandableListItems(docId: String): ExpandableListItemUi =
    when (this) {
        is ClaimDomain.Group -> {
            ExpandableListItemUi.NestedListItem(
                header =
                    ListItemDataUi(
                        itemId = path.toId(docId),
                        mainContentData = ListItemMainContentDataUi.Text(text = displayTitle),
                        trailingContentData = ListItemTrailingContentDataUi.Icon(iconData = AppIcons.KeyboardArrowDown),
                    ),
                nestedItems =
                    items.map {
                        it.toSelectiveExpandableListItems(docId)
                    },
                isExpanded = false,
            )
        }

        is ClaimDomain.Primitive -> {
            ExpandableListItemUi.SingleListItem(
                header =
                    ListItemDataUi(
                        itemId = path.toId(docId),
                        mainContentData = calculateMainContent(key, value),
                        overlineText = calculateOverlineText(displayTitle),
                        leadingContentData = calculateLeadingContent(key, value),
                        trailingContentData =
                            ListItemTrailingContentDataUi.Checkbox(
                                checkboxData =
                                    CheckboxDataUi(
                                        isChecked = true,
                                        enabled = !isRequired,
                                    ),
                            ),
                    ),
            )
        }
    }

fun ClaimDomain.toExpandableListItems(docId: String): ExpandableListItemUi =
    when (this) {
        is ClaimDomain.Group -> {
            ExpandableListItemUi.NestedListItem(
                header =
                    ListItemDataUi(
                        itemId = path.toId(docId),
                        mainContentData = ListItemMainContentDataUi.Text(text = displayTitle),
                        trailingContentData = ListItemTrailingContentDataUi.Icon(iconData = AppIcons.KeyboardArrowDown),
                    ),
                nestedItems = items.map { it.toExpandableListItems(docId = docId) },
                isExpanded = false,
            )
        }

        is ClaimDomain.Primitive -> {
            ExpandableListItemUi.SingleListItem(
                header =
                    ListItemDataUi(
                        itemId = path.toId(docId),
                        mainContentData = calculateMainContent(key, value),
                        overlineText = calculateOverlineText(displayTitle),
                        leadingContentData = calculateLeadingContent(key, value),
                    ),
            )
        }
    }

private fun calculateMainContent(
    key: ElementIdentifier,
    value: String,
): ListItemMainContentDataUi =
    when {
        keyIsPortrait(key = key) -> {
            ListItemMainContentDataUi.Text(text = "")
        }

        keyIsSignature(key = key) -> {
            ListItemMainContentDataUi.Image(base64Image = value)
        }

        else -> {
            ListItemMainContentDataUi.Text(text = value)
        }
    }

private fun calculateLeadingContent(
    key: ElementIdentifier,
    value: String,
): ListItemLeadingContentDataUi? =
    if (keyIsPortrait(key = key)) {
        ListItemLeadingContentDataUi.UserImage(userBase64Image = value)
    } else {
        null
    }

private fun calculateOverlineText(displayTitle: String): String? =
    displayTitle.ifBlank {
        null
    }
