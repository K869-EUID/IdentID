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

package com.k689.identid.di.issuance

import com.k689.identid.controller.core.WalletCoreDocumentsController
import com.k689.identid.interactor.common.DeviceAuthenticationInteractor
import com.k689.identid.interactor.issuance.AddDocumentInteractor
import com.k689.identid.interactor.issuance.AddDocumentInteractorImpl
import com.k689.identid.interactor.issuance.DocumentIssuanceSuccessInteractor
import com.k689.identid.interactor.issuance.DocumentIssuanceSuccessInteractorImpl
import com.k689.identid.interactor.issuance.DocumentOfferInteractor
import com.k689.identid.interactor.issuance.DocumentOfferInteractorImpl
import com.k689.identid.provider.UuidProvider
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.ui.serializer.UiSerializer
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module(includes = [IssuanceInteractorScanModule::class])
@ComponentScan("com.k689.identid.ui.issuance", "com.k689.identid.di.issuance")
class FeatureIssuanceModule

@Module
@ComponentScan("com.k689.identid.interactor.issuance")
class IssuanceInteractorScanModule

@Factory
fun provideAddDocumentInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    resourceProvider: ResourceProvider,
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    uiSerializer: UiSerializer,
): AddDocumentInteractor =
    AddDocumentInteractorImpl(
        walletCoreDocumentsController,
        deviceAuthenticationInteractor,
        resourceProvider,
        uiSerializer,
    )

@Factory
fun provideDocumentIssuanceSuccessInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    resourceProvider: ResourceProvider,
    uuIdProvider: UuidProvider,
): DocumentIssuanceSuccessInteractor =
    DocumentIssuanceSuccessInteractorImpl(
        walletCoreDocumentsController,
        resourceProvider,
        uuIdProvider,
    )

@Factory
fun provideDocumentOfferInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    resourceProvider: ResourceProvider,
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    uiSerializer: UiSerializer,
): DocumentOfferInteractor =
    DocumentOfferInteractorImpl(
        walletCoreDocumentsController,
        deviceAuthenticationInteractor,
        resourceProvider,
        uiSerializer,
    )
