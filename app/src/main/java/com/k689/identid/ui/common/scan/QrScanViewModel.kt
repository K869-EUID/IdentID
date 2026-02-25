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

package com.k689.identid.ui.common.scan

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.k689.identid.R
import com.k689.identid.config.ConfigNavigation
import com.k689.identid.config.IssuanceFlowType
import com.k689.identid.config.NavigationType
import com.k689.identid.config.OfferUiConfig
import com.k689.identid.config.PresentationMode
import com.k689.identid.config.QrScanFlow
import com.k689.identid.config.QrScanUiConfig
import com.k689.identid.config.RequestUriConfig
import com.k689.identid.di.common.getOrCreateCredentialOfferScope
import com.k689.identid.di.core.getOrCreatePresentationScope
import com.k689.identid.interactor.common.QrScanInteractor
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.navigation.IssuanceScreens
import com.k689.identid.navigation.PresentationScreens
import com.k689.identid.navigation.helper.generateComposableArguments
import com.k689.identid.navigation.helper.generateComposableNavigationLink
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import com.k689.identid.ui.serializer.UiSerializer
import com.k689.identid.validator.Form
import com.k689.identid.validator.Rule
import eu.europa.ec.eudi.rqesui.domain.extension.toUriOrEmpty
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

private const val MAX_ALLOWED_FAILED_SCANS = 5

data class State(
    val hasCameraPermission: Boolean = false,
    val shouldShowPermissionRational: Boolean = false,
    val finishedScanning: Boolean = false,
    val qrScannedConfig: QrScanUiConfig,
    val failedScanAttempts: Int = 0,
    val showInformativeText: Boolean = false,
    val informativeText: String,
) : ViewState

sealed class Event : ViewEvent {
    data object GoBack : Event()

    data class OnQrScanned(
        val context: Context,
        val resultQr: String,
    ) : Event()

    data object CameraAccessGranted : Event()

    data object ShowPermissionRational : Event()

    data object GoToAppSettings : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(
            val screenRoute: String,
        ) : Navigation()

        data object Pop : Navigation()

        data object GoToAppSettings : Navigation()
    }
}

