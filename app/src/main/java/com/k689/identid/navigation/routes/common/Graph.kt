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

package com.k689.identid.navigation.routes.common

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.k689.identid.BuildConfig
import com.k689.identid.config.BiometricUiConfig
import com.k689.identid.config.QrScanUiConfig
import com.k689.identid.config.SuccessUIConfig
import com.k689.identid.model.common.PinFlow
import com.k689.identid.navigation.CommonScreens
import com.k689.identid.navigation.ModuleRoute
import com.k689.identid.ui.common.biometric.BiometricScreen
import com.k689.identid.ui.common.pin.PinScreen
import com.k689.identid.ui.common.scan.QrScanScreen
import com.k689.identid.ui.common.success.SuccessScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

fun NavGraphBuilder.featureCommonGraph(navController: NavController) {
    navigation(
        startDestination = CommonScreens.Biometric.screenRoute,
        route = ModuleRoute.CommonModule.route,
    ) {
        composable(
            route = CommonScreens.Biometric.screenRoute,
            deepLinks =
                listOf(
                    navDeepLink {
                        uriPattern =
                            BuildConfig.DEEPLINK + CommonScreens.Biometric.screenRoute
                    },
                ),
            arguments =
                listOf(
                    navArgument(BiometricUiConfig.serializedKeyName) {
                        type = NavType.StringType
                    },
                ),
        ) {
            BiometricScreen(
                navController,
                koinViewModel(
                    parameters = {
                        parametersOf(
                            it.arguments?.getString(BiometricUiConfig.serializedKeyName).orEmpty(),
                        )
                    },
                ),
            )
        }

        composable(
            route = CommonScreens.Success.screenRoute,
            deepLinks =
                listOf(
                    navDeepLink {
                        uriPattern =
                            BuildConfig.DEEPLINK + CommonScreens.Success.screenRoute
                    },
                ),
            arguments =
                listOf(
                    navArgument(SuccessUIConfig.serializedKeyName) {
                        type = NavType.StringType
                    },
                ),
        ) {
            SuccessScreen(
                navController,
                koinViewModel(
                    parameters = {
                        parametersOf(
                            it.arguments?.getString(SuccessUIConfig.serializedKeyName).orEmpty(),
                        )
                    },
                ),
            )
        }

        composable(
            route = CommonScreens.QuickPin.screenRoute,
            deepLinks =
                listOf(
                    navDeepLink {
                        uriPattern = BuildConfig.DEEPLINK + CommonScreens.QuickPin.screenRoute
                    },
                ),
            arguments =
                listOf(
                    navArgument("pinFlow") {
                        type = NavType.StringType
                    },
                ),
        ) {
            PinScreen(
                navController,
                koinViewModel(
                    parameters = {
                        parametersOf(
                            PinFlow.valueOf(
                                it.arguments?.getString("pinFlow").orEmpty(),
                            ),
                        )
                    },
                ),
            )
        }

        composable(
            route = CommonScreens.QrScan.screenRoute,
            deepLinks =
                listOf(
                    navDeepLink {
                        uriPattern =
                            BuildConfig.DEEPLINK + CommonScreens.QrScan.screenRoute
                    },
                ),
            arguments =
                listOf(
                    navArgument(QrScanUiConfig.serializedKeyName) {
                        type = NavType.StringType
                    },
                ),
        ) {
            QrScanScreen(
                navController,
                koinViewModel(
                    parameters = {
                        parametersOf(
                            it.arguments?.getString(QrScanUiConfig.serializedKeyName).orEmpty(),
                        )
                    },
                ),
            )
        }
    }
}
