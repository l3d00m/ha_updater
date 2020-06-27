package de.l3d00m.ha_updater

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class Prefs(context: Context) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val apiToken = prefs.getString(context.resources.getString(R.string.HA_API_TOKEN), null)
    val homeassistantUrl = prefs.getString(context.resources.getString(R.string.HA_URL), null)
    val entityId = prefs.getString(context.resources.getString(R.string.ALARM_ENTITY_ID), null)
    val connectionState = prefs.getString(context.resources.getString(R.string.CONNECTION_STATE), null)
    val updatesEnabled = prefs.getBoolean(context.resources.getString(R.string.ENABLE_PUSH_ALARM), false)
}