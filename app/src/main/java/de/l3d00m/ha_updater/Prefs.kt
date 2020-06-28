package de.l3d00m.ha_updater

import android.content.Context
import de.l3d00m.ha_updater.PreferenceHelper.defaultPrefs
import de.l3d00m.ha_updater.PreferenceHelper.get
import de.l3d00m.ha_updater.PreferenceHelper.set
import de.l3d00m.ha_updater.ResourceHelper.strings

class Prefs(context: Context) {
    // Initialize shared preferences object (with applicationContext to avoid leaks)
    private val prefs = defaultPrefs(context.applicationContext)

    // Values that are defined through the preference fragment, so they are RO
    val apiToken: String = prefs[context.strings[R.string.HA_API_TOKEN], ""]!!
    val homeassistantUrl: String = prefs[context.strings[R.string.HA_URL], ""]!!
    val entityId: String = prefs[context.strings[R.string.ALARM_ENTITY_ID], ""]!!
    val updatesEnabled: Boolean = prefs[context.strings[R.string.ENABLE_PUSH_ALARM], false]!!

    // Manually defined values that are not defined through the Preference fragment
    companion object {
        private const val DATETIME_TO_PUBLISH_KEY = "DATETIME_TO_PUBLISH"
        private const val RETRY_COUNTER_KEY = "RETRY_COUNTER"
    }

    var datetimeToPublish: Long
        get() = prefs[DATETIME_TO_PUBLISH_KEY, 0]!!
        set(value) = prefs.set(DATETIME_TO_PUBLISH_KEY, value)
    var retryCounter: Int
        get() = prefs[RETRY_COUNTER_KEY, 0]!!
        set(value) = prefs.set(RETRY_COUNTER_KEY, value)
}

