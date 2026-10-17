package com.arnavgames.cloakrun.core.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Battery and power-saver state, which the sampling policy uses to save power. */
data class PowerState(
    val batteryPercent: Int,
    val charging: Boolean,
    val powerSaveMode: Boolean,
)

@Singleton
class DeviceConditions @Inject constructor(@param:ApplicationContext private val context: Context) {
    val power: Flow<PowerState> = callbackFlow {
        val powerManager = context.getSystemService<PowerManager>()
        fun read(battery: Intent?): PowerState {
            val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            return PowerState(
                batteryPercent = if (level >= 0 && scale > 0) level * 100 / scale else 100,
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
                powerSaveMode = powerManager?.isPowerSaveMode == true,
            )
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val battery = if (intent.action == Intent.ACTION_BATTERY_CHANGED) intent else stickyBattery()
                trySend(read(battery))
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        // ACTION_BATTERY_CHANGED is sticky: registering returns the current value immediately.
        val sticky = ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        trySend(read(sticky))
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()

    private fun stickyBattery(): Intent? =
        ContextCompat.registerReceiver(context, null, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
}
