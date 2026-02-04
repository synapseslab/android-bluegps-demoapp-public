/*
 * Created by Davide Agostini
 * Copyright (c) 2023-2025 Synapses srl - All rights reserved.
 * Last modified 17/12/25, 17:38
 */

package com.synapseslab.bluegpssdkdemo.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegpssdkdemo.constants.Environment

/**
 * This BroadcastReceiver is responsible for restarting the beacon scanning service
 * after the device has been rebooted. It listens for the standard system event
 * `ACTION_BOOT_COMPLETED`.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Check if this is actually ACTION_BOOT_COMPLETED
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Additional safety check: In development, Android Studio sometimes sends this broadcast
            // when starting the app. We want to avoid this and only run on real boot.
            val uptimeMillis = SystemClock.elapsedRealtime()
            val isLikelyRealBoot = uptimeMillis < 600000 // Less than 10 minutes since boot

            if (!isLikelyRealBoot) {
                Log.w("BootCompletedReceiver", "System has been up for ${uptimeMillis / 60000} minutes. This is likely a development broadcast, not a real boot. Skipping.")
                return
            }

            Log.d("BootCompletedReceiver", "Boot completed event received. Restarting beacon scanning.")

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

            // To start the service for background events, we must provide it with a PendingIntent
            // that it can use to notify the app. This creates an intent that points to our
            // BeaconEventReceiver, which is responsible for showing the notification.
            val beaconEventIntent = Intent(context, BeaconEventReceiver::class.java).apply {
                action = BlueGPSLib.ACTION_BEACON_EVENT // Essential for proper action handling
            }
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE  // MUTABLE allows fillInIntent data
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val beaconEventPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                beaconEventIntent,
                pendingIntentFlags
            )

            BlueGPSLib.instance.startTrackingUserPosition(
                context = context,
                pendingIntent = beaconEventPendingIntent,
                onError = { error ->
                    Log.e("BootCompletedReceiver", "Failed to start beacon tracking on boot: $error")
                }
            )
        } else {
            Log.d("BootCompletedReceiver", "Received non-boot intent: ${intent.action} - Ignoring")
        }
    }
}
