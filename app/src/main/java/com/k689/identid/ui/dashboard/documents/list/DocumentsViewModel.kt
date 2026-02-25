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

package com.k689.identid.ui.dashboard.documents.list

import androidx.lifecycle.viewModelScope
import com.k689.identid.R
import com.k689.identid.config.IssuanceFlowType
import com.k689.identid.config.IssuanceUiConfig
import com.k689.identid.config.QrScanFlow
import com.k689.identid.config.QrScanUiConfig
import com.k689.identid.interactor.dashboard.DocumentInteractorDeleteDocumentPartialState
import com.k689.identid.interactor.dashboard.DocumentInteractorFilterPartialState
import com.k689.identid.interactor.dashboard.DocumentInteractorGetDocumentsPartialState
import com.k689.identid.interactor.dashboard.DocumentInteractorRetryIssuingDeferredDocumentsPartialState
import com.k689.identid.interactor.dashboard.DocumentsInteractor
import com.k689.identid.model.core.DeferredDocumentDataDomain
import com.k689.identid.model.core.DocumentCategory
import com.k689.identid.model.core.FormatType
import com.k689.identid.model.validator.FilterableList
import com.k689.identid.model.validator.SortOrder
import com.k689.identid.navigation.CommonScreens
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.navigation.IssuanceScreens
import com.k689.identid.navigation.StartupScreens
import com.k689.identid.navigation.helper.generateComposableArguments
import com.k689.identid.navigation.helper.generateComposableNavigationLink
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.theme.values.ThemeColors
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.DualSelectorButton
import com.k689.identid.ui.component.DualSelectorButtonDataUi
import com.k689.identid.ui.component.ListItemTrailingContentDataUi
import com.k689.identid.ui.component.ModalOptionUi
import com.k689.identid.ui.component.content.ContentErrorConfig
import com.k689.identid.ui.component.wrap.ExpandableListItemUi
import com.k689.identid.ui.dashboard.documents.detail.model.DocumentIssuanceStateUi
import com.k689.identid.ui.dashboard.documents.list.DocumentsBottomSheetContent.DeferredDocumentPressed
import com.k689.identid.ui.dashboard.documents.list.DocumentsBottomSheetContent.Filters
import com.k689.identid.ui.dashboard.documents.list.model.DocumentUi
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import com.k689.identid.ui.serializer.UiSerializer
import eu.europa.ec.eudi.wallet.document.DocumentId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class State(
    val isLoading: Boolean,
    val error: ContentErrorConfig? = null,
    val isBottomSheetOpen: Boolean = false,
    val sheetContent: DocumentsBottomSheetContent = Filters(filters = emptyList()),
    val documentsUi: List<Pair<DocumentCategory, List<DocumentUi>>> = emptyList(),
    val showNoResultsFound: Boolean = false,
    val deferredFailedDocIds: List<DocumentId> = emptyList(),
    val searchText: String = "",
    val allowUserInteraction: Boolean = true,
    val isFromOnPause: Boolean = true,
    val shouldRevertFilterChanges: Boolean = true,
    val filtersUi: List<ExpandableListItemUi.NestedListItem> = emptyList(),
    val sortOrder: DualSelectorButtonDataUi,
    val isFilteringActive: Boolean,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()

    data object GetDocuments : Event()

    data object OnPause : Event()

    data class TryIssuingDeferredDocuments(
        val deferredDocs: Map<DocumentId, FormatType>,
    ) : Event()

    data object Pop : Event()

    data class GoToDocumentDetails(
        val docId: DocumentId,
    ) : Event()

    data class OnSearchQueryChanged(
        val query: String,
    ) : Event()

    data class OnFilterSelectionChanged(
        val filterId: String,
        val groupId: String,
    ) : Event()

    data object OnFiltersReset : Event()

    data object OnFiltersApply : Event()

    data class OnSortingOrderChanged(
        val sortingOrder: DualSelectorButton,
    ) : Event()

    data object AddDocumentPressed : Event()

    data object FiltersPressed : Event()

    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(
            val isOpen: Boolean,
        ) : BottomSheet()

        sealed class AddDocument : BottomSheet() {
            data object FromList : AddDocument()

            data object ScanQr : AddDocument()
        }

        sealed class DeferredDocument : BottomSheet() {
            sealed class DeferredNotReadyYet(
                open val documentId: DocumentId,
            ) : DeferredDocument() {
                data class DocumentSelected(
                    override val documentId: DocumentId,
                ) : DeferredNotReadyYet(documentId)

                data class PrimaryButtonPressed(
                    override val documentId: DocumentId,
                ) : DeferredNotReadyYet(documentId)

                data class SecondaryButtonPressed(
                    override val documentId: DocumentId,
                ) : DeferredNotReadyYet(documentId)
            }

            data class OptionListItemForSuccessfullyIssuingDeferredDocumentSelected(
                val documentId: DocumentId,
            ) : DeferredDocument()
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
    }

    data class DocumentsFetched(
        val deferredDocs: Map<DocumentId, FormatType>,
    ) : Effect()

    data object ShowBottomSheet : Effect()

    data object CloseBottomSheet : Effect()

    data object ResumeOnApplyFilter : Effect()
}

