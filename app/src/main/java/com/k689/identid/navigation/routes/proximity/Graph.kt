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

package com.k689.identid.navigation.routes.proximity

import ProximityRequestScreen
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.k689.identid.BuildConfig
import com.k689.identid.config.RequestUriConfig
import com.k689.identid.navigation.ModuleRoute
import com.k689.identid.navigation.ProximityScreens
import com.k689.identid.ui.proximity.loading.ProximityLoadingScreen
import com.k689.identid.ui.proximity.qr.ProximityQRScreen
import com.k689.identid.ui.proximity.success.ProximitySuccessScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

fun NavGraphBuilder.featureProximityGraph(navController: NavController) {
    navigation(
        startDestination = ProximityScreens.QR.screenRoute,
        route = ModuleRoute.ProximityModule.route,
    ) {
        // QR
        composable(
            route = ProximityScreens.QR.screenRoute,
            deepLinks =
                listOf(
                    navDeepLink {
                        uriPattern =
                            BuildConfig.DEEPLINK + ProximityScreens.QR.screenRoute
                    },
                ),
            arguments =
                listOf(
                    navArgument(RequestUriConfig.serializedKeyName) {
                        type = NavType.StringType
                    },
                ),
        ) {
            ProximityQRScreen(
                navController,
                koinViewModel(
                    parameters = {
                        parametersOf(
                            it.arguments?.getString(RequestUriConfig.serializedKeyName).orEmpty(),
                        )
                    },
                ),
            )
        }

        // Request
        composable(
            route = ProximityScreens.Request.screenRoute,
            deepLinks =
                listOf(
                    navDeepLink {
                        uriPattern =
                            BuildConfig.DEEPLINK + ProximityScreens.Request.screenRoute
                    },
                ),
            arguments =
                listOf(
                    navArgument(RequestUriConfig.serializedKeyName) {
                        type = NavType.StringType
                    },
                ),
        ) {
            ProximityRequestScreen(
                navController,
                koinViewModel(
                    parameters = {
                        parametersOf(
                            it.arguments?.getString(RequestUriConfig.serializedKeyName).orEmpty(),
                        )
                    },
                ),
            )
        }

        // Loading
        composable(
            route = ProximityScreens.Loading.screenRoute,
        ) {
            ProximityLoadingScreen(
                navController,
                koinViewModel(),
            )
        }

        // Success
        composable(
            route = ProximityScreens.Success.screenRoute,
            deepLinks =
                listOf(
                    navDeepLink {
                        uriPattern =
                            BuildConfig.DEEPLINK + ProximityScreens.Request.screenRoute
                    },
                ),
        ) {
            ProximitySuccessScreen(
                navController,
                koinViewModel(),
            )
        }
    }
}
