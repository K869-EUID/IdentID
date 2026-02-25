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

package com.k689.identid.interactor.presentation

import android.content.Context
import com.k689.identid.controller.authentication.BiometricsAvailability
import com.k689.identid.controller.authentication.DeviceAuthenticationResult
import com.k689.identid.controller.core.SendRequestedDocumentsPartialState
import com.k689.identid.controller.core.WalletCorePartialState
import com.k689.identid.controller.core.WalletCorePresentationController
import com.k689.identid.interactor.common.DeviceAuthenticationInteractor
import com.k689.identid.model.authentication.BiometricCrypto
import com.k689.identid.model.core.AuthenticationData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import java.net.URI

sealed class PresentationLoadingObserveResponsePartialState {
    data class UserAuthenticationRequired(
        val authenticationData: List<AuthenticationData>,
    ) : PresentationLoadingObserveResponsePartialState()

    data class Failure(
        val error: String,
    ) : PresentationLoadingObserveResponsePartialState()

    data object Success : PresentationLoadingObserveResponsePartialState()

    data class Redirect(
        val uri: URI,
    ) : PresentationLoadingObserveResponsePartialState()

    data object RequestReadyToBeSent : PresentationLoadingObserveResponsePartialState()
}

sealed class PresentationLoadingSendRequestedDocumentPartialState {
    data class Failure(
        val error: String,
    ) : PresentationLoadingSendRequestedDocumentPartialState()

    data object Success : PresentationLoadingSendRequestedDocumentPartialState()
}

interface PresentationLoadingInteractor {
    fun observeResponse(): Flow<PresentationLoadingObserveResponsePartialState>

    fun sendRequestedDocuments(): PresentationLoadingSendRequestedDocumentPartialState

    fun handleUserAuthentication(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult,
    )
}

class PresentationLoadingInteractorImpl(
    private val walletCorePresentationController: WalletCorePresentationController,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
) : PresentationLoadingInteractor {
    override fun observeResponse(): Flow<PresentationLoadingObserveResponsePartialState> =
        walletCorePresentationController.observeSentDocumentsRequest().mapNotNull { response ->
            when (response) {
                is WalletCorePartialState.Failure -> {
                    PresentationLoadingObserveResponsePartialState.Failure(
                        error = response.error,
                    )
                }

                is WalletCorePartialState.Redirect -> {
                    PresentationLoadingObserveResponsePartialState.Redirect(
                        uri = response.uri,
                    )
                }

                is WalletCorePartialState.Success -> {
                    PresentationLoadingObserveResponsePartialState.Success
                }

                is WalletCorePartialState.UserAuthenticationRequired -> {
                    PresentationLoadingObserveResponsePartialState.UserAuthenticationRequired(
                        response.authenticationData,
                    )
                }

                is WalletCorePartialState.RequestIsReadyToBeSent -> {
                    PresentationLoadingObserveResponsePartialState.RequestReadyToBeSent
                }
            }
        }

    override fun sendRequestedDocuments(): PresentationLoadingSendRequestedDocumentPartialState =
        when (val result = walletCorePresentationController.sendRequestedDocuments()) {
            is SendRequestedDocumentsPartialState.RequestSent -> {
                PresentationLoadingSendRequestedDocumentPartialState.Success
            }

            is SendRequestedDocumentsPartialState.Failure -> {
                PresentationLoadingSendRequestedDocumentPartialState.Failure(
                    result.error,
                )
            }
        }

    override fun handleUserAuthentication(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult,
    ) {
        deviceAuthenticationInteractor.getBiometricsAvailability {
            when (it) {
                is BiometricsAvailability.CanAuthenticate -> {
                    deviceAuthenticationInteractor.authenticateWithBiometrics(
                        context = context,
                        crypto = crypto,
                        notifyOnAuthenticationFailure = notifyOnAuthenticationFailure,
                        resultHandler = resultHandler,
                    )
                }

                is BiometricsAvailability.NonEnrolled -> {
                    deviceAuthenticationInteractor.launchBiometricSystemScreen()
                }

                is BiometricsAvailability.Failure -> {
                    resultHandler.onAuthenticationFailure()
                }
            }
        }
    }
}
