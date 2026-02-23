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

package com.k689.identid.ui.startup.splash

import androidx.lifecycle.viewModelScope
import com.k689.identid.interactor.startup.SplashInteractor
import com.k689.identid.navigation.ModuleRoute
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

data class State(
    val logoAnimationDuration: Int = 250,
) : ViewState

sealed class Event : ViewEvent {
    data object Initialize : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchModule(
            val moduleRoute: ModuleRoute,
        ) : Navigation()

        data class SwitchScreen(
            val route: String,
        ) : Navigation()
    }
}

@KoinViewModel
class SplashViewModel(
    private val interactor: SplashInteractor,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State = State()

    override fun handleEvents(event: Event) {
        when (event) {
            Event.Initialize -> enterApplication()
        }
    }

    private fun enterApplication() {
        viewModelScope.launch {
            val totalSplashDurationMs = 1000
            val remainingDelayMs = (totalSplashDurationMs - viewState.value.logoAnimationDuration).coerceAtLeast(0).toLong()
            delay(remainingDelayMs)
            val screenRoute = withContext(Dispatchers.IO) { interactor.getAfterSplashRoute() }
            setEffect {
                Effect.Navigation.SwitchScreen(screenRoute)
            }
        }
    }
}
