package de.l3d00m.ha_updater

import android.content.Context
import android.os.Bundle
import android.view.View
import android.webkit.URLUtil
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber
import java.util.*


class MainSettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_fragment, rootKey)

        val apiUrlEditText: EditTextPreference? = findPreference(resources.getString(R.string.HA_URL))
        val alarmEntityEditText: EditTextPreference? = findPreference(resources.getString(R.string.ALARM_ENTITY_ID))
        val tokenEditText: EditTextPreference? = findPreference(resources.getString(R.string.HA_API_TOKEN))
        val alarmEnabledSwitch: SwitchPreferenceCompat? = findPreference(resources.getString(R.string.ENABLE_PUSH_ALARM))
        val alarmInfoField: Preference? = findPreference(resources.getString(R.string.ALARM_SYNC_STATE))

        // Set hint for EditText here because it doesn't work in XML with androidx preferences
        apiUrlEditText?.setOnBindEditTextListener { editText -> editText.hint = "e.g. http://192.168.0.100:8123" }
        alarmEntityEditText?.setOnBindEditTextListener { editText -> editText.hint = "e.g. input_datetime.next_alarm" }

        apiUrlEditText?.onPreferenceChangeListener = this
        tokenEditText?.onPreferenceChangeListener = this
        alarmEntityEditText?.onPreferenceChangeListener = this
        alarmEnabledSwitch?.onPreferenceChangeListener = this
        alarmInfoField?.onPreferenceClickListener = this
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = context ?: return
        val prefs = Prefs(context)
        updateConnectionStatus(context)
        updateAlarmClockFields(prefs)
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        // If context should be null for any reason, just return true here
        val context = context ?: return true
        val prefs = Prefs(context)
        when (preference?.key) {
            resources.getString(R.string.HA_URL) -> {
                val url = newValue as? String ?: ""
                if (url.isNotEmpty() && !URLUtil.isValidUrl(url)) {
                    Toast.makeText(context, "Not saved - please enter a valid URL", Toast.LENGTH_LONG).show()
                    return false
                }
                updateConnectionStatus(context, url = url)
                updateAlarmClockFields(prefs, newEnabledState = isAllFieldsFilled(prefs, url = url))
            }
            resources.getString(R.string.HA_API_TOKEN) -> {
                val token = newValue as? String ?: ""
                updateConnectionStatus(context, token = token)
                updateAlarmClockFields(prefs, newEnabledState = isAllFieldsFilled(prefs, token = token))
            }
            resources.getString(R.string.ALARM_ENTITY_ID) -> {
                val entityId = newValue as? String ?: ""
                if (entityId.isNotEmpty() && !entityId.toLowerCase(Locale.ENGLISH).startsWith("input_datetime.")) {
                    Toast.makeText(context, "Not saved - entity has to be of type input_datetime", Toast.LENGTH_LONG).show()
                    return false
                }
                updateAlarmClockFields(prefs, newEnabledState = isAllFieldsFilled(prefs, entityId = entityId))
            }
            resources.getString(R.string.ENABLE_PUSH_ALARM) -> {
                val enabled = newValue as? Boolean ?: false
                updateAlarmClockFields(prefs, newCheckedState = enabled)
            }
        }
        return true
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        val context = context ?: return true
        updateAlarmSync(context)
        return true
    }

    private fun updateAlarmClockFields(prefs: Prefs, newEnabledState: Boolean? = null, newCheckedState: Boolean? = null) {
        val enabledState = newEnabledState ?: isAllFieldsFilled(prefs)
        val checkedState = newCheckedState ?: prefs.syncingEnabledUser
        val enableSwitch: SwitchPreferenceCompat = findPreference(resources.getString(R.string.ENABLE_PUSH_ALARM)) ?: return
        enableSwitch.isEnabled = enabledState
        prefs.syncingActive = enabledState && checkedState

        val syncStatus: Preference = findPreference(resources.getString(R.string.ALARM_SYNC_STATE)) ?: return
        if (!enabledState) {
            syncStatus.summary = "Disabled: Please fill all fields above"
        } else if (!checkedState) {
            syncStatus.summary = "Disabled by user"
        } else {
            syncStatus.summary = "Enabled"
        }


    }

    private fun isAllFieldsFilled(prefs: Prefs, url: String? = null, token: String? = null, entityId: String? = null): Boolean {
        val newUrl = url ?: prefs.homeassistantUrl
        val newToken = token ?: prefs.apiToken
        val newId = entityId ?: prefs.entityId
        return newUrl.isNotEmpty() && newToken.isNotEmpty() && newId.isNotEmpty()
    }

    private fun updateConnectionStatus(context: Context, url: String? = null, token: String? = null) {
        val prefs = Prefs(context)
        val baseUrl = url ?: prefs.homeassistantUrl
        val authToken = token ?: prefs.apiToken
        val connectionState: Preference? = findPreference(resources.getString(R.string.API_CONNECTION_STATE))
        if (authToken.isEmpty()) {
            connectionState?.summary = "Not connected - no access token provided"
            return
        }
        if (!URLUtil.isValidUrl(baseUrl)) {
            connectionState?.summary = "Not connected - no (valid) URL provided"
            return
        }
        connectionState?.summary = "Connecting..."

        val interactor = HomeassistantInteractor(context.applicationContext, token = authToken, url = baseUrl)
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
            Timber.w("Getting API status failed with $exception")
            when (exception) {
                is HttpException -> connectionState?.summary = "Connection failed - ${exception.code()} ${exception.message()}"
                else -> connectionState?.summary = "Connection failed with $exception"
            }
            Timber.w("Catched exception: $exception")
        }
        viewLifecycleOwner.lifecycleScope.launch(coroutineExceptionHandler) {
            val message = interactor.getApiStatus()
            connectionState?.summary = "Connected - $message"
        }
    }

    private fun updateAlarmSync(context: Context) {
        val alarmInfoField: Preference? = findPreference(resources.getString(R.string.ALARM_SYNC_STATE))
        val interactor = HomeassistantInteractor(context.applicationContext)

        val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
            Timber.w("Pushing alarm failed with $exception")
            when (exception) {
                is HttpException -> alarmInfoField?.summary = "Enabled - Last push failed with HTTP error ${exception.code()} ${exception.message()}"
                else -> alarmInfoField?.summary = "Enabled - Last push failed with $exception"
            }
        }
        viewLifecycleOwner.lifecycleScope.launch(coroutineExceptionHandler) {
            val state = interactor.pushNewAlarm()
            alarmInfoField?.summary = "Enabled - Last pushed alarm was $state"
        }
    }
}
