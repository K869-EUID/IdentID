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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.navigation.ModuleRoute
import com.k689.identid.navigation.StartupScreens
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.preview.PreviewTheme
import com.k689.identid.ui.component.utils.OneTimeLaunchedEffect
import com.k689.identid.ui.component.wrap.WrapImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    Content(
        state = state,
        effectFlow = viewModel.effect,
        onNavigationRequested = {
            when (it) {
                is Effect.Navigation.SwitchModule -> {
                    navController.navigate(it.moduleRoute.route) {
                        popUpTo(ModuleRoute.StartupModule.route) { inclusive = true }
                    }
                }

                is Effect.Navigation.SwitchScreen -> {
                    navController.navigate(it.route) {
                        popUpTo(StartupScreens.Splash.screenRoute) { inclusive = true }
                    }
                }
            }
        },
    )

    OneTimeLaunchedEffect {
        viewModel.setEvent(Event.Initialize)
    }
}

@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onNavigationRequested: (navigationEffect: Effect.Navigation) -> Unit,
) {
    val greenCardVisibility = remember { MutableTransitionState(false) }
    val redCardVisibility = remember { MutableTransitionState(false) }

    LaunchedEffect(Unit) {
        greenCardVisibility.targetState = true
        delay(150)
        redCardVisibility.targetState = true
    }

    Scaffold { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                AnimatedVisibility(
                    visibleState = redCardVisibility,
                    enter =
                        slideIn(
                            animationSpec = tween(550),
                            initialOffset = { IntOffset(x = 0, y = 80) },
                        ) +
                            fadeIn(
                                animationSpec = tween(450),
                            ),
                    exit = fadeOut(animationSpec = tween(state.logoAnimationDuration)),
                ) {
                    WrapImage(
                        iconData = AppIcons.RedCard,
                        modifier = Modifier.size(300.dp),
                    )
                }

                AnimatedVisibility(
                    visibleState = greenCardVisibility,
                    enter =
                        slideIn(
                            animationSpec = tween(550),
                            initialOffset = { IntOffset(x = 0, y = 80) },
                        ) +
                            fadeIn(
                                animationSpec = tween(450),
                            ),
                    exit = fadeOut(animationSpec = tween(state.logoAnimationDuration)),
                ) {
                    WrapImage(
                        iconData = AppIcons.GreenCard,
                        modifier = Modifier.size(300.dp),
                    )
                }

                WrapImage(
                    iconData = AppIcons.YellowCard,
                    modifier = Modifier.size(300.dp),
                )
            }
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

@Preview
@Composable
private fun SplashScreenPreview() {
    PreviewTheme {
        Content(
            state = State(),
            effectFlow = emptyFlow(),
            onNavigationRequested = {},
        )
    }
}
