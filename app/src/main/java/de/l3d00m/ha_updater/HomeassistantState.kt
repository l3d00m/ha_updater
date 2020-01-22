package de.l3d00m.ha_updater

import com.google.gson.annotations.SerializedName

data class HomeassistantState(
    @SerializedName("state")
    val newState: Int
)