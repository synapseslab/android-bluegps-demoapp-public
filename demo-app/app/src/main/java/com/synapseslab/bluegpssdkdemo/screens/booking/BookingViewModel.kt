package com.synapseslab.bluegpssdkdemo.screens.booking

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegps_sdk.core.schedule
import com.synapseslab.bluegps_sdk.data.model.booking.BGPResource
import com.synapseslab.bluegps_sdk.data.model.booking.ScheduleRequest
import com.synapseslab.bluegps_sdk.data.model.map.ResourceType
import com.synapseslab.bluegps_sdk.data.model.search.Filter
import com.synapseslab.bluegps_sdk.data.model.search.FilterType
import com.synapseslab.bluegps_sdk.data.model.search.SectionFilterType
import com.synapseslab.bluegps_sdk.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val TAG = "BookingViewModel"

/**
 * Represents the status of a booking schedule attempt.
 */
sealed class ScheduleStatus {
    /** The initial state, no scheduling has been attempted. */
    object Idle : ScheduleStatus()
    /** The scheduling was successful. Contains the original [ScheduleRequest]. */
    data class Success(val request: ScheduleRequest) : ScheduleStatus()
    /** An error occurred during scheduling. Contains an error message. */
    data class Error(val message: String) : ScheduleStatus()
}

/**
 * Represents the UI state for the Booking screen.
 *
 * @property resources The list of available resources to be displayed.
 * @property searchText The current text in the search input field.
 * @property isLoading `true` if data is currently being loaded, `false` otherwise.
 * @property errorMessage An optional error message to be displayed to the user.
 * @property scheduleStatus The current status of a booking attempt.
 */
data class BookingUiState(
    val resources: List<BGPResource> = emptyList(),
    val searchText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val scheduleStatus: ScheduleStatus = ScheduleStatus.Idle
)

/**
 * ViewModel for the [BookingScreen].
 *
 * This ViewModel is responsible for fetching and managing the list of bookable resources,
 * handling search functionality, and processing booking requests. It interacts with the
 * `BlueGPSLib` to perform search and schedule operations and exposes the UI state
 * through a [StateFlow].
 */
@Stable
class BookingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(isLoading = true) }
        search(uiState.value.searchText)
    }

    /**
     * Called when the search text changes. Updates the UI state and triggers a new search.
     * @param text The new search query.
     */
    fun onSearchTextChange(text: String) {
        _uiState.update { it.copy(searchText = text) }
        search(text)
    }

    /**
     * Resets the [scheduleStatus] to [ScheduleStatus.Idle], dismissing any booking-related dialogs.
     */
    fun onDismissDialog() {
        _uiState.update { it.copy(scheduleStatus = ScheduleStatus.Idle) }
    }

    /**
     * Performs a search for bookable resources using the BlueGPS SDK.
     * Updates the UI state with the search results, loading state, and any errors.
     * @param query The search query to filter resources.
     */
    private fun search(query: String) {
        viewModelScope.launch {
            when (val res = BlueGPSLib.instance.search(
                filter = Filter(
                    search = query,
                    section = SectionFilterType.BOOKING,
                    resourceTypes = listOf(ResourceType.ROOM),
                    filterType = FilterType.ROOM_BOOKING
                )
            )) {
                is Resource.Error -> {
                    val errorMessage = "${res.code} - ${res.message}"
                    Log.e(TAG, errorMessage)
                    _uiState.update {
                        it.copy(errorMessage = errorMessage, isLoading = false)
                    }
                }
                is Resource.Exception -> {
                    val errorMessage = res.e.toString()
                    Log.e(TAG, errorMessage)
                    _uiState.update {
                        it.copy(errorMessage = errorMessage, isLoading = false)
                    }
                }
                is Resource.Success -> {
                    Log.v(TAG, "${res.data}")
                    _uiState.update {
                        it.copy(resources = res.data, isLoading = false, errorMessage = null)
                    }
                }
            }
        }
    }

    /**
     * Creates a default [ScheduleRequest] for a given resource and initiates the booking process.
     * The booking is scheduled for the next day from 4 PM to 5 PM.
     * @param resource The [BGPResource] to be booked.
     */
    fun scheduleRoom(resource: BGPResource) {
        viewModelScope.launch {
            val tomorrow = LocalDate.now().plusDays(1)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val scheduleRequest = ScheduleRequest(
                dayStart = tomorrow.format(formatter),
                elementId = resource.id,
                elementType = ResourceType.ROOM,
                end = "17:00",
                meetingName = "Booking for ${resource.name}",
                start = "16:00",
                videoConference = false
            )
            schedule(scheduleRequest)
        }
    }

    /**
     * Sends a booking request to the BlueGPS SDK.
     * Updates the [scheduleStatus] in the UI state based on the result of the operation.
     * @param scheduleRequest The [ScheduleRequest] containing the booking details.
     */
    private fun schedule(scheduleRequest: ScheduleRequest) {
        viewModelScope.launch {
            when (val res = BlueGPSLib.instance.schedule(scheduleRequest)) {
                is Resource.Error -> {
                    val errorMessage = "${res.code} - ${res.message}"
                    Log.e(TAG, errorMessage)
                    _uiState.update {
                        it.copy(scheduleStatus = ScheduleStatus.Error(errorMessage))
                    }
                }
                is Resource.Exception -> {
                    val errorMessage = res.e.toString()
                    Log.e(TAG, errorMessage)
                    _uiState.update {
                        it.copy(scheduleStatus = ScheduleStatus.Error(errorMessage))
                    }
                }
                is Resource.Success -> {
                    Log.d(TAG, " ** ${res.data} **")
                    _uiState.update {
                        it.copy(scheduleStatus = ScheduleStatus.Success(scheduleRequest))
                    }
                }
            }
        }
    }
}