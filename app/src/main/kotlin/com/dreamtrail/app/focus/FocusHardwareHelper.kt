package com.dreamtrail.app.focus

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs

/**
 * Hardware convenience helpers for daytime focus sessions:
 * 1) Optional accelerometer face-down flip-to-focus detector (no special permissions).
 * 2) Safe completion notification helper (checks POST_NOTIFICATIONS on Android 13+).
 */
class FocusHardwareHelper(
    context: Context,
    private val onFaceDownChanged: (Boolean) -> Unit
) : SensorEventListener {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var activeListening = false
    private var lastFaceDown = false
    private var stableSamples = 0

    fun setListening(enabled: Boolean) {
        if (enabled && accelerometer != null && !activeListening) {
            runCatching {
                sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
                activeListening = true
            }
        } else if (!enabled && activeListening) {
            stopListening()
        }
    }

    fun stopListening() {
        if (activeListening) {
            runCatching { sensorManager?.unregisterListener(this) }
            activeListening = false
        }
        stableSamples = 0
        if (lastFaceDown) {
            lastFaceDown = false
            onFaceDownChanged(false)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val vals = event?.values ?: return
        if (vals.size < 3) return
        val x = vals[0]
        val y = vals[1]
        val z = vals[2]
        val candidate = z < -7.5f && abs(x) < 3.5f && abs(y) < 3.5f
        if (candidate != lastFaceDown) {
            stableSamples++
            if (stableSamples >= 3) {
                lastFaceDown = candidate
                stableSamples = 0
                onFaceDownChanged(candidate)
            }
        } else {
            stableSamples = 0
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val CHANNEL_ID = "solunatrail_timer"
        private const val NOTIFICATION_ID = 4001

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
                if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "昼梦途计时提醒",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "番茄定心或午后小憩倒计时结束时提醒收队或茶歇"
                    }
                    nm.createNotificationChannel(channel)
                }
            }
        }

        fun notifyCountdownComplete(context: Context, title: String, body: String) {
            ensureChannel(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }
            runCatching {
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            }
        }
    }
}
