package de.l3d00m.ha_updater

import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat


class MainSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_fragment, rootKey)

        // Set hint for EditText here because it doesn't work in XML with androidx preferences
        val apiUrlEditText: EditTextPreference? = findPreference(KeyConstants(resources).HA_URL_KEY)
        apiUrlEditText?.setOnBindEditTextListener { editText ->
            editText.hint = "http://ip:8123/"
        }
        val alarmEntityEditText: EditTextPreference? =
            findPreference(KeyConstants(resources).ENTITIY_ID_KEY)
        alarmEntityEditText?.setOnBindEditTextListener { editText ->
            editText.hint = "sensor.next_alarm_clock"
        }

        // Mask EditText as password here because it doesn't work in XML with androidx preferences
        val accessTokenEditText: EditTextPreference? =
            findPreference(KeyConstants(resources).API_TOKEN_KEY)
        accessTokenEditText?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_TEXT//fixme or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
    }
}
