/*
 * Copyright (c) 2026 Synapses s.r.l. All rights reserved.
 */

package com.synapseslab.bluegpssdkdemo.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The Stop action of the session notification. The SDK has no Stop of its own: the app decides
 * what its notification offers, and this one ends the session.
 */
class SessionStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                DemoSession.stop()
            } finally {
                pending.finish()
            }
        }
    }
}
