package com.synapseslab.bluegpssdkdemo.utils

import android.util.Base64
import org.json.JSONObject

fun getClaimRaw(jwt: String, key: String): Any? {
    return try {
        val parts = jwt.split(".")
        if (parts.size < 2) return null

        val payload = parts[1]
        val decodedBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP)
        val decodedString = String(decodedBytes, Charsets.UTF_8)

        val json = JSONObject(decodedString)
        if (json.has(key)) json.get(key) else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}