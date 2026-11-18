/*
 * Copyright (c) 2026 Synapses s.r.l. All rights reserved.
 */

package com.synapseslab.bluegpssdkdemo.screens.session

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synapseslab.bluegps_sdk.background.SessionState
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegps_sdk.core.backgroundSession

/**
 * Shown on the screens that need beacon scanning while the background session is not running:
 * in this demo the scan starts with the session.
 */
@Composable
fun SessionHint(modifier: Modifier = Modifier) {
    val state by BlueGPSLib.instance.backgroundSession.state.collectAsState()
    if (state is SessionState.Running) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            "Beacon scanning runs with the background session: start it in the Session tab.",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
