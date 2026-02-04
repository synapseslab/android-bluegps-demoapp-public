package com.synapseslab.bluegpssdkdemo.screens.login

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapseslab.bluegps_sdk.authentication.core.BlueGPSAuthManager
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegps_sdk.utils.Resource
import com.synapseslab.bluegpssdkdemo.advertising.AdvManager
import com.synapseslab.bluegpssdkdemo.constants.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "LoginViewModel"

/**
 * ViewModel for the [LoginScreen].
 *
 * This ViewModel handles the logic for user authentication, including checking the current
 * login status, performing a guest login, and logging out. It interacts with the
 * `BlueGPSAuthManager` and `BlueGPSLib` from the BlueGPS SDK to manage the user's session
 * and exposes the login state to the UI.
 */
@Stable
class LoginViewModel : ViewModel() {

    private val _isLoggedIn = MutableStateFlow<Boolean?>(false)
    /**
     * A [StateFlow] that represents the user's current login status.
     * - `true`: The user is logged in.
     * - `false`: The user is logged out.
     * - `null`: The login status is yet to be determined.
     */
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn

    init {
        checkLoginStatus()
    }

    /**
     * Checks the user's current login status by observing the session validity from the BlueGPS SDK.
     *
     * If the session is valid, it retrieves the user's configuration. If the session is invalid,
     * it updates the UI state to reflect that the user is logged out.
     */
    private fun checkLoginStatus() {
        viewModelScope.launch {
            // Observe the session status from the BlueGPS SDK.
            BlueGPSLib.instance.isAValidSession().collectLatest { isValid ->
                if (!isValid) {
                    // If the session is invalid, update the state to reflect that the user is logged out.
                    _isLoggedIn.value = false
                } else {
                    // If the session is valid, get the user's email from the access token claims.
                    BlueGPSLib.instance.accessToken()?.let {
                        getOrCreateConfiguration()
                        _isLoggedIn.value = true
                    }
                }
            }
        }
    }

    /**
     * Attempts to log in as a guest user.
     *
     * This function calls the `guestLogin` method from the `BlueGPSAuthManager`.
     * On success, it updates the SDK's token and sets the login state to `true`.
     * On failure, it logs the error and sets the login state to `false`.
     */
    fun guestLogin() {
        viewModelScope.launch {
            when (val result = BlueGPSAuthManager.instance.guestLogin()) {
                is Resource.Error -> {
                    Log.e(TAG, result.message)
                    _isLoggedIn.value = false
                }

                is Resource.Exception -> {
                    Log.e(TAG, result.e.localizedMessage ?: "Exception")
                    _isLoggedIn.value = false
                }

                is Resource.Success -> {
                    Log.v(TAG, "Login in guest mode, ${result.data}")

                    // update access token on the environment
                    Environment.sdkEnvironment.sdkToken = result.data.access_token

                    _isLoggedIn.value = true

                }
            }
        }
    }

    /**
     * Logs the current user out.
     *
     * This function calls the `logout` method from the `BlueGPSAuthManager`.
     * On successful logout, it updates the UI state to reflect that the user is logged out.
     * On failure, it logs an error message.
     */
    fun logout() {
        viewModelScope.launch {
            BlueGPSAuthManager.instance.logout(handleCallback = {
                if (it) {
                    Log.d(TAG, "SUCCESS logged out")
                    _isLoggedIn.value = false
                } else {
                    Log.e(TAG, "ERROR logged out")
                }
            })
        }
    }

    /**
     * Updates the login state with the provided value.
     * This is typically called from the [LoginScreen] after the SDK's login activity returns a result.
     *
     * @param value `true` if the login was successful, `false` otherwise.
     */
    fun updateIsLoggedIn(value: Boolean) {
        _isLoggedIn.value = value
    }

    /**
     * Retrieves (or creates if missing) the BlueGPS advertising configuration for the user.
     * This is necessary for the SDK to start monitoring the user's location.
     */
    private fun getOrCreateConfiguration() {
        viewModelScope.launch {
            when (val res = BlueGPSLib.instance.getOrCreateConfiguration()) {
                is Resource.Error -> {
                    // Handle API errors (e.g., network issues, server errors).
                    val errorMessage = "${res.code} ${res.message}"
                    Log.e(TAG, errorMessage)
                }

                is Resource.Exception -> {
                    // Handle unexpected exceptions during the process.
                    Log.e(TAG, "${res.e}")
                }

                is Resource.Success -> {
                    // On success, store the configuration and start monitoring.
                    Log.d(TAG, "Configuration received for tagId: ${res.data.tagid}")

                    // start advertising
                    AdvManager.startAdvertising(androidAdvConfiguration = res.data)
                }
            }
        }
    }
}