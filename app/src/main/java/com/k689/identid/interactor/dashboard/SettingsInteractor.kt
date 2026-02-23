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

import android.net.Uri
import com.k689.identid.R
import com.k689.identid.config.ConfigLogic
import com.k689.identid.controller.log.LogController
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.ListItemDataUi
import com.k689.identid.ui.component.ListItemLeadingContentDataUi
import com.k689.identid.ui.component.ListItemMainContentDataUi
import com.k689.identid.ui.component.ListItemTrailingContentDataUi
import com.k689.identid.ui.dashboard.settings.model.SettingsItemUi
import com.k689.identid.ui.dashboard.settings.model.SettingsMenuItemType

interface SettingsInteractor {
    fun getAppVersion(): String

    fun getChangelogUrl(): String?

    fun retrieveLogFileUris(): ArrayList<Uri>

    fun getSettingsItemsUi(changelogUrl: String?): List<SettingsItemUi>
}

class SettingsInteractorImpl(
    private val configLogic: ConfigLogic,
    private val logController: LogController,
    private val resourceProvider: ResourceProvider,
) : SettingsInteractor {
    override fun getAppVersion(): String = configLogic.appVersion

    override fun getChangelogUrl(): String? = configLogic.changelogUrl

    override fun retrieveLogFileUris(): ArrayList<Uri> = ArrayList(logController.retrieveLogFileUris())

    override fun getSettingsItemsUi(changelogUrl: String?): List<SettingsItemUi> =
        buildList {
            add(
                SettingsItemUi(
                    type = SettingsMenuItemType.PREFERENCES,
                    data =
                        ListItemDataUi(
                            itemId = resourceProvider.getString(R.string.settings_screen_option_preferences_id),
                            mainContentData =
                                ListItemMainContentDataUi.Text(
                                    text = resourceProvider.getString(R.string.settings_screen_option_preferences),
                                ),
                            leadingContentData =
                                ListItemLeadingContentDataUi.Icon(
                                    // TODO: add more icons later and change this one because
                                    //  there aren't enough different options to choose from
                                    iconData = AppIcons.Search,
                                ),
                            trailingContentData =
                                ListItemTrailingContentDataUi.Icon(
                                    iconData = AppIcons.KeyboardArrowRight,
                                ),
                        ),
                ),
            )
            add(
                SettingsItemUi(
                    type = SettingsMenuItemType.RETRIEVE_LOGS,
                    data =
                        ListItemDataUi(
                            itemId = resourceProvider.getString(R.string.settings_screen_option_retrieve_logs_id),
                            mainContentData =
                                ListItemMainContentDataUi.Text(
                                    text = resourceProvider.getString(R.string.settings_screen_option_retrieve_logs),
                                ),
                            leadingContentData =
                                ListItemLeadingContentDataUi.Icon(
                                    iconData = AppIcons.OpenNew,
                                ),
                            trailingContentData =
                                ListItemTrailingContentDataUi.Icon(
                                    iconData = AppIcons.KeyboardArrowRight,
                                ),
                        ),
                ),
            )

            if (changelogUrl != null) {
                add(
                    SettingsItemUi(
                        type = SettingsMenuItemType.CHANGELOG,
                        data =
                            ListItemDataUi(
                                itemId = resourceProvider.getString(R.string.settings_screen_option_changelog_id),
                                mainContentData =
                                    ListItemMainContentDataUi.Text(
                                        text = resourceProvider.getString(R.string.settings_screen_option_changelog),
                                    ),
                                leadingContentData =
                                    ListItemLeadingContentDataUi.Icon(
                                        iconData = AppIcons.OpenInBrowser,
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
}
