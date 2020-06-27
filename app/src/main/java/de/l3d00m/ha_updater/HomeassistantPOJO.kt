package de.l3d00m.ha_updater

import com.google.gson.annotations.SerializedName

object HomeassistantPOJO {
    data class EntityResponse(
        @SerializedName("entity_id")
        val entityId: String,
        val last_changed: String,
        val last_updated: String,
        val state: String
    )

    data class DatetimeServiceBody(
        @SerializedName("entity_id")
        val entityId: String,
        @SerializedName("datetime")
        val datetime: String
    )

    data class ApiResponse(
        val message: String
    )

}
