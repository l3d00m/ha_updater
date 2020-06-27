package de.l3d00m.ha_updater

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import retrofit2.HttpException
import timber.log.Timber

class AlarmClockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
            return // Wrong action, return
        val enabled = Prefs(context).updatesEnabled
        if (!enabled) {
            Timber.i("Updates to HA disabled")
            return
        }
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
            when (exception) {
                is HttpException -> {
                    Timber.w("Pushing new alarm date failed with HTTP Error ${exception.code()}: ${exception.message()}")
                }
                else -> {
                    Timber.w("Pushing new alarm date failed with $exception")
                }
            }
        }
        GlobalScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val entityId = HomeassistantInteractor(context).pushNewAlarm()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Updated alarm clock time in Homeassistant ($entityId)", Toast.LENGTH_LONG).show()
            }
        }

    }

}