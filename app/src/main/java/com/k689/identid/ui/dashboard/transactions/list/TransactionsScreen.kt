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

package com.k689.identid.ui.dashboard.transactions.list

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.R
import com.k689.identid.extension.ui.finish
import com.k689.identid.extension.ui.paddingFrom
import com.k689.identid.model.dashboard.SearchItemUi
import com.k689.identid.theme.values.success
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.DatePickerDialogType
import com.k689.identid.ui.component.DualSelectorButtonDataUi
import com.k689.identid.ui.component.DualSelectorButtons
import com.k689.identid.ui.component.FiltersDatePickerDialog
import com.k689.identid.ui.component.FiltersSearchBar
import com.k689.identid.ui.component.InlineSnackbar
import com.k689.identid.ui.component.ListItemDataUi
import com.k689.identid.ui.component.ListItemMainContentDataUi
import com.k689.identid.ui.component.ListItemTrailingContentDataUi
import com.k689.identid.ui.component.SectionTitle
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.preview.PreviewTheme
import com.k689.identid.ui.component.preview.ThemeModePreviews
import com.k689.identid.ui.component.utils.HSpacer
import com.k689.identid.ui.component.utils.LifecycleEffect
import com.k689.identid.ui.component.utils.OneTimeLaunchedEffect
import com.k689.identid.ui.component.utils.SPACING_EXTRA_SMALL
import com.k689.identid.ui.component.utils.SPACING_LARGE
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import com.k689.identid.ui.component.utils.SPACING_SMALL
import com.k689.identid.ui.component.utils.VSpacer
import com.k689.identid.ui.component.wrap.ButtonConfig
import com.k689.identid.ui.component.wrap.ButtonType
import com.k689.identid.ui.component.wrap.ExpandableListItemUi
import com.k689.identid.ui.component.wrap.GenericBottomSheet
import com.k689.identid.ui.component.wrap.WrapButton
import com.k689.identid.ui.component.wrap.WrapExpandableCard
import com.k689.identid.ui.component.wrap.WrapExpandableListItem
import com.k689.identid.ui.component.wrap.WrapIcon
import com.k689.identid.ui.component.wrap.WrapIconButton
import com.k689.identid.ui.component.wrap.WrapListItem
import com.k689.identid.ui.component.wrap.WrapListItems
import com.k689.identid.ui.component.wrap.WrapModalBottomSheet
import com.k689.identid.ui.dashboard.transactions.list.model.FilterDateRangeSelectionUi
import com.k689.identid.ui.dashboard.transactions.list.model.TransactionCategoryUi
import com.k689.identid.ui.dashboard.transactions.list.model.TransactionFilterIds
import com.k689.identid.ui.dashboard.transactions.list.model.TransactionUi
import com.k689.identid.ui.dashboard.transactions.model.TransactionStatusUi
import eu.europa.ec.eudi.rqesui.domain.util.safeLet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime

