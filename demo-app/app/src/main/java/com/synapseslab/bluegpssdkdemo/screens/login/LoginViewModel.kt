package com.synapseslab.bluegpssdkdemo.screens.login

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapseslab.bluegps_sdk.authentication.core.BlueGPSAuthManager
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegps_sdk.utils.Resource
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
     * Nothing else starts here: advertising and beacon scanning start with the background session
     * (Session tab). If the session is invalid, it updates the UI state to reflect that the user
     * is logged out.
     */
    private fun checkLoginStatus() {
        viewModelScope.launch {
            // Observe the session status from the BlueGPS SDK.
            BlueGPSLib.instance.isAValidSession().collectLatest { isValid ->
                if (!isValid) {
                    // If the session is invalid, update the state to reflect that the user is logged out.
                    _isLoggedIn.value = false
                } else {
                    BlueGPSLib.instance.accessToken()?.let {
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
                    // Never log result.data: it holds the access token.
                    Log.v(TAG, "Login in guest mode")

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
     * From SDK 6.4.0 the logout stops everything by itself: background session, advertising,
     * beacon scanning and the location modules stop before the credentials are cleared.
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
}
