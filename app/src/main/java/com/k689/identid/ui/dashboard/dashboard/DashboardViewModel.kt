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

package com.k689.identid.ui.dashboard.dashboard

import android.content.Intent
import android.net.Uri
import com.k689.identid.R
import com.k689.identid.config.ConfigNavigation
import com.k689.identid.config.NavigationType
import com.k689.identid.config.OfferUiConfig
import com.k689.identid.config.PresentationMode
import com.k689.identid.config.RequestUriConfig
import com.k689.identid.di.common.getOrCreateCredentialOfferScope
import com.k689.identid.di.core.getOrCreatePresentationScope
import com.k689.identid.interactor.dashboard.DashboardInteractor
import com.k689.identid.model.common.PinFlow
import com.k689.identid.model.core.RevokedDocumentDataDomain
import com.k689.identid.navigation.CommonScreens
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.navigation.helper.DeepLinkType
import com.k689.identid.navigation.helper.generateComposableArguments
import com.k689.identid.navigation.helper.generateComposableNavigationLink
import com.k689.identid.navigation.helper.hasDeepLink
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.ModalOptionUi
import com.k689.identid.ui.dashboard.dashboard.model.SideMenuItemUi
import com.k689.identid.ui.dashboard.dashboard.model.SideMenuTypeUi
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import com.k689.identid.ui.serializer.UiSerializer
import eu.europa.ec.eudi.wallet.document.DocumentId
import org.koin.android.annotation.KoinViewModel

data class State(
    // side menu
    val isSideMenuVisible: Boolean = false,
    val sideMenuTitle: String,
    val sideMenuOptions: List<SideMenuItemUi>,
    val sideMenuAnimation: SideMenuAnimation = SideMenuAnimation.SLIDE,
    val menuAnimationDuration: Int = 1500,
    val isBottomSheetOpen: Boolean = false,
    val sheetContent: DashboardBottomSheetContent =
        DashboardBottomSheetContent.DocumentRevocation(
            options = emptyList(),
        ),
) : ViewState

sealed class Event : ViewEvent {
    data class Init(
        val deepLinkUri: Uri?,
    ) : Event()

    data object Pop : Event()

    data class DocumentRevocationNotificationReceived(
        val payload: List<RevokedDocumentDataDomain>,
    ) : Event()

    // side menu events
    sealed class SideMenu : Event() {
        data object Open : SideMenu()

        data object Close : SideMenu()

        data class ItemClicked(
            val itemType: SideMenuTypeUi,
        ) : SideMenu()
    }

    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(
            val isOpen: Boolean,
        ) : BottomSheet()

        sealed class DocumentRevocation : BottomSheet() {
            data class OptionListItemForRevokedDocumentSelected(
                val documentId: String,
            ) : DocumentRevocation()
        }
    }
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()

        data class SwitchScreen(
            val screenRoute: String,
            val popUpToScreenRoute: String = DashboardScreens.Dashboard.screenRoute,
            val inclusive: Boolean = false,
        ) : Navigation()

        data class OpenDeepLinkAction(
            val deepLinkUri: Uri,
            val arguments: String?,
        ) : Navigation()

        data object OnAppSettings : Navigation()

        data object OnSystemSettings : Navigation()

        data class OpenUrlExternally(
            val url: Uri,
        ) : Navigation()
    }

    data class ShareLogFile(
        val intent: Intent,
        val chooserTitle: String,
    ) : Effect()

    data object ShowBottomSheet : Effect()

    data object CloseBottomSheet : Effect()
}

sealed class DashboardBottomSheetContent {
    data class DocumentRevocation(
        val options: List<ModalOptionUi<Event>>,
    ) : DashboardBottomSheetContent()
}

enum class SideMenuAnimation {
    SLIDE,
    FADE,
}

