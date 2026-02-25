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

package com.k689.identid.interactor.presentation

import com.k689.identid.config.RequestUriConfig
import com.k689.identid.config.toDomainConfig
import com.k689.identid.controller.core.TransferEventPartialState
import com.k689.identid.controller.core.WalletCoreDocumentsController
import com.k689.identid.controller.core.WalletCorePresentationController
import com.k689.identid.extension.business.safeAsync
import com.k689.identid.provider.UuidProvider
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.ui.common.request.model.RequestDocumentItemUi
import com.k689.identid.ui.common.request.transformer.RequestTransformer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

sealed class PresentationRequestInteractorPartialState {
    data class Success(
        val verifierName: String?,
        val verifierIsTrusted: Boolean,
        val requestDocuments: List<RequestDocumentItemUi>,
    ) : PresentationRequestInteractorPartialState()

    data class NoData(
        val verifierName: String?,
        val verifierIsTrusted: Boolean,
    ) : PresentationRequestInteractorPartialState()

    data class Failure(
        val error: String,
    ) : PresentationRequestInteractorPartialState()

    data object Disconnect : PresentationRequestInteractorPartialState()
}

interface PresentationRequestInteractor {
    fun getRequestDocuments(): Flow<PresentationRequestInteractorPartialState>

    fun stopPresentation()

    fun updateRequestedDocuments(items: List<RequestDocumentItemUi>)

    fun setConfig(config: RequestUriConfig)
}

class PresentationRequestInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val uuidProvider: UuidProvider,
    private val walletCorePresentationController: WalletCorePresentationController,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
) : PresentationRequestInteractor {
    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun setConfig(config: RequestUriConfig) {
        walletCorePresentationController.setConfig(config.toDomainConfig())
    }

    override fun getRequestDocuments(): Flow<PresentationRequestInteractorPartialState> =
        walletCorePresentationController.events
            .mapNotNull { response ->
                when (response) {
                    is TransferEventPartialState.RequestReceived -> {
                        if (response.requestData.all { it.requestedItems.isEmpty() }) {
                            PresentationRequestInteractorPartialState.NoData(
                                verifierName = response.verifierName,
                                verifierIsTrusted = response.verifierIsTrusted,
                            )
                        } else {
                            val documentsDomain =
                                RequestTransformer
                                    .transformToDomainItems(
                                        storageDocuments = walletCoreDocumentsController.getAllIssuedDocuments(),
                                        requestDocuments = response.requestData,
                                        resourceProvider = resourceProvider,
                                        uuidProvider = uuidProvider,
                                    ).getOrThrow()
                                    .filterNot {
                                        walletCoreDocumentsController.isDocumentRevoked(it.docId)
                                    }

                            if (documentsDomain.isNotEmpty()) {
                                PresentationRequestInteractorPartialState.Success(
                                    verifierName = response.verifierName,
                                    verifierIsTrusted = response.verifierIsTrusted,
                                    requestDocuments =
                                        RequestTransformer.transformToUiItems(
                                            documentsDomain = documentsDomain,
                                            resourceProvider = resourceProvider,
                                        ),
                                )
                            } else {
                                PresentationRequestInteractorPartialState.NoData(
                                    verifierName = response.verifierName,
                                    verifierIsTrusted = response.verifierIsTrusted,
                                )
                            }
                        }
                    }

                    is TransferEventPartialState.Error -> {
                        PresentationRequestInteractorPartialState.Failure(error = response.error)
                    }

                    is TransferEventPartialState.Disconnected -> {
                        PresentationRequestInteractorPartialState.Disconnect
                    }

                    else -> {
                        null
                    }
                }
            }.safeAsync {
                PresentationRequestInteractorPartialState.Failure(
                    error = it.localizedMessage ?: genericErrorMsg,
                )
            }

    override fun stopPresentation() {
        walletCorePresentationController.stopPresentation()
    }

    override fun updateRequestedDocuments(items: List<RequestDocumentItemUi>) {
        val disclosedDocuments = RequestTransformer.createDisclosedDocuments(items)
        walletCorePresentationController.updateRequestedDocuments(disclosedDocuments.toMutableList())
    }
}
