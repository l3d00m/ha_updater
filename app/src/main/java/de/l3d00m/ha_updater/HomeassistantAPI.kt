package de.l3d00m.ha_updater

import retrofit2.http.*


interface HomeassistantAPI {
    @POST("api/services/input_datetime/set_datetime")
    suspend fun updateEntity(
        @Body datetimeServiceBody: HomeassistantPOJO.DatetimeServiceBody
    ): List<HomeassistantPOJO.EntityResponse?>?

    @GET("api/")
    suspend fun getApiStatus(): HomeassistantPOJO.ApiResponse?

    @GET("api/states/{entity_id}")
    suspend fun getEntity(
        @Path("entity_id") entityId: String
    ): HomeassistantPOJO.EntityResponse?
}