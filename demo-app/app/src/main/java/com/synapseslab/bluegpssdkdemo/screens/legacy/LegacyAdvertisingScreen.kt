/*
 * Copyright (c) 2026 Synapses s.r.l. All rights reserved.
 */

@file:Suppress("DEPRECATION")   // the whole screen shows the deprecated API on purpose

package com.synapseslab.bluegpssdkdemo.screens.legacy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapseslab.bluegps_sdk.background.SessionState
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegps_sdk.core.backgroundSession
import com.synapseslab.bluegps_sdk.data.model.advertising.AdvertisingStatus
import com.synapseslab.bluegps_sdk.service.BlueGPSAdvertisingService

/**
 * Legacy advertising, opened from Settings. Separate from the Session tab on purpose: the two
 * ways must not be mixed in an app.
 */
@Composable
fun LegacyAdvertisingScreen(viewModel: LegacyAdvertisingViewModel = viewModel()) {
    val context = LocalContext.current
    val bound by viewModel.bound.collectAsState()
    val status by viewModel.status.collectAsState()
    // The two ways drive the same advertising: with a session running the old one stays off.
    val sessionRunning = BlueGPSLib.instance.backgroundSession.state.collectAsState().value is SessionState.Running

    // The service broadcasts its status to this app's package only (RECEIVER_NOT_EXPORTED).
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                IntentCompat.getParcelableExtra(intent, BlueGPSAdvertisingService.DATA_ADV, AdvertisingStatus::class.java)
                    ?.let { viewModel.onStatus("${it.status} ${it.message}".trim()) }
            }
        }
        ContextCompat.registerReceiver(
            context, receiver,
            IntentFilter(BlueGPSAdvertisingService.ACTION_ADV),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Legacy advertising", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                "Deprecated since SDK 6.4.0, removed in 7.0.0. This is the old way to advertise " +
                    "(BlueGPSAdvertisingService, bind and broadcast), for apps that still use it. " +
                    "New code uses the background session in the Session tab. Do not use the two " +
                    "together: they drive the same advertising.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (sessionRunning) {
            Text(
                "The background session is running: stop it in the Session tab to try the old way.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.bind(context) }, enabled = !bound && !sessionRunning) { Text("Bind") }
            OutlinedButton(onClick = { viewModel.unbind(context) }, enabled = bound) { Text("Unbind") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.start() }, enabled = bound && !sessionRunning) { Text("startAdv") }
            OutlinedButton(onClick = { viewModel.stop() }, enabled = bound && !sessionRunning) { Text("stopAdv") }
        }

        Text("Service", style = MaterialTheme.typography.labelMedium)
        Text(if (bound) "bound" else "not bound", style = MaterialTheme.typography.bodyMedium)
        Text("Last status broadcast", style = MaterialTheme.typography.labelMedium)
        Text(status, style = MaterialTheme.typography.bodyMedium)

        Text("Code: advertising/AdvManager.kt", style = MaterialTheme.typography.bodySmall)
    }
}
