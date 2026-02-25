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

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.k689.identid.R
import com.k689.identid.extension.business.getParcelableArrayListExtra
import com.k689.identid.extension.ui.finish
import com.k689.identid.extension.ui.getPendingDeepLink
import com.k689.identid.extension.ui.openAppSettings
import com.k689.identid.extension.ui.openBleSettings
import com.k689.identid.extension.ui.openIntentChooser
import com.k689.identid.extension.ui.openUrl
import com.k689.identid.model.core.RevokedDocumentDataDomain
import com.k689.identid.navigation.helper.handleDeepLinkAction
import com.k689.identid.ui.component.SystemBroadcastReceiver
import com.k689.identid.ui.component.utils.LifecycleEffect
import com.k689.identid.ui.component.wrap.BottomSheetTextDataUi
import com.k689.identid.ui.component.wrap.BottomSheetWithOptionsList
import com.k689.identid.ui.component.wrap.WrapModalBottomSheet
import com.k689.identid.ui.dashboard.component.BottomNavigationBar
import com.k689.identid.ui.dashboard.component.BottomNavigationItem
import com.k689.identid.ui.dashboard.dashboard.sidemenu.SideMenuScreen
import com.k689.identid.ui.dashboard.documents.list.DocumentsScreen
import com.k689.identid.ui.dashboard.documents.list.DocumentsViewModel
import com.k689.identid.ui.dashboard.home.HomeScreen
import com.k689.identid.ui.dashboard.home.HomeViewModel
import com.k689.identid.ui.dashboard.transactions.list.TransactionsScreen
import com.k689.identid.ui.dashboard.transactions.list.TransactionsViewModel
import com.k689.identid.util.core.CoreActions
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardScreen(
    hostNavController: NavController,
    viewModel: DashboardViewModel,
    documentsViewModel: DocumentsViewModel,
    homeViewModel: HomeViewModel,
    transactionsViewModel: TransactionsViewModel,
) {
    val context = LocalContext.current

    val bottomNavigationController = rememberNavController()
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val bottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

    Scaffold(
        bottomBar = { BottomNavigationBar(bottomNavigationController) },
    ) { padding ->
        NavHost(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding()),
            navController = bottomNavigationController,
            startDestination = BottomNavigationItem.Home.route,
        ) {
            composable(BottomNavigationItem.Home.route) {
                HomeScreen(
                    hostNavController,
                    homeViewModel,
                    onDashboardEventSent = { event ->
                        viewModel.setEvent(event)
                    },
                )
            }
            composable(BottomNavigationItem.Documents.route) {
                DocumentsScreen(
                    hostNavController,
                    documentsViewModel,
                    onDashboardEventSent = { event ->
                        viewModel.setEvent(event)
                    },
                )
            }
            composable(BottomNavigationItem.Transactions.route) {
                TransactionsScreen(
                    hostNavController,
                    transactionsViewModel,
                    onDashboardEventSent = { event ->
                        viewModel.setEvent(event)
                    },
                )
            }
        }

        if (state.isBottomSheetOpen) {
            WrapModalBottomSheet(
                onDismissRequest = {
                    viewModel.setEvent(
                        Event.BottomSheet.UpdateBottomSheetState(
                            isOpen = false,
                        ),
                    )
                },
                sheetState = bottomSheetState,
            ) {
                DashboardSheetContent(
                    sheetContent = state.sheetContent,
                    onEventSent = {
                        viewModel.setEvent(it)
                    },
                )
            }
        }
    }

    AnimatedVisibility(
        visible = state.isSideMenuVisible,
        modifier = Modifier.fillMaxSize(),
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit =
            when (state.sideMenuAnimation) {
                SideMenuAnimation.SLIDE -> slideOutHorizontally(targetOffsetX = { it })
                SideMenuAnimation.FADE -> fadeOut(animationSpec = tween(state.menuAnimationDuration))
            },
    ) {
        SideMenuScreen(
            state = state,
            onEventSent = { event -> viewModel.setEvent(event) },
        )
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME,
    ) {
        viewModel.setEvent(
            Event.Init(
                deepLinkUri = context.getPendingDeepLink(),
            ),
        )
    }

    LaunchedEffect(Unit) {
        viewModel.effect
            .onEach { effect ->
                when (effect) {
                    is Effect.Navigation -> {
                        handleNavigationEffect(effect, hostNavController, context)
                    }

                    is Effect.CloseBottomSheet -> {
                        scope
                            .launch {
                                bottomSheetState.hide()
                            }.invokeOnCompletion {
                                if (!bottomSheetState.isVisible) {
                                    viewModel.setEvent(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                                }
                            }
                    }

                    is Effect.ShowBottomSheet -> {
                        viewModel.setEvent(Event.BottomSheet.UpdateBottomSheetState(isOpen = true))
                    }

                    is Effect.ShareLogFile -> {
                        context.openIntentChooser(
                            effect.intent,
                            effect.chooserTitle,
                        )
                    }
                }
            }.collect()
    }

    SystemBroadcastReceiver(
        intentFilters =
            listOf(
                CoreActions.REVOCATION_WORK_MESSAGE_ACTION,
            ),
    ) { intent ->
        intent
            .getParcelableArrayListExtra<RevokedDocumentDataDomain>(
                action = CoreActions.REVOCATION_IDS_EXTRA,
            )?.let {
                viewModel.setEvent(
                    Event.DocumentRevocationNotificationReceived(it),
                )
            }
    }
}

private fun handleNavigationEffect(
    navigationEffect: Effect.Navigation,
    navController: NavController,
    context: Context,
) {
    when (navigationEffect) {
        is Effect.Navigation.Pop -> {
            context.finish()
        }

        is Effect.Navigation.SwitchScreen -> {
            navController.navigate(navigationEffect.screenRoute) {
                popUpTo(navigationEffect.popUpToScreenRoute) {
                    inclusive = navigationEffect.inclusive
                }
            }
        }

        is Effect.Navigation.OpenDeepLinkAction -> {
            handleDeepLinkAction(
                navController,
                navigationEffect.deepLinkUri,
                navigationEffect.arguments,
            )
        }

        is Effect.Navigation.OnAppSettings -> {
            context.openAppSettings()
        }

        is Effect.Navigation.OnSystemSettings -> {
            context.openBleSettings()
        }

        is Effect.Navigation.OpenUrlExternally -> {
            context.openUrl(uri = navigationEffect.url)
        }
    }
}

@Composable
private fun DashboardSheetContent(
    sheetContent: DashboardBottomSheetContent,
    onEventSent: (even: Event) -> Unit,
) {
    when (sheetContent) {
        is DashboardBottomSheetContent.DocumentRevocation -> {
            BottomSheetWithOptionsList(
                textData =
                    BottomSheetTextDataUi(
                        title =
                            stringResource(
                                id = R.string.dashboard_bottom_sheet_revoked_document_dialog_title,
                            ),
                        message =
                            stringResource(
                                id = R.string.dashboard_bottom_sheet_revoked_document_dialog_subtitle,
                            ),
                    ),
                options = sheetContent.options,
                onEventSent = onEventSent,
            )
        }
    }
}
