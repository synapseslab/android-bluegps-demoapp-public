/*
 * Created by Davide Agostini on 14/05/24, 12:33
 * Copyright (c) 2023-2024 Synapses s.r.l.s. All rights reserved.
 * Last modified 14/05/24, 12:33
 */

package com.synapseslab.bluegpssdkdemo.advertising

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.synapseslab.bluegps_sdk.data.model.advertising.AndroidAdvConfiguration
import com.synapseslab.bluegps_sdk.service.BlueGPSAdvertisingService

private const val TAG = "AdvManager"

/**
 * Centralized helper to work with [BlueGPSAdvertisingService].
 *
 * Responsibilities:
 * - Bind/unbind the advertising service.
 * - Forward start/stop advertising commands to the bound service.
 *
 * Typical lifecycle:
 * - Call [bindBlueGPSAdvertisingService] once (e.g., in Application/Activity startup).
 * - Call [startAdvertising] when an [AndroidAdvConfiguration] is available.
 * - Call [stopAdvertising] and then [unBindBlueGPSAdvertisingService] in teardown.
 */
object AdvManager {

    // Reference to the bound service. It's null when the service is not bound/connected.
    private var blueGPSAdvertisingService: BlueGPSAdvertisingService? = null

    // Connection callback used by bindService/unbindService to manage the service reference.
    private val advertisingServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            // We've bound to BlueGPSAdvertisingService; cast the IBinder and get service instance.
            val binder = service as BlueGPSAdvertisingService.LocalBinder
            blueGPSAdvertisingService = binder.serviceBlueGPS
        }

        override fun onServiceDisconnected(name: ComponentName) {
            // System disconnected the service (e.g., process killed). Clear the reference.
            blueGPSAdvertisingService = null
        }
    }

    /**
     * Start BLE advertising using the provided Android configuration.
     * No-op if the service is not yet bound.
     */
    fun startAdvertising(androidAdvConfiguration: AndroidAdvConfiguration) {
        blueGPSAdvertisingService?.startAdv(androidAdvConfiguration)
    }

    /**
     * Stop BLE advertising if the service is bound.
     */
    fun stopAdvertising() {
        blueGPSAdvertisingService?.stopAdv()
    }

    /**
     * Bind to [BlueGPSAdvertisingService]. Should be called before [startAdvertising].
     * Prefer passing an Application Context to avoid leaking an Activity.
     */
    fun bindBlueGPSAdvertisingService(context: Context) {
        // Explicitly bind the advertising service. The boolean result indicates if the binding
        // request was accepted. The actual connection happens asynchronously via the callback above.
        val serviceIntent = Intent(context, BlueGPSAdvertisingService::class.java)
        val result = context.bindService(
            serviceIntent,
            advertisingServiceConnection,
            Context.BIND_AUTO_CREATE
        )
        Log.i(TAG, "context: ${context.hashCode()}  bindBlueGPSAdvertisingService result: $result")
    }

    /**
     * Unbind from [BlueGPSAdvertisingService]. Call this symmetrically to the bind
     * (e.g., in onStop/onDestroy) to release resources.
     */
    fun unBindBlueGPSAdvertisingService(context: Context) {
        try {
            Log.i(TAG, "context: ${context.hashCode()}  unBindBlueGPSAdvertisingService")
            context.unbindService(advertisingServiceConnection)
        } catch (e: Exception) {
            // Unbinding may throw if we are not currently bound; guard and log.
            e.printStackTrace()
            Log.i(TAG, "${e.message}")
        }
    }
}