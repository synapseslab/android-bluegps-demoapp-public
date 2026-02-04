package com.synapseslab.bluegpssdkdemo.screens.settings

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegpssdkdemo.utils.getClaimRaw
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


@Stable
data class SettingsUIState(
    val loggedUser: String? = null,
    val tagId: String? = null,
)

@Stable
class SettingsViewModel : ViewModel() {


    private val _uiState = MutableStateFlow(SettingsUIState())

    // The publicly exposed, read-only state flow for the UI to observe.
    // The UI collects this flow to get updates and redraw itself.
    val uiState: StateFlow<SettingsUIState> = _uiState.asStateFlow()


    init {
        val tagId = BlueGPSLib.instance.userTagId() ?: "No tag assigned"

        BlueGPSLib.instance.accessToken()?.let {
            val email =
                getClaimRaw(it, "preferred_username") ?: getClaimRaw(it, "email")
            _uiState.update { state ->
                state.copy(
                    tagId = tagId,
                    loggedUser = email as? String ?: "Logged user"
                )
            }
        }
    }
}