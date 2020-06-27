package de.l3d00m.ha_updater

import android.app.AlarmManager
import android.content.Context
import android.webkit.URLUtil
import androidx.preference.PreferenceManager
import timber.log.Timber
import java.lang.Exception
import java.lang.NullPointerException

class HomeassistantInteractor(private val context: Context) {
    private var repository: HomeassistantRepository? = null

    suspend fun pushNewAlarm(): String {
        val prefs = Prefs(context)
        val baseUrl = prefs.homeassistantUrl
        if (!URLUtil.isValidUrl(baseUrl)) {
            throw Exception("Invalid URL provided, it was: $baseUrl")
        }

        val authToken = prefs.apiToken ?: throw NullPointerException("No API token provided")
        // Null cast because URLUtil returns false if URL is null
        repository = HomeassistantRepository(baseUrl!!, authToken)

        val response = repository?.putState(getNextAlarmMs(), context)
        val entityId: String = response?.elementAtOrNull(0)?.entityId ?: throw NullPointerException("Received wrong response from HA")
        Timber.i(entityId)
        return entityId
    }

    private fun getNextAlarmMs(): Long {
        val alarmManager: AlarmManager? =
            context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val clockInfo: AlarmManager.AlarmClockInfo? = alarmManager?.nextAlarmClock
        return clockInfo?.triggerTime ?: 0
    }

}