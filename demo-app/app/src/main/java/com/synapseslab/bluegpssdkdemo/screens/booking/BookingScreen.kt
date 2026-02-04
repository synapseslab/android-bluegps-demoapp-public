package com.synapseslab.bluegpssdkdemo.screens.booking

import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapseslab.bluegps_sdk.data.model.map.Position
import com.synapseslab.bluegpssdkdemo.ui.theme.Primary

/**
 * Composable function for the Booking screen.
 *
 * This screen allows users to search for and book resources, such as desks or meeting rooms.
 * It displays a list of available resources, provides a search bar to filter them, and offers
 * options to schedule a booking or navigate to the resource's location.
 *
 * The screen observes the [BookingViewModel] to manage its state, including the list of
 * resources, loading indicators, and error messages. It also displays dialogs to show the
 * status of a booking attempt (success or error).
 *
 * @param bookingViewModel The [BookingViewModel] instance for managing the screen's state and logic.
 * @param navigateToNavigationScreen A callback function to navigate to the navigation screen,
 *                                   passing the position of the selected resource.
 */
@Composable
fun BookingScreen(
    bookingViewModel: BookingViewModel = viewModel(),
    navigateToNavigationScreen: (position: Position) -> Unit
) {
    val uiState by bookingViewModel.uiState.collectAsState()

    val context = LocalContext.current

    // Display an AlertDialog based on the scheduleStatus
    when (val status = uiState.scheduleStatus) {
        is ScheduleStatus.Success -> {
            AlertDialog(
                onDismissRequest = { bookingViewModel.onDismissDialog() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color.Green, CircleShape)
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Meeting scheduled")
                    }
                },
                text = {
                    Column {
                        Text("Name: ${status.request.meetingName}")
                        Text("Day: ${status.request.dayStart}")
                        Text("Start: ${status.request.start}")
                        Text("End: ${status.request.end}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { bookingViewModel.onDismissDialog() }) {
                        Text("OK")
                    }
                }
            )
        }

        is ScheduleStatus.Error -> {
            AlertDialog(
                onDismissRequest = { bookingViewModel.onDismissDialog() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color.Red, CircleShape)
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Error")
                    }
                },
                text = { Text(status.message) },
                confirmButton = {
                    TextButton(onClick = { bookingViewModel.onDismissDialog() }) {
                        Text("OK")
                    }
                }
            )
        }

        ScheduleStatus.Idle -> {} // Do nothing when the status is idle
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Booking Screen",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        TextField(
            value = uiState.searchText,
            onValueChange = bookingViewModel::onSearchTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            placeholder = { Text("Search a room") }
        )
        Text(
            "Desk list",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Box {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.resources) { resource ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = resource.name,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { bookingViewModel.scheduleRoom(resource) }) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Book"
                            )
                        }
                        IconButton(onClick = {
                            if (resource.position != null) {
                                navigateToNavigationScreen(resource.position!!)
                            } else {
                                Toast.makeText(
                                    context,
                                    "${resource.name} don't have position",
                                    LENGTH_SHORT
                                ).show()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = "Navigate"
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }

            if (!uiState.errorMessage.isNullOrEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        uiState.errorMessage ?: "Error",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}