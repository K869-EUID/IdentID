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

import android.content.Context
import com.k689.identid.R
import com.k689.identid.config.ConfigNavigation
import com.k689.identid.config.SuccessUIConfig
import com.k689.identid.controller.authentication.BiometricsAvailability
import com.k689.identid.controller.authentication.DeviceAuthenticationResult
import com.k689.identid.controller.core.IssueDocumentsPartialState
import com.k689.identid.controller.core.ResolveDocumentOfferPartialState
import com.k689.identid.controller.core.WalletCoreDocumentsController
import com.k689.identid.extension.business.safeAsync
import com.k689.identid.extension.core.documentIdentifier
import com.k689.identid.extension.core.getIssuerLogo
import com.k689.identid.extension.core.getIssuerName
import com.k689.identid.extension.core.getName
import com.k689.identid.interactor.common.DeviceAuthenticationInteractor
import com.k689.identid.model.authentication.BiometricCrypto
import com.k689.identid.model.core.DocumentIdentifier
import com.k689.identid.navigation.CommonScreens
import com.k689.identid.navigation.helper.generateComposableArguments
import com.k689.identid.navigation.helper.generateComposableNavigationLink
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.theme.values.ThemeColors
import com.k689.identid.ui.common.CredentialOfferIssuanceScope
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.utils.PERCENTAGE_25
import com.k689.identid.ui.issuance.offer.model.DocumentOfferUi
import com.k689.identid.ui.serializer.UiSerializer
import com.k689.identid.util.business.safeLet
import eu.europa.ec.eudi.openid4vci.TxCodeInputMode
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped
import java.net.URI

sealed class ResolveDocumentOfferInteractorPartialState {
    data class Success(
        val documents: List<DocumentOfferUi>,
        val issuerName: String,
        val issuerLogo: URI?,
        val txCodeLength: Int?,
    ) : ResolveDocumentOfferInteractorPartialState()

    data class NoDocument(
        val issuerName: String,
        val issuerLogo: URI?,
    ) : ResolveDocumentOfferInteractorPartialState()

    data class Failure(
        val errorMessage: String,
    ) : ResolveDocumentOfferInteractorPartialState()
}

sealed class IssueDocumentsInteractorPartialState {
    data class Success(
        val documentIds: List<DocumentId>,
    ) : IssueDocumentsInteractorPartialState()

    data class DeferredSuccess(
        val successRoute: String,
    ) : IssueDocumentsInteractorPartialState()

    data class Failure(
        val errorMessage: String,
    ) : IssueDocumentsInteractorPartialState()

    data class UserAuthRequired(
        val crypto: BiometricCrypto,
        val resultHandler: DeviceAuthenticationResult,
    ) : IssueDocumentsInteractorPartialState()
}

interface DocumentOfferInteractor {
    val credentialOffers: MutableMap<String, Offer>

    fun resolveDocumentOffer(offerUri: String): Flow<ResolveDocumentOfferInteractorPartialState>

    fun issueDocuments(
        offerUri: String,
        issuerName: String,
        navigation: ConfigNavigation,
        txCode: String? = null,
    ): Flow<IssueDocumentsInteractorPartialState>

    fun handleUserAuthentication(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult,
    )

    fun resumeOpenId4VciWithAuthorization(uri: String)
}

