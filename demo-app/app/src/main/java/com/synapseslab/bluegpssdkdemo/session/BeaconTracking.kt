/*
 * Copyright (c) 2026 Synapses s.r.l. All rights reserved.
 */

package com.synapseslab.bluegpssdkdemo.session

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegpssdkdemo.receiver.BeaconEventReceiver

private const val TAG = "BeaconTracking"

/**
 * Beacon scanning (user position tracking). In this demo it runs under the background session:
 * [DemoSession.start] starts it, and stopping the session or logging out stops it.
 */
object BeaconTracking {

    fun start(context: Context, onError: (String) -> Unit = {}) {
        // The scan service notifies beacon events to the app through this PendingIntent,
        // which targets BeaconEventReceiver.
        val beaconEventIntent = Intent(context, BeaconEventReceiver::class.java).apply {
            action = BlueGPSLib.ACTION_BEACON_EVENT // This is crucial for the receiver to work
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE  // MUTABLE allows fillInIntent data
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val beaconEventPendingIntent = PendingIntent.getBroadcast(context, 0, beaconEventIntent, flags)

        BlueGPSLib.instance.startTrackingUserPosition(
            context,
            beaconEventPendingIntent,
            onError = {
                Log.e(TAG, "Beacon tracking not started: $it")
                onError(it.toString())
            },
        )
    }
}
