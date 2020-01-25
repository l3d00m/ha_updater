package de.l3d00m.ha_updater

import android.app.AlarmManager
import android.content.Context
import android.webkit.URLUtil
import androidx.preference.PreferenceManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

class HomeassistantInteractor(private val context: Context) {
    fun pushNewAlarm() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val baseUrl = sharedPreferences.getString(KeyConstants(context.resources).HA_URL_KEY, "")
        if (!URLUtil.isValidUrl(baseUrl)) {
            Timber.i("URL not valid. Was: $baseUrl")
            return
        }
        // Null cast because URLUtil returns false if URL is null
        val repository = HomeassistantRepository(baseUrl!!)

        GlobalScope.launch { repository.putState(getNextAlarmSeconds(), context) }
    }

    private fun getNextAlarmSeconds(): Long {
        val alarmManager: AlarmManager? =
            context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val clockInfo: AlarmManager.AlarmClockInfo? = alarmManager?.nextAlarmClock
        val timeMs = clockInfo?.triggerTime ?: 0
        return timeMs / 1000
    }

}