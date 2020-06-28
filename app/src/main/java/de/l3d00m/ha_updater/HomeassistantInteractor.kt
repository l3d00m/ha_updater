package de.l3d00m.ha_updater

import android.app.AlarmManager
import android.content.Context
import android.webkit.URLUtil
import androidx.preference.PreferenceManager
import timber.log.Timber
import java.lang.Exception
import java.lang.NullPointerException
import java.text.SimpleDateFormat
import java.util.*

class HomeassistantInteractor(private val context: Context, token: String? = null, url: String? = null) {
    private val repository: HomeassistantRepository? by lazy {
        val prefs = Prefs(context)
        val baseUrl = url ?: prefs.homeassistantUrl
        if (!URLUtil.isValidUrl(baseUrl)) {
            throw Exception("Invalid URL provided, it was: $baseUrl")
        }

        // Fetch token and remove whitespace
        var authToken = token ?: prefs.apiToken
        authToken = authToken.trim()
        if (authToken.isEmpty()) throw NullPointerException("No API token provided")
        // Null cast because URLUtil returns false if URL is null
        HomeassistantRepository(baseUrl, authToken)
    }

    suspend fun pushNewAlarm(): String {
        val entityId = Prefs(context).entityId
        if (entityId.isEmpty()) throw Exception("No entity ID specified")
        val timeString = convertDatetimeToString(getNextAlarmMs())

        val response = repository?.putState(entityId, timeString) ?: throw Exception("Received unexpected response from HA service API (was null)")
        val returnedEntityId: String = response.elementAtOrNull(0)?.entityId ?: throw NullPointerException("Received empty response from HA (wrong Entity ID)")
        Timber.i(returnedEntityId)
        return returnedEntityId
    }

    suspend fun getApiStatus(): String {
        val response = repository?.getApiStatus() ?: throw Exception("Received unexpected API status response (was null)")
        return response.message
    }

    private fun getNextAlarmMs(): Long {
        val alarmManager: AlarmManager? = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val clockInfo: AlarmManager.AlarmClockInfo? = alarmManager?.nextAlarmClock
        return clockInfo?.triggerTime ?: 0
    }

    private fun convertDatetimeToString(input: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        val netDate = Date(input)
        return sdf.format(netDate)
    }

}