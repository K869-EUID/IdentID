/*
 * Copyright (c) 2026 European Commission
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

package com.k689.identid.theme

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.LocaleList
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Represents the supported languages for the application.
 *
 * On API 33+ the framework [LocaleManager] is used, which also integrates with
 * the system per-app language settings (via `locales_config.xml`).
 *
 * On API 29-32 the selected locale is persisted in [SharedPreferences] and applied
 * manually via [wrapContext] in `attachBaseContext`.
 *
 * @property tag The BCP-47 language tag (e.g. "en", "lt"), or empty for system default.
 * @property displayName The user-facing name of the language.
 */
enum class AppLanguage(
    val tag: String,
    val displayName: String,
) {
    SYSTEM(tag = "", displayName = "System"),
    ENGLISH(tag = "en", displayName = "English"),
    LITHUANIAN(tag = "lt", displayName = "Lietuvių"),
    ;

    companion object {
        private const val PREFS_NAME = "eudi-wallet-locale"
        private const val KEY_LANGUAGE_TAG = "selected_language_tag"

        /**
         * Returns the [AppLanguage] matching the currently applied application locale,
         * or [SYSTEM] if none matches.
         */
        fun fromCurrentLocale(context: Context): AppLanguage {
            val tag =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val localeManager = context.getSystemService(LocaleManager::class.java)
                    val locales = localeManager.applicationLocales
                    if (locales.isEmpty) "" else locales.get(0)?.toLanguageTag().orEmpty()
                } else {
                    getStoredTag(context)
                }
            if (tag.isEmpty()) return SYSTEM
            return entries.firstOrNull { it.tag.isNotEmpty() && tag.startsWith(it.tag) }
                ?: SYSTEM
        }

        /**
         * Applies the given [language] as the application locale and restarts the app
         * so that all singletons, ViewModels, and cached resource strings are recreated
         * with the new locale.
         */
        fun applyAndRestart(
            context: Context,
            language: AppLanguage,
        ) {
            // Persist the locale.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                localeManager.applicationLocales =
                    if (language == SYSTEM || language.tag.isEmpty()) {
                        LocaleList.getEmptyLocaleList()
                    } else {
                        LocaleList.forLanguageTags(language.tag)
                    }
            } else {
                storeTag(context, language.tag)
            }

            // Restart the process so every component picks up the new locale.
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            exitProcess(0)
        }

        /**
         * Wraps the given [base] context with the stored locale.
         * Intended for use in `attachBaseContext` on API < 33.
         */
        fun wrapContext(base: Context): Context {
            val tag = getStoredTag(base)
            if (tag.isEmpty()) return base
            val locale = Locale.forLanguageTag(tag)
            Locale.setDefault(locale)
            val config = base.resources.configuration
            config.setLocale(locale)
            config.setLocales(LocaleList(locale))
            return base.createConfigurationContext(config)
        }

        private fun getStoredTag(context: Context): String = prefs(context).getString(KEY_LANGUAGE_TAG, "").orEmpty()

        private fun storeTag(
            context: Context,
            tag: String,
        ) {
            prefs(context).edit().putString(KEY_LANGUAGE_TAG, tag).apply()
        }

        private fun prefs(context: Context): SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
