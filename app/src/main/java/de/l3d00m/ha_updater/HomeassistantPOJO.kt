package de.l3d00m.ha_updater

import com.google.gson.annotations.SerializedName

object HomeassistantPOJO {
    data class EntityResponse(
        val entity_id: String,
        val last_changed: String,
        val last_updated: String,
        val state: String
    )

    data class Entity(
        @SerializedName("state")
        val newState: Long
    )

    data class ApiResponse(
        val message: String
    )

}
