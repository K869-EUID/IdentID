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

package com.k689.identid.navigation.routes.issuance

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.k689.identid.BuildConfig
import com.k689.identid.config.IssuanceSuccessUiConfig
import com.k689.identid.config.IssuanceUiConfig
import com.k689.identid.config.OfferCodeUiConfig
import com.k689.identid.config.OfferUiConfig
import com.k689.identid.navigation.IssuanceScreens
import com.k689.identid.navigation.ModuleRoute
import com.k689.identid.ui.issuance.add.AddDocumentScreen
import com.k689.identid.ui.issuance.code.DocumentOfferCodeScreen
import com.k689.identid.ui.issuance.offer.DocumentOfferScreen
import com.k689.identid.ui.issuance.success.DocumentIssuanceSuccessScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

fun NavGraphBuilder.featureIssuanceGraph(navController: NavController) {
    navigation(
        startDestination = IssuanceScreens.AddDocument.screenRoute,
        route = ModuleRoute.IssuanceModule.route,
    ) {
        // Add Document
        composable(
            route = IssuanceScreens.AddDocument.screenRoute,
            deepLinks =
                listOf(
                    navDeepLink {
                        uriPattern =
                            BuildConfig.DEEPLINK + IssuanceScreens.AddDocument.screenRoute
                    },
                ),
            arguments =
                listOf(
                    navArgument(IssuanceUiConfig.serializedKeyName) {
                        type = NavType.StringType
                    },
                ),
        ) {
            AddDocumentScreen(
                navController,
                koinViewModel(
                    parameters = {
                        parametersOf(
                            it.arguments?.getString(IssuanceUiConfig.serializedKeyName).orEmpty(),
                        )
                    },
                ),
            )
        }

        // Document Offer
        composable(
            route = IssuanceScreens.DocumentOffer.screenRoute,
            deepLinks =
                listOf(
                    navDeepLink {
                        uriPattern =
                            BuildConfig.DEEPLINK + IssuanceScreens.DocumentOffer.screenRoute
                    },
                ),
            arguments =
                listOf(
                    navArgument(OfferUiConfig.serializedKeyName) {
                        type = NavType.StringType
                    },
                ),
        ) {
            DocumentOfferScreen(
                navController,
                koinViewModel(
                    parameters = {
                        parametersOf(
                            it.arguments?.getString(OfferUiConfig.serializedKeyName).orEmpty(),
                        )
                    },
                ),
            )
        }

        // Document Offer Code
        composable(
            route = IssuanceScreens.DocumentOfferCode.screenRoute,
            deepLinks =
                listOf(
                    navDeepLink {
                        uriPattern =
                            BuildConfig.DEEPLINK + IssuanceScreens.DocumentOfferCode.screenRoute
                    },
                ),
            arguments =
                listOf(
                    navArgument(OfferCodeUiConfig.serializedKeyName) {
                        type = NavType.StringType
                    },
                ),
        ) {
            DocumentOfferCodeScreen(
                navController = navController,
                viewModel =
                    koinViewModel(
                        parameters = {
                            parametersOf(
                                it.arguments?.getString(OfferCodeUiConfig.serializedKeyName).orEmpty(),
                            )
                        },
                    ),
            )
        }

        // Document Issuance Success
        composable(
            route = IssuanceScreens.DocumentIssuanceSuccess.screenRoute,
            deepLinks =
                listOf(
                    navDeepLink {
                        uriPattern =
                            BuildConfig.DEEPLINK + IssuanceScreens.DocumentIssuanceSuccess.screenRoute
                    },
                ),
            arguments =
                listOf(
                    navArgument(IssuanceSuccessUiConfig.serializedKeyName) {
                        type = NavType.StringType
                    },
                ),
        ) {
            DocumentIssuanceSuccessScreen(
                navController,
                koinViewModel(
                    parameters = {
                        parametersOf(
                            it.arguments
                                ?.getString(IssuanceSuccessUiConfig.serializedKeyName)
                                .orEmpty(),
                        )
                    },
                ),
            )
        }
    }
}
