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


class MainSettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_fragment, rootKey)

        // Set hint for EditText here because it doesn't work in XML with androidx preferences
        val apiUrlEditText: EditTextPreference? = findPreference(resources.getString(R.string.HA_URL))
        apiUrlEditText?.setOnBindEditTextListener { editText -> editText.hint = "e.g. http://192.168.0.100:8123" }

        val alarmEntityEditText: EditTextPreference? = findPreference(resources.getString(R.string.ALARM_ENTITY_ID))
        alarmEntityEditText?.setOnBindEditTextListener { editText -> editText.hint = "e.g. input_datetime.next_alarm" }

        val tokenEditText: EditTextPreference? = findPreference(resources.getString(R.string.HA_API_TOKEN))

        apiUrlEditText?.onPreferenceChangeListener = this
        tokenEditText?.onPreferenceChangeListener = this
        alarmEntityEditText?.onPreferenceChangeListener = this
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = context ?: return
        val prefs = Prefs(context)
        updateConnectionStatus(context)
        setEnableSwitchState(prefs)
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        // If context should be null for any reason, just return true here
        val context = context ?: return true
        val prefs = Prefs(context)
        when (preference?.key) {
            resources.getString(R.string.HA_URL) -> {
                val url = newValue as? String ?: ""
                updateConnectionStatus(context, url = url)
                setEnableSwitchState(prefs, url = url)
            }
            resources.getString(R.string.HA_API_TOKEN) -> {
                val token = newValue as? String ?: ""
                updateConnectionStatus(context, token = token)
                setEnableSwitchState(prefs, token = token)
            }
            resources.getString(R.string.ALARM_ENTITY_ID) -> {
                val entityId = newValue as? String ?: ""
                if (entityId.isNotEmpty() && !entityId.toLowerCase(Locale.ENGLISH).startsWith("input_datetime.")) {
                    Toast.makeText(context, "Not saved - entity has to be of type input_datetime", Toast.LENGTH_LONG).show()
                    return false
                }
                setEnableSwitchState(prefs, entityId = entityId)
            }
        }
        return true
    }

    private fun setEnableSwitchState(prefs: Prefs, url: String? = null, token: String? = null, entityId: String? = null) {
        val enableSwitch: SwitchPreferenceCompat? = findPreference(resources.getString(R.string.ENABLE_PUSH_ALARM))!!
        val newUrl = url ?: prefs.homeassistantUrl
        val newToken = token ?: prefs.apiToken
        val newId = entityId ?: prefs.entityId
        val shouldBeEnabled = newUrl.isNotEmpty() && newToken.isNotEmpty() && newId.isNotEmpty()
        enableSwitch?.isChecked = if (shouldBeEnabled)
            enableSwitch!!.isChecked
        else
            false
        enableSwitch?.isEnabled = shouldBeEnabled
    }

    private fun updateConnectionStatus(context: Context, url: String? = null, token: String? = null) {
        val prefs = Prefs(context)
        val baseUrl = url ?: prefs.homeassistantUrl
        val authToken = token ?: prefs.apiToken
        val connectionState: Preference? = findPreference(resources.getString(R.string.CONNECTION_STATE))
        if (authToken.isEmpty()) {
            connectionState?.summary = "Not connected - no access token provided"
            return
        }
        if (!URLUtil.isValidUrl(baseUrl)) {
            connectionState?.summary = "Not connected - no (valid) URL provided"
            return
        }
        connectionState?.summary = "Connecting..."

        val interactor = HomeassistantInteractor(context.applicationContext, token=authToken, url=baseUrl)
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
            when (exception) {
                is HttpException -> {
                    Timber.w("Getting API status failed with HTTP ${exception.code()}: ${exception.message()}")
                    connectionState?.summary = "Connection failed - ${exception.code()} ${exception.message()}"
                }
                else -> {
                    Timber.w("Getting API status failed with $exception")
                    connectionState?.summary = "Connection failed with $exception"
                }
            }
            Timber.w("Catched exception: $exception")
        }
        viewLifecycleOwner.lifecycleScope.launch(coroutineExceptionHandler) {
            val message = interactor.getApiStatus()
            connectionState?.summary = "Connected - $message"
        }
    }
}
