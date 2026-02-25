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

package com.k689.identid.ui.dashboard.documents.list.model

import com.k689.identid.model.core.DocumentCategory
import com.k689.identid.model.core.DocumentIdentifier
import com.k689.identid.model.validator.FilterableItemPayload
import com.k689.identid.ui.component.ListItemDataUi
import com.k689.identid.ui.dashboard.documents.detail.model.DocumentIssuanceStateUi

data class DocumentUi(
    val documentIssuanceState: DocumentIssuanceStateUi,
    val uiData: ListItemDataUi,
    val documentIdentifier: DocumentIdentifier,
    val documentCategory: DocumentCategory,
) : FilterableItemPayload
