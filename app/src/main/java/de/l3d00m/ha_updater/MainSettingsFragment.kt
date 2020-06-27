package de.l3d00m.ha_updater

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber


class MainSettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_fragment, rootKey)

        // Set hint for EditText here because it doesn't work in XML with androidx preferences
        val apiUrlEditText: EditTextPreference? = findPreference(KeyConstants(resources).HA_URL_KEY)
        apiUrlEditText?.setOnBindEditTextListener { editText -> editText.hint = "http://192.168.0.100:8123/" }

        val alarmEntityEditText: EditTextPreference? = findPreference(KeyConstants(resources).ENTITIY_ID_KEY)
        alarmEntityEditText?.setOnBindEditTextListener { editText -> editText.hint = "input_datetime.next_alarm_clock" }

        val tokenEditText: EditTextPreference? = findPreference(KeyConstants(resources).API_TOKEN_KEY)

        apiUrlEditText?.onPreferenceChangeListener = this
        tokenEditText?.onPreferenceChangeListener = this
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val token = PreferenceManager.getDefaultSharedPreferences(context).getString(KeyConstants(resources).API_TOKEN_KEY, null)
        val url = PreferenceManager.getDefaultSharedPreferences(context).getString(KeyConstants(resources).HA_URL_KEY, null)
        updateConnectionStatus(url, token)
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        val url: String?
        val token: String?
        when (preference?.key) {
            KeyConstants(resources).HA_URL_KEY -> {
                token = PreferenceManager.getDefaultSharedPreferences(context).getString(KeyConstants(resources).API_TOKEN_KEY, null)
                url = newValue as? String
            }
            KeyConstants(resources).API_TOKEN_KEY -> {
                url = PreferenceManager.getDefaultSharedPreferences(context).getString(KeyConstants(resources).HA_URL_KEY, null)
                token = newValue as? String
            }
            else -> return true
        }
        updateConnectionStatus(url, token)
        return true
    }

    private fun updateConnectionStatus(url: String?, token: String?) {
        val connectionState: Preference? = findPreference(KeyConstants(resources).CONNECTION_STATE)
        if (token == null) {
            connectionState?.summary = "Not connected - no access token provided"
            return
        }
        if (!URLUtil.isValidUrl(url)) {
            connectionState?.summary = "Not connected - no (valid) URL provided"
            return
        }
        connectionState?.summary = "Connecting..."

        // Force Url not null because URLUtil would have complained if null
        val repository = HomeassistantRepository(url!!, token.trim())
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
