package de.l3d00m.ha_updater

import android.content.res.Resources

class KeyConstants(resources: Resources) {
    val API_TOKEN_KEY = resources.getString(R.string.HA_API_TOKEN)
    val HA_URL_KEY = resources.getString(R.string.HA_URL)
    val ENTITIY_ID_KEY = resources.getString(R.string.ALARM_ENTITY_ID)
    val CONNECTION_STATE = resources.getString(R.string.CONNECTION_STATE)
}