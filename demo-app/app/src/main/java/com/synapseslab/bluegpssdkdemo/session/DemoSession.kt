/*
 * Copyright (c) 2026 Synapses s.r.l. All rights reserved.
 */

package com.synapseslab.bluegpssdkdemo.session

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.synapseslab.bluegps_sdk.advertising.AdvertisingException
import com.synapseslab.bluegps_sdk.advertising.AdvertisingState
import com.synapseslab.bluegps_sdk.background.IndicatorChannel
import com.synapseslab.bluegps_sdk.background.IndicatorState
import com.synapseslab.bluegps_sdk.background.SessionEvent
import com.synapseslab.bluegps_sdk.background.SessionState
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegps_sdk.core.advertising
import com.synapseslab.bluegps_sdk.core.backgroundSession
import com.synapseslab.bluegps_sdk.core.startAdvertising
import com.synapseslab.bluegpssdkdemo.MainActivity
import com.synapseslab.bluegpssdkdemo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "DemoSession"

/**
 * What the session notification shows. The SDK puts nothing of its own in it: every word is ours.
 */
data class SessionContent(
    val beaconScan: Boolean = false,
    val advertising: String = "Advertising off",
    val updates: Int = 0,
) {
    val beaconScanLine: String
        get() = if (beaconScan) "Beacon scan on" else "Beacon scan off"

    /** One line, for the collapsed notification. */
    val summary: String
        get() = "$beaconScanLine · $advertising"

    /** Two lines, for the expanded notification: one per background job. */
    val details: String
        get() = "$beaconScanLine\n$advertising"
}

/**
 * Example of a background session (SDK 6.4.0+). In this demo the session is the one switch for
 * all the background work: [start] starts the session, beacon scanning and advertising together;
 * [stop], the Stop action of the notification and the logout stop everything.
 *
 * It lives as long as the app, not in a ViewModel: the notification content comes from
 * [content], and a flow tied to a screen would stop with it and freeze the notification on its
 * last value.
 */
object DemoSession {

    /** The app's own channel id. Its name in the system settings is `R.string.session_channel_name`. */
    private const val CHANNEL_ID = "live_session"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _content = MutableStateFlow(SessionContent())
    val content: StateFlow<SessionContent> = _content

    private const val PREFS = "demo_session"
    private const val KEY_WANTED = "wanted"

    private val _errors = MutableStateFlow<List<String>>(emptyList())
    /** What did not start with the last session (beacon scan, advertising), with the reason. */
    val errors: StateFlow<List<String>> = _errors

    /** Set by the first [start]: what the "left on" flag is written with. */
    @Volatile
    private var appContext: Context? = null

    private var advertisingJob: Job? = null

    init {
        // The notification follows the beacon scan and the advertising state by itself.
        scope.launch {
            BlueGPSLib.instance.advertising.collect { state ->
                _content.update { it.copy(advertising = describe(state)) }
            }
        }
        scope.launch {
            BlueGPSLib.instance.beaconScanRunningFlow.collect { running ->
                _content.update { it.copy(beaconScan = running) }
            }
        }
        // Any end of the session clears the "left on" flag: the logout and the system included.
        scope.launch {
            BlueGPSLib.instance.backgroundSession.events.collect { event ->
                if (event is SessionEvent.Stopped) appContext?.let { setWanted(it, false) }
            }
        }
    }

