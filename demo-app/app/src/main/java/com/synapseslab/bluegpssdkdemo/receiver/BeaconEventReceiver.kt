/*
 * Created by Davide Agostini
 * Copyright (c) 2023-2025 Synapses srl - All rights reserved.
 * Last modified 17/12/25, 17:45
 */

package com.synapseslab.bluegpssdkdemo.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegps_sdk.data.model.beacon.BeaconEvent
import com.synapseslab.bluegps_sdk.data.model.beacon.BeaconLocation
import com.synapseslab.bluegpssdkdemo.MainActivity
import com.synapseslab.bluegpssdkdemo.R
import kotlin.math.abs

/**
 * This BroadcastReceiver handles beacon events that occur when the app is in the background
 * or closed. It receives BeaconLocation data via PendingIntent from the BGPScanBeaconService
 * and displays a notification to the user.
 */
class BeaconEventReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BeaconEventReceiver"
        private const val CHANNEL_ID = "beacon_notifications"
        private const val BASE_NOTIFICATION_ID = 1001
        private const val NOTIFICATION_GROUP_KEY = "bluegps_beacon_events"

        /**
         * Generates a unique notification ID for each beacon event.
         * This allows multiple beacon notifications to be displayed simultaneously.
         */
        private fun generateNotificationId(beaconId: String, eventName: String?): Int {
            // Create a unique ID based on beacon ID and event type
            val uniqueString = "${beaconId}_${eventName ?: "unknown"}"
            // Use hashCode to convert string to int, ensure positive value
            return BASE_NOTIFICATION_ID + abs(uniqueString.hashCode() % 1000)
        }


    }

    override fun onReceive(context: Context, intent: Intent) {

        // Check for beacon event by action or presence of primitive beacon data
        val isBeaconEvent = intent.action == BlueGPSLib.ACTION_BEACON_EVENT ||
                           intent.hasExtra("beacon_id")

        if (isBeaconEvent) {
            val beaconId = intent.getStringExtra("beacon_id")
            val beaconMessage = intent.getStringExtra("beacon_message")
            val beaconData = intent.getStringExtra("beacon_data")
            val beaconEventName = intent.getStringExtra("beacon_event_name")

            if (beaconId != null && beaconMessage != null) {
                Log.i(TAG, "Beacon event received: $beaconEventName at $beaconMessage")

                // Try to reconstruct a BeaconLocation from primitive data
                val reconstructedBeaconLocation = reconstructBeaconLocationFromPrimitives(
                    beaconId, beaconMessage, beaconData, beaconEventName
                )

                if (reconstructedBeaconLocation != null) {
                    showBeaconNotification(context, reconstructedBeaconLocation)
                } else {
                    Log.w(TAG, "Could not reconstruct BeaconLocation from primitive data")
                }
            } else {
                Log.e(TAG, "Invalid beacon data received")
            }
        } else {
            Log.w(TAG, "Received intent with unknown action: ${intent.action}")
        }
    }

    /**
     * Creates and displays a notification for the beacon event.
     */
    private fun showBeaconNotification(context: Context, beaconLocation: BeaconLocation) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (required for Android O and above)
        createNotificationChannel(notificationManager)

        // Create an intent to open the app when notification is tapped
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(BlueGPSLib.EXTRA_BEACON_LOCATION, beaconLocation)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val appPendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            pendingIntentFlags
        )

        // Build the notification
        val eventText = when (beaconLocation.event?.name) {
            "ENTER" -> "Entered area"
            "EXIT" -> "Exited area"
            else -> "Beacon detected"
        }

        val locationText = beaconLocation.message

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Make sure this icon exists
            .setContentTitle("BlueGPS: $eventText")
            .setContentText(locationText)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$eventText: $locationText\n${beaconLocation.data?.let { "\nData: $it" } ?: ""}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(appPendingIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP_KEY) // Group multiple beacon notifications
            .setWhen(System.currentTimeMillis()) // Set notification timestamp
            .setShowWhen(true) // Show timestamp in notification
            .build()

        // Generate unique notification ID for this beacon event
        val uniqueNotificationId = generateNotificationId(beaconLocation.id, beaconLocation.event?.name)
        notificationManager.notify(uniqueNotificationId, notification)
    }

    /**
     * Reconstructs a BeaconLocation object from primitive data.
     * This provides the best user experience by creating a full object instead of primitive notification.
     */
    private fun reconstructBeaconLocationFromPrimitives(
        beaconId: String,
        beaconMessage: String,
        beaconData: String?,
        beaconEventName: String?
    ): BeaconLocation? {
        return try {
            // Convert event name string back to BeaconEvent enum
            val beaconEvent = when (beaconEventName) {
                "ENTER" -> BeaconEvent.ENTER
                "EXIT" -> BeaconEvent.EXIT
                else -> null // Handle unknown event types gracefully
            }

            // Reconstruct the BeaconLocation object
            BeaconLocation(
                id = beaconId,
                message = beaconMessage,
                data = beaconData,
                event = beaconEvent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reconstruct BeaconLocation from primitives", e)
            null
        }
    }

    /**
     * Creates the notification channel for beacon notifications.
     */
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existingChannel == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Beacon Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for beacon location events"
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
