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

import android.bluetooth.BluetoothManager
import android.content.Context
import com.k689.identid.config.WalletCoreConfig
import com.k689.identid.controller.core.WalletCoreDocumentsController
import com.k689.identid.extension.business.safeAsync
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.util.common.DocumentJsonKeys
import com.k689.identid.util.common.extractValueFromDocumentOrEmpty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class HomeInteractorGetUserNameViaMainPidDocumentPartialState {
    data class Success(
        val userFirstName: String,
    ) : HomeInteractorGetUserNameViaMainPidDocumentPartialState()

    data class Failure(
        val error: String,
    ) : HomeInteractorGetUserNameViaMainPidDocumentPartialState()
}

interface HomeInteractor {
    fun isBleAvailable(): Boolean

    fun isBleCentralClientModeEnabled(): Boolean

    fun getUserNameViaMainPidDocument(): Flow<HomeInteractorGetUserNameViaMainPidDocumentPartialState>
}

class HomeInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val walletCoreConfig: WalletCoreConfig,
) : HomeInteractor {
    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun isBleAvailable(): Boolean {
        val bluetoothManager: BluetoothManager? =
            resourceProvider
                .provideContext()
                .getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager?.adapter?.isEnabled == true
    }

    override fun isBleCentralClientModeEnabled(): Boolean = walletCoreConfig.config.enableBleCentralMode

    override fun getUserNameViaMainPidDocument(): Flow<HomeInteractorGetUserNameViaMainPidDocumentPartialState> =
        flow {
            val mainPid = walletCoreDocumentsController.getMainPidDocument()
            val userFirstName =
                mainPid
                    ?.let {
                        return@let extractValueFromDocumentOrEmpty(
                            document = it,
                            key = DocumentJsonKeys.FIRST_NAME,
                        )
                    }.orEmpty()

            emit(
                HomeInteractorGetUserNameViaMainPidDocumentPartialState.Success(
                    userFirstName = userFirstName,
                ),
            )
        }.safeAsync {
            HomeInteractorGetUserNameViaMainPidDocumentPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg,
            )
        }
}