typealias DashboardEvent = com.k689.identid.ui.dashboard.dashboard.Event
typealias OpenSideMenuEvent = com.k689.identid.ui.dashboard.dashboard.Event.SideMenu.Open

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    navHostController: NavController,
    viewModel: TransactionsViewModel,
    onDashboardEventSent: (DashboardEvent) -> Unit,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val datePickerDialogConfig = state.datePickerDialogConfig

    val scope = rememberCoroutineScope()
    val bottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

    ContentScreen(
        isLoading = state.isLoading,
        contentErrorConfig = null,
        navigatableAction = ScreenNavigateAction.NONE,
        onBack = { context.finish() },
        topBar = {
            TopBar(
                onDashboardEventSent = onDashboardEventSent,
            )
        },
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(navigationEffect, navHostController, context)
            },
            paddingValues = paddingValues,
            coroutineScope = scope,
            modalBottomSheetState = bottomSheetState,
        )

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
                TransactionsSheetContent(
                    sheetContent = state.sheetContent,
                    filtersUi = state.filtersUi,
                    snapshotFilterDateRangeData = state.snapshotFilterDateRangeSelectionUi,
                    sortOrder = state.sortOrder,
                    onEventSent = {
                        viewModel.setEvent(it)
                    },
                )
            }
        }

        if (state.isDatePickerDialogVisible) {
            FiltersDatePickerDialog(
                onDateSelected = { millis ->
                    safeLet(
                        datePickerDialogConfig.type,
                        millis,
                    ) { dateSelectionType, safeMillis ->
                        when (dateSelectionType) {
                            DatePickerDialogType.SelectStartDate -> {
                                viewModel.setEvent(
                                    Event.OnStartDateSelected(
                                        selectedDateUtcMillis = safeMillis,
                                    ),
                                )
                            }

                            DatePickerDialogType.SelectEndDate -> {
                                viewModel.setEvent(
                                    Event.OnEndDateSelected(
                                        selectedDateUtcMillis = safeMillis,
                                    ),
                                )
                            }
                        }
                    }
                },
                onDismiss = {
                    viewModel.setEvent(
                        Event.DatePickerDialog.UpdateDialogState(isVisible = false),
                    )
                },
                datePickerDialogConfig = datePickerDialogConfig,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (navigationEffect: Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
    coroutineScope: CoroutineScope,
    modalBottomSheetState: SheetState,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .paddingFrom(paddingValues, bottom = false),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = SPACING_MEDIUM.dp),
        ) {
            item {
                val searchItemUi =
                    SearchItemUi(searchLabel = stringResource(R.string.transactions_screen_search_label))
                FiltersSearchBar(
                    placeholder = searchItemUi.searchLabel,
                    onValueChange = { onEventSend(Event.OnSearchQueryChanged(it)) },
                    onFilterClick = { onEventSend(Event.FiltersPressed) },
                    onClearClick = { onEventSend(Event.OnSearchQueryChanged("")) },
                    isFilteringActive = state.isFilteringActive,
                    text = state.searchText,
                )
                VSpacer.Large()
            }

            if (state.showNoResultsFound) {
                item {
                    NoResults(modifier = Modifier.fillMaxWidth())
                }
            } else {
                itemsIndexed(items = state.transactionsUi) { index, (documentCategory, documents) ->
                    TransactionCategory(
                        modifier = Modifier.fillMaxWidth(),
                        category = documentCategory,
                        transactions = documents,
                        onEventSend = onEventSend,
                    )

                    if (index != state.transactionsUi.lastIndex) {
                        VSpacer.ExtraLarge()
                    }
                }
            }
        }

        if (state.error != null) {
            InlineSnackbar(
                error = state.error,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = SPACING_EXTRA_SMALL.dp),
            )
        }
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME,
    ) {
        onEventSend(Event.OnResume)
    }
    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_PAUSE,
    ) {
        onEventSend(Event.OnPause)
    }

    OneTimeLaunchedEffect {
        onEventSend(Event.Init)
    }

    LaunchedEffect(Unit) {
        effectFlow
            .onEach { effect ->
                when (effect) {
                    is Effect.Navigation -> {
                        onNavigationRequested(effect)
                    }

                    is Effect.CloseBottomSheet -> {
                        coroutineScope.launch {
                            modalBottomSheetState.hide()
                            if (!modalBottomSheetState.isVisible) {
                                onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                            }
                        }
                    }

                    is Effect.ShowBottomSheet -> {
                        onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = true))
                    }

                    is Effect.ShowDatePickerDialog -> {
                        onEventSend(Event.DatePickerDialog.UpdateDialogState(isVisible = true))
                    }
                }
            }.collect()
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
    }
}

