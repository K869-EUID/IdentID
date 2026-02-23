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

package com.k689.identid.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.k689.identid.ui.component.preview.PreviewTheme
import com.k689.identid.ui.component.preview.ThemeModePreviews
import com.k689.identid.ui.component.utils.SPACING_SMALL
import com.k689.identid.ui.component.wrap.WrapImage

data class AppIconAndTextDataUi(
    val appIcon: IconDataUi = AppIcons.CardLogo,
    val appText: IconDataUi = AppIcons.CardLogoText,
)

@Composable
fun AppIconAndText(
    modifier: Modifier = Modifier,
    appIconAndTextData: AppIconAndTextDataUi,
) {
    Row(
        modifier = modifier,
        horizontalArrangement =
            Arrangement.spacedBy(
                space = SPACING_SMALL.dp,
                alignment = Alignment.CenterHorizontally,
            ),
        verticalAlignment = Alignment.Top,
    ) {
        WrapImage(
            modifier = Modifier.size(width = 44.dp, height = 32.dp),
            iconData = appIconAndTextData.appIcon,
        )
        WrapImage(
            modifier = Modifier.size(width = 73.dp, height = 38.dp),
            iconData = appIconAndTextData.appText,
        )
    }
}

@ThemeModePreviews
@Composable
private fun AppIconAndTextPreview() {
    PreviewTheme {
        AppIconAndText(
            appIconAndTextData =
                AppIconAndTextDataUi(
                    appIcon = AppIcons.CardLogo,
                    appText = AppIcons.CardLogoText,
                ),
        )
    }
}
