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

package com.k689.identid.extension.business

import com.k689.identid.model.validator.FilterAction
import com.k689.identid.model.validator.FilterElement
import com.k689.identid.model.validator.FilterableList
import com.k689.identid.model.validator.Filters

fun FilterableList.filterByQuery(searchQuery: String): FilterableList =
    copy(
        items =
            items.filter { item ->
                item.attributes.searchTags.any { searchTag ->
                    searchTag.contains(
                        other = searchQuery,
                        ignoreCase = true,
                    )
                }
            },
    )

internal fun FilterableList.applySort(filters: Filters): FilterableList =
    filters.filterGroups
        .flatMap { it.filters }
        .firstOrNull { it.filterableAction is FilterAction.Sort<*, *> && it.selected }
        ?.filterableAction
        ?.applyFilter(
            filters.sortOrder,
            this,
            FilterElement.FilterItem.emptyFilter(),
        ) ?: this
