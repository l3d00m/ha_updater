package de.l3d00m.ha_updater

import com.google.gson.annotations.SerializedName

object HomeassistantPOJO {
    data class EntityResponse(
        @SerializedName("entity_id")
        val entityId: String?,
        @SerializedName("last_changed")
        val lastChanged: String?,
        @SerializedName("last_updated")
        val lastUpdated: String?,
        val state: String?,
        val message: String?
    )

    data class DatetimeServiceBody(
        @SerializedName("entity_id")
        val entityId: String?,
        val datetime: String?
    )

    data class ApiResponse(
        val message: String?
    )

}
