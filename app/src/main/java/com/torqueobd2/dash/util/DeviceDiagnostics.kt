package com.torqueobd2.dash.util

import android.content.Context
import android.os.Build

object DeviceDiagnostics {
    fun describe(context: Context): String {
        val version = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
        } catch (_: Exception) {
            "?"
        }
        return "App $version · ${Build.MANUFACTURER} ${Build.MODEL}\n" +
            "Android ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT} · " +
            Build.SUPPORTED_ABIS.joinToString()
    }
}
