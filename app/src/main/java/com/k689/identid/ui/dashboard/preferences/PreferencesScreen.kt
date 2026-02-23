package com.k689.identid.ui.dashboard.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.theme.AppTheme
import com.k689.identid.ui.component.ListItemDataUi
import com.k689.identid.ui.component.ListItemMainContentDataUi
import com.k689.identid.ui.component.ListItemTrailingContentDataUi
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ContentTitle
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.wrap.RadioButtonDataUi
import com.k689.identid.ui.component.wrap.WrapListItem
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun PreferencesScreen(
    navController: NavController,
    viewModel: PreferencesViewModel,
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effect
            .onEach { effect ->
                when (effect) {
                    is Effect.Navigation.Pop -> navController.popBackStack()
                }
            }.collect()
    }

    ContentScreen(
        navigatableAction = ScreenNavigateAction.BACKABLE,
        isLoading = false,
        onBack = { viewModel.setEvent(Event.Pop) },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            ContentTitle(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                title = state.screenTitle,
            )
            AppTheme.entries.onEach { theme ->
                val isSelected = theme == state.selectedTheme
                WrapListItem(
                    item =
                        ListItemDataUi(
                            itemId = theme.name,
                            mainContentData =
                                ListItemMainContentDataUi.Text(theme.name),
                            trailingContentData =
                                ListItemTrailingContentDataUi.RadioButton(
                                    RadioButtonDataUi(
                                        isSelected = isSelected,
                                        onCheckedChange = {
                                            viewModel.setEvent(
                                                Event.OnThemeSelected(
                                                    theme,
                                                ),
                                            )
                                        },
                                    ),
                                ),
                        ),
                    modifier = Modifier.padding(8.dp),
                    onItemClick = { viewModel.setEvent(Event.OnThemeSelected(theme)) },
                )
            }
        }
    }
}
