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

package com.k689.identid.ui.issuance.add

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.k689.identid.R
import com.k689.identid.config.ConfigNavigation
import com.k689.identid.config.IssuanceFlowType
import com.k689.identid.config.IssuanceSuccessUiConfig
import com.k689.identid.config.IssuanceUiConfig
import com.k689.identid.config.NavigationType
import com.k689.identid.config.OfferUiConfig
import com.k689.identid.config.PresentationMode
import com.k689.identid.config.QrScanFlow
import com.k689.identid.config.QrScanUiConfig
import com.k689.identid.config.RequestUriConfig
import com.k689.identid.controller.authentication.DeviceAuthenticationResult
import com.k689.identid.controller.core.IssuanceMethod
import com.k689.identid.controller.core.IssueDocumentPartialState
import com.k689.identid.di.common.getOrCreateCredentialOfferScope
import com.k689.identid.di.core.getOrCreatePresentationScope
import com.k689.identid.interactor.issuance.AddDocumentInteractor
import com.k689.identid.interactor.issuance.AddDocumentInteractorPartialState
import com.k689.identid.navigation.CommonScreens
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.navigation.IssuanceScreens
import com.k689.identid.navigation.PresentationScreens
import com.k689.identid.navigation.helper.DeepLinkAction
import com.k689.identid.navigation.helper.DeepLinkType
import com.k689.identid.navigation.helper.generateComposableArguments
import com.k689.identid.navigation.helper.generateComposableNavigationLink
import com.k689.identid.navigation.helper.hasDeepLink
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.ui.component.ListItemMainContentDataUi
import com.k689.identid.ui.component.content.ContentErrorConfig
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.issuance.add.model.AddDocumentUi
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
    val navigatableAction: ScreenNavigateAction,
    val onBackAction: (() -> Unit)? = null,
    val issuanceConfig: IssuanceUiConfig,
    val isLoading: Boolean = false,
    val error: ContentErrorConfig? = null,
    val isInitialised: Boolean = false,
    val notifyOnAuthenticationFailure: Boolean = false,
    val title: String = "",
    val subtitle: String = "",
    val options: List<Pair<String, List<AddDocumentUi>>> = emptyList(),
    val filteredOptions: List<Pair<String, List<AddDocumentUi>>> = emptyList(),
    val noOptions: Boolean = false,
    val showFooterScanner: Boolean,
    val searchQuery: String = "",
) : ViewState

sealed class Event : ViewEvent {
    data class Init(
        val deepLink: Uri?,
    ) : Event()

    data object GoToQrScan : Event()

    data object Pop : Event()

    data object OnPause : Event()

    data class OnResumeIssuance(
        val uri: String,
    ) : Event()

    data class OnDynamicPresentation(
        val uri: String,
    ) : Event()

    data object Finish : Event()

    data object DismissError : Event()

    data class OnSearchQueryChanged(
        val query: String,
    ) : Event()

    data class IssueDocument(
        val issuanceMethod: IssuanceMethod,
        val issuerId: String,
        val configId: String,
        val context: Context,
    ) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()

        data object Finish : Navigation()

        data class SwitchScreen(
            val screenRoute: String,
            val inclusive: Boolean,
        ) : Navigation()

        data class OpenDeepLinkAction(
            val deepLinkUri: Uri,
            val arguments: String?,
        ) : Navigation()
    }
}

