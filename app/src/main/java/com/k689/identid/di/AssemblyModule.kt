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

package com.k689.identid.di

import android.app.Application
import com.k689.identid.di.authentication.LogicAuthenticationModule
import com.k689.identid.di.business.LogicBusinessModule
import com.k689.identid.di.common.FeatureCommonModule
import com.k689.identid.di.core.LogicCoreModule
import com.k689.identid.di.dashboard.FeatureDashboardModule
import com.k689.identid.di.issuance.FeatureIssuanceModule
import com.k689.identid.di.network.LogicNetworkModule
import com.k689.identid.di.presentation.FeaturePresentationModule
import com.k689.identid.di.proximity.FeatureProximityModule
import com.k689.identid.di.resources.LogicResourceModule
import com.k689.identid.di.startup.FeatureStartupModule
import com.k689.identid.di.storage.LogicStorageModule
import com.k689.identid.di.ui.LogicUiModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.KoinApplication
import org.koin.core.context.GlobalContext.startKoin
import org.koin.ksp.generated.module

private val assembledModules =
    listOf(
        // Logic Modules
        LogicNetworkModule().module,
        LogicUiModule().module,
        LogicResourceModule().module,
        LogicBusinessModule().module,
        LogicAuthenticationModule().module,
        LogicCoreModule().module,
        LogicStorageModule().module,
        // Feature Modules
        FeatureCommonModule().module,
        FeatureDashboardModule().module,
        FeatureStartupModule().module,
        FeaturePresentationModule().module,
        FeatureProximityModule().module,
        FeatureIssuanceModule().module,
    )

internal fun Application.setupKoin(): KoinApplication =
    startKoin {
        androidContext(this@setupKoin)
        androidLogger()
        modules(assembledModules)
    }
