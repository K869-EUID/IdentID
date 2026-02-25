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

package com.k689.identid.di.presentation

import com.k689.identid.controller.core.WalletCoreDocumentsController
import com.k689.identid.controller.core.WalletCorePresentationController
import com.k689.identid.di.core.PRESENTATION_SCOPE_ID
import com.k689.identid.interactor.common.DeviceAuthenticationInteractor
import com.k689.identid.interactor.presentation.PresentationLoadingInteractor
import com.k689.identid.interactor.presentation.PresentationLoadingInteractorImpl
import com.k689.identid.interactor.presentation.PresentationRequestInteractor
import com.k689.identid.interactor.presentation.PresentationRequestInteractorImpl
import com.k689.identid.interactor.presentation.PresentationSuccessInteractor
import com.k689.identid.interactor.presentation.PresentationSuccessInteractorImpl
import com.k689.identid.provider.UuidProvider
import com.k689.identid.provider.resources.ResourceProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.ScopeId

@Module
@ComponentScan("com.k689.identid.ui.presentation", "com.k689.identid.di.presentation")
class FeaturePresentationModule

@Factory
fun providePresentationRequestInteractor(
    resourceProvider: ResourceProvider,
    uuidProvider: UuidProvider,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    @ScopeId(name = PRESENTATION_SCOPE_ID) walletCorePresentationController: WalletCorePresentationController,
): PresentationRequestInteractor =
    PresentationRequestInteractorImpl(
        resourceProvider,
        uuidProvider,
        walletCorePresentationController,
        walletCoreDocumentsController,
    )

@Factory
fun providePresentationLoadingInteractor(
    @ScopeId(name = PRESENTATION_SCOPE_ID) walletCorePresentationController: WalletCorePresentationController,
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
): PresentationLoadingInteractor =
    PresentationLoadingInteractorImpl(
        walletCorePresentationController,
        deviceAuthenticationInteractor,
    )

@Factory
fun providePresentationSuccessInteractor(
    @ScopeId(name = PRESENTATION_SCOPE_ID) walletCorePresentationController: WalletCorePresentationController,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    resourceProvider: ResourceProvider,
    uuidProvider: UuidProvider,
): PresentationSuccessInteractor =
    PresentationSuccessInteractorImpl(
        walletCorePresentationController,
        walletCoreDocumentsController,
        resourceProvider,
        uuidProvider,
    )
