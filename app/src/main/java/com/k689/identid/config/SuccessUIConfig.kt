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

import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.k689.identid.config.ConfigNavigation
import com.k689.identid.config.NavigationType
import com.k689.identid.theme.values.ThemeColors
import com.k689.identid.ui.component.AppIconAndTextDataUi
import com.k689.identid.ui.component.IconDataUi
import com.k689.identid.ui.component.content.ContentHeaderConfig
import com.k689.identid.ui.component.utils.PERCENTAGE_60
import com.k689.identid.ui.serializer.UiSerializable
import com.k689.identid.ui.serializer.UiSerializableParser
import com.k689.identid.ui.serializer.adapter.SerializableTypeAdapter

data class SuccessUIConfig(
    val textElementsConfig: TextElementsConfig,
    val headerConfig: ContentHeaderConfig =
        ContentHeaderConfig(
            appIconAndTextData = AppIconAndTextDataUi(),
            description = null,
        ),
    val imageConfig: ImageConfig,
    val buttonConfig: List<ButtonConfig>,
    val onBackScreenToNavigate: ConfigNavigation,
) : UiSerializable {
    data class ImageConfig(
        val type: Type = Type.Default,
        val tint: Color? = ThemeColors.success,
        val screenPercentageSize: Float = PERCENTAGE_60,
    ) {
        sealed class Type {
            data object Default : Type()

            data class Drawable(
                val icon: IconDataUi,
            ) : Type()
        }
    }

    data class ButtonConfig(
        val text: String,
        val style: Style,
        val navigation: ConfigNavigation,
    ) {
        enum class Style {
            PRIMARY,
            OUTLINE,
        }
    }

    data class TextElementsConfig(
        val text: String,
        val description: String,
        val color: Color = ThemeColors.success,
    )

    companion object Parser : UiSerializableParser {
        override val serializedKeyName = "successConfig"

        override fun provideParser(): Gson =
            GsonBuilder()
                .registerTypeAdapter(
                    NavigationType::class.java,
                    SerializableTypeAdapter<NavigationType>(),
                ).registerTypeAdapter(
                    ImageConfig.Type::class.java,
                    SerializableTypeAdapter<ImageConfig.Type>(),
                ).create()
    }
}
