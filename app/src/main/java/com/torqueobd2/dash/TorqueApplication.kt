package com.torqueobd2.dash

import android.app.Application
import com.torqueobd2.dash.service.ObdRuntime

class TorqueApplication : Application() {
    val obdRuntime by lazy { ObdRuntime(this) }
}
