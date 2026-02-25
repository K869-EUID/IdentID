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

package com.k689.identid.ui.proximity.qr

import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import com.k689.identid.config.PresentationMode
import com.k689.identid.config.RequestUriConfig
import com.k689.identid.di.core.getOrCreatePresentationScope
import com.k689.identid.interactor.proximity.ProximityQRInteractor
import com.k689.identid.interactor.proximity.ProximityQRPartialState
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.navigation.ProximityScreens
import com.k689.identid.navigation.helper.generateComposableArguments
import com.k689.identid.navigation.helper.generateComposableNavigationLink
import com.k689.identid.ui.component.content.ContentErrorConfig
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import com.k689.identid.ui.serializer.UiSerializer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

data class State(
    val isLoading: Boolean = true,
    val error: ContentErrorConfig? = null,
    val qrCode: String = "",
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()

    data object GoBack : Event()

    data class NfcEngagement(
        val componentActivity: ComponentActivity,
        val enable: Boolean,
    ) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(
            val screenRoute: String,
        ) : Navigation()

        data object Pop : Navigation()
    }
}

@KoinViewModel
class ProximityQRViewModel(
    private val interactor: ProximityQRInteractor,
    private val uiSerializer: UiSerializer,
    @InjectedParam private val requestUriConfigRaw: String,
) : MviViewModel<Event, State, Effect>() {
    private var interactorJob: Job? = null

    override fun setInitialState(): State = State()

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                initializeConfig()
                generateQrCode()
            }

            is Event.GoBack -> {
                cleanUp()
                setState { copy(error = null) }
                setEffect { Effect.Navigation.Pop }
            }

            is Event.NfcEngagement -> {
                interactor.toggleNfcEngagement(
                    event.componentActivity,
                    event.enable,
                )
            }
        }
    }

    private fun initializeConfig() {
        val requestUriConfig =
            uiSerializer.fromBase64(
                requestUriConfigRaw,
                RequestUriConfig::class.java,
                RequestUriConfig.Parser,
            ) ?: throw RuntimeException("RequestUriConfig:: is Missing or invalid")

        interactor.setConfig(requestUriConfig)
    }

    private fun generateQrCode() {
        setState {
            copy(
                isLoading = true,
                error = null,
            )
        }

        interactorJob =
            viewModelScope.launch {
                interactor.startQrEngagement().collect { response ->
                    when (response) {
                        is ProximityQRPartialState.Error -> {
                            setState {
                                copy(
                                    isLoading = false,
                                    error =
                                        ContentErrorConfig(
                                            onRetry = { setEvent(Event.Init) },
                                            errorSubTitle = response.error,
                                            onCancel = { setEvent(Event.GoBack) },
                                        ),
                                )
                            }
                        }

                        is ProximityQRPartialState.QrReady -> {
                            setState {
                                copy(
                                    isLoading = false,
                                    error = null,
                                    qrCode = response.qrCode,
                                )
                            }
                        }

                        is ProximityQRPartialState.Connected -> {
                            unsubscribe()
                            setEffect {
                                Effect.Navigation.SwitchScreen(
                                    screenRoute =
                                        generateComposableNavigationLink(
                                            screen = ProximityScreens.Request,
                                            arguments =
                                                generateComposableArguments(
                                                    mapOf(
                                                        RequestUriConfig.serializedKeyName to
                                                            uiSerializer.toBase64(
                                                                RequestUriConfig(
                                                                    PresentationMode.Ble(
                                                                        DashboardScreens.Dashboard.screenRoute,
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

                        is ProximityQRPartialState.Disconnected -> {
                            unsubscribe()
                            setEvent(Event.GoBack)
                        }
                    }
                }
            }
    }

    /**
     * Required in order to stop receiving emissions from interactor Flow
     * */
    private fun unsubscribe() {
        interactorJob?.cancel()
    }

    /**
     * Stop presentation and remove scope/listeners
     * */
    private fun cleanUp() {
        unsubscribe()
        getOrCreatePresentationScope().close()
        interactor.cancelTransfer()
    }
}
