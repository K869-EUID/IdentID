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

package com.k689.identid.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.k689.identid.config.ConfigNavigation
import com.k689.identid.config.NavigationType
import com.k689.identid.ui.serializer.UiSerializable
import com.k689.identid.ui.serializer.UiSerializableParser
import com.k689.identid.ui.serializer.adapter.SerializableTypeAdapter

sealed interface BiometricMode {
    data class Default(
        val descriptionWhenBiometricsEnabled: String,
        val descriptionWhenBiometricsNotEnabled: String,
        val textAbovePin: String,
    ) : BiometricMode

    data class Login(
        val title: String,
        val subTitleWhenBiometricsEnabled: String,
        val subTitleWhenBiometricsNotEnabled: String,
    ) : BiometricMode
}

data class BiometricUiConfig(
    val mode: BiometricMode,
    val isPreAuthorization: Boolean = false,
    val shouldInitializeBiometricAuthOnCreate: Boolean = true,
    val onSuccessNavigation: ConfigNavigation,
    val onBackNavigationConfig: OnBackNavigationConfig,
) : UiSerializable {
    companion object Parser : UiSerializableParser {
        override val serializedKeyName = "biometricConfig"

        override fun provideParser(): Gson =
            GsonBuilder()
                .registerTypeAdapter(
                    NavigationType::class.java,
                    SerializableTypeAdapter<NavigationType>(),
                ).registerTypeAdapter(
                    BiometricMode::class.java,
                    SerializableTypeAdapter<BiometricMode>(),
                ).create()
    }
}

data class OnBackNavigationConfig(
    val onBackNavigation: ConfigNavigation?,
    private val hasToolbarBackIcon: Boolean,
) {
    val isBackable: Boolean get() = hasToolbarBackIcon && onBackNavigation != null
}
