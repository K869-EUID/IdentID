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

data class OfferCodeUiConfig(
    val offerUri: String,
    val txCodeLength: Int,
    val issuerName: String,
    val onSuccessNavigation: ConfigNavigation,
) : UiSerializable {
    companion object Parser : UiSerializableParser {
        override val serializedKeyName = "offerCodeConfig"

        override fun provideParser(): Gson =
            GsonBuilder()
                .registerTypeAdapter(
                    NavigationType::class.java,
                    SerializableTypeAdapter<NavigationType>(),
                ).create()
    }
}