@Composable
private fun TransactionCategory(
    modifier: Modifier = Modifier,
    category: TransactionCategoryUi,
    transactions: List<TransactionUi>,
    onEventSend: (Event) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
    ) {
        SectionTitle(
            modifier = Modifier.fillMaxWidth(),
            text = category.displayName ?: stringResource(category.stringResId),
        )

        val transactionItems =
            remember(key1 = transactions) {
                transactions.map { it.uiData }
            }
        val transactionMap =
            remember(key1 = transactions) {
                transactions.associateBy { it.uiData.header.itemId }
            }

        val listShape = MaterialTheme.shapes.small
        WrapListItems(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.outline, listShape),
            shape = listShape,
            items = transactionItems,
            onItemClick = { item ->
                onEventSend(
                    Event.TransactionItemPressed(itemId = item.itemId),
                )
            },
            onExpandedChange = null,
            overlineTextStyle = { item ->
                val transactionUi = transactionMap[item.itemId]

                val overlineTextColor =
                    when (transactionUi?.uiStatus) {
                        TransactionStatusUi.Completed -> MaterialTheme.colorScheme.success
                        TransactionStatusUi.Failed -> MaterialTheme.colorScheme.error
                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                MaterialTheme.typography.labelMedium.copy(
                    color = overlineTextColor,
                )
            },
        )
    }
}

@Composable
private fun NoResults(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        WrapListItem(
            item =
                ListItemDataUi(
                    itemId = stringResource(R.string.transactions_screen_search_no_results_id),
                    mainContentData =
                        ListItemMainContentDataUi.Text(
                            text = stringResource(R.string.transactions_screen_search_no_results),
                        ),
                ),
            onItemClick = null,
            modifier = Modifier.fillMaxWidth(),
            mainContentVerticalPadding = SPACING_MEDIUM.dp,
        )
    }
}

@Composable
private fun TopBar(
    onDashboardEventSent: (DashboardEvent) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    all = SPACING_SMALL.dp,
                ),
    ) {
        WrapIconButton(
            modifier = Modifier.align(Alignment.CenterStart),
            iconData = AppIcons.Menu,
            customTint = MaterialTheme.colorScheme.onSurface,
        ) {
            onDashboardEventSent(OpenSideMenuEvent)
        }

        Text(
            modifier = Modifier.align(Alignment.Center),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium,
            text = stringResource(R.string.transactions_screen_title),
        )
    }
}

