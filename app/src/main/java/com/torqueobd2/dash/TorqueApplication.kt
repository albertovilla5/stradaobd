package com.torqueobd2.dash

import android.app.Application
import android.content.res.Configuration
import com.torqueobd2.dash.i18n.AppI18n
import com.torqueobd2.dash.service.ObdRuntime

class TorqueApplication : Application() {
    val obdRuntime by lazy { ObdRuntime(this) }

    override fun onCreate() {
        super.onCreate()
        AppI18n.syncWithContext(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AppI18n.syncWithContext(this)
    }
}
