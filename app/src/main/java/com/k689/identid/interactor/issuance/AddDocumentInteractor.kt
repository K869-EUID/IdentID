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
import com.k689.identid.config.IssuanceFlowType
import com.k689.identid.config.NavigationType
import com.k689.identid.config.SuccessUIConfig
import com.k689.identid.controller.authentication.BiometricsAvailability
import com.k689.identid.controller.authentication.DeviceAuthenticationResult
import com.k689.identid.controller.core.FetchScopedDocumentsPartialState
import com.k689.identid.controller.core.IssuanceMethod
import com.k689.identid.controller.core.IssueDocumentPartialState
import com.k689.identid.controller.core.WalletCoreDocumentsController
import com.k689.identid.extension.business.safeAsync
import com.k689.identid.interactor.common.DeviceAuthenticationInteractor
import com.k689.identid.model.authentication.BiometricCrypto
import com.k689.identid.model.core.FormatType
import com.k689.identid.navigation.CommonScreens
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.navigation.IssuanceScreens
import com.k689.identid.navigation.helper.generateComposableArguments
import com.k689.identid.navigation.helper.generateComposableNavigationLink
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.theme.values.ThemeColors
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.ListItemDataUi
import com.k689.identid.ui.component.ListItemMainContentDataUi
import com.k689.identid.ui.component.ListItemTrailingContentDataUi
import com.k689.identid.ui.component.utils.PERCENTAGE_25
import com.k689.identid.ui.issuance.add.model.AddDocumentUi
import com.k689.identid.ui.serializer.UiSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class AddDocumentInteractorPartialState {
    data class Success(
        val options: List<Pair<String, List<AddDocumentUi>>>,
    ) : AddDocumentInteractorPartialState()

    data class NoOptions(
        val errorMsg: String,
    ) : AddDocumentInteractorPartialState()

    data class Failure(
        val error: String,
    ) : AddDocumentInteractorPartialState()
}

interface AddDocumentInteractor {
    fun getAddDocumentOption(
        flowType: IssuanceFlowType,
    ): Flow<AddDocumentInteractorPartialState>

    fun issueDocument(
        issuanceMethod: IssuanceMethod,
        configId: String,
        issuerId: String,
    ): Flow<IssueDocumentPartialState>

    fun handleUserAuth(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult,
    )

    fun buildGenericSuccessRouteForDeferred(flowType: IssuanceFlowType): String

    fun resumeOpenId4VciWithAuthorization(uri: String)
}

class AddDocumentInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
) : AddDocumentInteractor {
    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getAddDocumentOption(
        flowType: IssuanceFlowType,
    ): Flow<AddDocumentInteractorPartialState> =
        flow {
            val state =
                walletCoreDocumentsController.getScopedDocuments(resourceProvider.getLocale())
            when (state) {
                is FetchScopedDocumentsPartialState.Failure -> {
                    emit(
                        AddDocumentInteractorPartialState.Failure(
                            error = state.errorMessage,
                        ),
                    )
                }

                is FetchScopedDocumentsPartialState.Success -> {
                    val customFormatType: FormatType? =
                        (flowType as? IssuanceFlowType.ExtraDocument)?.formatType

                    val options: List<Pair<String, List<AddDocumentUi>>> =
                        state.documents
                            .asSequence()
                            .filter { doc ->
                                (customFormatType == null || doc.formatType == customFormatType) &&
                                    (flowType !is IssuanceFlowType.NoDocument || doc.isPid)
                            }.sortedWith(
                                compareBy(
                                    { it.credentialIssuerId },
                                    { it.name.lowercase() },
                                ),
                            ).map { doc ->
                                AddDocumentUi(
                                    credentialIssuerId = doc.credentialIssuerId,
                                    configurationId = doc.configurationId,
                                    itemData =
                                        ListItemDataUi(
                                            itemId = doc.configurationId,
                                            mainContentData = ListItemMainContentDataUi.Text(text = doc.name),
                                            trailingContentData =
                                                ListItemTrailingContentDataUi.Icon(
                                                    iconData = AppIcons.Add,
                                                ),
                                        ),
                                )
                            }.groupBy { it.credentialIssuerId }
                            .entries
                            .map { (issuer, items) -> issuer to items }

                    if (options.isEmpty()) {
                        emit(
                            AddDocumentInteractorPartialState.NoOptions(
                                errorMsg = resourceProvider.getString(R.string.issuance_add_document_no_options),
                            ),
                        )
                    } else {
                        emit(
                            AddDocumentInteractorPartialState.Success(
                                options = options,
                            ),
                        )
                    }
                }
            }
        }.safeAsync {
            AddDocumentInteractorPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg,
            )
        }

    override fun issueDocument(
        issuanceMethod: IssuanceMethod,
        configId: String,
        issuerId: String,
    ): Flow<IssueDocumentPartialState> =
        walletCoreDocumentsController.issueDocument(
            issuanceMethod = issuanceMethod,
            configId = configId,
            issuerId = issuerId,
        )

    override fun handleUserAuth(
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

    override fun buildGenericSuccessRouteForDeferred(flowType: IssuanceFlowType): String {
        val navigation =
            when (flowType) {
                is IssuanceFlowType.NoDocument -> {
                    ConfigNavigation(
                        navigationType =
                            NavigationType.PushRoute(
                                route = DashboardScreens.Dashboard.screenRoute,
                                popUpToRoute = IssuanceScreens.AddDocument.screenRoute,
                            ),
                    )
                }

                is IssuanceFlowType.ExtraDocument -> {
                    ConfigNavigation(
                        navigationType =
                            NavigationType.PopTo(
                                screen = DashboardScreens.Dashboard,
                            ),
                    )
                }
            }
        val successScreenArguments = getSuccessScreenArgumentsForDeferred(navigation)
        return generateComposableNavigationLink(
            screen = CommonScreens.Success,
            arguments = successScreenArguments,
        )
    }

    override fun resumeOpenId4VciWithAuthorization(uri: String) {
        walletCoreDocumentsController.resumeOpenId4VciWithAuthorization(uri)
    }

    private fun getSuccessScreenArgumentsForDeferred(
        navigation: ConfigNavigation,
    ): String {
        val (textElementsConfig, imageConfig, buttonText) =
            Triple(
                first =
                    SuccessUIConfig.TextElementsConfig(
                        text = resourceProvider.getString(R.string.issuance_add_document_deferred_success_text),
                        description = resourceProvider.getString(R.string.issuance_add_document_deferred_success_description),
                        color = ThemeColors.pending,
                    ),
                second =
                    SuccessUIConfig.ImageConfig(
                        type = SuccessUIConfig.ImageConfig.Type.Drawable(icon = AppIcons.InProgress),
                        tint = ThemeColors.primary,
                        screenPercentageSize = PERCENTAGE_25,
                    ),
                third = resourceProvider.getString(R.string.issuance_add_document_deferred_success_primary_button_text),
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
