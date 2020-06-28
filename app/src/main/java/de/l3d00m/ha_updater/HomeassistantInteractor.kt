package de.l3d00m.ha_updater

import android.app.AlarmManager
import android.content.Context
import android.webkit.URLUtil
import timber.log.Timber
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
        if (authToken.isBlank()) throw NullPointerException("No API token provided")
        // Null cast because URLUtil returns false if URL is null
        HomeassistantRepository(baseUrl, authToken)
    }

    suspend fun pushNewAlarm(): String {
        val entityId = Prefs(context).entityId
        if (entityId.isBlank()) throw Exception("No entity ID specified")
        val entityResponse = repository?.getEntityStatus(entityId)

        val timeString = convertDatetimeToString(getNextAlarmMs())

        val serviceResponse = repository?.putState(entityId, timeString) ?: throw Exception("Received unexpected response from HA service API (was null)")
        val newState: String? = serviceResponse.elementAtOrNull(0)?.state
        if (newState == null) Timber.i("Entity was already set to the same alarm")
        return newState ?: timeString
    }

    suspend fun getApiStatus(): String {
        val response = repository?.getApiStatus() ?: throw Exception("Received unexpected API status response (was null)")
        return response.message ?: ""
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