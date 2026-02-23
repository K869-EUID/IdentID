package com.k689.identid.ui.dashboard.preferences

import com.k689.identid.R
import com.k689.identid.controller.storage.PrefKeys
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.theme.AppTheme
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import org.koin.android.annotation.KoinViewModel

data class State(
    val screenTitle: String,
    val selectedTheme: AppTheme = AppTheme.SYSTEM,
) : ViewState

sealed class Event : ViewEvent {
    data object Pop : Event()

    data class OnThemeSelected(
        val theme: AppTheme,
    ) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
    }
}

@KoinViewModel
class PreferencesViewModel(
    private val resourceProvider: ResourceProvider,
    private val prefKeys: PrefKeys,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State =
        State(
            screenTitle = resourceProvider.getString(R.string.preferences_screen_title),
            selectedTheme = prefKeys.theme.value,
        )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Pop -> {
                setEffect { Effect.Navigation.Pop }
            }

            is Event.OnThemeSelected -> {
                prefKeys.setTheme(event.theme)
                setState { copy(selectedTheme = event.theme) }
            }
        }
    }
}
