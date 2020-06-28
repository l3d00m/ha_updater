package de.l3d00m.ha_updater

import android.content.Context
import android.content.SharedPreferences
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


class MainSettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener,
    SharedPreferences.OnSharedPreferenceChangeListener {
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
        tokenEditText?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            if (preference.text.isBlank()) "Not set"
            else "Token set"
        }


        apiUrlEditText?.onPreferenceChangeListener = this
        tokenEditText?.onPreferenceChangeListener = this
        alarmEntityEditText?.onPreferenceChangeListener = this
        alarmEnabledSwitch?.onPreferenceChangeListener = this
        alarmInfoField?.onPreferenceClickListener = this

        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = context ?: return
        val prefs = Prefs(context)
        updateApiStatus(context)
        updateAlarmClockFields(prefs)
    }

    override fun onDestroy() {
        super.onDestroy()
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        // If context should be null for any reason, just return true here
        val context = context ?: return true
        when (preference?.key) {
            resources.getString(R.string.HA_URL) -> {
                val url = newValue as? String ?: ""
                if (url.isNotEmpty() && !URLUtil.isValidUrl(url)) {
                    Toast.makeText(context, "Not saved - please enter a valid URL", Toast.LENGTH_LONG).show()
                    return false
                }
            }
            resources.getString(R.string.ALARM_ENTITY_ID) -> {
                val entityId = newValue as? String ?: ""
                if (entityId.isNotEmpty() && !entityId.toLowerCase(Locale.ENGLISH).startsWith("input_datetime.")) {
                    Toast.makeText(context, "Not saved - entity has to be of type input_datetime", Toast.LENGTH_LONG).show()
                    return false
                }
            }
        }
        return true
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        val context = context ?: return true
        if (isAllFieldsFilled(Prefs(context)))
            syncAlarm(context)
        else
            Toast.makeText(context, "Please fill all fields first", Toast.LENGTH_SHORT).show()
        return true
    }

    private fun updateAlarmClockFields(prefs: Prefs): Boolean {
        val enabledState = isAllFieldsFilled(prefs)
        val checkedState = prefs.syncingEnabledUser
        val enableSwitch: SwitchPreferenceCompat = findPreference(resources.getString(R.string.ENABLE_PUSH_ALARM)) ?: return false
        enableSwitch.isEnabled = enabledState
        prefs.syncingActive = enabledState && checkedState

        val syncStatus: Preference = findPreference(resources.getString(R.string.ALARM_SYNC_STATE)) ?: return false
        if (!enabledState) {
            syncStatus.summary = "Disabled: Please fill all fields above"
        } else if (!checkedState) {
            syncStatus.summary = "Disabled by user"
        } else {
            syncStatus.summary = "Enabled"
            return true
        }
        return false
    }

    private fun isAllFieldsFilled(prefs: Prefs): Boolean {
        val newUrl = prefs.homeassistantUrl
        val newToken = prefs.apiToken
        val newId = prefs.entityId
        return newUrl.isNotEmpty() && newToken.isNotEmpty() && newId.isNotEmpty()
    }

    private fun updateApiStatus(context: Context) {
        val prefs = Prefs(context)
        val baseUrl = prefs.homeassistantUrl
        val authToken = prefs.apiToken
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

    private fun syncAlarm(context: Context) {
        val alarmInfoField: Preference? = findPreference(resources.getString(R.string.ALARM_SYNC_STATE))
        val interactor = HomeassistantInteractor(context.applicationContext)
        val prefs = Prefs(context)
        val statusPrefix = if (prefs.syncingActive) "Enabled"
        else "Disabled"

        val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
            Timber.w("Pushing alarm failed with $exception")
            when (exception) {
                is HttpException -> alarmInfoField?.summary = "$statusPrefix - Last push failed with HTTP error ${exception.code()} ${exception.message()}"
                else -> alarmInfoField?.summary = "$statusPrefix - Last push failed with $exception"
            }
        }
        viewLifecycleOwner.lifecycleScope.launch(coroutineExceptionHandler) {
            val state = interactor.pushNewAlarm()
            alarmInfoField?.summary = "$statusPrefix - Last pushed alarm is $state"
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Timber.i("Preference changed key: $key")
        // If context should be null for any reason, just return true here
        val context = context ?: return
        val prefs = Prefs(context)
        val validKeys = listOf(
            resources.getString(R.string.HA_URL),
            resources.getString(R.string.HA_API_TOKEN),
            resources.getString(R.string.ALARM_ENTITY_ID),
            resources.getString(R.string.ENABLE_PUSH_ALARM)
        )
        if (key?.toLowerCase(Locale.ENGLISH) in validKeys) {
            if (key?.toLowerCase(Locale.ENGLISH) in listOf(resources.getString(R.string.HA_URL), resources.getString(R.string.HA_API_TOKEN)))
                updateApiStatus(context)

            updateAlarmClockFields(prefs)
            // If syncing is active, sync the alarm (again) after a preference has changed
            if (prefs.syncingActive) syncAlarm(context)
        }
    }
}
