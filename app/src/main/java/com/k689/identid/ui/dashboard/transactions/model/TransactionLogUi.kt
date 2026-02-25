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

package com.k689.identid.ui.dashboard.transactions.model

import com.k689.identid.R
import com.k689.identid.model.core.TransactionLogDataDomain
import com.k689.identid.provider.resources.ResourceProvider
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog

enum class TransactionStatusUi {
    Completed,
    Failed,
    ;

    companion object {
        fun TransactionStatusUi.toUiText(resourceProvider: ResourceProvider): String =
            when (this) {
                Completed -> resourceProvider.getString(R.string.transactions_filter_item_status_completed)
                Failed -> resourceProvider.getString(R.string.transactions_filter_item_status_failed)
            }
    }
}

enum class TransactionTypeUi {
    PRESENTATION,
    ISSUANCE,
    SIGNING,
}

fun TransactionLog.Status.toTransactionStatusUi(): TransactionStatusUi =
    when (this) {
        TransactionLog.Status.Incomplete, TransactionLog.Status.Error -> TransactionStatusUi.Failed
        TransactionLog.Status.Completed -> TransactionStatusUi.Completed
    }

fun TransactionLogDataDomain.toTransactionTypeUi(): TransactionTypeUi =
    when (this) {
        is TransactionLogDataDomain.IssuanceLog -> TransactionTypeUi.ISSUANCE
        is TransactionLogDataDomain.PresentationLog -> TransactionTypeUi.PRESENTATION
        is TransactionLogDataDomain.SigningLog -> TransactionTypeUi.SIGNING
    }
