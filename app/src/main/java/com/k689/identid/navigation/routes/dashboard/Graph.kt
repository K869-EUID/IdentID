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

package com.k689.identid.navigation.routes.dashboard

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.k689.identid.BuildConfig
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.navigation.ModuleRoute
import com.k689.identid.ui.dashboard.dashboard.DashboardScreen
import com.k689.identid.ui.dashboard.documents.detail.DocumentDetailsScreen
import com.k689.identid.ui.dashboard.preferences.PreferencesScreen
import com.k689.identid.ui.dashboard.settings.SettingsScreen
import com.k689.identid.ui.dashboard.sign.DocumentSignScreen
import com.k689.identid.ui.dashboard.transactions.detail.TransactionDetailsScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

fun NavGraphBuilder.featureDashboardGraph(navController: NavController) {
    navigation(
        startDestination = DashboardScreens.Dashboard.screenRoute,
        route = ModuleRoute.DashboardModule.route,
    ) {
        composable(
            route = DashboardScreens.Dashboard.screenRoute,
            deepLinks =
                listOf(
                    navDeepLink {
                        uriPattern =
                            BuildConfig.DEEPLINK + DashboardScreens.Dashboard.screenRoute
                    },
                ),
        ) {
            DashboardScreen(
                hostNavController = navController,
                viewModel = koinViewModel(),
                documentsViewModel = koinViewModel(),
                homeViewModel = koinViewModel(),
                transactionsViewModel = koinViewModel(),
            )
        }

        composable(
            route = DashboardScreens.Settings.screenRoute,
            deepLinks =
                listOf(
                    navDeepLink {
                        uriPattern =
                            BuildConfig.DEEPLINK + DashboardScreens.Settings.screenRoute
                    },
                ),
        ) {
            SettingsScreen(
                navController = navController,
                viewModel = koinViewModel(),
            )
        }

        composable(
            route = DashboardScreens.Preferences.screenRoute,
        ) {
            PreferencesScreen(navController, koinViewModel())
        }

        composable(
            route = DashboardScreens.DocumentSign.screenRoute,
        ) {
            DocumentSignScreen(navController, koinViewModel())
        }

        composable(
            route = DashboardScreens.DocumentDetails.screenRoute,
            deepLinks =
                listOf(
                    navDeepLink {
                        uriPattern =
                            BuildConfig.DEEPLINK + DashboardScreens.DocumentDetails.screenRoute
                    },
                ),
            arguments =
                listOf(
                    navArgument("documentId") {
                        type = NavType.StringType
                    },
                ),
        ) {
            DocumentDetailsScreen(
                navController,
                koinViewModel(
                    parameters = {
                        parametersOf(
                            it.arguments?.getString("documentId").orEmpty(),
                        )
                    },
                ),
            )
        }

        composable(
            route = DashboardScreens.TransactionDetails.screenRoute,
            deepLinks =
                listOf(
                    navDeepLink {
                        uriPattern =
                            BuildConfig.DEEPLINK + DashboardScreens.TransactionDetails.screenRoute
                    },
                ),
            arguments =
                listOf(
                    navArgument("transactionId") {
                        type = NavType.StringType
                    },
                ),
        ) {
            TransactionDetailsScreen(
                navController,
                koinViewModel(
                    parameters = {
                        parametersOf(
                            it.arguments?.getString("transactionId").orEmpty(),
                        )
                    },
                ),
            )
        }
    }
}
