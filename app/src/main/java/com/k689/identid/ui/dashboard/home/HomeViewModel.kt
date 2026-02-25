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

package com.k689.identid.ui.dashboard.home

import androidx.lifecycle.viewModelScope
import com.k689.identid.R
import com.k689.identid.config.PresentationMode
import com.k689.identid.config.QrScanFlow
import com.k689.identid.config.QrScanUiConfig
import com.k689.identid.config.RequestUriConfig
import com.k689.identid.di.core.getOrCreatePresentationScope
import com.k689.identid.interactor.dashboard.HomeInteractor
import com.k689.identid.interactor.dashboard.HomeInteractorGetUserNameViaMainPidDocumentPartialState
import com.k689.identid.navigation.CommonScreens
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.navigation.ProximityScreens
import com.k689.identid.navigation.helper.generateComposableArguments
import com.k689.identid.navigation.helper.generateComposableNavigationLink
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.wrap.ActionCardConfig
import com.k689.identid.ui.dashboard.home.HomeScreenBottomSheetContent.Bluetooth
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import com.k689.identid.ui.serializer.UiSerializer
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

enum class BleAvailability {
    AVAILABLE,
    NO_PERMISSION,
    DISABLED,
    UNKNOWN,
}

data class State(
    val isLoading: Boolean = false,
    val isBottomSheetOpen: Boolean = false,
    val sheetContent: HomeScreenBottomSheetContent = HomeScreenBottomSheetContent.Authenticate,
    val welcomeUserMessage: String,
    val authenticateCardConfig: ActionCardConfig,
    val signCardConfig: ActionCardConfig,
    val bleAvailability: BleAvailability = BleAvailability.UNKNOWN,
    val isBleCentralClientModeEnabled: Boolean = false,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()

    data object StartProximityFlow : Event()

    sealed class AuthenticateCard : Event() {
        data object AuthenticatePressed : Event()

        data object LearnMorePressed : Event()
    }

    sealed class SignDocumentCard : Event() {
        data object SignDocumentPressed : Event()

        data object LearnMorePressed : Event()
    }

    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(
            val isOpen: Boolean,
        ) : BottomSheet()

        data object Close : BottomSheet()

        sealed class Authenticate : BottomSheet() {
            data object OpenAuthenticateInPerson : Authenticate()

            data object OpenAuthenticateOnLine : Authenticate()
        }

        sealed class SignDocument : BottomSheet() {
            data object OpenFromDevice : Authenticate()

            data object OpenScanQR : Authenticate()
        }

        sealed class Bluetooth : BottomSheet() {
            data class PrimaryButtonPressed(
                val availability: BleAvailability,
            ) : Bluetooth()

            data object SecondaryButtonPressed : Bluetooth()
        }
    }

    data object OnShowPermissionsRational : Event()

    data class OnPermissionStateChanged(
        val availability: BleAvailability,
    ) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(
            val screenRoute: String,
            val popUpToScreenRoute: String = DashboardScreens.Dashboard.screenRoute,
            val inclusive: Boolean = false,
        ) : Navigation()

        data object OnAppSettings : Navigation()

        data object OnSystemSettings : Navigation()
    }

    data object ShowBottomSheet : Effect()

    data class CloseBottomSheet(
        val hasNextBottomSheet: Boolean,
    ) : Effect()
}

sealed class HomeScreenBottomSheetContent {
    data object Authenticate : HomeScreenBottomSheetContent()

    data object LearnMoreAboutAuthenticate : HomeScreenBottomSheetContent()

    data object LearnMoreAboutSignDocument : HomeScreenBottomSheetContent()

    data object Sign : HomeScreenBottomSheetContent()

    data class Bluetooth(
        val availability: BleAvailability,
    ) : HomeScreenBottomSheetContent()
}

