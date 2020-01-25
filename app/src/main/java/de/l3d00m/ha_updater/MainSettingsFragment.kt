package de.l3d00m.ha_updater

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat


class MainSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_fragment, rootKey)
        // Set hint for EditText here because it doesn't work in XML
        val apiUrlEditText: EditTextPreference? = preferenceManager.findPreference("ha_api_url")
        apiUrlEditText?.setOnBindEditTextListener { editText ->
            editText.hint = "http://ip:8123/api/"
        }

        val alarmEntityEditTeyt: EditTextPreference? =
            preferenceManager.findPreference("alarm_entity_id")
        alarmEntityEditTeyt?.setOnBindEditTextListener { editText ->
            editText.hint = "sensor.next_alarm_clock"
        }
    }
}