sealed class DocumentsBottomSheetContent {
    data class Filters(
        val filters: List<ExpandableListItemUi.SingleListItem>,
    ) : DocumentsBottomSheetContent()

    data object AddDocument : DocumentsBottomSheetContent()

    data class DeferredDocumentPressed(
        val documentId: DocumentId,
    ) : DocumentsBottomSheetContent()

    data class DeferredDocumentsReady(
        val successfullyIssuedDeferredDocuments: List<DeferredDocumentDataDomain>,
        val options: List<ModalOptionUi<Event>>,
    ) : DocumentsBottomSheetContent()
}

@KoinViewModel
class DocumentsViewModel(
    private val interactor: DocumentsInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
) : MviViewModel<Event, State, Effect>() {
    private var retryDeferredDocsJob: Job? = null
    private var fetchDocumentsJob: Job? = null

    override fun setInitialState(): State =
        State(
            isLoading = true,
            sortOrder =
                DualSelectorButtonDataUi(
                    first = resourceProvider.getString(R.string.documents_screen_filters_ascending),
                    second = resourceProvider.getString(R.string.documents_screen_filters_descending),
                    selectedButton = DualSelectorButton.FIRST,
                ),
            isFilteringActive = false,
        )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                filterStateChanged()
            }

            is Event.GetDocuments -> {
                getDocuments(
                    event = event,
                    deferredFailedDocIds = viewState.value.deferredFailedDocIds,
                )
            }

            is Event.OnPause -> {
                stopDeferredIssuing()
                stopFetchDocuments()
                setState { copy(isFromOnPause = true) }
            }

            is Event.TryIssuingDeferredDocuments -> {
                tryIssuingDeferredDocuments(event, event.deferredDocs)
            }

            is Event.Pop -> {
                setEffect { Effect.Navigation.Pop }
            }

            is Event.GoToDocumentDetails -> {
                goToDocumentDetails(event.docId)
            }

            is Event.AddDocumentPressed -> {
                showBottomSheet(sheetContent = DocumentsBottomSheetContent.AddDocument)
            }

            is Event.FiltersPressed -> {
                stopDeferredIssuing()
                showBottomSheet(sheetContent = Filters(filters = emptyList()))
            }

            is Event.OnSearchQueryChanged -> {
                applySearch(event.query)
            }

            is Event.OnFilterSelectionChanged -> {
                updateFilter(event.filterId, event.groupId)
            }

            is Event.OnFiltersApply -> {
                applySelectedFilters()
            }

            is Event.OnFiltersReset -> {
                resetFilters()
            }

            is Event.OnSortingOrderChanged -> {
                sortOrderChanged(event.sortingOrder)
            }

            is Event.BottomSheet.UpdateBottomSheetState -> {
                if (viewState.value.sheetContent is Filters &&
                    !event.isOpen
                ) {
                    setEffect { Effect.ResumeOnApplyFilter }
                }
                revertFilters(event.isOpen)
            }

            is Event.BottomSheet.AddDocument.FromList -> {
                hideBottomSheet()
                goToAddDocument()
            }

            is Event.BottomSheet.AddDocument.ScanQr -> {
                hideBottomSheet()
                goToQrScan()
            }

            is Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.DocumentSelected -> {
                showBottomSheet(
                    sheetContent =
                        DeferredDocumentPressed(
                            documentId = event.documentId,
                        ),
                )
            }

            is Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.PrimaryButtonPressed -> {
                hideBottomSheet()
                deleteDocument(event = event, documentId = event.documentId)
            }

            is Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.SecondaryButtonPressed -> {
                hideBottomSheet()
            }

            is Event.BottomSheet.DeferredDocument.OptionListItemForSuccessfullyIssuingDeferredDocumentSelected -> {
                hideBottomSheet()
                goToDocumentDetails(docId = event.documentId)
            }
        }
    }

    private fun filterStateChanged() {
        viewModelScope.launch {
            interactor.onFilterStateChange().collect { result ->
                when (result) {
                    is DocumentInteractorFilterPartialState.FilterApplyResult -> {
                        setState {
                            copy(
                                isFilteringActive = !result.allDefaultFiltersAreSelected,
                                documentsUi = result.documents,
                                showNoResultsFound = result.documents.isEmpty(),
                                filtersUi = result.filters,
                                sortOrder = sortOrder.copy(selectedButton = result.sortOrder),
                            )
                        }
                    }

                    is DocumentInteractorFilterPartialState.FilterUpdateResult -> {
                        setState {
                            copy(
                                filtersUi = result.filters,
                                sortOrder = sortOrder.copy(selectedButton = result.sortOrder),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun getDocuments(
        event: Event,
        deferredFailedDocIds: List<DocumentId>,
    ) {
        setState {
            copy(
                isLoading = documentsUi.isEmpty(),
                error = null,
            )
        }
        fetchDocumentsJob =
            viewModelScope.launch {
                interactor
                    .getDocuments()
                    .collect { response ->
                        when (response) {
                            is DocumentInteractorGetDocumentsPartialState.Failure -> {
                                setState {
                                    copy(
                                        isLoading = false,
                                        error =
                                            ContentErrorConfig(
                                                onRetry = { setEvent(event) },
                                                errorSubTitle = response.error,
                                                onCancel = {
                                                    setState { copy(error = null) }
                                                    setEvent(Event.Pop)
                                                },
                                            ),
                                    )
                                }
                            }

                            is DocumentInteractorGetDocumentsPartialState.Success -> {
                                val deferredDocs: MutableMap<DocumentId, FormatType> = mutableMapOf()
                                response.allDocuments.items
                                    .filter { document ->
                                        with(document.payload as DocumentUi) {
                                            documentIssuanceState == DocumentIssuanceStateUi.Pending
                                        }
                                    }.forEach { documentItem ->
                                        with(documentItem.payload as DocumentUi) {
                                            deferredDocs[uiData.itemId] =
                                                documentIdentifier.formatType
                                        }
                                    }
                                val documentsWithFailed =
                                    response.allDocuments
                                        .generateFailedDeferredDocs(deferredFailedDocIds)

                                if (viewState.value.isFromOnPause) {
                                    interactor.initializeFilters(
                                        filterableList = documentsWithFailed,
                                    )
                                } else {
                                    interactor.updateLists(
                                        filterableList = documentsWithFailed,
                                    )
                                }

                                interactor.applyFilters()

                                setState {
                                    copy(
                                        isLoading = false,
                                        error = null,
                                        deferredFailedDocIds = deferredFailedDocIds,
                                        allowUserInteraction = response.shouldAllowUserInteraction,
                                        isFromOnPause = false,
                                    )
                                }
                                setEffect { Effect.DocumentsFetched(deferredDocs) }
                            }
                        }
                    }
            }
    }

    private fun FilterableList.generateFailedDeferredDocs(deferredFailedDocIds: List<DocumentId>): FilterableList =
        copy(
            items =
                items.map { filterableItem ->
                    val data = filterableItem.payload as DocumentUi
                    val failedUiItem =
                        if (data.uiData.itemId in deferredFailedDocIds) {
                            data.copy(
                                documentIssuanceState = DocumentIssuanceStateUi.Failed,
                                uiData =
                                    data.uiData.copy(
                                        supportingText = resourceProvider.getString(R.string.dashboard_document_deferred_failed),
                                        trailingContentData =
                                            ListItemTrailingContentDataUi.Icon(
                                                iconData = AppIcons.ErrorFilled,
                                                tint = ThemeColors.error,
                                            ),
                                    ),
                            )
                        } else {
                            data
                        }

                    filterableItem.copy(payload = failedUiItem)
                },
        )

    private fun tryIssuingDeferredDocuments(
        event: Event,
        deferredDocs: Map<DocumentId, FormatType>,
    ) {
        setState {
            copy(
                isLoading = false,
                error = null,
            )
        }

        stopDeferredIssuing()
        retryDeferredDocsJob =
            viewModelScope.launch {
                if (deferredDocs.isEmpty()) {
                    return@launch
                }

                delay(5000L)

                interactor.tryIssuingDeferredDocumentsFlow(deferredDocs).collect { response ->
                    when (response) {
                        is DocumentInteractorRetryIssuingDeferredDocumentsPartialState.Failure -> {
                            setState {
                                copy(
                                    isLoading = false,
                                    error =
                                        ContentErrorConfig(
                                            onRetry = { setEvent(event) },
                                            errorSubTitle = response.errorMessage,
                                            onCancel = {
                                                setState { copy(error = null) }
                                            },
                                        ),
                                )
                            }
                        }

                        is DocumentInteractorRetryIssuingDeferredDocumentsPartialState.Result -> {
                            val successDocs = response.successfullyIssuedDeferredDocuments
                            if (successDocs.isNotEmpty() &&
                                (
                                    !viewState.value.isBottomSheetOpen ||
                                        (
                                            viewState.value.isBottomSheetOpen &&
                                                viewState.value.sheetContent !is DocumentsBottomSheetContent.DeferredDocumentsReady
                                        )
                                )
                            ) {
                                showBottomSheet(
                                    sheetContent =
                                        DocumentsBottomSheetContent.DeferredDocumentsReady(
                                            successfullyIssuedDeferredDocuments = successDocs,
                                            options =
                                                getBottomSheetOptions(
                                                    deferredDocumentsData = successDocs,
                                                ),
                                        ),
                                )
                            }

                            getDocuments(
                                event = event,
                                deferredFailedDocIds = response.failedIssuedDeferredDocuments,
                            )
                        }
                    }
                }
            }
    }

    private fun getBottomSheetOptions(deferredDocumentsData: List<DeferredDocumentDataDomain>): List<ModalOptionUi<Event>> =
        deferredDocumentsData.map {
            ModalOptionUi(
                title = it.docName,
                trailingIcon = AppIcons.KeyboardArrowRight,
                event =
                    Event.BottomSheet.DeferredDocument.OptionListItemForSuccessfullyIssuingDeferredDocumentSelected(
                        documentId = it.documentId,
                    ),
            )
        }

    private fun deleteDocument(
        event: Event,
        documentId: DocumentId,
    ) {
        setState {
            copy(
                isLoading = true,
                error = null,
            )
        }

        viewModelScope.launch {
            interactor
                .deleteDocument(
                    documentId = documentId,
                ).collect { response ->
                    when (response) {
                        is DocumentInteractorDeleteDocumentPartialState.AllDocumentsDeleted -> {
                            setState {
                                copy(
                                    isLoading = false,
                                    error = null,
                                )
                            }

                            setEffect {
                                Effect.Navigation.SwitchScreen(
                                    screenRoute = StartupScreens.Splash.screenRoute,
                                    popUpToScreenRoute = DashboardScreens.Dashboard.screenRoute,
                                    inclusive = true,
                                )
                            }
                        }

                        is DocumentInteractorDeleteDocumentPartialState.SingleDocumentDeleted -> {
                            getDocuments(
                                event = event,
                                deferredFailedDocIds = viewState.value.deferredFailedDocIds,
                            )
                        }

                        is DocumentInteractorDeleteDocumentPartialState.Failure -> {
                            setState {
                                copy(
                                    isLoading = false,
                                    error =
                                        ContentErrorConfig(
                                            onRetry = { setEvent(event) },
                                            errorSubTitle = response.errorMessage,
                                            onCancel = {
                                                setState {
                                                    copy(error = null)
                                                }
                                            },
                                        ),
                                )
                            }
                        }
                    }
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

    private fun goToAddDocument() {
        val addDocumentScreenRoute =
            generateComposableNavigationLink(
                screen = IssuanceScreens.AddDocument,
                arguments =
                    generateComposableArguments(
                        mapOf(
                            IssuanceUiConfig.serializedKeyName to
                                uiSerializer.toBase64(
                                    model =
                                        IssuanceUiConfig(
                                            flowType =
                                                IssuanceFlowType.ExtraDocument(
                                                    formatType = null,
                                                ),
                                        ),
                                    parser = IssuanceUiConfig.Parser,
                                ),
                        ),
                    ),
            )
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = addDocumentScreenRoute,
            )
        }
    }

    private fun goToQrScan() {
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
                                                qrScanFlow =
                                                    QrScanFlow.Issuance(
                                                        issuanceFlowType =
                                                            IssuanceFlowType.ExtraDocument(
                                                                formatType = null,
                                                            ),
                                                    ),
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

    private fun showBottomSheet(sheetContent: DocumentsBottomSheetContent) {
        setState {
            copy(sheetContent = sheetContent)
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

    private fun applySearch(queryText: String) {
        interactor.applySearch(queryText)
        setState {
            copy(searchText = queryText)
        }
    }

    private fun updateFilter(
        filterId: String,
        groupId: String,
    ) {
        setState { copy(shouldRevertFilterChanges = true) }
        interactor.updateFilter(filterGroupId = groupId, filterId = filterId)
    }

    private fun applySelectedFilters() {
        interactor.applyFilters()
        setState {
            copy(
                shouldRevertFilterChanges = false,
            )
        }
        hideBottomSheet()
    }

    private fun resetFilters() {
        interactor.resetFilters()
        hideBottomSheet()
    }

    private fun revertFilters(isOpening: Boolean) {
        if (viewState.value.sheetContent is Filters &&
            !isOpening &&
            viewState.value.shouldRevertFilterChanges
        ) {
            interactor.revertFilters()
            setState { copy(shouldRevertFilterChanges = true) }
        }

        setState {
            copy(isBottomSheetOpen = isOpening)
        }
    }

    private fun sortOrderChanged(orderButton: DualSelectorButton) {
        val sortOrder =
            when (orderButton) {
                DualSelectorButton.FIRST -> SortOrder.Ascending(isDefault = true)
                DualSelectorButton.SECOND -> SortOrder.Descending()
            }
        setState { copy(shouldRevertFilterChanges = true) }
        interactor.updateSortOrder(sortOrder)
    }

    private fun stopDeferredIssuing() {
        retryDeferredDocsJob?.cancel()
    }

    private fun stopFetchDocuments() {
        fetchDocumentsJob?.cancel()
    }
}
