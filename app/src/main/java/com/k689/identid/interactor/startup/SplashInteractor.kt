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

package com.k689.identid.interactor.startup

import com.k689.identid.R
import com.k689.identid.config.BiometricMode
import com.k689.identid.config.BiometricUiConfig
import com.k689.identid.config.ConfigNavigation
import com.k689.identid.config.IssuanceFlowType
import com.k689.identid.config.IssuanceUiConfig
import com.k689.identid.config.NavigationType
import com.k689.identid.config.OnBackNavigationConfig
import com.k689.identid.controller.core.WalletCoreDocumentsController
import com.k689.identid.interactor.common.QuickPinInteractor
import com.k689.identid.model.common.PinFlow
import com.k689.identid.navigation.CommonScreens
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.navigation.IssuanceScreens
import com.k689.identid.navigation.helper.generateComposableArguments
import com.k689.identid.navigation.helper.generateComposableNavigationLink
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.ui.serializer.UiSerializer

interface SplashInteractor {
    fun getAfterSplashRoute(): String
}

class SplashInteractorImpl(
    private val quickPinInteractor: QuickPinInteractor,
    private val uiSerializer: UiSerializer,
    private val resourceProvider: ResourceProvider,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
) : SplashInteractor {
    private val hasDocuments: Boolean
        get() = walletCoreDocumentsController.getAllDocuments().isNotEmpty()

    override fun getAfterSplashRoute(): String =
        when (quickPinInteractor.hasPin()) {
            true -> {
                getBiometricsConfig()
            }

            false -> {
                getQuickPinConfig()
            }
        }

    private fun getQuickPinConfig(): String =
        generateComposableNavigationLink(
            screen = CommonScreens.QuickPin,
            arguments = generateComposableArguments(mapOf("pinFlow" to PinFlow.CREATE)),
        )

    private fun getBiometricsConfig(): String =
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
                                            BiometricMode.Login(
                                                title = resourceProvider.getString(R.string.biometric_login_title),
                                                subTitleWhenBiometricsEnabled = resourceProvider.getString(R.string.biometric_login_biometrics_enabled_subtitle),
                                                subTitleWhenBiometricsNotEnabled = resourceProvider.getString(R.string.biometric_login_biometrics_not_enabled_subtitle),
                                            ),
                                        isPreAuthorization = true,
                                        shouldInitializeBiometricAuthOnCreate = true,
                                        onSuccessNavigation =
                                            ConfigNavigation(
                                                navigationType =
                                                    NavigationType.PushScreen(
                                                        screen =
                                                            if (hasDocuments) {
                                                                DashboardScreens.Dashboard
                                                            } else {
                                                                IssuanceScreens.AddDocument
                                                            },
                                                        arguments =
                                                            if (!hasDocuments) {
                                                                mapOf(
                                                                    IssuanceUiConfig.serializedKeyName to
                                                                        uiSerializer.toBase64(
                                                                            model =
                                                                                IssuanceUiConfig(
                                                                                    flowType = IssuanceFlowType.NoDocument,
                                                                                ),
                                                                            parser = IssuanceUiConfig.Parser,
                                                                        ),
                                                                )
                                                            } else {
                                                                emptyMap()
                                                            },
                                                    ),
                                            ),
                                        onBackNavigationConfig =
                                            OnBackNavigationConfig(
                                                onBackNavigation =
                                                    ConfigNavigation(
                                                        navigationType = NavigationType.Finish,
                                                    ),
                                                hasToolbarBackIcon = false,
                                            ),
                                    ),
                                    BiometricUiConfig.Parser,
                                ).orEmpty(),
                    ),
                ),
        )
}
