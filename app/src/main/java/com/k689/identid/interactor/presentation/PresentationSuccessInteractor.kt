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

import com.k689.identid.R
import com.k689.identid.controller.core.WalletCoreDocumentsController
import com.k689.identid.controller.core.WalletCorePresentationController
import com.k689.identid.extension.business.ifEmptyOrNull
import com.k689.identid.extension.business.safeAsync
import com.k689.identid.extension.common.toExpandableListItems
import com.k689.identid.extension.core.toClaimPath
import com.k689.identid.provider.UuidProvider
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.ListItemDataUi
import com.k689.identid.ui.component.ListItemMainContentDataUi
import com.k689.identid.ui.component.ListItemTrailingContentDataUi
import com.k689.identid.ui.component.RelyingPartyDataUi
import com.k689.identid.ui.component.content.ContentHeaderConfig
import com.k689.identid.ui.component.wrap.ExpandableListItemUi
import com.k689.identid.util.common.transformPathsToDomainClaims
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.URI

sealed class PresentationSuccessInteractorGetUiItemsPartialState {
    data class Success(
        val documentsUi: List<ExpandableListItemUi.NestedListItem>,
        val headerConfig: ContentHeaderConfig,
    ) : PresentationSuccessInteractorGetUiItemsPartialState()

    data class Failed(
        val errorMessage: String,
    ) : PresentationSuccessInteractorGetUiItemsPartialState()
}

interface PresentationSuccessInteractor {
    val initiatorRoute: String
    val redirectUri: URI?

    fun getUiItems(): Flow<PresentationSuccessInteractorGetUiItemsPartialState>

    fun stopPresentation()
}

class PresentationSuccessInteractorImpl(
    private val walletCorePresentationController: WalletCorePresentationController,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val resourceProvider: ResourceProvider,
    private val uuidProvider: UuidProvider,
) : PresentationSuccessInteractor {
    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override val initiatorRoute: String = walletCorePresentationController.initiatorRoute

    override val redirectUri: URI? = walletCorePresentationController.redirectUri

    override fun getUiItems(): Flow<PresentationSuccessInteractorGetUiItemsPartialState> =
        flow {
            val documentsUi = mutableListOf<ExpandableListItemUi.NestedListItem>()

            val verifierName = walletCorePresentationController.verifierName

            val isVerified = walletCorePresentationController.verifierIsTrusted == true

            walletCorePresentationController.disclosedDocuments?.forEach { disclosedDocument ->
                try {
                    val documentId = disclosedDocument.documentId
                    val document =
                        walletCoreDocumentsController.getDocumentById(documentId = documentId) as IssuedDocument

                    val disclosedClaimPaths =
                        disclosedDocument.disclosedItems.map {
                            it.toClaimPath()
                        }

                    val disclosedClaims =
                        transformPathsToDomainClaims(
                            paths = disclosedClaimPaths,
                            claims = document.data.claims,
                            resourceProvider = resourceProvider,
                            uuidProvider = uuidProvider,
                        )

                    val disclosedClaimsUi =
                        disclosedClaims.map { disclosedClaim ->
                            disclosedClaim.toExpandableListItems(docId = documentId)
                        }

                    if (disclosedClaimsUi.isNotEmpty()) {
                        val disclosedDocumentUi =
                            ExpandableListItemUi.NestedListItem(
                                header =
                                    ListItemDataUi(
                                        itemId = documentId,
                                        mainContentData = ListItemMainContentDataUi.Text(text = document.name),
                                        supportingText = resourceProvider.getString(R.string.document_success_collapsed_supporting_text),
                                        trailingContentData =
                                            ListItemTrailingContentDataUi.Icon(
                                                iconData = AppIcons.KeyboardArrowDown,
                                            ),
                                    ),
                                nestedItems = disclosedClaimsUi,
                                isExpanded = false,
                            )

                        documentsUi.add(disclosedDocumentUi)
                    }
                } catch (_: Exception) {
                }
            }

            val headerConfigDescription =
                if (documentsUi.isEmpty()) {
                    resourceProvider.getString(R.string.document_success_header_description_when_error)
                } else {
                    resourceProvider.getString(R.string.document_success_header_description)
                }
            val headerConfig =
                ContentHeaderConfig(
                    description = headerConfigDescription,
                    relyingPartyData =
                        RelyingPartyDataUi(
                            name =
                                verifierName.ifEmptyOrNull(
                                    default = resourceProvider.getString(R.string.document_success_relying_party_default_name),
                                ),
                            isVerified = isVerified,
                        ),
                )

            emit(
                PresentationSuccessInteractorGetUiItemsPartialState.Success(
                    documentsUi = documentsUi,
                    headerConfig = headerConfig,
                ),
            )
        }.safeAsync {
            PresentationSuccessInteractorGetUiItemsPartialState.Failed(
                errorMessage = it.localizedMessage ?: genericErrorMsg,
            )
        }

    override fun stopPresentation() {
        walletCorePresentationController.stopPresentation()
    }
}
