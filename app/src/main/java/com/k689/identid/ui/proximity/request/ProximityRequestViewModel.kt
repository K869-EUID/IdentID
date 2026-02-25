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

package com.k689.identid.ui.proximity.request

import androidx.lifecycle.viewModelScope
import com.k689.identid.R
import com.k689.identid.config.BiometricMode
import com.k689.identid.config.BiometricUiConfig
import com.k689.identid.config.ConfigNavigation
import com.k689.identid.config.NavigationType
import com.k689.identid.config.OnBackNavigationConfig
import com.k689.identid.config.RequestUriConfig
import com.k689.identid.extension.business.ifEmptyOrNull
import com.k689.identid.interactor.proximity.ProximityRequestInteractor
import com.k689.identid.interactor.proximity.ProximityRequestInteractorPartialState
import com.k689.identid.navigation.CommonScreens
import com.k689.identid.navigation.ProximityScreens
import com.k689.identid.navigation.helper.generateComposableArguments
import com.k689.identid.navigation.helper.generateComposableNavigationLink
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.ui.common.request.Event
import com.k689.identid.ui.common.request.RequestViewModel
import com.k689.identid.ui.common.request.model.RequestDocumentItemUi
import com.k689.identid.ui.component.RelyingPartyDataUi
import com.k689.identid.ui.component.content.ContentErrorConfig
import com.k689.identid.ui.component.content.ContentHeaderConfig
import com.k689.identid.ui.serializer.UiSerializer
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class ProximityRequestViewModel(
    private val interactor: ProximityRequestInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
    @InjectedParam private val requestUriConfigRaw: String,
) : RequestViewModel() {
    override fun getHeaderConfig(): ContentHeaderConfig =
        ContentHeaderConfig(
            description = resourceProvider.getString(R.string.request_header_description),
            mainText = resourceProvider.getString(R.string.request_header_main_text),
            relyingPartyData =
                getRelyingPartyData(
                    name = null,
                    isVerified = false,
                ),
        )

    override fun getNextScreen(): String =
        generateComposableNavigationLink(
            screen = CommonScreens.Biometric,
            arguments =
                generateComposableArguments(
                    mapOf(
                        BiometricUiConfig.serializedKeyName to
                            uiSerializer
                                .toBase64(
                                    BiometricUiConfig(
                                        mode =
                                            BiometricMode.Default(
                                                descriptionWhenBiometricsEnabled = resourceProvider.getString(R.string.loading_biometry_biometrics_enabled_description),
                                                descriptionWhenBiometricsNotEnabled = resourceProvider.getString(R.string.loading_biometry_biometrics_not_enabled_description),
                                                textAbovePin = resourceProvider.getString(R.string.biometric_default_mode_text_above_pin_field),
                                            ),
                                        isPreAuthorization = false,
                                        shouldInitializeBiometricAuthOnCreate = true,
                                        onSuccessNavigation =
                                            ConfigNavigation(
                                                navigationType = NavigationType.PushScreen(ProximityScreens.Loading),
                                            ),
                                        onBackNavigationConfig =
                                            OnBackNavigationConfig(
                                                onBackNavigation =
                                                    ConfigNavigation(
                                                        navigationType = NavigationType.PopTo(ProximityScreens.Request),
                                                    ),
                                                hasToolbarBackIcon = true,
                                            ),
                                    ),
                                    BiometricUiConfig.Parser,
                                ).orEmpty(),
                    ),
                ),
        )

    override fun doWork() {
        setState {
            copy(
                isLoading = true,
                error = null,
            )
        }

        val requestUriConfig =
            uiSerializer.fromBase64(
                requestUriConfigRaw,
                RequestUriConfig::class.java,
                RequestUriConfig.Parser,
            ) ?: throw RuntimeException("RequestUriConfig:: is Missing or invalid")

        interactor.setConfig(requestUriConfig)

        viewModelJob =
            viewModelScope.launch {
                interactor.getRequestDocuments().collect { response ->
                    when (response) {
                        is ProximityRequestInteractorPartialState.Failure -> {
                            setState {
                                copy(
                                    isLoading = false,
                                    error =
                                        ContentErrorConfig(
                                            onRetry = { setEvent(Event.DoWork) },
                                            errorSubTitle = response.error,
                                            onCancel = { setEvent(Event.Pop) },
                                        ),
                                )
                            }
                        }

                        is ProximityRequestInteractorPartialState.Success -> {
                            updateData(response.requestDocuments)

                            val updatedHeaderConfig =
                                viewState.value.headerConfig.copy(
                                    relyingPartyData =
                                        getRelyingPartyData(
                                            name = response.verifierName,
                                            isVerified = response.verifierIsTrusted,
                                        ),
                                )

                            setState {
                                copy(
                                    isLoading = false,
                                    error = null,
                                    headerConfig = updatedHeaderConfig,
                                    items = response.requestDocuments,
                                )
                            }
                        }

                        is ProximityRequestInteractorPartialState.Disconnect -> {
                            setEvent(Event.Pop)
                        }

                        is ProximityRequestInteractorPartialState.NoData -> {
                            val updatedHeaderConfig =
                                viewState.value.headerConfig.copy(
                                    relyingPartyData =
                                        getRelyingPartyData(
                                            name = response.verifierName,
                                            isVerified = response.verifierIsTrusted,
                                        ),
                                )

                            setState {
                                copy(
                                    isLoading = false,
                                    error = null,
                                    headerConfig = updatedHeaderConfig,
                                    noItems = true,
                                )
                            }
                        }
                    }
                }
            }
    }

    override fun updateData(
        updatedItems: List<RequestDocumentItemUi>,
        allowShare: Boolean?,
    ) {
        super.updateData(updatedItems, allowShare)
        interactor.updateRequestedDocuments(updatedItems)
    }

    override fun cleanUp() {
        super.cleanUp()
        interactor.stopPresentation()
    }

    private fun getRelyingPartyData(
        name: String?,
        isVerified: Boolean,
    ): RelyingPartyDataUi =
        RelyingPartyDataUi(
            isVerified = isVerified,
            name =
                name.ifEmptyOrNull(
                    default = resourceProvider.getString(R.string.request_relying_party_default_name),
                ),
            description = resourceProvider.getString(R.string.request_relying_party_description),
        )
}
