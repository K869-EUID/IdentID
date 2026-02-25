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

package com.k689.identid.ui.dashboard.transactions.list.model

import androidx.annotation.StringRes
import com.k689.identid.R
import com.k689.identid.util.business.endOfDay
import com.k689.identid.util.business.endOfMonth
import com.k689.identid.util.business.endOfWeek
import com.k689.identid.util.business.monthYearFormatter
import com.k689.identid.util.business.startOfDay
import com.k689.identid.util.business.startOfMonth
import com.k689.identid.util.business.startOfWeek
import java.time.LocalDateTime

sealed class TransactionCategoryUi(
    @param:StringRes val stringResId: Int,
    val id: Int,
    val order: Int,
    val dateRange: ClosedRange<LocalDateTime>? = null,
    val displayName: String? = null,
) {
    data object Today : TransactionCategoryUi(
        stringResId = R.string.transaction_category_today,
        id = 1,
        order = Int.MAX_VALUE,
        dateRange = LocalDateTime.now().startOfDay()..LocalDateTime.now().endOfDay(),
    )

    data object ThisWeek : TransactionCategoryUi(
        stringResId = R.string.transaction_category_this_week,
        id = 2,
        order = Int.MAX_VALUE - 1,
        dateRange = LocalDateTime.now().startOfWeek()..LocalDateTime.now().endOfWeek(),
    )

    class Month(
        dateTime: LocalDateTime,
    ) : TransactionCategoryUi(
            stringResId = R.string.transaction_category_month_year,
            id = generateMonthId(dateTime),
            order = calculateMonthOrder(dateTime),
            dateRange = dateTime.startOfMonth()..dateTime.endOfMonth(),
            displayName = monthYearFormatter.format(dateTime).uppercase(),
        ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Month) return false

            val thisStart = this.dateRange?.start
            val otherStart = other.dateRange?.start

            return thisStart?.year == otherStart?.year &&
                thisStart?.monthValue == otherStart?.monthValue
        }

        override fun hashCode(): Int =
            dateRange?.let {
                it.start.year * 100 + it.start.monthValue
            } ?: 0
    }
}

private fun generateMonthId(dateTime: LocalDateTime): Int = dateTime.year * 100 + dateTime.monthValue

private fun calculateMonthOrder(dateTime: LocalDateTime): Int = dateTime.year * 100 + dateTime.monthValue
