/*
 * Copyright (c) 2026 Synapses s.r.l. All rights reserved.
 */

@file:Suppress("DEPRECATION")   // the whole screen shows the deprecated API on purpose

package com.synapseslab.bluegpssdkdemo.screens.legacy

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegps_sdk.utils.Resource
import com.synapseslab.bluegpssdkdemo.advertising.AdvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The old way to advertise: `BlueGPSAdvertisingService` bound through [AdvManager], status
 * received by broadcast. Deprecated since SDK 6.4.0, removed in 7.0.0. Kept for apps that still
 * use it; new code uses the background session (Session tab).
 */
class LegacyAdvertisingViewModel : ViewModel() {

    val bound: StateFlow<Boolean> = AdvManager.isBound

    private val _status = MutableStateFlow("No status yet")
    /** The last `AdvertisingStatus` broadcast by the service, received while this screen is open. */
    val status: StateFlow<String> = _status

    fun onStatus(status: String) {
        _status.value = status
    }

    fun bind(context: Context) = AdvManager.bindBlueGPSAdvertisingService(context.applicationContext)

    fun unbind(context: Context) = AdvManager.unBindBlueGPSAdvertisingService(context.applicationContext)

    /** The old flow: configuration from `getOrCreateConfiguration()`, then `startAdv(config)`. */
    fun start() = viewModelScope.launch {
        when (val res = BlueGPSLib.instance.getOrCreateConfiguration()) {
            is Resource.Success -> AdvManager.startAdvertising(androidAdvConfiguration = res.data)
            is Resource.Error -> _status.value = "Configuration error: ${res.code} ${res.message}"
            is Resource.Exception -> _status.value = "Configuration error: ${res.e}"
        }
    }

    fun stop() = AdvManager.stopAdvertising()
}
