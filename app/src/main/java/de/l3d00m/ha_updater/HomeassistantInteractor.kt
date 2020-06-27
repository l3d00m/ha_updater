package de.l3d00m.ha_updater

import android.app.AlarmManager
import android.content.Context
import android.webkit.URLUtil
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.Exception
import java.lang.NullPointerException

class HomeassistantInteractor(private val context: Context) {
    private var repository: HomeassistantRepository? = null

    suspend fun pushNewAlarm(): String {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val baseUrl = sharedPreferences.getString(KeyConstants(context.resources).HA_URL_KEY, "")
        if (!URLUtil.isValidUrl(baseUrl)) {
            throw Exception("Invalid URL provided, it was: $baseUrl")
        }

        val authTokenKey = context.resources.getString(R.string.HA_API_TOKEN)
        val authToken = sharedPreferences.getString(authTokenKey, "")!!
        // Null cast because URLUtil returns false if URL is null
        repository = HomeassistantRepository(baseUrl!!, authToken.trim())

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