    /**
     * Whether the user left the session on: `BootCompletedReceiver` restarts it after a reboot
     * only in that case. Set by [start]; cleared by [stop] and by any `Stopped` event (logout,
     * the Stop of the notification). A reboot kills the process without a `Stopped` event, so the
     * choice survives it.
     */
    fun wasLeftOn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_WANTED, false)

    /**
     * Called when the app's UI starts: a flag still set while no session runs means the process
     * died without a `Stopped` event (crash, low memory). The session is off, so is the flag.
     */
    fun reconcile(context: Context) {
        if (wasLeftOn(context) && BlueGPSLib.instance.backgroundSession.state.value !is SessionState.Running) {
            setWanted(context, false)
        }
    }

    private fun setWanted(context: Context, wanted: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_WANTED, wanted).apply()

    /**
     * Starts the session, then beacon scanning and advertising under it: one notification for all.
     *
     * Call it with the app in the foreground: from the background Android refuses the foreground
     * service and the indicator is `Unavailable(REQUEST_FAILED)`. The exception is right after a
     * reboot (`BOOT_COMPLETED`, see `BootCompletedReceiver`).
     *
     * @return false when a session is already running.
     */
    fun start(context: Context): Boolean {
        val app = context.applicationContext
        val openApp = PendingIntent.getActivity(
            app, 0,
            Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getBroadcast(
            app, 0,
            Intent(app, SessionStopReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val started = BlueGPSLib.instance.backgroundSession.start(
            channel = IndicatorChannel(id = CHANNEL_ID, name = app.getString(R.string.session_channel_name)),
            initial = _content.value,
            updates = _content,
        ) { c ->
            // render: runs at every new value of `updates`. Keep it quick and synchronous.
            setSmallIcon(R.drawable.ic_session)          // required by Android
            setContentTitle(app.getString(R.string.session_title))
            setContentText(c.summary)                                   // collapsed: one line
            setStyle(NotificationCompat.BigTextStyle().bigText(c.details)) // expanded: one line per job
            setSubText(app.getString(R.string.session_updates, c.updates))
            setContentIntent(openApp)
            addAction(0, app.getString(R.string.session_stop), stop)   // our own Stop
        }
        if (started) {
            appContext = app
            setWanted(app, true)
            _errors.value = emptyList()
            // Started after the session, the scan runs under its notification instead of its own.
            BeaconTracking.start(app) { reason -> addError("Beacon scan not started: $reason") }
            startAdvertising()
        }
        return started
    }

    /**
     * Advertising starts with the app in the foreground or with a visible notification. After a
     * reboot the app is not in the foreground: wait until the notification is up (or refused),
     * then start.
     */
    private fun startAdvertising() {
        advertisingJob = scope.launch {
            withTimeoutOrNull(5_000) {
                BlueGPSLib.instance.backgroundSession.indicator.first { it !is IndicatorState.NotRequested }
            }
            // Stopped (or logged out) while waiting: nothing to start.
            if (BlueGPSLib.instance.backgroundSession.state.value !is SessionState.Running) return@launch
            if (BlueGPSLib.instance.advertising.value is AdvertisingState.On) return@launch   // a restart is not needed
            try {
                BlueGPSLib.instance.startAdvertising()   // configuration from the server
            } catch (e: AdvertisingException) {
                // e.code: BLUETOOTH_OFF, BLUETOOTH_NOT_AUTHORIZED, INVALID_TAG_ID,
                // CONFIGURATION_UNAVAILABLE, STARTED_FROM_BACKGROUND, ADVERTISING_NOT_SUPPORTED,
                // ADVERTISING_FAILED (with platformCode)
                Log.e(TAG, "Advertising not started: ${e.code}")
                addError("Advertising not started: ${e.code}")
            }
        }
    }

    private fun addError(message: String) = _errors.update { it + message }

    /** Changes the content: the notification follows (at most one redraw per second). */
    fun update() = _content.update { it.copy(updates = it.updates + 1) }

    /** Stops the session and everything under it: advertising and beacon scanning. */
    suspend fun stop() {
        advertisingJob?.cancel()
        appContext?.let { setWanted(it, false) }
        _errors.value = emptyList()
        BlueGPSLib.instance.backgroundSession.stop()
    }

    fun describe(state: AdvertisingState): String = when (state) {
        AdvertisingState.Off -> "Advertising off"
        is AdvertisingState.On -> "Advertising on, tag ${state.configuration.tagId}"
        is AdvertisingState.Failed -> "Advertising failed: ${state.code}"
    }
}
