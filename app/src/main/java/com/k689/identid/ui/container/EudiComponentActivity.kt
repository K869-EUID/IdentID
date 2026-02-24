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

package com.k689.identid.ui.container

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.k689.identid.controller.storage.PrefKeys
import com.k689.identid.extension.ui.exposeTestTagsAsResourceId
import com.k689.identid.navigation.IssuanceScreens
import com.k689.identid.navigation.RouterHost
import com.k689.identid.navigation.helper.DeepLinkAction
import com.k689.identid.navigation.helper.DeepLinkType
import com.k689.identid.navigation.helper.handleDeepLinkAction
import com.k689.identid.navigation.helper.hasDeepLink
import com.k689.identid.theme.AppLanguage
import com.k689.identid.theme.AppTheme
import com.k689.identid.theme.ThemeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.annotation.KoinExperimentalAPI
import kotlin.system.exitProcess

open class EudiComponentActivity : FragmentActivity() {

    companion object {
        /**
         * Tracks the locale tag across activity recreations within the same process.
         * When the system per-app language setting changes the locale, the framework
         * recreates the activity but keeps the process alive. Koin singletons and
         * ViewModels survive with stale cached strings. By comparing the current locale
         * to this saved value, we detect the change and restart the process.
         */
        private var lastLocaleTag: String? = null
    }

    private val routerHost: RouterHost by inject()
    private val prefKeys: PrefKeys by inject()

    private var flowStarted: Boolean = false

    internal var pendingDeepLink: Uri? = null

    internal fun cacheDeepLink(intent: Intent?) {
        pendingDeepLink = intent?.data
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restartIfLocaleChanged()
    }

    override fun attachBaseContext(newBase: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            super.attachBaseContext(AppLanguage.wrapContext(newBase))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    /**
     * Detects whether the per-app locale was changed externally (e.g. system settings)
     * since the last time this activity was created. If so, restarts the process so
     * that all singletons, ViewModels, and cached resource strings pick up the new locale.
     *
     * On the very first launch (fresh process) [lastLocaleTag] is null, so the check
     * is skipped and the current tag is just recorded.
     */
    private fun restartIfLocaleChanged() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val localeManager = getSystemService(LocaleManager::class.java)
        val locales = localeManager.applicationLocales
        val currentTag = if (locales.isEmpty) "" else locales.get(0)?.toLanguageTag().orEmpty()

        if (lastLocaleTag != null && lastLocaleTag != currentTag) {
            lastLocaleTag = currentTag
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            exitProcess(0)
        }
        lastLocaleTag = currentTag
    }

    @OptIn(KoinExperimentalAPI::class)
    @Composable
    protected fun Content(
        intent: Intent?,
        builder: NavGraphBuilder.(NavController) -> Unit,
    ) {
        val themeMode by prefKeys.theme.collectAsState()
        val darkTheme =
            when (themeMode) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }

        SideEffect {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.isAppearanceLightStatusBars = !darkTheme
            windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
        }

        ThemeManager.instance.Theme(darkTheme = darkTheme) {
            Surface(
                modifier =
                    Modifier
                        .exposeTestTagsAsResourceId()
                        .fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
            ) {
                routerHost.StartFlow {
                    builder(it)
                }
                flowStarted = true
                handleDeepLink(intent, coldBoot = true)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (flowStarted) {
            handleDeepLink(intent)
        } else {
            runPendingDeepLink(intent)
        }
    }

    private fun runPendingDeepLink(intent: Intent?) {
        lifecycleScope.launch {
            var count = 0
            while (!flowStarted && count <= 10) {
                count++
                delay(500)
            }
            if (count <= 10) {
                handleDeepLink(intent)
            }
        }
    }

    private fun handleDeepLink(
        intent: Intent?,
        coldBoot: Boolean = false,
    ) {
        hasDeepLink(intent?.data)?.let {
            if (it.type == DeepLinkType.ISSUANCE && !coldBoot) {
                handleDeepLinkAction(
                    routerHost.getNavController(),
                    it.link,
                )
            } else if (
                it.type == DeepLinkType.CREDENTIAL_OFFER &&
                !routerHost.userIsLoggedInWithDocuments() &&
                routerHost.userIsLoggedInWithNoDocuments()
            ) {
                cacheDeepLink(intent)
                routerHost.popToIssuanceOnboardingScreen()
            } else if (it.type == DeepLinkType.OPENID4VP &&
                routerHost.userIsLoggedInWithDocuments() &&
                (
                    routerHost.isScreenOnBackStackOrForeground(IssuanceScreens.AddDocument) ||
                        routerHost.isScreenOnBackStackOrForeground(IssuanceScreens.DocumentOffer)
                )
            ) {
                handleDeepLinkAction(
                    routerHost.getNavController(),
                    DeepLinkAction(it.link, DeepLinkType.DYNAMIC_PRESENTATION),
                )
            } else if (it.type != DeepLinkType.ISSUANCE) {
                cacheDeepLink(intent)
                if (routerHost.userIsLoggedInWithDocuments()) {
                    routerHost.popToDashboardScreen()
                }
            }
            setIntent(Intent())
        }
    }
}
