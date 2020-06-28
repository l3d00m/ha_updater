package de.l3d00m.ha_updater

import android.app.AlarmManager
import android.content.Context
import android.webkit.URLUtil
import de.l3d00m.ha_updater.ApiResult.HomeassistantResult
import de.l3d00m.ha_updater.ApiResult.ResultCode
import retrofit2.HttpException
import timber.log.Timber
import java.net.ConnectException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeoutException

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

    suspend fun pushNewAlarm(): HomeassistantResult {
        var code: ResultCode
        var message: String? = null
        val entityId = Prefs(context).entityId
        if (entityId.isBlank()) code = ResultCode.MISSING_FIELD
        try {
            val entityResponse = repository?.getEntityStatus(entityId)
            code = if (entityResponse == null) ResultCode.API_ERROR
            else ResultCode.SUCCESS
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    if (e.code() == 401 || e.code() == 403) {
                        code = ResultCode.INVALID_TOKEN
                    } else if (e.code() == 404) {
                        code = ResultCode.INVALID_ENTITY
                    } else {
                        message = "${e.code()} ${e.message()}"
                        code = ResultCode.NETWORK_ERROR
                    }
                }
                is TimeoutException -> {
                    code = ResultCode.NETWORK_ERROR
                }
                is ConnectException -> {
                    code = ResultCode.NETWORK_ERROR
                }
                else -> {
                    code = ResultCode.UNKNOWN_ERROR
                    message = "${e.message}"
                }
            }
        }


        val timeString = convertDatetimeToString(getNextAlarmMs())

        val serviceResponse = repository?.putState(entityId, timeString) ?: throw Exception("Received unexpected response from HA service API (was null)")
        val newState: String? = serviceResponse.elementAtOrNull(0)?.state
        if (newState == null) Timber.i("Entity was already set to the same alarm")
        return HomeassistantResult(code)
    }

    suspend fun getApiStatus(): HomeassistantResult {
        var code: ResultCode
        var message: String? = null
        try {
            val response = repository?.getApiStatus()
            code = if (response == null) ResultCode.API_ERROR
            else ResultCode.SUCCESS
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    if (e.code() == 401 || e.code() == 403) {
                        code = ResultCode.INVALID_TOKEN
                    } else {
                        message = "${e.code()} ${e.message()}"
                        code = ResultCode.NETWORK_ERROR
                    }
                }
                is TimeoutException -> {
                    code = ResultCode.NETWORK_ERROR
                }
                is ConnectException -> {
                    code = ResultCode.NETWORK_ERROR
                }
                else -> {
                    code = ResultCode.UNKNOWN_ERROR
                    message = "${e.message}"
                }
            }
        }
        return HomeassistantResult(result = code, message = message)
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