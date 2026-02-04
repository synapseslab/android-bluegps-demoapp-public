package com.synapseslab.bluegpssdkdemo.screens.sse

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegps_sdk.core.startNotifyRegionChanges
import com.synapseslab.bluegps_sdk.core.stopNotifyRegionChanges
import com.synapseslab.bluegps_sdk.data.model.region.BGPRegion
import com.synapseslab.bluegps_sdk.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Represents the UI state for the Server-Sent Events (SSE) screen.
 *
 * @property beaconStatus A string containing the accumulated log of beacon scan events.
 * @property regionStatus A string containing the accumulated log of region enter/exit events.
 */
data class SSEUiState(
    val beaconStatus: String = "",
    val regionStatus: String = "",
)

private const val TAG = "SSEViewModel"

/**
 * ViewModel for the [SSEScreen].
 *
 * This ViewModel is responsible for managing the state related to Server-Sent Events (SSE),
 * specifically for beacon tracking and region monitoring. It interacts with the `BlueGPSLib`
 * to receive real-time updates and exposes them to the UI via a [StateFlow].
 */
@Stable
class SSEViewModel: ViewModel() {

    // The private, mutable state flow that holds the current UI state.
    private val _uiState = MutableStateFlow(SSEUiState())

    /**
     * The publicly exposed, read-only [StateFlow] for the UI to observe.
     *
     * UI components should collect from this flow to react to changes in the SSE screen's state.
     */
    val uiState: StateFlow<SSEUiState> = _uiState.asStateFlow()

    /**
     * A buffer to accumulate beacon scan log messages.
     * New messages are appended to this string, which is then used to update [uiState.beaconStatus].
     */
    private var beaconLogText = ""

    /**
     * A buffer to accumulate region enter/exit log messages.
     * New messages are appended to this string, which is then used to update [uiState.regionStatus].
     */
    private var regionLogText = ""

    /**
     * Tracks the last known set of regions per key (e.g., tag ID) returned by the SDK's region
     * change callback. This is used to compute ENTER and EXIT events by diffing with the current
     * set of regions.
     */
    private val lastRegionsByKey = mutableMapOf<String, Set<String>>()

    init {
        // Collect and display beacon tracking data from the BlueGPS SDK.
        // This coroutine runs as long as the ViewModel is active.
        viewModelScope.launch {
            BlueGPSLib.instance.bgpBeaconLocationFlow.collect { beacon ->
                val message = "${beacon.event}: ${beacon.message}"
                beaconLogText += message + "\n"
                _uiState.update { it.copy(beaconStatus = beaconLogText) }
            }
        }
    }

    /**
     * Clears the accumulated beacon scan log messages from [beaconLogText] and updates the UI state.
     */
    fun clearBeaconLogs() {
        beaconLogText = ""
        _uiState.update { it.copy(beaconStatus = "") }
    }

    /**
     * Clears the accumulated region enter/exit log messages from [regionLogText] and
     * resets the internal state of [lastRegionsByKey] to prevent false ENTER/EXIT events
     * after the logs have been cleared.
     */
    fun clearRegionLogs() {
        regionLogText = ""
        // Reset also the last known regions to avoid false exits/enters after a clear.
        lastRegionsByKey.clear()
        _uiState.update { it.copy(regionStatus = "") }
    }

    /**
     * Initiates the process of listening for region changes from the BlueGPS SDK.
     *
     * This function sets up a callback handler that receives updates on which regions a user
     * (identified by a tag ID) is currently inside. It then computes and logs ENTER/EXIT events
     * by comparing the current regions with the previously known regions for each tag.
     *
     * Notes:
     * - When the SDK callback provides an empty map for a key, previously known regions for that
     *   key are treated as disappeared, and their regions are logged as EXIT.
     * - The 'tags' parameter is filtered to the device's `userTagId` when available; otherwise,
     *   it listens for all tags.
     */
    fun startNotifyRegion() {
        viewModelScope.launch {
            val regions = getListOfRegions(BlueGPSLib.instance).getOrNull().orEmpty()
            val tagId = BlueGPSLib.instance.userTagId()
            Log.d(TAG, "tagId: $tagId")

            BlueGPSLib.instance.startNotifyRegionChanges(
                // If available, restrict notifications to this device tagId; otherwise, listen for all tags
                tags = if (tagId != null) listOf(tagId) else null,
                // Pre-fetched list of regions we are interested in; SDK will match user presence against these
                regions = regions,
                // Callback contract: Map<key, regions> where key is typically the Tag ID and
                // value is the current list of regions the user (for that key) is inside at this instant.
                callbackHandler = { regionMap: Map<String, MutableList<BGPRegion>> ->

                    val sb = StringBuilder()

                    // Compute enter/exit per key present in this callback
                    regionMap.forEach { (key, regionsForKey) ->
                        val current: Set<String> = regionsForKey
                            .mapNotNull { it.name?.trim()?.takeIf { n -> n.isNotEmpty() } }
                            .toSet()
                        val previous: Set<String> = lastRegionsByKey[key] ?: emptySet()

                        val entered = current - previous
                        val exited = previous - current

                        entered.forEach { name -> sb.append("ENTER: ").append(name).append('\n') }
                        exited.forEach { name -> sb.append("EXIT: ").append(name).append('\n') }

                        if (previous != current) {
                            lastRegionsByKey[key] = current
                        }
                    }

                    // Handle keys that disappeared entirely in this update: all their regions are exits.
                    // This also covers the case where the callback comes with an empty map: all previous
                    // regions for all keys will be emitted as EXIT and the memory of them cleared.
                    val currentKeys = regionMap.keys
                    val disappearedKeys = lastRegionsByKey.keys - currentKeys
                    disappearedKeys.forEach { missingKey ->
                        lastRegionsByKey[missingKey]?.forEach { name ->
                            sb.append("EXIT: ").append(name).append('\n')
                        }
                        lastRegionsByKey.remove(missingKey)
                    }

                    // If there were any changes, append them to the log buffer and update the UI state
                    val changes = sb.toString()
                    if (changes.isNotEmpty()) {
                        Log.d(TAG, changes.trim())
                        regionLogText += changes
                        _uiState.update { it.copy(regionStatus = regionLogText) }
                    }
                },
                onStop = {
                    Log.e(TAG, it)
                }
            )
        }
    }

    /**
     * Stops listening for region changes.
     * This function calls the `stopNotifyRegionChanges` method of the BlueGPS SDK.
     */
    fun stopNotifyRegion() {
        BlueGPSLib.instance.stopNotifyRegionChanges()
    }

    /**
     * Helper function that wraps the SDK call to fetch available regions and converts its
     * [Resource] return type into a more idiomatic Kotlin [Result] type.
     *
     * @param gps The [BlueGPSLib] instance to use for fetching regions.
     * @return A [Result] containing a list of [BGPRegion] on success, or an [Exception] on failure.
     */
    private suspend fun getListOfRegions(gps: BlueGPSLib): Result<List<BGPRegion>> {
        return when (val result = gps.getRoomsCoordinates()) {
            is Resource.Success -> Result.success(result.data)
            is Resource.Error -> Result.failure(Exception(result.message))
            is Resource.Exception -> Result.failure(result.e)
        }
    }
}