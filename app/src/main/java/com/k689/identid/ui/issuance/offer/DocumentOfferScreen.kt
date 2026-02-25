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

package com.k689.identid.ui.issuance.offer

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.R
import com.k689.identid.config.ConfigNavigation
import com.k689.identid.config.NavigationType
import com.k689.identid.config.OfferUiConfig
import com.k689.identid.extension.ui.cacheDeepLink
import com.k689.identid.extension.ui.getPendingDeepLink
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.navigation.IssuanceScreens
import com.k689.identid.navigation.helper.handleDeepLinkAction
import com.k689.identid.ui.component.ErrorInfo
import com.k689.identid.ui.component.ListItemDataUi
import com.k689.identid.ui.component.ListItemMainContentDataUi
import com.k689.identid.ui.component.RelyingPartyDataUi
import com.k689.identid.ui.component.content.BroadcastAction
import com.k689.identid.ui.component.content.ContentHeader
import com.k689.identid.ui.component.content.ContentHeaderConfig
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.preview.PreviewTheme
import com.k689.identid.ui.component.preview.ThemeModePreviews
import com.k689.identid.ui.component.utils.LifecycleEffect
import com.k689.identid.ui.component.utils.SPACING_LARGE
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import com.k689.identid.ui.component.utils.SPACING_SMALL
import com.k689.identid.ui.component.wrap.ButtonConfig
import com.k689.identid.ui.component.wrap.ButtonType
import com.k689.identid.ui.component.wrap.StickyBottomConfig
import com.k689.identid.ui.component.wrap.StickyBottomType
import com.k689.identid.ui.component.wrap.WrapListItem
import com.k689.identid.ui.component.wrap.WrapStickyBottomContent
import com.k689.identid.util.core.CoreActions
import com.k689.identid.util.issuance.TestTag
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun DocumentOfferScreen(
    navController: NavController,
    viewModel: DocumentOfferViewModel,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ContentScreen(
        isLoading = state.isLoading,
        contentErrorConfig = state.error,
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { viewModel.setEvent(Event.BackButtonPressed) },
        stickyBottom = { paddingValues ->
            WrapStickyBottomContent(
                modifier =
                    Modifier
                        .testTag(TestTag.DocumentOfferScreen.BUTTON)
                        .fillMaxWidth()
                        .padding(paddingValues),
                stickyBottomConfig =
                    StickyBottomConfig(
                        type =
                            StickyBottomType.OneButton(
                                config =
                                    ButtonConfig(
                                        type = ButtonType.PRIMARY,
                                        enabled = !state.isLoading && !state.noDocument,
                                        onClick = { viewModel.setEvent(Event.StickyButtonPressed(context)) },
                                    ),
                            ),
                    ),
            ) {
                Text(text = stringResource(R.string.issuance_document_offer_primary_button_text_add))
            }
        },
        broadcastAction =
            BroadcastAction(
                intentFilters =
                    listOf(
                        CoreActions.VCI_RESUME_ACTION,
                        CoreActions.VCI_DYNAMIC_PRESENTATION,
                    ),
                callback = {
                    when (it?.action) {
                        CoreActions.VCI_RESUME_ACTION -> {
                            it.extras?.getString("uri")?.let { link ->
                                viewModel.setEvent(Event.OnResumeIssuance(link))
                            }
                        }

                        CoreActions.VCI_DYNAMIC_PRESENTATION -> {
                            it.extras
                                ?.getString("uri")
                                ?.let { link ->
                                    viewModel.setEvent(Event.OnDynamicPresentation(link))
                                }
                        }
                    }
                },
            ),
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(context, navigationEffect, navController)
            },
            paddingValues = paddingValues,
        )
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_PAUSE,
    ) {
        viewModel.setEvent(Event.OnPause)
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME,
    ) {
        viewModel.setEvent(Event.Init(context.getPendingDeepLink()))
    }
}

@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onNavigationRequested: (Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
    ) {
        ContentHeader(
            modifier = Modifier.fillMaxWidth(),
            config = state.headerConfig,
            descriptionTestTag = TestTag.DocumentOfferScreen.CONTENT_HEADER_DESCRIPTION,
        )

        if (state.noDocument) {
            ErrorInfo(
                modifier = Modifier.fillMaxSize(),
                informativeText = stringResource(id = R.string.issuance_document_offer_error_no_document),
            )
        } else {
            // Screen Main Content
            MainContent(
                modifier = Modifier.fillMaxSize(),
                documents = state.documents,
            )
        }
    }

    LaunchedEffect(Unit) {
        effectFlow
            .onEach { effect ->
                when (effect) {
                    is Effect.Navigation -> onNavigationRequested(effect)
                }
            }.collect()
    }
}

@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
    documents: List<ListItemDataUi>,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = SPACING_SMALL.dp),
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
    ) {
        items(documents.size) { index ->
            WrapListItem(
                modifier = Modifier.fillMaxWidth(),
                item = documents[index],
                onItemClick = null,
                mainContentVerticalPadding = SPACING_LARGE.dp,
            )
        }
    }
}

private fun handleNavigationEffect(
    context: Context,
    navigationEffect: Effect.Navigation,
    navController: NavController,
) {
    when (navigationEffect) {
        is Effect.Navigation.SwitchScreen -> {
            navController.navigate(navigationEffect.screenRoute) {
                if (navigationEffect.shouldPopToSelf) {
                    popUpTo(IssuanceScreens.DocumentOffer.screenRoute) {
                        inclusive = true
                    }
                }
            }
        }

        is Effect.Navigation.PopBackStackUpTo -> {
            navController.popBackStack(
                route = navigationEffect.screenRoute,
                inclusive = navigationEffect.inclusive,
            )
        }

        is Effect.Navigation.DeepLink -> {
            navigationEffect.routeToPop?.let {
                context.cacheDeepLink(navigationEffect.link)
                navController.popBackStack(
                    route = it,
                    inclusive = false,
                )
            } ?: handleDeepLinkAction(navController, navigationEffect.link)
        }

        is Effect.Navigation.Pop -> {
            navController.popBackStack()
        }
    }
}

@ThemeModePreviews
@Composable
private fun ContentPreview() {
    PreviewTheme {
        val previewState =
            State(
                isLoading = false,
                error = null,
                isInitialised = true,
                documents =
                    listOf(
                        ListItemDataUi(
                            itemId = "doc_1",
                            mainContentData = ListItemMainContentDataUi.Text(text = "PID"),
                        ),
                    ),
                noDocument = false,
                headerConfig =
                    ContentHeaderConfig(
                        description = stringResource(R.string.issuance_document_offer_description),
                        mainText = stringResource(R.string.issuance_document_offer_header_main_text),
                        relyingPartyData =
                            RelyingPartyDataUi(
                                isVerified = true,
                                name = stringResource(R.string.issuance_document_offer_relying_party_default_name),
                                description = stringResource(R.string.issuance_document_offer_relying_party_description),
                            ),
                    ),
                offerUiConfig =
                    OfferUiConfig(
                        offerUri = "",
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
            )

        Content(
            state = previewState,
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onNavigationRequested = {},
            paddingValues = PaddingValues(SPACING_MEDIUM.dp),
        )
    }
}
