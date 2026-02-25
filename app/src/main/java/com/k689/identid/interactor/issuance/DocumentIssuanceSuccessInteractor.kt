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

package com.k689.identid.interactor.issuance

import com.k689.identid.R
import com.k689.identid.controller.core.WalletCoreDocumentsController
import com.k689.identid.extension.business.safeAsync
import com.k689.identid.extension.common.toExpandableListItems
import com.k689.identid.extension.core.localizedIssuerMetadata
import com.k689.identid.extension.core.toClaimPaths
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
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.URI

sealed class DocumentIssuanceSuccessInteractorGetUiItemsPartialState {
    data class Success(
        val documentsUi: List<ExpandableListItemUi.NestedListItem>,
        val headerConfig: ContentHeaderConfig,
    ) : DocumentIssuanceSuccessInteractorGetUiItemsPartialState()

    data class Failed(
        val errorMessage: String,
    ) : DocumentIssuanceSuccessInteractorGetUiItemsPartialState()
}

interface DocumentIssuanceSuccessInteractor {
    fun getUiItems(documentIds: List<DocumentId>): Flow<DocumentIssuanceSuccessInteractorGetUiItemsPartialState>
}

class DocumentIssuanceSuccessInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val resourceProvider: ResourceProvider,
    private val uuidProvider: UuidProvider,
) : DocumentIssuanceSuccessInteractor {
    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getUiItems(documentIds: List<DocumentId>): Flow<DocumentIssuanceSuccessInteractorGetUiItemsPartialState> =
        flow {
            val documentsUi = mutableListOf<ExpandableListItemUi.NestedListItem>()

            var issuerName =
                resourceProvider.getString(R.string.issuance_success_header_issuer_default_name)
            val issuerIsTrusted = false
            var issuerLogo: URI? = null

            val userLocale = resourceProvider.getLocale()

            documentIds.forEach { documentId ->
                try {
                    val document =
                        walletCoreDocumentsController.getDocumentById(documentId = documentId) as IssuedDocument

                    val localizedIssuerMetadata = document.localizedIssuerMetadata(userLocale)

                    localizedIssuerMetadata?.name?.let { safeIssuerName ->
                        issuerName = safeIssuerName
                    }

                    localizedIssuerMetadata?.logo?.uri?.let { safeIssuerLogo ->
                        issuerLogo = safeIssuerLogo
                    }

                    val claimsPaths =
                        document.data.claims.flatMap { claim ->
                            claim.toClaimPaths()
                        }

                    val domainClaims =
                        transformPathsToDomainClaims(
                            paths = claimsPaths,
                            claims = document.data.claims,
                            resourceProvider = resourceProvider,
                            uuidProvider = uuidProvider,
                        )

                    val claimsUi =
                        domainClaims.map { selectedDomainClaim ->
                            selectedDomainClaim.toExpandableListItems(docId = documentId)
                        }

                    val documentUi =
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
                            nestedItems = claimsUi,
                            isExpanded = false,
                        )

                    documentsUi.add(documentUi)
                } catch (_: Exception) {
                }
            }

            val headerConfigDescription =
                if (documentsUi.isEmpty()) {
                    resourceProvider.getString(R.string.issuance_success_header_description_when_error)
                } else {
                    resourceProvider.getString(R.string.issuance_success_header_description)
                }
            val headerConfig =
                ContentHeaderConfig(
                    description = headerConfigDescription,
                    relyingPartyData =
                        RelyingPartyDataUi(
                            logo = issuerLogo,
                            name = issuerName,
                            isVerified = issuerIsTrusted,
                        ),
                )

            emit(
                DocumentIssuanceSuccessInteractorGetUiItemsPartialState.Success(
                    documentsUi = documentsUi,
                    headerConfig = headerConfig,
                ),
            )
        }.safeAsync {
            DocumentIssuanceSuccessInteractorGetUiItemsPartialState.Failed(
                errorMessage = it.localizedMessage ?: genericErrorMsg,
            )
        }
}
