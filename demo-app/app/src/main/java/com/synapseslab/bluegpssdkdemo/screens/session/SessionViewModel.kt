/*
 * Copyright (c) 2026 Synapses s.r.l. All rights reserved.
 */

package com.synapseslab.bluegpssdkdemo.screens.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapseslab.bluegps_sdk.advertising.AdvertisingState
import com.synapseslab.bluegps_sdk.background.SessionEvent
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegps_sdk.core.advertising
import com.synapseslab.bluegps_sdk.core.backgroundSession
import com.synapseslab.bluegpssdkdemo.session.DemoSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exposes what the SDK says about the background session and advertising, and the actions of the
 * Session screen. The session itself lives in [DemoSession], not here.
 */
class SessionViewModel : ViewModel() {

    private val session = BlueGPSLib.instance.backgroundSession

    val state = session.state
    val indicator = session.indicator
    val limitations = session.limitations
    val advertising: StateFlow<AdvertisingState> = BlueGPSLib.instance.advertising
    val content = DemoSession.content

    private val _events = MutableStateFlow<List<String>>(emptyList())
    /** The last events received while this screen is open, newest first. */
    val events: StateFlow<List<String>> = _events

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    /** What did not start with the session (beacon scan, advertising), with the reason. */
    val errors = DemoSession.errors

    init {
        viewModelScope.launch {
            session.events.collect { event -> _events.update { (listOf(describe(event)) + it).take(8) } }
        }
    }

    fun start(context: Context) {
        val started = DemoSession.start(context)
        _message.value = if (started) null else "A session is already running"
    }

    fun stop() = viewModelScope.launch { DemoSession.stop() }

    fun update() = DemoSession.update()

    private fun describe(event: SessionEvent): String {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val text = when (event) {
            is SessionEvent.Started -> "Started"
            is SessionEvent.Stopped -> "Stopped"
            is SessionEvent.IndicatorChanged -> "Indicator: ${event.state}"
            is SessionEvent.AdvertisingChanged -> DemoSession.describe(event.state)
        }
        return "$time  $text"
    }
}