@Composable
private fun TransactionsSheetContent(
    sheetContent: TransactionsBottomSheetContent,
    filtersUi: List<ExpandableListItemUi.NestedListItem>,
    snapshotFilterDateRangeData: FilterDateRangeSelectionUi,
    sortOrder: DualSelectorButtonDataUi,
    onEventSent: (event: Event) -> Unit,
) {
    when (sheetContent) {
        is TransactionsBottomSheetContent.Filters -> {
            GenericBottomSheet(
                titleContent = {
                    Text(
                        text = stringResource(R.string.transactions_screen_filters_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                bodyContent = {
                    val expandStateList by remember {
                        mutableStateOf(filtersUi.map { false }.toMutableStateList())
                    }

                    var buttonsRowHeight by remember { mutableIntStateOf(0) }

                    Box {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(bottom = with(LocalDensity.current) { buttonsRowHeight.toDp() }),
                            verticalArrangement = Arrangement.spacedBy(SPACING_LARGE.dp),
                        ) {
                            DualSelectorButtons(sortOrder) {
                                onEventSent(
                                    Event.OnSortingOrderChanged(it),
                                )
                            }

                            filtersUi.forEachIndexed { index, filter ->
                                when {
                                    filter.header.itemId == TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_GROUP_ID -> {
                                        WrapExpandableCard(
                                            cardCollapsedContent = {
                                                WrapListItem(
                                                    mainContentVerticalPadding = SPACING_MEDIUM.dp,
                                                    item = filter.header,
                                                    onItemClick = {
                                                        expandStateList[index] =
                                                            !expandStateList[index]
                                                    },
                                                    mainContentTextStyle =
                                                        MaterialTheme.typography.bodyLarge.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                        ),
                                                )
                                            },
                                            cardExpandedContent = {
                                                Column(
                                                    modifier =
                                                        Modifier.padding(
                                                            start = SPACING_MEDIUM.dp,
                                                            end = SPACING_MEDIUM.dp,
                                                            bottom = SPACING_MEDIUM.dp,
                                                        ),
                                                ) {
                                                    FiltersDatePickerField(
                                                        dialogType = DatePickerDialogType.SelectStartDate,
                                                        selectDateLabel = stringResource(R.string.transactions_screen_filters_date_from),
                                                        displayedSelectedDate = snapshotFilterDateRangeData.displayedStartDate,
                                                        onEventSent = onEventSent,
                                                    )

                                                    FiltersDatePickerField(
                                                        dialogType = DatePickerDialogType.SelectEndDate,
                                                        selectDateLabel = stringResource(R.string.transactions_screen_filters_date_to),
                                                        displayedSelectedDate = snapshotFilterDateRangeData.displayedEndDate,
                                                        onEventSent = onEventSent,
                                                    )
                                                }
                                            },
                                            isExpanded = expandStateList[index],
                                        )
                                    }

                                    filter.nestedItems.isNotEmpty() -> {
                                        WrapExpandableListItem(
                                            header = filter.header,
                                            data = filter.nestedItems,
                                            isExpanded = expandStateList[index],
                                            onExpandedChange = {
                                                expandStateList[index] = !expandStateList[index]
                                            },
                                            onItemClick = {
                                                val id = it.itemId
                                                val groupId = filter.header.itemId
                                                onEventSent(
                                                    Event.OnFilterSelectionChanged(
                                                        filterId = id,
                                                        groupId,
                                                    ),
                                                )
                                            },
                                            addDivider = false,
                                            collapsedMainContentVerticalPadding = SPACING_MEDIUM.dp,
                                            expandedMainContentVerticalPadding = SPACING_MEDIUM.dp,
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                    .onGloballyPositioned { coordinates ->
                                        buttonsRowHeight = coordinates.size.height
                                    }.padding(top = SPACING_LARGE.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            WrapButton(
                                modifier = Modifier.weight(1f),
                                buttonConfig =
                                    ButtonConfig(
                                        type = ButtonType.SECONDARY,
                                        onClick = {
                                            onEventSent(Event.OnFiltersReset)
                                        },
                                    ),
                            ) {
                                Text(text = stringResource(R.string.transactions_screen_filters_reset))
                            }
                            HSpacer.Small()
                            WrapButton(
                                modifier = Modifier.weight(1f),
                                buttonConfig =
                                    ButtonConfig(
                                        type = ButtonType.PRIMARY,
                                        onClick = {
                                            onEventSent(Event.OnFiltersApply)
                                        },
                                    ),
                            ) {
                                Text(text = stringResource(R.string.transactions_screen_filters_apply))
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
fun FiltersDatePickerField(
    modifier: Modifier = Modifier,
    dialogType: DatePickerDialogType,
    selectDateLabel: String,
    displayedSelectedDate: String,
    onEventSent: (event: Event) -> Unit,
) {
    OutlinedTextField(
        readOnly = true,
        value = displayedSelectedDate,
        onValueChange = {},
        label = { Text(selectDateLabel) },
        placeholder = { Text(stringResource(R.string.transactions_screen_text_field_date_pattern)) },
        trailingIcon = { WrapIcon(AppIcons.DateRange) },
        colors =
            OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .pointerInput(displayedSelectedDate) {
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        if (upEvent != null) {
                            onEventSent(
                                Event.ShowDatePicker(datePickerType = dialogType),
                            )
                        }
                    }
                },
    )
}

@ThemeModePreviews
@Composable
private fun TransactionsScreenPreview() {
    data class PreviewEntry(
        val name: String,
        val relying: String,
        val time: String,
        val status: TransactionStatusUi,
        val category: TransactionCategoryUi,
    )

    val previewEntries =
        listOf(
            PreviewEntry(
                name = "Age Verification",
                relying = "Online Shop Ltd.",
                time = "Today · 14:32",
                status = TransactionStatusUi.Completed,
                category = TransactionCategoryUi.Today,
            ),
            PreviewEntry(
                name = "Identity Check",
                relying = "Gov Portal",
                time = "Today · 09:15",
                status = TransactionStatusUi.Failed,
                category = TransactionCategoryUi.Today,
            ),
            PreviewEntry(
                name = "Address Proof",
                relying = "Bank Services",
                time = "Mon · 18:44",
                status = TransactionStatusUi.Completed,
                category = TransactionCategoryUi.ThisWeek,
            ),
            PreviewEntry(
                name = "Driving Licence",
                relying = "Car Rental Co.",
                time = "Sun · 11:02",
                status = TransactionStatusUi.Completed,
                category = TransactionCategoryUi.ThisWeek,
            ),
            PreviewEntry(
                name = "Passport Share",
                relying = "Travel Agency",
                time = "15 Jan · 10:00",
                status = TransactionStatusUi.Completed,
                category =
                    TransactionCategoryUi.Month(
                        LocalDateTime(year = 2026, month = 1, day = 15, hour = 10, minute = 0, second = 0, nanosecond = 0).toJavaLocalDateTime(),
                    ),
            ),
            PreviewEntry(
                name = "Tax ID Disclosure",
                relying = "Finance Dept.",
                time = "3 Jan · 08:30",
                status = TransactionStatusUi.Failed,
                category =
                    TransactionCategoryUi.Month(
                        LocalDateTime(year = 2026, month = 1, day = 3, hour = 8, minute = 30, second = 0, nanosecond = 0).toJavaLocalDateTime(),
                    ),
            ),
        )

    val transactions =
        previewEntries.mapIndexed { index, entry ->
            TransactionUi(
                uiData =
                    ExpandableListItemUi.SingleListItem(
                        header =
                            ListItemDataUi(
                                itemId = index.toString(),
                                mainContentData = ListItemMainContentDataUi.Text(entry.name),
                                overlineText =
                                    when (entry.status) {
                                        TransactionStatusUi.Completed -> "Completed"
                                        TransactionStatusUi.Failed -> "Failed"
                                    },
                                supportingText = "${entry.relying}  ·  ${entry.time}",
                                trailingContentData =
                                    ListItemTrailingContentDataUi.Icon(
                                        iconData = AppIcons.KeyboardArrowRight,
                                    ),
                            ),
                    ),
                uiStatus = entry.status,
                transactionCategoryUi = entry.category,
            )
        }

    val groupedTransactions =
        transactions
            .groupBy { it.transactionCategoryUi.id }
            .map { (_, transactions) -> transactions.first().transactionCategoryUi to transactions }

    PreviewTheme {
        ContentScreen(
            isLoading = false,
            navigatableAction = ScreenNavigateAction.NONE,
            onBack = { },
            topBar = {
                TopBar(
                    onDashboardEventSent = {},
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .paddingFrom(paddingValues, bottom = false),
                contentPadding = PaddingValues(bottom = SPACING_MEDIUM.dp),
            ) {
                item {
                    FiltersSearchBar(
                        placeholder = "Search transactions",
                        onValueChange = {},
                        onFilterClick = {},
                        onClearClick = {},
                        isFilteringActive = false,
                        text = "",
                    )
                    VSpacer.Large()
                }

                itemsIndexed(items = groupedTransactions) { index, (category, transactionsUis) ->
                    TransactionCategory(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = SPACING_MEDIUM.dp),
                        category = category,
                        transactions = transactionsUis,
                        onEventSend = {},
                    )

                    if (index != groupedTransactions.lastIndex) {
                        VSpacer.ExtraLarge()
                    }
                }
            }
        }
    }
}
