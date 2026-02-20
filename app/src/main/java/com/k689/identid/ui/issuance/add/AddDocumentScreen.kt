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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.R
import com.k689.identid.config.IssuanceFlowType
import com.k689.identid.config.IssuanceUiConfig
import com.k689.identid.controller.core.IssuanceMethod
import com.k689.identid.extension.ui.finish
import com.k689.identid.extension.ui.getPendingDeepLink
import com.k689.identid.extension.ui.paddingFrom
import com.k689.identid.navigation.IssuanceScreens
import com.k689.identid.navigation.helper.handleDeepLinkAction
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.ErrorInfo
import com.k689.identid.ui.component.ListItemDataUi
import com.k689.identid.ui.component.ListItemMainContentDataUi
import com.k689.identid.ui.component.ListItemTrailingContentDataUi
import com.k689.identid.ui.component.content.BroadcastAction
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ContentTitle
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.preview.PreviewTheme
import com.k689.identid.ui.component.preview.ThemeModePreviews
import com.k689.identid.ui.component.utils.LifecycleEffect
import com.k689.identid.ui.component.utils.SIZE_LARGE
import com.k689.identid.ui.component.utils.SPACING_LARGE
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import com.k689.identid.ui.component.utils.VSpacer
import com.k689.identid.ui.component.wrap.ButtonConfig
import com.k689.identid.ui.component.wrap.ButtonType
import com.k689.identid.ui.component.wrap.TextConfig
import com.k689.identid.ui.component.wrap.WrapButton
import com.k689.identid.ui.component.wrap.WrapIcon
import com.k689.identid.ui.component.wrap.WrapListItem
import com.k689.identid.ui.component.wrap.WrapText
import com.k689.identid.ui.issuance.add.model.AddDocumentUi
import com.k689.identid.util.core.CoreActions
import com.k689.identid.util.issuance.TestTag
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun AddDocumentScreen(
    navController: NavController,
    viewModel: AddDocumentViewModel,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = state.navigatableAction,
        onBack = state.onBackAction,
        contentErrorConfig = state.error,
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
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                when (navigationEffect) {
                    is Effect.Navigation.Pop -> {
                        navController.popBackStack()
                    }

                    is Effect.Navigation.SwitchScreen -> {
                        navController.navigate(navigationEffect.screenRoute) {
                            popUpTo(IssuanceScreens.AddDocument.screenRoute) {
                                inclusive = navigationEffect.inclusive
                            }
                        }
                    }

                    is Effect.Navigation.Finish -> {
                        context.finish()
                    }

                    is Effect.Navigation.OpenDeepLinkAction -> {
                        handleDeepLinkAction(
                            navController,
                            navigationEffect.deepLinkUri,
                            navigationEffect.arguments,
                        )
                    }
                }
            },
            paddingValues = paddingValues,
            context = context,
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
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
    context: Context,
) {
    val layoutDirection = LocalLayoutDirection.current

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        MainContent(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .paddingFrom(paddingValues, bottom = false),
            state = state,
            onEventSend = onEventSend,
            context = context,
        )

        if (state.showFooterScanner) {
            VSpacer.ExtraSmall()
            FloatingFooter(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = SPACING_MEDIUM.dp,
                            start = paddingValues.calculateStartPadding(layoutDirection),
                            end = paddingValues.calculateEndPadding(layoutDirection),
                            bottom = paddingValues.calculateBottomPadding(),
                        ),
                onEventSend = onEventSend,
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
    state: State,
    onEventSend: (Event) -> Unit,
    context: Context,
) {
    Column(
        modifier = modifier,
    ) {
        ContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = state.title,
            subtitle = state.subtitle,
            titleStyle =
                MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            subtitleTestTag = TestTag.AddDocumentScreen.SUBTITLE,
        )

        VSpacer.Medium()

        TextField(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(CircleShape),
            value = state.searchQuery,
            onValueChange = { onEventSend(Event.OnSearchQueryChanged(it)) },
            placeholder = { Text(stringResource(R.string.issuance_add_document_search_hint)) },
            leadingIcon = {
                WrapIcon(iconData = AppIcons.Search)
            },
            singleLine = true,
            colors =
                TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
        )

        if (state.noOptions) {
            ErrorInfo(
                modifier = Modifier.fillMaxSize(),
                informativeText = stringResource(R.string.issuance_add_document_no_options),
            )
        } else {
            VSpacer.Medium()

            Options(
                options = state.filteredOptions,
                modifier = Modifier.fillMaxSize(),
                onOptionClicked = { itemId, issuerId ->
                    onEventSend(
                        Event.IssueDocument(
                            issuanceMethod = IssuanceMethod.OPENID4VCI,
                            issuerId = issuerId,
                            configId = itemId,
                            context = context,
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun Options(
    options: List<Pair<String, List<AddDocumentUi>>>,
    modifier: Modifier = Modifier,
    onOptionClicked: (itemId: String, issuerId: String) -> Unit,
) {
    val listState = remember(options) { LazyListState() }

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
        contentPadding = PaddingValues(bottom = SPACING_LARGE.dp),
    ) {
        options.forEachIndexed { _, (issuerId, items) ->

            stickyHeader(key = "hdr-$issuerId") {
                WrapText(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background),
                    text = issuerId,
                    textConfig =
                        TextConfig(
                            style =
                                MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                ),
                        ),
                )
            }

            itemsIndexed(
                items = items,
                key = { _, item -> "$issuerId-${item.configurationId}" },
            ) { _, item ->
                val testTag =
                    TestTag.AddDocumentScreen.optionItem(
                        issuerId = issuerId,
                        configId = item.configurationId,
                    )

                ElevatedCard(
                    modifier =
                        Modifier
                            .testTag(testTag)
                            .fillMaxWidth(),
                    shape = RoundedCornerShape(SIZE_LARGE.dp),
                    colors =
                        CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    onClick = {
                        onOptionClicked(item.configurationId, issuerId)
                    },
                ) {
                    WrapListItem(
                        modifier = Modifier.fillMaxWidth(),
                        item = item.itemData,
                        mainContentVerticalPadding = SPACING_LARGE.dp,
                        mainContentTextStyle = MaterialTheme.typography.titleMedium,
                        onItemClick = {
                            onOptionClicked(item.configurationId, issuerId)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingFooter(
    modifier: Modifier = Modifier,
    onEventSend: (Event) -> Unit,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.issuance_add_document_scan_qr_footer_text),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
            )

            WrapButton(
                modifier = Modifier.fillMaxWidth(),
                buttonConfig =
                    ButtonConfig(
                        type = ButtonType.PRIMARY,
                        onClick = {
                            onEventSend(Event.GoToQrScan)
                        },
                    ),
            ) {
                WrapIcon(
                    iconData = AppIcons.QrScanner,
                    modifier = Modifier.padding(end = 8.dp).size(35.dp),
                )
                Text(text = stringResource(R.string.issuance_add_document_scan_qr_footer_button_text))
            }
        }
    }
}

@ThemeModePreviews
@Composable
private fun IssuanceAddDocumentScreenPreview() {
    val previewOptions =
        listOf(
            Pair(
                "issuer1",
                listOf(
                    AddDocumentUi(
                        credentialIssuerId = "issuer1",
                        configurationId = "configId1",
                        itemData =
                            ListItemDataUi(
                                itemId = "configId1",
                                mainContentData = ListItemMainContentDataUi.Text(text = "National ID"),
                                trailingContentData =
                                    ListItemTrailingContentDataUi.Icon(iconData = AppIcons.Add),
                            ),
                    ),
                ),
            ),
            Pair(
                "issuer2",
                listOf(
                    AddDocumentUi(
                        credentialIssuerId = "issuer2",
                        configurationId = "configId2",
                        itemData =
                            ListItemDataUi(
                                itemId = "configId2",
                                mainContentData = ListItemMainContentDataUi.Text(text = "Driving Licence"),
                                trailingContentData =
                                    ListItemTrailingContentDataUi.Icon(iconData = AppIcons.Add),
                            ),
                    ),
                ),
            ),
        )
    PreviewTheme {
        Content(
            state =
                State(
                    issuanceConfig = IssuanceUiConfig(flowType = IssuanceFlowType.NoDocument),
                    showFooterScanner = true,
                    navigatableAction = ScreenNavigateAction.NONE,
                    title = stringResource(R.string.issuance_add_document_title),
                    subtitle = stringResource(R.string.issuance_add_document_subtitle),
                    options = previewOptions,
                    filteredOptions = previewOptions,
                ),
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(all = SPACING_LARGE.dp),
            context = LocalContext.current,
        )
    }
}

@ThemeModePreviews
@Composable
private fun DashboardAddDocumentScreenPreview() {
    val previewOptions =
        listOf(
            Pair(
                "issuer1",
                listOf(
                    AddDocumentUi(
                        credentialIssuerId = "issuer1",
                        configurationId = "configId1",
                        itemData =
                            ListItemDataUi(
                                itemId = "configId1",
                                mainContentData = ListItemMainContentDataUi.Text(text = "National ID"),
                                trailingContentData =
                                    ListItemTrailingContentDataUi.Icon(iconData = AppIcons.Add),
                            ),
                    ),
                ),
            ),
            Pair(
                "issuer2",
                listOf(
                    AddDocumentUi(
                        credentialIssuerId = "issuer2",
                        configurationId = "configId2",
                        itemData =
                            ListItemDataUi(
                                itemId = "configId2",
                                mainContentData = ListItemMainContentDataUi.Text(text = "Driving Licence"),
                                trailingContentData =
                                    ListItemTrailingContentDataUi.Icon(iconData = AppIcons.Add),
                            ),
                    ),
                ),
            ),
        )
    PreviewTheme {
        Content(
            state =
                State(
                    issuanceConfig =
                        IssuanceUiConfig(
                            flowType = IssuanceFlowType.ExtraDocument(formatType = null),
                        ),
                    showFooterScanner = false,
                    navigatableAction = ScreenNavigateAction.BACKABLE,
                    title = stringResource(R.string.issuance_add_document_title),
                    subtitle = stringResource(R.string.issuance_add_document_subtitle),
                    options = previewOptions,
                    filteredOptions = previewOptions,
                ),
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(all = SPACING_LARGE.dp),
            context = LocalContext.current,
        )
    }
}
