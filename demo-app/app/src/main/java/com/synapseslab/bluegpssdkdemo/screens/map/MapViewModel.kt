package com.synapseslab.bluegpssdkdemo.screens.map

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegps_sdk.data.model.map.Floor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sealed class representing the various commands that can be sent to the map view.
 *
 * This class defines a set of specific actions that the [MapScreen] can perform on the
 * `MapViewManager`. Using a sealed class ensures type safety and allows for exhaustive
 * handling of all possible map commands.
 */
sealed class MapCommand {
    /**
     * Command to enable or disable the "follow me" mode on the map.
     * @property enable `true` to enable "follow me" mode, `false` to disable it.
     */
    data class ForceFollowMe(val enable: Boolean) : MapCommand()

    /**
     * Command to request the list of available floors from the map.
     */
    object GetFloorList : MapCommand()

    /**
     * Command to change the currently displayed floor on the map.
     * @property floor The [Floor] object to switch to.
     */
    data class GotoFloor(val floor: Floor) : MapCommand()

    /**
     * Command to center the map view on a specific tag.
     * @property tagId The ID of the tag to focus on.
     */
    data class ShowTag(val tagId: String) : MapCommand()
}

/**
 * ViewModel for the [MapScreen].
 *
 * This ViewModel acts as a bridge between the UI layer ([MapScreen]) and the map logic
 * encapsulated in `MapViewManager`. It exposes a [SharedFlow] of [MapCommand]s that the
 * screen can collect to receive instructions for map manipulations. It also manages the
 * state of UI elements, such as the "follow me" toggle.
 */
@Stable
class MapViewModel : ViewModel() {
    private val _mapCommands = MutableSharedFlow<MapCommand>()
    /**
     * A [SharedFlow] that emits [MapCommand]s to be executed by the `MapViewManager`.
     * The [MapScreen] collects this flow and translates the commands into method calls
     * on the `MapViewManager` instance.
     */
    val mapCommands = _mapCommands.asSharedFlow()

    private val _isFollowMeEnabled = MutableStateFlow(false)
    /**
     * A [StateFlow] that holds the current state of the "follow me" mode.
     * The UI observes this flow to update the state of the "follow me" toggle button.
     */
    val isFollowMeEnabled = _isFollowMeEnabled.asStateFlow()

    /**
     * Emits a [MapCommand.GetFloorList] command to fetch the list of available floors.
     */
    fun getFloorList() {
        viewModelScope.launch {
            _mapCommands.emit(MapCommand.GetFloorList)
        }
    }

    /**
     * Emits a [MapCommand.GotoFloor] command to change the current floor on the map.
     * @param floor The [Floor] to display.
     */
    fun setFloor(floor: Floor) {
        viewModelScope.launch {
            _mapCommands.emit(MapCommand.GotoFloor(floor)) // Emit command to set the floor
        }
    }

    /**
     * Toggles the "follow me" mode and emits a [MapCommand.ForceFollowMe] command.
     * It updates the [isFollowMeEnabled] state and instructs the map to either start
     * or stop following the user's location.
     */
    fun toggleFollowMe() {
        _isFollowMeEnabled.value = !_isFollowMeEnabled.value
        viewModelScope.launch {
            _mapCommands.emit(MapCommand.ForceFollowMe(_isFollowMeEnabled.value))
        }
    }

    /**
     * Emits a [MapCommand.ShowTag] command to center the map on the user's tag.
     * It retrieves the user's tag ID from `BlueGPSLib` and, if available, sends the
     * command to the map.
     */
    fun showTag() {
        viewModelScope.launch {
            val tagId = BlueGPSLib.instance.userTagId()
            tagId?.let { tagId ->
                _mapCommands.emit(MapCommand.ShowTag(tagId))
            }
        }
    }
}
