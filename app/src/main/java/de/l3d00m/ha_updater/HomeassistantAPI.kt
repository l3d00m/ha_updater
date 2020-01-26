package de.l3d00m.ha_updater

import retrofit2.http.*


interface HomeassistantAPI {
    @Headers("Content-Type: application/json")
    @POST("api/states/{entity_id}")
    suspend fun updateEntity(
        @Header("Authorization") token: String,
        @Path("entity_id") entity_id: String,
        @Body entity: HomeassistantPOJO.Entity
    ): HomeassistantPOJO.EntityResponse?

    @Headers("Content-Type: application/json")
    @GET("api/")
    suspend fun getApiStatus(@Header("Authorization") token: String): HomeassistantPOJO.ApiResponse?
}