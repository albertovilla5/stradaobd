package com.torqueobd2.dash.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.torqueobd2.dash.MainActivity
import com.torqueobd2.dash.TorqueApplication
import com.torqueobd2.dash.data.model.ConnectionType
import com.torqueobd2.dash.i18n.AppI18n
import com.torqueobd2.dash.i18n.tr
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** connectedDevice mantiene el enlace OBD; no reinicia grabaciones tras morir el proceso. */
class TelemetryForegroundService : Service() {
    private val runtime get() = (application as TorqueApplication).obdRuntime
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var started = false
    private var lastWakeRefresh = 0L

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    tr("Lectura OBD y grabación", "OBD reading and recording"),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = tr(
                        "Control de las sesiones de lectura de sensores en segundo plano",
                        "Control of background sensor reading sessions"
                    )
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                }
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!started) {
            try {
                ServiceCompat.startForeground(this, NOTIFICATION, notification(),
                    if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0)
                started = true
                runtime.onServiceStarted()
                scope.launch {
                    combine(
                        runtime.recording.map { it.active?.id }.distinctUntilChanged(),
                        runtime.manager.connectionState.map { Triple(it.type, it.status, it.statusMessage) }.distinctUntilChanged(),
                        AppI18n.languageFlow
                    ) { _, _, _ -> Unit }
                        .collect {
                            ensureChannel()
                            getSystemService(NotificationManager::class.java).notify(NOTIFICATION, notification())
                        }
                }
                scope.launch {
                    while (isActive) {
                        updateLocks()
                        getSystemService(NotificationManager::class.java).notify(NOTIFICATION, notification())
                        delay(5000)
                    }
                }
            } catch (e: RuntimeException) {
                runtime.manager.logDiagnostic(
                    tr("No se pudo iniciar el servicio OBD: ${e.message}", "Could not start OBD service: ${e.message}")
                )
                stopSelf()
                return START_NOT_STICKY
            }
        }
        when (intent?.action) {
            STOP_RECORDING -> runtime.stopRecording()
            STOP_ALL -> runtime.stopEverything()
        }
        return START_NOT_STICKY
    }

    private fun notification(): Notification {
        val connection = runtime.manager.connectionState.value
        val session = runtime.recording.value.active
        val simulated = connection.type == ConnectionType.SIMULATION
        val title = when {
            session != null && simulated -> tr("Grabando · SIMULACIÓN", "Recording · SIMULATION")
            session != null -> tr("Grabando sensores OBD", "Recording OBD sensors")
            simulated -> tr("Lectura en segundo plano · SIMULACIÓN", "Background reading · SIMULATION")
            else -> tr("Lectura OBD en segundo plano", "Background OBD reading")
        }
        val text = if (session != null) {
            tr(
                "${session.context.vehicleName} · ${session.sampleCount} muestras · ${connection.statusMessage}",
                "${session.context.vehicleName} · ${session.sampleCount} samples · ${connection.statusMessage}"
            )
        } else {
            "${runtime.manager.vehicleProfile.value.name} · ${connection.statusMessage}"
        }
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(com.torqueobd2.dash.R.drawable.ic_obd_notification)
            .setContentTitle(title).setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open).setShowWhen(false).setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .apply {
                if (session != null) addAction(0, tr("Detener grabación", "Stop recording"), action(STOP_RECORDING, 1))
                addAction(0, tr("Detener lectura", "Stop reading"), action(STOP_ALL, 2))
            }.build()
    }

    private fun action(action: String, code: Int) = PendingIntent.getService(this, code,
        Intent(this, TelemetryForegroundService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    @Suppress("DEPRECATION")
    private fun updateLocks() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (wakeLock == null) wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StradaOBD:telemetry").apply { setReferenceCounted(false) }
        // Plazo acotado, renovado mientras el servicio sigue vivo.
        if (wakeLock?.isHeld != true || now - lastWakeRefresh >= 300_000) {
            wakeLock?.acquire(600_000)
            lastWakeRefresh = now
        }
        val wifi = runtime.manager.connectionState.value.type == ConnectionType.WIFI
        if (wifi && wifiLock == null) wifiLock = (applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)
            ?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "StradaOBD:wifi")?.apply { setReferenceCounted(false) }
        if (wifi && wifiLock?.isHeld != true) wifiLock?.acquire()
        if (!wifi && wifiLock?.isHeld == true) wifiLock?.release()
    }

    override fun onDestroy() {
        scope.cancel()
        if (wakeLock?.isHeld == true) wakeLock?.release()
        if (wifiLock?.isHeld == true) wifiLock?.release()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        runtime.onServiceDestroyed()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "obd_telemetry"
        private const val NOTIFICATION = 42
        private const val STOP_RECORDING = "com.stradaobd.dash.STOP_RECORDING"
        private const val STOP_ALL = "com.stradaobd.dash.STOP_ALL"
    }
}