@KoinViewModel
class DashboardViewModel(
    private val dashboardInteractor: DashboardInteractor,
    private val uiSerializer: UiSerializer,
    private val resourceProvider: ResourceProvider,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State =
        State(
            sideMenuTitle = resourceProvider.getString(R.string.dashboard_side_menu_title),
            sideMenuOptions = dashboardInteractor.getSideMenuOptions(),
        )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                handleDeepLink(event.deepLinkUri)
            }

            is Event.Pop -> {
                setEffect { Effect.Navigation.Pop }
            }

            is Event.SideMenu.ItemClicked -> {
                handleSideMenuItemClicked(event.itemType)
            }

            is Event.SideMenu.Close -> {
                setState {
                    copy(
                        isSideMenuVisible = false,
                        sideMenuAnimation = SideMenuAnimation.SLIDE,
                    )
                }
            }

            is Event.SideMenu.Open -> {
                setState {
                    copy(
                        isSideMenuVisible = true,
                        sideMenuAnimation = SideMenuAnimation.SLIDE,
                    )
                }
            }

            is Event.DocumentRevocationNotificationReceived -> {
                showBottomSheet(
                    sheetContent =
                        DashboardBottomSheetContent.DocumentRevocation(
                            options = getDocumentRevocationBottomSheetOptions(event.payload),
                        ),
                )
            }

            is Event.BottomSheet.UpdateBottomSheetState -> {
                setState {
                    copy(isBottomSheetOpen = event.isOpen)
                }
            }

            is Event.BottomSheet.DocumentRevocation.OptionListItemForRevokedDocumentSelected -> {
                hideBottomSheet()
                goToDocumentDetails(docId = event.documentId)
            }
        }
    }

    private fun goToDocumentDetails(docId: DocumentId) {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute =
                    generateComposableNavigationLink(
                        screen = DashboardScreens.DocumentDetails,
                        arguments =
                            generateComposableArguments(
                                mapOf(
                                    "documentId" to docId,
                                ),
                            ),
                    ),
            )
        }
    }

    private fun showBottomSheet(sheetContent: DashboardBottomSheetContent) {
        setState {
            copy(
                sheetContent = sheetContent,
            )
        }
        setEffect {
            Effect.ShowBottomSheet
        }
    }

    private fun hideBottomSheet() {
        setEffect {
            Effect.CloseBottomSheet
        }
    }

    private fun hideSideMenu() {
        setState {
            copy(
                isSideMenuVisible = false,
                sideMenuAnimation = SideMenuAnimation.FADE,
            )
        }
    }

    private fun getDocumentRevocationBottomSheetOptions(revokedDocumentData: List<RevokedDocumentDataDomain>): List<ModalOptionUi<Event>> =
        revokedDocumentData.map {
            ModalOptionUi(
                title = it.name,
                trailingIcon = AppIcons.KeyboardArrowRight,
                event =
                    Event.BottomSheet.DocumentRevocation.OptionListItemForRevokedDocumentSelected(
                        documentId = it.id,
                    ),
            )
        }

    private fun handleSideMenuItemClicked(itemType: SideMenuTypeUi) {
        when (itemType) {
            SideMenuTypeUi.CHANGE_PIN -> {
                val nextScreenRoute =
                    generateComposableNavigationLink(
                        screen = CommonScreens.QuickPin,
                        arguments =
                            generateComposableArguments(
                                mapOf("pinFlow" to PinFlow.UPDATE),
                            ),
                    )

                hideSideMenu()
                setEffect { Effect.Navigation.SwitchScreen(screenRoute = nextScreenRoute) }
            }

            SideMenuTypeUi.SETTINGS -> {
                hideSideMenu()
                setEffect { Effect.Navigation.SwitchScreen(screenRoute = DashboardScreens.Settings.screenRoute) }
            }
        }
    }

    private fun handleDeepLink(deepLinkUri: Uri?) {
        deepLinkUri?.let { uri ->
            hasDeepLink(uri)?.let {
                val arguments: String? =
                    when (it.type) {
                        DeepLinkType.OPENID4VP -> {
                            getOrCreatePresentationScope()
                            generateComposableArguments(
                                mapOf(
                                    RequestUriConfig.serializedKeyName to
                                        uiSerializer.toBase64(
                                            RequestUriConfig(
                                                PresentationMode.OpenId4Vp(
                                                    uri.toString(),
                                                    DashboardScreens.Dashboard.screenRoute,
                                                ),
                                            ),
                                            RequestUriConfig.Parser,
                                        ),
                                ),
                            )
                        }

                        DeepLinkType.CREDENTIAL_OFFER -> {
                            getOrCreateCredentialOfferScope()
                            generateComposableArguments(
                                mapOf(
                                    OfferUiConfig.serializedKeyName to
                                        uiSerializer.toBase64(
                                            OfferUiConfig(
                                                offerUri = it.link.toString(),
                                                onSuccessNavigation =
                                                    ConfigNavigation(
                                                        navigationType =
                                                            NavigationType.PopTo(
                                                                screen = DashboardScreens.Dashboard,
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
                            )
                        }

                        else -> {
                            null
                        }
                    }
                setEffect {
                    Effect.Navigation.OpenDeepLinkAction(
                        deepLinkUri = uri,
                        arguments = arguments,
                    )
                }
            }
        }
    }
}
