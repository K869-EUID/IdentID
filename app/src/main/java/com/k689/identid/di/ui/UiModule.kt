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

package com.k689.identid.di.ui

import com.k689.identid.config.ConfigUILogic
import com.k689.identid.config.ConfigUILogicImpl
import com.k689.identid.navigation.RouterHost
import com.k689.identid.navigation.RouterHostImpl
import com.k689.identid.ui.serializer.UiSerializer
import com.k689.identid.ui.serializer.UiSerializerImpl
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("com.k689.identid.di.ui")
class LogicUiModule

@Single
fun provideRouterHost(
    configUILogic: ConfigUILogic,
): RouterHost = RouterHostImpl(configUILogic)

@Factory
fun provideUiSerializer(): UiSerializer = UiSerializerImpl()

@Single
fun provideConfigUILogic(): ConfigUILogic = ConfigUILogicImpl()
