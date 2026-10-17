package com.arnavgames.cloakrun.core.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.arnavgames.cloakrun.core.game.location.TrackingMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Keeps match tracking alive with the screen off. It is visible (a persistent notification with
 * a Stop action), runs only during an active match, and is always started while the app is in
 * the foreground, so background location permission is never needed.
 */
@AndroidEntryPoint
class MatchTrackingService : LifecycleService() {
    @Inject lateinit var repository: LocationRepository

    @Inject lateinit var controller: TrackingController

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            controller.stop()
            return START_NOT_STICKY
        }
        startInForeground()
        repository.request(KEY, TrackingMode.Match)
        lifecycleScope.launch { repository.fixes.collect { } }
        controller.markRunning(true)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        repository.release(KEY)
        controller.markRunning(false)
        super.onDestroy()
    }

    private fun startInForeground() {
        ensureChannel(this)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
    }

    private fun buildNotification(): Notification {
        val open = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, MatchTrackingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_cloakrun)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(getString(R.string.tracking_notification_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_LOCATION_SHARING)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(open)
            .addAction(0, getString(R.string.tracking_notification_stop), stop)
            .build()
    }

    companion object {
        private const val KEY = "match-service"
        private const val CHANNEL_ID = "match_tracking"
        private const val NOTIFICATION_ID = 0x0C10A
        private const val ACTION_STOP = "com.arnavgames.cloakrun.action.STOP_TRACKING"

        internal fun ensureChannel(context: Context) {
            val manager = context.getSystemService<NotificationManager>() ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.tracking_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.tracking_channel_description) }
            manager.createNotificationChannel(channel)
        }
    }
}

/** Starts and stops match tracking and tells the UI whether location is being shared right now. */
@Singleton
class TrackingController @Inject constructor() {
    private val running = MutableStateFlow(false)

    /** True while the match tracking service is running. Drives the in-app sharing indicator. */
    val isTracking: StateFlow<Boolean> = running.asStateFlow()

    private var appContext: Context? = null

    fun start(context: Context) {
        appContext = context.applicationContext
        ContextCompat.startForegroundService(context, Intent(context, MatchTrackingService::class.java))
    }

    fun stop() {
        appContext?.let { it.stopService(Intent(it, MatchTrackingService::class.java)) }
        running.value = false
    }

    internal fun markRunning(value: Boolean) {
        running.value = value
    }
}
