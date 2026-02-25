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

package com.k689.identid.ui.issuance.success

import androidx.lifecycle.viewModelScope
import com.k689.identid.config.ConfigNavigation
import com.k689.identid.config.IssuanceSuccessUiConfig
import com.k689.identid.interactor.issuance.DocumentIssuanceSuccessInteractor
import com.k689.identid.interactor.issuance.DocumentIssuanceSuccessInteractorGetUiItemsPartialState
import com.k689.identid.ui.common.document.sucess.DocumentSuccessViewModel
import com.k689.identid.ui.serializer.UiSerializer
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class DocumentIssuanceSuccessViewModel(
    private val interactor: DocumentIssuanceSuccessInteractor,
    private val uiSerializer: UiSerializer,
    @InjectedParam private val issuanceSuccessSerializedConfig: String,
) : DocumentSuccessViewModel() {
    override fun getNextScreenConfigNavigation(): ConfigNavigation {
        val deserializedIssuanceSuccessUiConfig = getDeserializedIssuanceSuccessUiConfig()

        return deserializedIssuanceSuccessUiConfig.onSuccessNavigation
    }

    override fun doWork() {
        val deserializedIssuanceSuccessUiConfig = getDeserializedIssuanceSuccessUiConfig()

        setState {
            copy(isLoading = true)
        }

        viewModelScope.launch {
            interactor
                .getUiItems(
                    documentIds = deserializedIssuanceSuccessUiConfig.documentIds,
                ).collect { response ->
                    when (response) {
                        is DocumentIssuanceSuccessInteractorGetUiItemsPartialState.Failed -> {
                            setState {
                                copy(
                                    isLoading = false,
                                )
                            }
                        }

                        is DocumentIssuanceSuccessInteractorGetUiItemsPartialState.Success -> {
                            setState {
                                copy(
                                    headerConfig = response.headerConfig,
                                    items = response.documentsUi,
                                    isLoading = false,
                                )
                            }
                        }
                    }
                }
        }
    }

    private fun getDeserializedIssuanceSuccessUiConfig(): IssuanceSuccessUiConfig {
        val deserializedIssuanceSuccessUiConfig =
            uiSerializer.fromBase64(
                payload = issuanceSuccessSerializedConfig,
                model = IssuanceSuccessUiConfig::class.java,
                parser = IssuanceSuccessUiConfig.Parser,
            ) ?: throw RuntimeException("IssuanceSuccessUiConfig:: is Missing or invalid")
        return deserializedIssuanceSuccessUiConfig
    }
}