@KoinViewModel
class QrScanViewModel(
    private val interactor: QrScanInteractor,
    private val uiSerializer: UiSerializer,
    private val resourceProvider: ResourceProvider,
    @InjectedParam private val qrScannedConfig: String,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State {
        val deserializedConfig: QrScanUiConfig =
            uiSerializer.fromBase64(
                qrScannedConfig,
                QrScanUiConfig::class.java,
                QrScanUiConfig.Parser,
            ) ?: throw RuntimeException("QrScanUiConfig:: is Missing or invalid")
        return State(
            qrScannedConfig = deserializedConfig,
            informativeText = calculateInformativeText(deserializedConfig.qrScanFlow),
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.GoBack -> {
                setEffect { Effect.Navigation.Pop }
            }

            is Event.OnQrScanned -> {
                if (viewState.value.finishedScanning) {
                    return
                }
                setState {
                    copy(finishedScanning = true)
                }

                handleScannedQr(context = event.context, scannedQr = event.resultQr)
            }

            is Event.CameraAccessGranted -> {
                setState {
                    copy(hasCameraPermission = true)
                }
            }

            is Event.ShowPermissionRational -> {
                setState {
                    copy(shouldShowPermissionRational = true)
                }
            }

            is Event.GoToAppSettings -> {
                setEffect { Effect.Navigation.GoToAppSettings }
            }
        }
    }

    private fun handleScannedQr(
        context: Context,
        scannedQr: String,
    ) {
        viewModelScope.launch {
            val currentState = viewState.value

            // Validate the scanned QR code
            val urlIsValid =
                validateForm(
                    form =
                        Form(
                            inputs =
                                mapOf(
                                    listOf(
                                        Rule.ValidateUrl(
                                            errorMessage = "",
                                            shouldValidateSchema = true,
                                            shouldValidateHost = false,
                                            shouldValidatePath = false,
                                            shouldValidateQuery = true,
                                        ),
                                    ) to scannedQr,
                                ),
                        ),
                )

            // Handle valid QR code
            if (urlIsValid) {
                calculateNextStep(
                    context = context,
                    qrScanFlow = currentState.qrScannedConfig.qrScanFlow,
                    scanResult = scannedQr,
                )
            } else {
                // Increment failed attempts
                val updatedFailedAttempts = currentState.failedScanAttempts + 1
                val maxFailedAttemptsExceeded = updatedFailedAttempts > MAX_ALLOWED_FAILED_SCANS

                setState {
                    copy(
                        failedScanAttempts = updatedFailedAttempts,
                        showInformativeText = maxFailedAttemptsExceeded,
                        finishedScanning = false,
                    )
                }
            }
        }
    }

    private suspend fun validateForm(form: Form): Boolean {
        val validationResult =
            interactor.validateForm(
                form = form,
            )
        return validationResult.isValid
    }

    private fun calculateNextStep(
        context: Context,
        qrScanFlow: QrScanFlow,
        scanResult: String,
    ) {
        when (qrScanFlow) {
            is QrScanFlow.Presentation -> {
                navigateToPresentationRequest(scanResult)
            }

            is QrScanFlow.Issuance -> {
                navigateToDocumentOffer(
                    scanResult = scanResult,
                    issuanceFlowType = qrScanFlow.issuanceFlowType,
                )
            }

            is QrScanFlow.Signature -> {
                navigateToRqesSdk(context, scanResult)
            }
        }
    }

    private fun calculateInformativeText(
        qrScanFlow: QrScanFlow,
    ): String =
        with(resourceProvider) {
            when (qrScanFlow) {
                is QrScanFlow.Presentation -> getString(R.string.qr_scan_informative_text_presentation_flow)
                is QrScanFlow.Issuance -> getString(R.string.qr_scan_informative_text_issuance_flow)
                is QrScanFlow.Signature -> getString(R.string.qr_scan_informative_text_signature_flow)
            }
        }

    private fun navigateToPresentationRequest(scanResult: String) {
        setEffect {
            getOrCreatePresentationScope()
            Effect.Navigation.SwitchScreen(
                screenRoute =
                    generateComposableNavigationLink(
                        screen = PresentationScreens.PresentationRequest,
                        arguments =
                            generateComposableArguments(
                                mapOf(
                                    RequestUriConfig.serializedKeyName to
                                        uiSerializer.toBase64(
                                            RequestUriConfig(
                                                PresentationMode.OpenId4Vp(
                                                    uri = scanResult,
                                                    initiatorRoute = DashboardScreens.Dashboard.screenRoute,
                                                ),
                                            ),
                                            RequestUriConfig.Parser,
                                        ),
                                ),
                            ),
                    ),
            )
        }
    }

    private fun navigateToDocumentOffer(
        scanResult: String,
        issuanceFlowType: IssuanceFlowType,
    ) {
        getOrCreateCredentialOfferScope()
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute =
                    generateComposableNavigationLink(
                        screen = IssuanceScreens.DocumentOffer,
                        arguments =
                            generateComposableArguments(
                                mapOf(
                                    OfferUiConfig.serializedKeyName to
                                        uiSerializer.toBase64(
                                            OfferUiConfig(
                                                offerUri = scanResult,
                                                onSuccessNavigation =
                                                    calculateOnSuccessNavigation(
                                                        issuanceFlowType,
                                                    ),
                                                onCancelNavigation =
                                                    calculateOnCancelNavigation(
                                                        issuanceFlowType,
                                                    ),
                                            ),
                                            OfferUiConfig.Parser,
                                        ),
                                ),
                            ),
                    ),
            )
        }
    }

    private fun navigateToRqesSdk(
        context: Context,
        scanResult: String,
    ) {
        interactor.launchRqesSdk(
            context = context,
            uri = scanResult.toUriOrEmpty(),
        )
        setEffect {
            Effect.Navigation.Pop
        }
    }

    private fun calculateOnSuccessNavigation(issuanceFlowType: IssuanceFlowType): ConfigNavigation =
        when (issuanceFlowType) {
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

    private fun calculateOnCancelNavigation(issuanceFlowType: IssuanceFlowType): ConfigNavigation =
        when (issuanceFlowType) {
            is IssuanceFlowType.NoDocument -> {
                ConfigNavigation(
                    navigationType = NavigationType.Pop,
                )
            }

            is IssuanceFlowType.ExtraDocument -> {
                ConfigNavigation(
                    navigationType = NavigationType.Pop,
                )
            }
        }
}
