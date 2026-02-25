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

package com.k689.identid.di.authentication

import com.k689.identid.config.StorageConfig
import com.k689.identid.config.StorageConfigImpl
import com.k689.identid.controller.authentication.BiometricAuthenticationController
import com.k689.identid.controller.authentication.BiometricAuthenticationControllerImpl
import com.k689.identid.controller.authentication.DeviceAuthenticationController
import com.k689.identid.controller.authentication.DeviceAuthenticationControllerImpl
import com.k689.identid.controller.crypto.CryptoController
import com.k689.identid.controller.storage.BiometryStorageController
import com.k689.identid.controller.storage.BiometryStorageControllerImpl
import com.k689.identid.controller.storage.PinStorageController
import com.k689.identid.controller.storage.PinStorageControllerImpl
import com.k689.identid.controller.storage.PrefsController
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.storage.prefs.PrefsBiometryStorageProvider
import com.k689.identid.storage.prefs.PrefsPinStorageProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("com.k689.identid.di.authentication")
class LogicAuthenticationModule

@Single
fun provideStorageConfig(
    prefsController: PrefsController,
    cryptoController: CryptoController,
): StorageConfig =
    StorageConfigImpl(
        pinImpl = PrefsPinStorageProvider(prefsController, cryptoController),
        biometryImpl = PrefsBiometryStorageProvider(prefsController),
    )

@Factory
fun provideBiometricAuthenticationController(
    cryptoController: CryptoController,
    biometryStorageController: BiometryStorageController,
    resourceProvider: ResourceProvider,
): BiometricAuthenticationController =
    BiometricAuthenticationControllerImpl(
        resourceProvider,
        cryptoController,
        biometryStorageController,
    )

@Factory
fun provideDeviceAuthenticationController(
    resourceProvider: ResourceProvider,
    biometricAuthenticationController: BiometricAuthenticationController,
): DeviceAuthenticationController =
    DeviceAuthenticationControllerImpl(
        resourceProvider,
        biometricAuthenticationController,
    )

@Factory
fun providePinStorageController(
    storageConfig: StorageConfig,
): PinStorageController = PinStorageControllerImpl(storageConfig)

@Factory
fun provideBiometryStorageController(
    storageConfig: StorageConfig,
): BiometryStorageController = BiometryStorageControllerImpl(storageConfig)
