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

package com.k689.identid.ui.presentation.success

import androidx.lifecycle.viewModelScope
import com.k689.identid.config.ConfigNavigation
import com.k689.identid.config.NavigationType
import com.k689.identid.di.core.getOrCreatePresentationScope
import com.k689.identid.interactor.presentation.PresentationSuccessInteractor
import com.k689.identid.interactor.presentation.PresentationSuccessInteractorGetUiItemsPartialState
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.ui.common.document.sucess.DocumentSuccessViewModel
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class PresentationSuccessViewModel(
    private val interactor: PresentationSuccessInteractor,
) : DocumentSuccessViewModel() {
    override fun getNextScreenConfigNavigation(): ConfigNavigation {
        val redirectUri = interactor.redirectUri
        val deepLinkWithUriOrPopToDashboard =
            ConfigNavigation(
                navigationType =
                    redirectUri?.let {
                        NavigationType.Deeplink(it.toString(), interactor.initiatorRoute)
                    } ?: NavigationType.PopTo(DashboardScreens.Dashboard),
            )

        return deepLinkWithUriOrPopToDashboard
    }

    override fun doWork() {
        setState {
            copy(isLoading = true)
        }

        viewModelScope.launch {
            interactor.getUiItems().collect { response ->
                when (response) {
                    is PresentationSuccessInteractorGetUiItemsPartialState.Failed -> {
                        setState {
                            copy(
                                isLoading = false,
                            )
                        }
                    }

                    is PresentationSuccessInteractorGetUiItemsPartialState.Success -> {
                        setState {
                            copy(
                                headerConfig = response.headerConfig,
                                items = response.documentsUi,
                                isLoading = false,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        interactor.stopPresentation()
        getOrCreatePresentationScope().close()
    }
}
