/*
 * Copyright (c) 2023 European Commission
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

package com.k689.identid.di.business

import android.content.Context
import com.k689.identid.config.ConfigLogic
import com.k689.identid.config.ConfigLogicImpl
import com.k689.identid.controller.crypto.CryptoController
import com.k689.identid.controller.crypto.CryptoControllerImpl
import com.k689.identid.controller.crypto.KeystoreController
import com.k689.identid.controller.crypto.KeystoreControllerImpl
import com.k689.identid.controller.log.LogController
import com.k689.identid.controller.log.LogControllerImpl
import com.k689.identid.controller.storage.PrefKeys
import com.k689.identid.controller.storage.PrefKeysImpl
import com.k689.identid.controller.storage.PrefsController
import com.k689.identid.controller.storage.PrefsControllerImpl
import com.k689.identid.provider.UuidProvider
import com.k689.identid.provider.UuidProviderImpl
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.validator.FilterValidator
import com.k689.identid.validator.FilterValidatorImpl
import com.k689.identid.validator.FormValidator
import com.k689.identid.validator.FormValidatorImpl
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("com.k689.identid.di.business")
class LogicBusinessModule

@Single
fun provideConfigLogic(context: Context): ConfigLogic = ConfigLogicImpl(context)

@Single
fun provideLogController(
    context: Context,
    configLogic: ConfigLogic,
): LogController = LogControllerImpl(context, configLogic)

@Single
fun providePrefsController(resourceProvider: ResourceProvider): PrefsController = PrefsControllerImpl(resourceProvider)

@Single
fun providePrefKeys(prefsController: PrefsController): PrefKeys = PrefKeysImpl(prefsController)

@Single
fun provideKeystoreController(
    prefKeys: PrefKeys,
    logController: LogController,
    uuidProvider: UuidProvider,
): KeystoreController = KeystoreControllerImpl(prefKeys, logController, uuidProvider)

@Factory
fun provideCryptoController(keystoreController: KeystoreController): CryptoController = CryptoControllerImpl(keystoreController)

@Factory
fun provideFormValidator(logController: LogController): FormValidator = FormValidatorImpl(logController)

@Factory
fun provideFiltersValidator(): FilterValidator = FilterValidatorImpl()

@Single
fun provideUuidProvider(): UuidProvider = UuidProviderImpl()