@KoinViewModel
class AddDocumentViewModel(
    private val addDocumentInteractor: AddDocumentInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
    @InjectedParam private val issuanceConfig: String,
) : MviViewModel<Event, State, Effect>() {
    private var issuanceJob: Job? = null

    override fun setInitialState(): State {
        val deserializedConfig: IssuanceUiConfig =
            uiSerializer.fromBase64(
                payload = issuanceConfig,
                model = IssuanceUiConfig::class.java,
                parser = IssuanceUiConfig.Parser,
            ) ?: throw RuntimeException("IssuanceUiConfig:: is Missing or invalid")

        return State(
            issuanceConfig = deserializedConfig,
            navigatableAction = getNavigatableAction(deserializedConfig.flowType),
            onBackAction = getOnBackAction(deserializedConfig.flowType),
            title = resourceProvider.getString(R.string.issuance_add_document_title),
            subtitle = resourceProvider.getString(R.string.issuance_add_document_subtitle),
            showFooterScanner = shouldShowFooterScanner(deserializedConfig.flowType),
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                if (viewState.value.options.isEmpty()) {
                    getOptions(event, event.deepLink)
                } else {
                    handleDeepLink(event.deepLink)
                }
            }

            is Event.Pop -> {
                setEffect { Effect.Navigation.Pop }
            }

            is Event.DismissError -> {
                setState { copy(error = null) }
            }

            is Event.IssueDocument -> {
                issueDocument(
                    issuanceMethod = event.issuanceMethod,
                    configId = event.configId,
                    issuerId = event.issuerId,
                    context = event.context,
                )
            }

            is Event.Finish -> {
                setEffect { Effect.Navigation.Finish }
            }

            is Event.OnPause -> {
                if (viewState.value.isInitialised) {
                    setState { copy(isLoading = false) }
                }
            }

            is Event.OnResumeIssuance -> {
                setState {
                    copy(isLoading = true)
                }
                addDocumentInteractor.resumeOpenId4VciWithAuthorization(event.uri)
            }

            is Event.OnDynamicPresentation -> {
                getOrCreatePresentationScope()
                setEffect {
                    Effect.Navigation.SwitchScreen(
                        generateComposableNavigationLink(
                            PresentationScreens.PresentationRequest,
                            generateComposableArguments(
                                mapOf(
                                    RequestUriConfig.serializedKeyName to
                                        uiSerializer.toBase64(
                                            RequestUriConfig(
                                                PresentationMode.OpenId4Vp(
                                                    event.uri,
                                                    IssuanceScreens.AddDocument.screenRoute,
                                                ),
                                            ),
                                            RequestUriConfig.Parser,
                                        ),
                                ),
                            ),
                        ),
                        inclusive = false,
                    )
                }
            }

            is Event.GoToQrScan -> {
                navigateToQrScanScreen()
            }

            is Event.OnSearchQueryChanged -> {
                applySearch(event.query)
            }
        }
    }

    private fun getOptions(
        event: Event,
        deepLinkUri: Uri?,
    ) {
        setState {
            copy(
                isLoading = true,
                error = null,
            )
        }

        viewModelScope.launch {
            addDocumentInteractor
                .getAddDocumentOption(
                    flowType = viewState.value.issuanceConfig.flowType,
                ).collect { response ->
                    when (response) {
                        is AddDocumentInteractorPartialState.Success -> {
                            setState {
                                copy(
                                    error = null,
                                    options = response.options,
                                    filteredOptions = response.options,
                                    noOptions = false,
                                    showFooterScanner =
                                        shouldShowFooterScanner(
                                            flowType = viewState.value.issuanceConfig.flowType,
                                        ),
                                    isInitialised = true,
                                    isLoading = false,
                                )
                            }
                            handleDeepLink(deepLinkUri)
                        }

                        is AddDocumentInteractorPartialState.Failure -> {
                            val deepLinkAction = getDeepLinkAction(deepLinkUri)

                            setState {
                                copy(
                                    error =
                                        if (deepLinkAction == null) {
                                            ContentErrorConfig(
                                                onRetry = { setEvent(event) },
                                                errorSubTitle = response.error,
                                                onCancel = { setEvent(Event.DismissError) },
                                            )
                                        } else {
                                            null
                                        },
                                    options = emptyList(),
                                    noOptions = false,
                                    showFooterScanner = false,
                                    isInitialised = true,
                                    isLoading = false,
                                )
                            }
                            deepLinkAction?.let {
                                handleDeepLink(it.first, it.second)
                            }
                        }

                        is AddDocumentInteractorPartialState.NoOptions -> {
                            setState {
                                copy(
                                    error = null,
                                    options = emptyList(),
                                    noOptions = true,
                                    showFooterScanner = true,
                                    isInitialised = true,
                                    isLoading = false,
                                )
                            }
                        }
                    }
                }
        }
    }

    private fun issueDocument(
        issuanceMethod: IssuanceMethod,
        issuerId: String,
        configId: String,
        context: Context,
    ) {
        issuanceJob?.cancel()
        issuanceJob =
            viewModelScope.launch {
                setState {
                    copy(
                        isLoading = true,
                        error = null,
                    )
                }

                addDocumentInteractor
                    .issueDocument(
                        issuanceMethod = issuanceMethod,
                        issuerId = issuerId,
                        configId = configId,
                    ).collect { response ->
                        when (response) {
                            is IssueDocumentPartialState.Failure -> {
                                setState {
                                    copy(
                                        error =
                                            ContentErrorConfig(
                                                onRetry = null,
                                                errorSubTitle = response.errorMessage,
                                                onCancel = { setEvent(Event.DismissError) },
                                            ),
                                        isLoading = false,
                                    )
                                }
                            }

                            is IssueDocumentPartialState.Success -> {
                                setState {
                                    copy(
                                        error = null,
                                        isLoading = false,
                                    )
                                }
                                navigateToDocumentIssuanceSuccessScreen(
                                    documentId = response.documentId,
                                )
                            }

                            is IssueDocumentPartialState.DeferredSuccess -> {
                                setState {
                                    copy(
                                        error = null,
                                        isLoading = false,
                                    )
                                }
                                navigateToGenericSuccessScreen(
                                    route =
                                        addDocumentInteractor.buildGenericSuccessRouteForDeferred(
                                            viewState.value.issuanceConfig.flowType,
                                        ),
                                )
                            }

                            is IssueDocumentPartialState.UserAuthRequired -> {
                                addDocumentInteractor.handleUserAuth(
                                    context = context,
                                    crypto = response.crypto,
                                    notifyOnAuthenticationFailure = viewState.value.notifyOnAuthenticationFailure,
                                    resultHandler =
                                        DeviceAuthenticationResult(
                                            onAuthenticationSuccess = {
                                                response.resultHandler.onAuthenticationSuccess()
                                            },
                                            onAuthenticationError = {
                                                response.resultHandler.onAuthenticationError()
                                            },
                                        ),
                                )
                            }
                        }
                    }
            }
    }

    private fun navigateToDocumentIssuanceSuccessScreen(documentId: String) {
        val onSuccessNavigation =
            when (viewState.value.issuanceConfig.flowType) {
                is IssuanceFlowType.NoDocument -> {
                    ConfigNavigation(
                        navigationType =
                            NavigationType.PushScreen(
                                screen = DashboardScreens.Dashboard,
                                popUpToScreen = IssuanceScreens.AddDocument,
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

        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute =
                    generateComposableNavigationLink(
                        screen = IssuanceScreens.DocumentIssuanceSuccess,
                        arguments =
                            generateComposableArguments(
                                mapOf(
                                    IssuanceSuccessUiConfig.serializedKeyName to
                                        uiSerializer
                                            .toBase64(
                                                model =
                                                    IssuanceSuccessUiConfig(
                                                        documentIds = listOf(documentId),
                                                        onSuccessNavigation = onSuccessNavigation,
                                                    ),
                                                parser = IssuanceSuccessUiConfig.Parser,
                                            ).orEmpty(),
                                ),
                            ),
                    ),
                inclusive = false,
            )
        }
    }

    private fun navigateToGenericSuccessScreen(route: String) {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = route,
                inclusive = true,
            )
        }
    }

    private fun navigateToQrScanScreen() {
        setEffect {
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
                                                title = resourceProvider.getString(R.string.issuance_qr_scan_title),
                                                subTitle = resourceProvider.getString(R.string.issuance_qr_scan_subtitle),
                                                qrScanFlow = QrScanFlow.Issuance(viewState.value.issuanceConfig.flowType),
                                            ),
                                            QrScanUiConfig.Parser,
                                        ),
                                ),
                            ),
                    ),
                inclusive = false,
            )
        }
    }

    private fun applySearch(query: String) {
        val allOptions = viewState.value.options
        val filtered =
            if (query.isBlank()) {
                allOptions
            } else {
                allOptions
                    .map { (issuerId, items) ->
                        issuerId to
                            items.filter { item ->
                                val text = (item.itemData.mainContentData as? ListItemMainContentDataUi.Text)?.text
                                text == null || text.contains(query, ignoreCase = true)
                            }
                    }.filter { (_, items) -> items.isNotEmpty() }
            }
        setState {
            copy(
                searchQuery = query,
                filteredOptions = filtered,
            )
        }
    }

    private fun shouldShowFooterScanner(flowType: IssuanceFlowType): Boolean =
        when (flowType) {
            is IssuanceFlowType.NoDocument -> true
            is IssuanceFlowType.ExtraDocument -> false
        }

    private fun getNavigatableAction(flowType: IssuanceFlowType): ScreenNavigateAction =
        when (flowType) {
            is IssuanceFlowType.NoDocument -> ScreenNavigateAction.NONE
            is IssuanceFlowType.ExtraDocument -> ScreenNavigateAction.BACKABLE
        }

    private fun getOnBackAction(flowType: IssuanceFlowType): (() -> Unit) =
        when (flowType) {
            is IssuanceFlowType.NoDocument -> {
                { setEvent(Event.Finish) }
            }

            is IssuanceFlowType.ExtraDocument -> {
                { setEvent(Event.Pop) }
            }
        }

    private fun getDeepLinkAction(deepLinkUri: Uri?): Pair<Uri, DeepLinkAction>? =
        deepLinkUri?.let { uri ->
            hasDeepLink(uri)?.let {
                uri to it
            }
        }

    private fun handleDeepLink(deepLinkUri: Uri?) {
        getDeepLinkAction(deepLinkUri)?.let { pair ->
            handleDeepLink(pair.first, pair.second)
        }
    }

    private fun handleDeepLink(
        uri: Uri,
        action: DeepLinkAction,
    ) {
        when (action.type) {
            DeepLinkType.CREDENTIAL_OFFER -> {
                getOrCreateCredentialOfferScope()
                setEffect {
                    Effect.Navigation.OpenDeepLinkAction(
                        deepLinkUri = uri,
                        arguments =
                            generateComposableArguments(
                                mapOf(
                                    OfferUiConfig.serializedKeyName to
                                        uiSerializer.toBase64(
                                            OfferUiConfig(
                                                offerUri = action.link.toString(),
                                                onSuccessNavigation =
                                                    ConfigNavigation(
                                                        navigationType =
                                                            NavigationType.PushScreen(
                                                                screen = DashboardScreens.Dashboard,
                                                                popUpToScreen = IssuanceScreens.AddDocument,
                                                            ),
                                                    ),
                                                onCancelNavigation =
                                                    ConfigNavigation(
                                                        navigationType = NavigationType.Pop,
                                                    ),
                                            ),
                                            OfferUiConfig.Parser,
                                        ),
                                ),
                            ),
                    )
                }
            }

            DeepLinkType.EXTERNAL -> {
                setEffect {
                    Effect.Navigation.OpenDeepLinkAction(
                        deepLinkUri = uri,
                        arguments = null,
                    )
                }
            }

            else -> {}
        }
    }
}
