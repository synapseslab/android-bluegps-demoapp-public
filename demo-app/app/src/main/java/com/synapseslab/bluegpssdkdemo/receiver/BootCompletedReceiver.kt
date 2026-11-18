/*
 * Created by Davide Agostini
 * Copyright (c) 2023-2025 Synapses srl - All rights reserved.
 * Last modified 17/12/25, 17:38
 */

package com.synapseslab.bluegpssdkdemo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegpssdkdemo.constants.Environment
import com.synapseslab.bluegpssdkdemo.session.DemoSession

/**
 * Restarts the background session (and beacon scanning and advertising under it) after the
 * device has been rebooted, if the user left it on and is still logged in. It listens for the
 * standard system event `ACTION_BOOT_COMPLETED`, delivered after the first unlock.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Check if this is actually ACTION_BOOT_COMPLETED
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootCompletedReceiver", "Boot completed event received.")

            // Initialize the BlueGPS SDK first (required for network calls)
            // Use applicationContext to avoid ReceiverRestrictedContext ClassCastException
            try {
                BlueGPSLib.instance.initSDK(
                    sdkEnvironment = Environment.sdkEnvironment,
                    context = context.applicationContext,
                    enabledNetworkLogs = true
                )
                Log.d("BootCompletedReceiver", "BlueGPS SDK initialized successfully")
            } catch (e: Exception) {
                Log.e("BootCompletedReceiver", "Failed to initialize BlueGPS SDK", e)
                return // Exit early if SDK initialization fails
            }

            // Restart the background session (and beacon scanning and advertising under it) only if
            // the user left it on and is still logged in. Android may also send BOOT_COMPLETED
            // after the app's data is cleared: with no session to restore, nothing starts.
            if (!DemoSession.wasLeftOn(context) || BlueGPSLib.instance.accessToken() == null) {
                Log.d("BootCompletedReceiver", "Session was off or no user logged in: nothing to restart.")
                return
            }
            // Android lets an app start a foreground service right after BOOT_COMPLETED: from any
            // other background moment the session cannot start (indicator Unavailable(REQUEST_FAILED)).
            val started = DemoSession.start(context.applicationContext)
            Log.d("BootCompletedReceiver", "Background session started on boot: $started")
        } else {
            Log.d("BootCompletedReceiver", "Received non-boot intent: ${intent.action} - Ignoring")
        }
    }
}
