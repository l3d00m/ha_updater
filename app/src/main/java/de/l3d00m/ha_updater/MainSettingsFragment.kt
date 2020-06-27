package de.l3d00m.ha_updater

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


class MainSettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_fragment, rootKey)

        // Set hint for EditText here because it doesn't work in XML with androidx preferences
        val apiUrlEditText: EditTextPreference? = findPreference(resources.getString(R.string.HA_URL))
        apiUrlEditText?.setOnBindEditTextListener { editText -> editText.hint = "http://192.168.0.100:8123/" }

        val alarmEntityEditText: EditTextPreference? = findPreference(resources.getString(R.string.ALARM_ENTITY_ID))
        alarmEntityEditText?.setOnBindEditTextListener { editText -> editText.hint = "input_datetime.next_alarm_clock" }

        val tokenEditText: EditTextPreference? = findPreference(resources.getString(R.string.HA_API_TOKEN))

        apiUrlEditText?.onPreferenceChangeListener = this
        tokenEditText?.onPreferenceChangeListener = this
        alarmEntityEditText?.onPreferenceChangeListener = this
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = Prefs(requireContext())
        updateConnectionStatus(prefs.homeassistantUrl, prefs.apiToken)
        setEnableSwitchState(prefs.homeassistantUrl, prefs.apiToken, prefs.entityId)
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        val prefs = Prefs(requireContext())
        var url = prefs.homeassistantUrl
        var token = prefs.apiToken
        var entityId = prefs.entityId

        when (preference?.key) {
            resources.getString(R.string.HA_URL) -> {
                url = newValue as? String ?: ""
                updateConnectionStatus(url, token)
            }
            resources.getString(R.string.HA_API_TOKEN) -> {
                token = newValue as? String ?: ""
                updateConnectionStatus(url, token)
            }
            resources.getString(R.string.ALARM_ENTITY_ID) -> {
                entityId = newValue as? String ?: ""
                if (entityId.isNotEmpty() && !entityId.startsWith("input_datetime.")) {
                    Toast.makeText(context, "Not saved - entity has to be of type input_datetime", Toast.LENGTH_LONG).show()
                    return false
                }
            }
        }
        setEnableSwitchState(url, token, entityId)

        return true
    }

    private fun setEnableSwitchState(url: String, token: String, entityId: String) {
        val enableSwitch: SwitchPreferenceCompat? = findPreference(resources.getString(R.string.ENABLE_PUSH_ALARM))!!
        val shouldBeEnabled = url.isNotEmpty() && token.isNotEmpty() && entityId.isNotEmpty()
        enableSwitch?.isChecked = if (shouldBeEnabled)
            enableSwitch!!.isChecked
        else
            false
        enableSwitch?.isEnabled = shouldBeEnabled


    }

    private fun updateConnectionStatus(url: String, token: String) {
        val connectionState: Preference? = findPreference(resources.getString(R.string.CONNECTION_STATE))
        if (token.isEmpty()) {
            connectionState?.summary = "Not connected - no access token provided"
            return
        }
        if (!URLUtil.isValidUrl(url)) {
            connectionState?.summary = "Not connected - no (valid) URL provided"
            return
        }
        connectionState?.summary = "Connecting..."

        val repository = HomeassistantRepository(url, token.trim())
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
            val message = repository.getStatus()?.message
            connectionState?.summary = "Connected - $message"
        }
    }
}
