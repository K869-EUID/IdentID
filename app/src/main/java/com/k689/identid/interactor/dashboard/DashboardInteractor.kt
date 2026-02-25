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

package com.k689.identid.interactor.dashboard

import com.k689.identid.R
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.ListItemDataUi
import com.k689.identid.ui.component.ListItemLeadingContentDataUi
import com.k689.identid.ui.component.ListItemMainContentDataUi
import com.k689.identid.ui.component.ListItemTrailingContentDataUi
import com.k689.identid.ui.dashboard.dashboard.model.SideMenuItemUi
import com.k689.identid.ui.dashboard.dashboard.model.SideMenuTypeUi

interface DashboardInteractor {
    fun getSideMenuOptions(): List<SideMenuItemUi>
}

class DashboardInteractorImpl(
    private val resourceProvider: ResourceProvider,
) : DashboardInteractor {
    override fun getSideMenuOptions(): List<SideMenuItemUi> =
        buildList {
            add(
                SideMenuItemUi(
                    type = SideMenuTypeUi.CHANGE_PIN,
                    data =
                        ListItemDataUi(
                            itemId = resourceProvider.getString(R.string.dashboard_side_menu_option_change_pin_id),
                            mainContentData =
                                ListItemMainContentDataUi.Text(
                                    text = resourceProvider.getString(R.string.dashboard_side_menu_option_change_pin),
                                ),
                            leadingContentData =
                                ListItemLeadingContentDataUi.Icon(
                                    iconData = AppIcons.ChangePin,
                                ),
                            trailingContentData =
                                ListItemTrailingContentDataUi.Icon(
                                    iconData = AppIcons.KeyboardArrowRight,
                                ),
                        ),
                ),
            )

            add(
                SideMenuItemUi(
                    type = SideMenuTypeUi.SETTINGS,
                    data =
                        ListItemDataUi(
                            itemId = resourceProvider.getString(R.string.dashboard_side_menu_option_settings_id),
                            mainContentData =
                                ListItemMainContentDataUi.Text(
                                    text = resourceProvider.getString(R.string.dashboard_side_menu_option_settings),
                                ),
                            leadingContentData =
                                ListItemLeadingContentDataUi.Icon(
                                    iconData = AppIcons.Settings,
                                ),
                            trailingContentData =
                                ListItemTrailingContentDataUi.Icon(
                                    iconData = AppIcons.KeyboardArrowRight,
                                ),
                        ),
                ),
            )
        }
}