@Scope(CredentialOfferIssuanceScope::class)
@Scoped
class DocumentOfferInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
) : DocumentOfferInteractor {
    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override val credentialOffers: MutableMap<String, Offer> = mutableMapOf()

    override fun resolveDocumentOffer(offerUri: String): Flow<ResolveDocumentOfferInteractorPartialState> =
        flow {
            val userLocale = resourceProvider.getLocale()
            walletCoreDocumentsController
                .resolveDocumentOffer(
                    offerUri = offerUri,
                ).map { response ->
                    when (response) {
                        is ResolveDocumentOfferPartialState.Failure -> {
                            ResolveDocumentOfferInteractorPartialState.Failure(errorMessage = response.errorMessage)
                        }

                        is ResolveDocumentOfferPartialState.Success -> {
                            credentialOffers[offerUri] = response.offer

                            val offerHasNoDocuments = response.offer.offeredDocuments.isEmpty()
                            if (offerHasNoDocuments) {
                                ResolveDocumentOfferInteractorPartialState.NoDocument(
                                    issuerName = response.offer.getIssuerName(userLocale),
                                    issuerLogo = response.offer.getIssuerLogo(userLocale),
                                )
                            } else {
                                val codeMinLength = 4
                                val codeMaxLength = 6

                                safeLet(
                                    response.offer.txCodeSpec?.inputMode,
                                    response.offer.txCodeSpec?.length,
                                ) { inputMode, length ->

                                    if ((length !in codeMinLength..codeMaxLength) || inputMode == TxCodeInputMode.TEXT) {
                                        return@map ResolveDocumentOfferInteractorPartialState.Failure(
                                            errorMessage =
                                                resourceProvider.getString(
                                                    R.string.issuance_document_offer_error_invalid_txcode_format,
                                                    codeMinLength,
                                                    codeMaxLength,
                                                ),
                                        )
                                    }
                                }

                                val hasMainPid =
                                    walletCoreDocumentsController.getMainPidDocument() != null

                                val hasPidInOffer =
                                    response.offer.offeredDocuments.any { offeredDocument ->
                                        val id = offeredDocument.documentIdentifier
                                        id == DocumentIdentifier.MdocPid || id == DocumentIdentifier.SdJwtPid
                                    }

                                if (hasMainPid || hasPidInOffer) {
                                    ResolveDocumentOfferInteractorPartialState.Success(
                                        documents =
                                            response.offer.offeredDocuments.map { offeredDocument ->
                                                DocumentOfferUi(
                                                    title = offeredDocument.getName(userLocale).orEmpty(),
                                                )
                                            },
                                        issuerName = response.offer.getIssuerName(userLocale),
                                        issuerLogo = response.offer.getIssuerLogo(userLocale),
                                        txCodeLength = response.offer.txCodeSpec?.length,
                                    )
                                } else {
                                    ResolveDocumentOfferInteractorPartialState.Failure(
                                        errorMessage =
                                            resourceProvider.getString(
                                                R.string.issuance_document_offer_error_missing_pid_text,
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }.collect {
                    emit(it)
                }
        }.safeAsync {
            ResolveDocumentOfferInteractorPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMsg,
            )
        }

    override fun issueDocuments(
        offerUri: String,
        issuerName: String,
        navigation: ConfigNavigation,
        txCode: String?,
    ): Flow<IssueDocumentsInteractorPartialState> =
        flow {
            credentialOffers[offerUri]?.let { offer ->
                walletCoreDocumentsController
                    .issueDocumentsByOffer(
                        offer = offer,
                        txCode = txCode,
                    ).map { response ->
                        when (response) {
                            is IssueDocumentsPartialState.Failure -> {
                                IssueDocumentsInteractorPartialState.Failure(errorMessage = response.errorMessage)
                            }

                            is IssueDocumentsPartialState.PartialSuccess -> {
                                IssueDocumentsInteractorPartialState.Success(
                                    documentIds = response.documentIds,
                                )
                            }

                            is IssueDocumentsPartialState.Success -> {
                                IssueDocumentsInteractorPartialState.Success(
                                    documentIds = response.documentIds,
                                )
                            }

                            is IssueDocumentsPartialState.UserAuthRequired -> {
                                IssueDocumentsInteractorPartialState.UserAuthRequired(
                                    crypto = response.crypto,
                                    resultHandler = response.resultHandler,
                                )
                            }

                            is IssueDocumentsPartialState.DeferredSuccess -> {
                                IssueDocumentsInteractorPartialState.DeferredSuccess(
                                    successRoute =
                                        buildGenericSuccessRouteForDeferred(
                                            description =
                                                resourceProvider.getString(
                                                    R.string.issuance_document_offer_deferred_success_description,
                                                    issuerName,
                                                ),
                                            navigation = navigation,
                                        ),
                                )
                            }
                        }
                    }.collect {
                        emit(it)
                    }
            } ?: emit(
                IssueDocumentsInteractorPartialState.Failure(
                    errorMessage = genericErrorMsg,
                ),
            )
        }.safeAsync {
            IssueDocumentsInteractorPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMsg,
            )
        }

    override fun handleUserAuthentication(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult,
    ) {
        deviceAuthenticationInteractor.getBiometricsAvailability {
            when (it) {
                is BiometricsAvailability.CanAuthenticate -> {
                    deviceAuthenticationInteractor.authenticateWithBiometrics(
                        context = context,
                        crypto = crypto,
                        notifyOnAuthenticationFailure = notifyOnAuthenticationFailure,
                        resultHandler = resultHandler,
                    )
                }

                is BiometricsAvailability.NonEnrolled -> {
                    deviceAuthenticationInteractor.launchBiometricSystemScreen()
                }

                is BiometricsAvailability.Failure -> {
                    resultHandler.onAuthenticationFailure()
                }
            }
        }
    }

    override fun resumeOpenId4VciWithAuthorization(uri: String) {
        walletCoreDocumentsController.resumeOpenId4VciWithAuthorization(uri)
    }

    private fun buildGenericSuccessRouteForDeferred(
        description: String,
        navigation: ConfigNavigation,
    ): String {
        val successScreenArguments = getDeferredSuccessScreenArguments(description, navigation)
        return generateComposableNavigationLink(
            screen = CommonScreens.Success,
            arguments = successScreenArguments,
        )
    }

    private fun getDeferredSuccessScreenArguments(
        description: String,
        navigation: ConfigNavigation,
    ): String {
        val (textElementsConfig, imageConfig, buttonText) =
            Triple(
                first =
                    SuccessUIConfig.TextElementsConfig(
                        text = resourceProvider.getString(R.string.issuance_document_offer_deferred_success_text),
                        description = description,
                        color = ThemeColors.pending,
                    ),
                second =
                    SuccessUIConfig.ImageConfig(
                        type =
                            SuccessUIConfig.ImageConfig.Type.Drawable(
                                icon = AppIcons.InProgress,
                            ),
                        tint = ThemeColors.primary,
                        screenPercentageSize = PERCENTAGE_25,
                    ),
                third = resourceProvider.getString(R.string.issuance_document_offer_deferred_success_primary_button_text),
            )

        return generateComposableArguments(
            mapOf(
                SuccessUIConfig.serializedKeyName to
                    uiSerializer
                        .toBase64(
                            SuccessUIConfig(
                                textElementsConfig = textElementsConfig,
                                imageConfig = imageConfig,
                                buttonConfig =
                                    listOf(
                                        SuccessUIConfig.ButtonConfig(
                                            text = buttonText,
                                            style = SuccessUIConfig.ButtonConfig.Style.PRIMARY,
                                            navigation = navigation,
                                        ),
                                    ),
                                onBackScreenToNavigate = navigation,
                            ),
                            SuccessUIConfig.Parser,
                        ).orEmpty(),
            ),
        )
    }
}