@KoinViewModel
class HomeViewModel(
    private val homeInteractor: HomeInteractor,
    private val uiSerializer: UiSerializer,
    private val resourceProvider: ResourceProvider,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State =
        State(
            welcomeUserMessage = resourceProvider.getString(R.string.home_screen_welcome),
            authenticateCardConfig =
                ActionCardConfig(
                    title = resourceProvider.getString(R.string.home_screen_authentication_card_title),
                    icon = AppIcons.IdCards,
                    primaryButtonText = resourceProvider.getString(R.string.home_screen_authenticate),
                    secondaryButtonText = resourceProvider.getString(R.string.home_screen_learn_more),
                ),
            signCardConfig =
                ActionCardConfig(
                    title = resourceProvider.getString(R.string.home_screen_sign_card_title),
                    icon = AppIcons.Contract,
                    primaryButtonText = resourceProvider.getString(R.string.home_screen_sign),
                    secondaryButtonText = resourceProvider.getString(R.string.home_screen_learn_more),
                ),
            isBleCentralClientModeEnabled = homeInteractor.isBleCentralClientModeEnabled(),
        )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                getUserNameViaMainPidDocument()
            }

            is Event.AuthenticateCard.AuthenticatePressed -> {
                showBottomSheet(
                    sheetContent = HomeScreenBottomSheetContent.Authenticate,
                )
            }

            is Event.AuthenticateCard.LearnMorePressed -> {
                showBottomSheet(
                    sheetContent = HomeScreenBottomSheetContent.LearnMoreAboutAuthenticate,
                )
            }

            is Event.SignDocumentCard.SignDocumentPressed -> {
                showBottomSheet(
                    sheetContent = HomeScreenBottomSheetContent.Sign,
                )
            }

            is Event.SignDocumentCard.LearnMorePressed -> {
                showBottomSheet(
                    sheetContent = HomeScreenBottomSheetContent.LearnMoreAboutSignDocument,
                )
            }

            is Event.BottomSheet.UpdateBottomSheetState -> {
                setState {
                    copy(isBottomSheetOpen = event.isOpen)
                }
            }

            is Event.BottomSheet.Close -> {
                hideBottomSheet()
            }

            is Event.BottomSheet.Authenticate.OpenAuthenticateInPerson -> {
                checkIfBluetoothIsEnabled()
            }

            is Event.BottomSheet.Authenticate.OpenAuthenticateOnLine -> {
                hideBottomSheet()
                navigateToQrScan()
            }

            is Event.BottomSheet.SignDocument.OpenFromDevice -> {
                hideBottomSheet()
                navigateToDocumentSign()
            }

            is Event.BottomSheet.SignDocument.OpenScanQR -> {
                hideBottomSheet()
                navigateToQrSignatureScan()
            }

            is Event.OnPermissionStateChanged -> {
                setState { copy(bleAvailability = event.availability) }
            }

            is Event.OnShowPermissionsRational -> {
                setState { copy(bleAvailability = BleAvailability.UNKNOWN) }
                showBottomSheet(
                    sheetContent =
                        Bluetooth(
                            BleAvailability.NO_PERMISSION,
                        ),
                )
            }

            is Event.StartProximityFlow -> {
                hideBottomSheet()
                startProximityFlow()
            }

            is Event.BottomSheet.Bluetooth.PrimaryButtonPressed -> {
                hideBottomSheet()
                onBleUserAction(event.availability)
            }

            is Event.BottomSheet.Bluetooth.SecondaryButtonPressed -> {
                hideBottomSheet()
            }
        }
    }

    private fun checkIfBluetoothIsEnabled() {
        if (homeInteractor.isBleAvailable()) {
            setState { copy(bleAvailability = BleAvailability.NO_PERMISSION) }
        } else {
            setState { copy(bleAvailability = BleAvailability.DISABLED) }
            hideAndShowNextBottomSheet()
            showBottomSheet(
                sheetContent = Bluetooth(BleAvailability.DISABLED),
            )
        }
    }

    private fun onBleUserAction(availability: BleAvailability) {
        when (availability) {
            BleAvailability.NO_PERMISSION -> {
                setEffect { Effect.Navigation.OnAppSettings }
            }

            BleAvailability.DISABLED -> {
                setEffect { Effect.Navigation.OnSystemSettings }
            }

            else -> {
                // no implementation
            }
        }
    }

    private fun showBottomSheet(sheetContent: HomeScreenBottomSheetContent) {
        setState {
            copy(sheetContent = sheetContent)
        }
        setEffect {
            Effect.ShowBottomSheet
        }
    }

    private fun hideBottomSheet() {
        setEffect {
            Effect.CloseBottomSheet(false)
        }
    }

    private fun hideAndShowNextBottomSheet() {
        setEffect {
            Effect.CloseBottomSheet(true)
        }
    }

    private fun navigateToDocumentSign() {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = DashboardScreens.DocumentSign.screenRoute,
            )
        }
    }

    private fun startProximityFlow() {
        setState { copy(bleAvailability = BleAvailability.AVAILABLE) }
        // Create Koin scope for presentation
        getOrCreatePresentationScope()
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute =
                    generateComposableNavigationLink(
                        screen = ProximityScreens.QR,
                        arguments =
                            generateComposableArguments(
                                mapOf(
                                    RequestUriConfig.serializedKeyName to
                                        uiSerializer.toBase64(
                                            RequestUriConfig(PresentationMode.Ble(DashboardScreens.Dashboard.screenRoute)),
                                            RequestUriConfig.Parser,
                                        ),
                                ),
                            ),
                    ),
            )
        }
    }

    private fun navigateToQrSignatureScan() {
        val navigationEffect =
            Effect.Navigation.SwitchScreen(
                screenRoute =
                    generateComposableNavigationLink(
                        screen = CommonScreens.QrScan,
                        arguments =
                            generateComposableArguments(
                                mapOf(
                                    QrScanUiConfig.serializedKeyName to
                                        uiSerializer.toBase64(
                                            QrScanUiConfig(
                                                title = resourceProvider.getString(R.string.signature_qr_scan_title),
                                                subTitle = resourceProvider.getString(R.string.signature_qr_scan_subtitle),
                                                qrScanFlow = QrScanFlow.Signature,
                                            ),
                                            QrScanUiConfig.Parser,
                                        ),
                                ),
                            ),
                    ),
            )
        setEffect {
            navigationEffect
        }
    }

    private fun navigateToQrScan() {
        val navigationEffect =
            Effect.Navigation.SwitchScreen(
                screenRoute =
                    generateComposableNavigationLink(
                        screen = CommonScreens.QrScan,
                        arguments =
                            generateComposableArguments(
                                mapOf(
                                    QrScanUiConfig.serializedKeyName to
                                        uiSerializer.toBase64(
                                            QrScanUiConfig(
                                                title = resourceProvider.getString(R.string.presentation_qr_scan_title),
                                                subTitle = resourceProvider.getString(R.string.presentation_qr_scan_subtitle),
                                                qrScanFlow = QrScanFlow.Presentation,
                                            ),
                                            QrScanUiConfig.Parser,
                                        ),
                                ),
                            ),
                    ),
            )
        setEffect {
            navigationEffect
        }
    }

    private fun getUserNameViaMainPidDocument() {
        setState {
            copy(
                isLoading = true,
            )
        }
        viewModelScope.launch {
            homeInteractor.getUserNameViaMainPidDocument().collect { response ->
                when (response) {
                    is HomeInteractorGetUserNameViaMainPidDocumentPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                            )
                        }
                    }

                    is HomeInteractorGetUserNameViaMainPidDocumentPartialState.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                welcomeUserMessage =
                                    if (response.userFirstName.isNotBlank()) {
                                        resourceProvider.getString(
                                            R.string.home_screen_welcome_user_message,
                                            response.userFirstName,
                                        )
                                    } else {
                                        resourceProvider.getString(R.string.home_screen_welcome)
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}
