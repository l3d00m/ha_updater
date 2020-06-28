package de.l3d00m.ha_updater

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.*
import timber.log.Timber

class AlarmClockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
            return // Wrong action, return
        val enabled = Prefs(context).syncingActive
        if (!enabled) {
            Timber.i("Updates to HA disabled")
            return
        }
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
            Timber.w("Pushing new alarm date failed with $exception")
        }
        GlobalScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val newState = HomeassistantInteractor(context.applicationContext).pushNewAlarm()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Updated alarm clock time in Homeassistant ($newState)", Toast.LENGTH_LONG).show()
            }
        }

    }

}