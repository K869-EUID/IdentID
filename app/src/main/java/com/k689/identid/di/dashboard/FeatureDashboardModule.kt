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

package com.k689.identid.di.dashboard

import com.k689.identid.config.ConfigLogic
import com.k689.identid.config.WalletCoreConfig
import com.k689.identid.controller.core.WalletCoreDocumentsController
import com.k689.identid.controller.log.LogController
import com.k689.identid.interactor.dashboard.DashboardInteractor
import com.k689.identid.interactor.dashboard.DashboardInteractorImpl
import com.k689.identid.interactor.dashboard.DocumentDetailsInteractor
import com.k689.identid.interactor.dashboard.DocumentDetailsInteractorImpl
import com.k689.identid.interactor.dashboard.DocumentSignInteractor
import com.k689.identid.interactor.dashboard.DocumentSignInteractorImpl
import com.k689.identid.interactor.dashboard.DocumentsInteractor
import com.k689.identid.interactor.dashboard.DocumentsInteractorImpl
import com.k689.identid.interactor.dashboard.HomeInteractor
import com.k689.identid.interactor.dashboard.HomeInteractorImpl
import com.k689.identid.interactor.dashboard.SettingsInteractor
import com.k689.identid.interactor.dashboard.SettingsInteractorImpl
import com.k689.identid.interactor.dashboard.TransactionDetailsInteractor
import com.k689.identid.interactor.dashboard.TransactionDetailsInteractorImpl
import com.k689.identid.interactor.dashboard.TransactionsInteractor
import com.k689.identid.interactor.dashboard.TransactionsInteractorImpl
import com.k689.identid.provider.UuidProvider
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.validator.FilterValidator
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
@ComponentScan("com.k689.identid.ui.dashboard", "com.k689.identid.di.dashboard")
class FeatureDashboardModule

@Factory
fun provideDashboardInteractor(
    resourceProvider: ResourceProvider,
): DashboardInteractor =
    DashboardInteractorImpl(
        resourceProvider,
    )

@Factory
fun provideSettingsInteractor(
    configLogic: ConfigLogic,
    logController: LogController,
    resourceProvider: ResourceProvider,
): SettingsInteractor =
    SettingsInteractorImpl(
        configLogic,
        logController,
        resourceProvider,
    )

@Factory
fun provideHomeInteractor(
    resourceProvider: ResourceProvider,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    walletCoreConfig: WalletCoreConfig,
): HomeInteractor =
    HomeInteractorImpl(
        resourceProvider,
        walletCoreDocumentsController,
        walletCoreConfig,
    )

@Factory
fun provideDocumentsInteractor(
    resourceProvider: ResourceProvider,
    documentsController: WalletCoreDocumentsController,
    filterValidator: FilterValidator,
): DocumentsInteractor =
    DocumentsInteractorImpl(
        resourceProvider,
        documentsController,
        filterValidator,
    )

@Factory
fun provideTransactionInteractor(
    resourceProvider: ResourceProvider,
    filterValidator: FilterValidator,
    walletCoreDocumentsController: WalletCoreDocumentsController,
): TransactionsInteractor =
    TransactionsInteractorImpl(
        resourceProvider,
        filterValidator,
        walletCoreDocumentsController,
    )

@Factory
fun provideDocumentSignInteractor(
    resourceProvider: ResourceProvider,
): DocumentSignInteractor =
    DocumentSignInteractorImpl(
        resourceProvider,
    )

@Factory
fun provideDocumentDetailsInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    resourceProvider: ResourceProvider,
    uuidProvider: UuidProvider,
): DocumentDetailsInteractor =
    DocumentDetailsInteractorImpl(
        walletCoreDocumentsController,
        resourceProvider,
        uuidProvider,
    )

@Factory
fun provideTransactionDetailsInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    resourceProvider: ResourceProvider,
    uuidProvider: UuidProvider,
): TransactionDetailsInteractor =
    TransactionDetailsInteractorImpl(
        walletCoreDocumentsController,
        resourceProvider,
        uuidProvider,
    )
